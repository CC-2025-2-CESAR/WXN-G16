package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.StatusTicket;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.repository.AtendenteRepositorio;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;
import br.com.wxnchatbot.backend.model.Empresa;

@ExtendWith(MockitoExtension.class)
class FilaConversasServicoTest {
	
	private static final Empresa EMPRESA = new Empresa("Empresa Demo", "12345678000199");

	@Mock
	private ConversaRepositorio conversas;

	@Mock
	private AtendenteRepositorio atendentes;

	private FilaConversasServico servico;

	@BeforeEach
	void preparar() {
		servico = new FilaConversasServico(conversas, atendentes);
	}

	private Conversa conversaComChatbot(String nome, String telefone) {
		Conversa conversa = new Conversa(new Chatbot("Lily", "Oi, sou a Lily!"));
		conversa.vincularCliente(new Cliente(EMPRESA, nome, telefone));
		return conversa;
	}

	private Conversa conversaNaFila(String nome, String telefone, String motivo) {
		Conversa conversa = conversaComChatbot(nome, telefone);
		conversa.adicionarMensagem(conversa.getCliente().orElseThrow(), "Preciso de ajuda");
		conversa.solicitarAtendente(motivo);
		return conversa;
	}

	private Atendente atendente() {
		return new Atendente("Carlos Lima", "carlos@wxn.com.br");
	}

	@Test
	void visualizarFilaNumeraAPartirDeUmNaOrdemDevolvidaPeloRepositorio() {
		Conversa primeira = conversaNaFila("Maria Souza", "81999990001", "Quer falar com uma pessoa");
		Conversa segunda = conversaNaFila("Joao Pedro", "81999990002", "Problema com a nota fiscal");
		when(conversas.buscarFilaAguardando()).thenReturn(List.of(primeira, segunda));

		List<ConversaNaFila> fila = servico.visualizarFila();

		assertEquals(2, fila.size());
		assertEquals(1, fila.get(0).posicao());
		assertEquals(primeira.getId(), fila.get(0).conversaId());
		assertEquals("Maria Souza", fila.get(0).clienteNome());
		assertEquals("81999990001", fila.get(0).clienteTelefone());
		assertEquals("Quer falar com uma pessoa", fila.get(0).motivo());
		assertEquals(primeira.getTicket().orElseThrow().getAbertoEm(), fila.get(0).abertoEm());
		assertEquals(2, fila.get(1).posicao());
		assertEquals(segunda.getId(), fila.get(1).conversaId());
	}

	@Test
	void visualizarFilaVaziaDevolveListaVazia() {
		when(conversas.buscarFilaAguardando()).thenReturn(List.of());

		assertTrue(servico.visualizarFila().isEmpty());
	}

