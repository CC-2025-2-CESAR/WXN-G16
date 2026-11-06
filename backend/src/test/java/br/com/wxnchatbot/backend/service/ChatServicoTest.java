package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.repository.ChatbotRepositorio;
import br.com.wxnchatbot.backend.repository.ClienteRepositorio;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;
import br.com.wxnchatbot.backend.repository.EmpresaRepositorio;

@ExtendWith(MockitoExtension.class)
class ChatServicoTest {

	private static final String EMPRESA_PADRAO = "Empresa Padrão";

	@Mock
	private ClienteRepositorio clientes;
	@Mock
	private EmpresaRepositorio empresas;
	@Mock
	private ChatbotRepositorio chatbots;
	@Mock
	private ConversaRepositorio conversas;
	@Mock
	private OrquestradorConversaServico orquestrador;
	@Mock
	private PlatformTransactionManager transacoes;

	private final Empresa empresa = new Empresa(EMPRESA_PADRAO);
	private final Chatbot lily = new Chatbot("Lily", "Oi!");

	private ChatServico servico(String empresaPadrao) {
		return new ChatServico(clientes, empresas, chatbots, conversas, orquestrador, transacoes, empresaPadrao);
	}

	@BeforeEach
	void preparar() {
		lenient().when(conversas.save(any(Conversa.class))).thenAnswer(chamada -> chamada.getArgument(0));
	}

	private Cliente cliente(Empresa deQuem, String telefone) {
		return new Cliente(deQuem, "Maria Souza", telefone);
	}

	@Test
	void clienteCadastradoSemConversaAbertaGanhaUmaConversaComALily() {
		Cliente maria = cliente(empresa, "81999990101");
		when(clientes.findByTelefoneIn(anyCollection())).thenReturn(List.of(maria));
		when(conversas.findFirstByClienteIdAndStatusNotOrderByIniciadaEmDesc(maria.getId(), StatusConversa.ENCERRADA))
				.thenReturn(Optional.empty());
		when(chatbots.findFirstByAtivoTrueOrderByIdAsc()).thenReturn(Optional.of(lily));
		when(orquestrador.processarTurno(any(), eq("Oi"))).thenReturn(new ResultadoTurno("Olá!", StatusConversa.COM_CHATBOT));

		RespostaChat resposta = servico(EMPRESA_PADRAO).receber("5581999990101", "Maria", "Oi");

		ArgumentCaptor<Conversa> nova = ArgumentCaptor.forClass(Conversa.class);
		verify(conversas).save(nova.capture());
		assertEquals(maria, nova.getValue().getCliente());
		assertEquals(lily, nova.getValue().getChatbot());
		assertEquals(nova.getValue().getId(), resposta.conversaId());
		assertEquals("Olá!", resposta.resposta());
		verify(clientes, never()).save(any());
	}

	@Test
	void conversaEmAbertoContinua() {
		Cliente maria = cliente(empresa, "81999990101");
		Conversa aberta = new Conversa(maria, lily);
		when(clientes.findByTelefoneIn(anyCollection())).thenReturn(List.of(maria));
		when(conversas.findFirstByClienteIdAndStatusNotOrderByIniciadaEmDesc(maria.getId(), StatusConversa.ENCERRADA))
				.thenReturn(Optional.of(aberta));
		when(orquestrador.processarTurno(aberta.getId(), "Oi")).thenReturn(new ResultadoTurno(null, StatusConversa.COM_ATENDENTE));

		RespostaChat resposta = servico("").receber("81999990101", null, "Oi");

		assertEquals(aberta.getId(), resposta.conversaId());
		assertEquals(StatusConversa.COM_ATENDENTE, resposta.status());
		verify(conversas, never()).save(any());
	}