	@Test
	void abrirConversaDaFilaMostraClienteMotivoEMensagensNaOrdem() {
		Conversa conversa = conversaComChatbot("Maria Souza", "81999990001");
		conversa.getChatbot().saudar(conversa);
		conversa.adicionarMensagem(conversa.getCliente().orElseThrow(), "Preciso de ajuda");
		conversa.solicitarAtendente("Quer falar com uma pessoa");
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));

		DetalheConversaFila detalhe = servico.abrirConversaDaFila(conversa.getId());

		assertEquals(conversa.getId(), detalhe.conversaId());
		assertEquals("Maria Souza", detalhe.clienteNome());
		assertEquals("81999990001", detalhe.clienteTelefone());
		assertEquals("Quer falar com uma pessoa", detalhe.motivo());
		assertEquals(2, detalhe.mensagens().size());
		assertEquals(TipoRemetente.CHATBOT, detalhe.mensagens().get(0).tipoRemetente());
		assertEquals("Oi, sou a Lily!", detalhe.mensagens().get(0).conteudo());
		assertEquals(TipoRemetente.CLIENTE, detalhe.mensagens().get(1).tipoRemetente());
		assertEquals("Preciso de ajuda", detalhe.mensagens().get(1).conteudo());
	}

	@Test
	void abrirConversaQueNaoEstaNaFilaDaErro() {
		Conversa comChatbot = conversaComChatbot("Maria Souza", "81999990001");
		Conversa emAtendimento = conversaNaFila("Joao Pedro", "81999990002", "Quer uma pessoa");
		emAtendimento.atribuirAtendente(atendente());
		Conversa encerrada = conversaComChatbot("Ana Lima", "81999990003");
		encerrada.encerrar();
		when(conversas.findById(comChatbot.getId())).thenReturn(Optional.of(comChatbot));
		when(conversas.findById(emAtendimento.getId())).thenReturn(Optional.of(emAtendimento));
		when(conversas.findById(encerrada.getId())).thenReturn(Optional.of(encerrada));

		assertThrows(IllegalStateException.class, () -> servico.abrirConversaDaFila(comChatbot.getId()));
		assertThrows(IllegalStateException.class, () -> servico.abrirConversaDaFila(emAtendimento.getId()));
		assertThrows(IllegalStateException.class, () -> servico.abrirConversaDaFila(encerrada.getId()));
	}

	@Test
	void abrirConversaInexistenteDaNaoEncontrado() {
		UUID id = UUID.randomUUID();
		when(conversas.findById(id)).thenReturn(Optional.empty());

		RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
				() -> servico.abrirConversaDaFila(id));

		assertTrue(erro.getMessage().contains(id.toString()));
	}

	@Test
	void solicitarAtendenteRegistraOPedidoSalvaEDevolveAPosicaoNaFila() {
		Conversa outra = conversaNaFila("Joao Pedro", "81999990002", "Problema com a nota fiscal");
		Conversa conversa = conversaComChatbot("Maria Souza", "81999990001");
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));
		when(conversas.buscarFilaAguardando()).thenReturn(List.of(outra, conversa));

		ConversaNaFila resultado = servico.solicitarAtendente(conversa.getId(), "Quer falar com uma pessoa");

		assertEquals(2, resultado.posicao());
		assertEquals(conversa.getId(), resultado.conversaId());
		assertEquals("Quer falar com uma pessoa", resultado.motivo());
		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, conversa.getStatus());
		verify(conversas).save(conversa);
	}

	@Test
	void solicitarAtendenteDeConversaQueNaoApareceNaFilaIndicaInconsistencia() {
		Conversa conversa = conversaComChatbot("Maria Souza", "81999990001");
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));
		when(conversas.buscarFilaAguardando()).thenReturn(List.of());

		assertThrows(IllegalStateException.class, () -> servico.solicitarAtendente(conversa.getId(), "Preciso de ajuda"));
	}

	@Test
	void solicitarAtendenteDuasVezesDaErroENaoSalva() {
		Conversa conversa = conversaNaFila("Maria Souza", "81999990001", "Primeiro pedido");
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));

		assertThrows(IllegalStateException.class, () -> servico.solicitarAtendente(conversa.getId(), "Segundo pedido"));

		verify(conversas, never()).save(any());
		assertEquals("Primeiro pedido", conversa.getTicket().orElseThrow().getMotivo());
	}

	@Test
	void solicitarAtendenteSemMotivoDaErroENaoMudaAConversa() {
		Conversa conversa = conversaComChatbot("Maria Souza", "81999990001");
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));

		assertThrows(IllegalArgumentException.class, () -> servico.solicitarAtendente(conversa.getId(), "  "));

		assertEquals(StatusConversa.COM_CHATBOT, conversa.getStatus());
		assertTrue(conversa.getTicket().isEmpty());
		verify(conversas, never()).save(any());
	}

	@Test
	void solicitarAtendenteDeConversaInexistenteDaNaoEncontrado() {
		UUID id = UUID.randomUUID();
		when(conversas.findById(id)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> servico.solicitarAtendente(id, "Preciso de ajuda"));
	}

	@Test
	void atribuirAtendenteMudaOStatusIniciaOTicketESalva() {
		Conversa conversa = conversaNaFila("Maria Souza", "81999990001", "Quer falar com uma pessoa");
		Atendente carlos = atendente();
		when(conversas.buscarPorIdParaAtualizar(conversa.getId())).thenReturn(Optional.of(conversa));
		when(atendentes.findById(carlos.getId())).thenReturn(Optional.of(carlos));

		servico.atribuirAtendente(conversa.getId(), carlos.getId());

		assertEquals(StatusConversa.COM_ATENDENTE, conversa.getStatus());
		assertSame(carlos, conversa.getAtendente().orElseThrow());
		assertEquals(StatusTicket.EM_ATENDIMENTO, conversa.getTicket().orElseThrow().getStatus());
		verify(conversas).save(conversa);
	}

	@Test
	void atribuirAtendenteBuscaAConversaComBloqueioParaNaoAssumirEmDobro() {
		Conversa conversa = conversaNaFila("Maria Souza", "81999990001", "Quer falar com uma pessoa");
		Atendente carlos = atendente();
		when(conversas.buscarPorIdParaAtualizar(conversa.getId())).thenReturn(Optional.of(conversa));
		when(atendentes.findById(carlos.getId())).thenReturn(Optional.of(carlos));

		servico.atribuirAtendente(conversa.getId(), carlos.getId());

		verify(conversas, never()).findById(any());
	}

	@Test
	void atribuirConversaInexistenteDaNaoEncontradoSemConsultarOAtendente() {
		UUID conversaId = UUID.randomUUID();
		when(conversas.buscarPorIdParaAtualizar(conversaId)).thenReturn(Optional.empty());

		RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
				() -> servico.atribuirAtendente(conversaId, UUID.randomUUID()));

		assertTrue(erro.getMessage().contains("Conversa"));
		verifyNoInteractions(atendentes);
	}

	@Test
	void atribuirAtendenteInexistenteDaNaoEncontradoENaoMudaAConversa() {
		Conversa conversa = conversaNaFila("Maria Souza", "81999990001", "Quer falar com uma pessoa");
		UUID atendenteId = UUID.randomUUID();
		when(conversas.buscarPorIdParaAtualizar(conversa.getId())).thenReturn(Optional.of(conversa));
		when(atendentes.findById(atendenteId)).thenReturn(Optional.empty());

		RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
				() -> servico.atribuirAtendente(conversa.getId(), atendenteId));

		assertTrue(erro.getMessage().contains("Atendente"));
		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, conversa.getStatus());
		verify(conversas, never()).save(any());
	}

	@Test
	void atendenteIndisponivelNaoAssumeAConversa() {
		Conversa conversa = conversaNaFila("Maria Souza", "81999990001", "Quer falar com uma pessoa");
		Atendente carlos = atendente();
		carlos.marcarIndisponivel();
		when(conversas.buscarPorIdParaAtualizar(conversa.getId())).thenReturn(Optional.of(conversa));
		when(atendentes.findById(carlos.getId())).thenReturn(Optional.of(carlos));

		assertThrows(IllegalStateException.class, () -> servico.atribuirAtendente(conversa.getId(), carlos.getId()));

		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, conversa.getStatus());
		assertTrue(conversa.getAtendente().isEmpty());
		verify(conversas, never()).save(any());
	}

	@Test
	void naoSeAtribuiAtendenteAConversaQueNaoPediuAtendente() {
		Conversa conversa = conversaComChatbot("Maria Souza", "81999990001");
		Atendente carlos = atendente();
		when(conversas.buscarPorIdParaAtualizar(conversa.getId())).thenReturn(Optional.of(conversa));
		when(atendentes.findById(carlos.getId())).thenReturn(Optional.of(carlos));

		assertThrows(IllegalStateException.class, () -> servico.atribuirAtendente(conversa.getId(), carlos.getId()));

		assertEquals(StatusConversa.COM_CHATBOT, conversa.getStatus());
	}

	@Test
	void segundoAtendenteNaoTomaAConversaDoPrimeiro() {
		Conversa conversa = conversaNaFila("Maria Souza", "81999990001", "Quer falar com uma pessoa");
		Atendente carlos = atendente();
		Atendente ana = new Atendente("Ana Costa", "ana@wxn.com.br");
		when(conversas.buscarPorIdParaAtualizar(conversa.getId())).thenReturn(Optional.of(conversa));
		when(atendentes.findById(carlos.getId())).thenReturn(Optional.of(carlos));
		when(atendentes.findById(ana.getId())).thenReturn(Optional.of(ana));
		servico.atribuirAtendente(conversa.getId(), carlos.getId());

		assertThrows(IllegalStateException.class, () -> servico.atribuirAtendente(conversa.getId(), ana.getId()));

		assertSame(carlos, conversa.getAtendente().orElseThrow());
	}

	@Test
	void encerrarMarcaComoEncerradaResolveOTicketESalva() {
		Conversa conversa = conversaNaFila("Maria Souza", "81999990001", "Quer falar com uma pessoa");
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));

		servico.encerrar(conversa.getId());

		assertEquals(StatusConversa.ENCERRADA, conversa.getStatus());
		assertEquals(StatusTicket.RESOLVIDO, conversa.getTicket().orElseThrow().getStatus());
		assertTrue(conversa.getEncerradaEm().isPresent());
		verify(conversas).save(conversa);
	}

	@Test
	void encerrarConversaSemTicketTambemFunciona() {
		Conversa conversa = conversaComChatbot("Maria Souza", "81999990001");
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));

		servico.encerrar(conversa.getId());

		assertEquals(StatusConversa.ENCERRADA, conversa.getStatus());
		assertTrue(conversa.getTicket().isEmpty());
	}

	@Test
	void encerrarConversaJaEncerradaDaErroENaoSalvaDeNovo() {
		Conversa conversa = conversaComChatbot("Maria Souza", "81999990001");
		conversa.encerrar();
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));

		assertThrows(IllegalStateException.class, () -> servico.encerrar(conversa.getId()));

		verify(conversas, never()).save(any());
	}

	@Test
	void encerrarConversaInexistenteDaNaoEncontrado() {
		UUID id = UUID.randomUUID();
		when(conversas.findById(id)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> servico.encerrar(id));
	}
}