	@Test
	void procuraOTelefoneEmTodasAsFormasQueOWhatsAppEOCadastroUsam() {
		when(clientes.findByTelefoneIn(anyCollection())).thenReturn(List.of());

		assertThrows(RecursoNaoEncontradoException.class, () -> servico("").receber("558199990101", null, "Oi"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<java.util.Collection<String>> procurados = ArgumentCaptor.forClass(java.util.Collection.class);
		verify(clientes).findByTelefoneIn(procurados.capture());
		assertTrue(procurados.getValue().containsAll(List.of("8199990101", "81999990101", "558199990101", "5581999990101")));
	}

	@Test
	void telefoneSemCadastroESemEmpresaPadraoNaoCriaNada() {
		when(clientes.findByTelefoneIn(anyCollection())).thenReturn(List.of());

		RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
				() -> servico("  ").receber("81999990101", "Maria", "Oi"));

		assertEquals("Não há cadastro para este telefone", erro.getMessage());
		verify(clientes, never()).save(any());
		verify(empresas, never()).save(any());
		verify(orquestrador, never()).processarTurno(any(), any());
	}

	@Test
	void telefoneNovoComEmpresaPadraoCadastraOClienteNaEmpresaExistente() {
		when(clientes.findByTelefoneIn(anyCollection())).thenReturn(List.of());
		when(empresas.findFirstByNomeOrderByIdAsc(EMPRESA_PADRAO)).thenReturn(Optional.of(empresa));
		when(clientes.save(any(Cliente.class))).thenAnswer(chamada -> chamada.getArgument(0));
		when(chatbots.findFirstByAtivoTrueOrderByIdAsc()).thenReturn(Optional.of(lily));
		when(orquestrador.processarTurno(any(), any())).thenReturn(new ResultadoTurno("Olá!", StatusConversa.COM_CHATBOT));

		servico(EMPRESA_PADRAO).receber("+55 (81) 99999-0101", "  Maria Souza ", "Oi");

		ArgumentCaptor<Cliente> novo = ArgumentCaptor.forClass(Cliente.class);
		verify(clientes).save(novo.capture());
		assertEquals("81999990101", novo.getValue().getTelefone());
		assertEquals("Maria Souza", novo.getValue().getNome());
		assertEquals(empresa, novo.getValue().getEmpresa());
		verify(empresas, never()).save(any());
	}

	@Test
	void empresaPadraoQueAindaNaoExisteECriadaECadastraOChatbotQuandoNaoHaNenhum() {
		when(clientes.findByTelefoneIn(anyCollection())).thenReturn(List.of());
		when(empresas.findFirstByNomeOrderByIdAsc(EMPRESA_PADRAO)).thenReturn(Optional.empty());
		when(empresas.save(any(Empresa.class))).thenAnswer(chamada -> chamada.getArgument(0));
		when(clientes.save(any(Cliente.class))).thenAnswer(chamada -> chamada.getArgument(0));
		when(chatbots.findFirstByAtivoTrueOrderByIdAsc()).thenReturn(Optional.empty());
		when(chatbots.save(any(Chatbot.class))).thenAnswer(chamada -> chamada.getArgument(0));
		when(orquestrador.processarTurno(any(), any())).thenReturn(new ResultadoTurno("Olá!", StatusConversa.COM_CHATBOT));

		servico(EMPRESA_PADRAO).receber("81999990101", null, "Oi");

		ArgumentCaptor<Empresa> criada = ArgumentCaptor.forClass(Empresa.class);
		verify(empresas).save(criada.capture());
		assertEquals(EMPRESA_PADRAO, criada.getValue().getNome());
		ArgumentCaptor<Chatbot> bot = ArgumentCaptor.forClass(Chatbot.class);
		verify(chatbots).save(bot.capture());
		assertEquals("Lily", bot.getValue().getNome());
		ArgumentCaptor<Cliente> novo = ArgumentCaptor.forClass(Cliente.class);
		verify(clientes).save(novo.capture());
		assertEquals("Cliente 0101", novo.getValue().getNome());
	}

	@Test
	void telefoneEmDuasEmpresasFicaComADaEmpresaPadrao() {
		Empresa outra = new Empresa("Outra Empresa");
		Cliente daOutra = cliente(outra, "81999990101");
		Cliente daPadrao = cliente(empresa, "81999990101");
		Conversa aberta = new Conversa(daPadrao, lily);
		when(clientes.findByTelefoneIn(anyCollection())).thenReturn(List.of(daOutra, daPadrao));
		when(conversas.findFirstByClienteIdAndStatusNotOrderByIniciadaEmDesc(daPadrao.getId(), StatusConversa.ENCERRADA))
				.thenReturn(Optional.of(aberta));
		when(orquestrador.processarTurno(any(), any())).thenReturn(new ResultadoTurno("Olá!", StatusConversa.COM_CHATBOT));

		RespostaChat resposta = servico(EMPRESA_PADRAO).receber("81999990101", null, "Oi");

		assertEquals(aberta.getId(), resposta.conversaId());
	}

	@Test
	void telefoneEmDuasEmpresasSemUmaPadraoCorrespondenteEAmbiguo() {
		Cliente umCliente = cliente(new Empresa("A"), "81999990101");
		Cliente outroCliente = cliente(new Empresa("B"), "81999990101");
		when(clientes.findByTelefoneIn(anyCollection())).thenReturn(List.of(umCliente, outroCliente));

		IllegalStateException erro = assertThrows(IllegalStateException.class,
				() -> servico(EMPRESA_PADRAO).receber("81999990101", null, "Oi"));

		assertEquals("O telefone está cadastrado em mais de uma empresa", erro.getMessage());
		verify(orquestrador, never()).processarTurno(any(UUID.class), any());
	}

	@Test
	void telefoneInvalidoParaCadastroLancaArgumentoInvalido() {
		when(clientes.findByTelefoneIn(anyCollection())).thenReturn(List.of());
		when(empresas.findFirstByNomeOrderByIdAsc(EMPRESA_PADRAO)).thenReturn(Optional.of(empresa));

		assertThrows(IllegalArgumentException.class, () -> servico(EMPRESA_PADRAO).receber("123", null, "Oi"));
	}
}
