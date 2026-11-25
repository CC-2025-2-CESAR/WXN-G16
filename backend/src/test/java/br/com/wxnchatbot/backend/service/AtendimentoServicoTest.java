package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.access.AccessDeniedException;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.Mensagem;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;
import br.com.wxnchatbot.backend.service.DetalheConversaFila.MensagemResumo;

@ExtendWith(MockitoExtension.class)
class AtendimentoServicoTest {

	private static final Empresa EMPRESA = new Empresa("Empresa Demo");

	@Mock
	private ConversaRepositorio conversas;

	@Mock
	private PlatformTransactionManager transacoes;

	@Mock
	private CanalDeSaida telegram;

	private AtendimentoServico servico;

	@BeforeEach
	void preparar() {
		lenient().when(telegram.origem()).thenReturn(OrigemConversa.TELEGRAM);
		servico = new AtendimentoServico(conversas, transacoes, List.of(telegram));
	}

	private static Conversa conversaComALily(OrigemConversa origem, String telefone) {
		Conversa conversa = new Conversa(new Cliente(EMPRESA, "Maria Souza", telefone), new Chatbot("Lily", "Oi, sou a Lily!"),
				origem);
		conversa.adicionarMensagem(conversa.getCliente(), "Quero falar com um atendente");
		conversa.adicionarMensagem(conversa.getChatbot(), "Já chamei alguém da equipe");
		return conversa;
	}

	private static Conversa naFila(OrigemConversa origem, String telefone) {
		Conversa conversa = conversaComALily(origem, telefone);
		conversa.solicitarAtendente("O cliente pediu um atendente");
		return conversa;
	}

	private static Conversa emAtendimento(OrigemConversa origem, String telefone) {
		Conversa conversa = naFila(origem, telefone);
		conversa.atribuirAtendente(new Atendente("Carlos Lima", "carlos@wxn.com.br"));
		return conversa;
	}

	private void achar(Conversa conversa) {
		when(conversas.buscarPorIdParaAtualizar(conversa.getId())).thenReturn(Optional.of(conversa));
	}

	private static Mensagem ultima(Conversa conversa) {
		return conversa.getMensagens().get(conversa.getMensagens().size() - 1);
	}

	@Test
	void respostaDoAtendenteEhGravadaEEntregueNoCanalDoCliente() {
		Conversa conversa = emAtendimento(OrigemConversa.TELEGRAM, "tg:1111111");
		achar(conversa);

		ResultadoDoEnvio resultado = servico.enviarMensagem(conversa.getId(), "Oi, sou o Carlos. Como posso ajudar?");

		assertTrue(resultado.entregue());
		assertNull(resultado.aviso());
		assertEquals(TipoRemetente.ATENDENTE, resultado.mensagem().tipoRemetente());
		assertEquals("Oi, sou o Carlos. Como posso ajudar?", resultado.mensagem().conteudo());
		assertEquals(TipoRemetente.ATENDENTE, ultima(conversa).getTipoRemetente());
		verify(conversas).save(conversa);
		verify(telegram).entregar("tg:1111111", "Oi, sou o Carlos. Como posso ajudar?");
	}

	@Test
	void outroAtendenteNaoPodeResponderNemLerOHistoricoDoResponsavel() {
		Conversa conversa = emAtendimento(OrigemConversa.TELEGRAM, "tg:1111111");
		achar(conversa);
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));
		UUID outroAtendente = UUID.randomUUID();

		assertThrows(AccessDeniedException.class,
				() -> servico.enviarMensagem(conversa.getId(), "Mensagem indevida", outroAtendente));
		assertThrows(AccessDeniedException.class,
				() -> servico.listarMensagens(conversa.getId(), outroAtendente));
		verify(conversas, never()).save(any());
		verify(telegram, never()).entregar(any(), any());
	}

	@Test
	void seOCanalNaoEntregarAMensagemContinuaNoHistoricoEOAtendenteERecebeOAviso() {
		Conversa conversa = emAtendimento(OrigemConversa.TELEGRAM, "tg:1111111");
		achar(conversa);
		doThrow(new FalhaNaEntregaException("O cliente bloqueou o bot")).when(telegram).entregar(any(), any());

		ResultadoDoEnvio resultado = servico.enviarMensagem(conversa.getId(), "Ainda está aí?");

		assertFalse(resultado.entregue());
		assertEquals("O cliente bloqueou o bot", resultado.aviso());
		assertEquals("Ainda está aí?", resultado.mensagem().conteudo());
		assertEquals("Ainda está aí?", ultima(conversa).getConteudo());
		verify(conversas).save(conversa);
	}

	@Test
	void falhaInesperadaDoCanalViraAvisoGenericoSemPerderAMensagem() {
		Conversa conversa = emAtendimento(OrigemConversa.TELEGRAM, "tg:1111111");
		achar(conversa);
		doThrow(new IllegalStateException("bug no canal")).when(telegram).entregar(any(), any());

		ResultadoDoEnvio resultado = servico.enviarMensagem(conversa.getId(), "Oi");

		assertFalse(resultado.entregue());
		assertTrue(resultado.aviso().contains("não foi possível entregá-la"));
		assertFalse(resultado.aviso().contains("bug no canal"));
		assertEquals("Oi", ultima(conversa).getConteudo());
	}

	@Test
	void conversaDeUmaOrigemSemCanalGuardaNoHistoricoEExplicaPorQueNaoSaiu() {
		for (OrigemConversa origem : List.of(OrigemConversa.SIMULADOR, OrigemConversa.DEMONSTRACAO, OrigemConversa.WHATSAPP)) {
			Conversa conversa = emAtendimento(origem, "81999990001");
			achar(conversa);

			ResultadoDoEnvio resultado = servico.enviarMensagem(conversa.getId(), "Olá");

			assertFalse(resultado.entregue(), origem.name());
			assertFalse(resultado.aviso().isBlank(), origem.name());
			assertEquals("Olá", ultima(conversa).getConteudo(), origem.name());
		}
		verify(telegram, never()).entregar(any(), any());
	}

	@Test
	void semOCanalDoTelegramLigadoAConversaDoTelegramSoGuardaNoHistorico() {
		AtendimentoServico semCanais = new AtendimentoServico(conversas, transacoes, List.of());
		Conversa conversa = emAtendimento(OrigemConversa.TELEGRAM, "tg:1111111");
		achar(conversa);

		ResultadoDoEnvio resultado = semCanais.enviarMensagem(conversa.getId(), "Olá");

		assertFalse(resultado.entregue());
		assertTrue(resultado.aviso().contains("Telegram"));
		assertEquals("Olá", ultima(conversa).getConteudo());
	}

	@Test
	void soAceitaMensagemDeConversaEmAtendimento() {
		Conversa comALily = conversaComALily(OrigemConversa.TELEGRAM, "tg:1111111");
		Conversa naFila = naFila(OrigemConversa.TELEGRAM, "tg:2222222");
		Conversa encerrada = emAtendimento(OrigemConversa.TELEGRAM, "tg:3333333");
		encerrada.encerrar();

		for (Conversa conversa : List.of(comALily, naFila, encerrada)) {
			achar(conversa);
			int antes = conversa.getMensagens().size();

			assertThrows(IllegalStateException.class, () -> servico.enviarMensagem(conversa.getId(), "Oi"),
					conversa.getStatus().name());

			assertEquals(antes, conversa.getMensagens().size(), conversa.getStatus().name());
		}
		verify(conversas, never()).save(any());
		verifyNoInteractions(telegram);
		assertEquals(StatusConversa.ENCERRADA, encerrada.getStatus());
	}

	@Test
	void conversaInexistenteNaoEhEncontrada() {
		UUID id = UUID.randomUUID();
		when(conversas.buscarPorIdParaAtualizar(id)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> servico.enviarMensagem(id, "Oi"));

		verifyNoInteractions(telegram);
	}

	@Test
	void textoEmBrancoNaoEhGravadoNemEntregue() {
		Conversa conversa = emAtendimento(OrigemConversa.TELEGRAM, "tg:1111111");
		achar(conversa);
		int antes = conversa.getMensagens().size();

		assertThrows(IllegalArgumentException.class, () -> servico.enviarMensagem(conversa.getId(), "   "));

		assertEquals(antes, conversa.getMensagens().size());
		verify(conversas, never()).save(any());
		verifyNoInteractions(telegram);
	}

	@Test
	void historicoVemEmOrdemComQuemEscreveuECarimboDeHora() {
		Conversa conversa = emAtendimento(OrigemConversa.TELEGRAM, "tg:1111111");
		conversa.adicionarMensagem(conversa.getAtendente().orElseThrow(), "Pode me passar o protocolo?");
		conversa.adicionarMensagem(conversa.getCliente(), "WXN-2026-0001");
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));

		List<MensagemResumo> historico = servico.listarMensagens(conversa.getId());

		assertEquals(List.of(TipoRemetente.CLIENTE, TipoRemetente.CHATBOT, TipoRemetente.ATENDENTE, TipoRemetente.CLIENTE),
				historico.stream().map(MensagemResumo::tipoRemetente).toList());
		assertEquals("WXN-2026-0001", historico.get(3).conteudo());
		assertTrue(historico.stream().allMatch(m -> m.enviadaEm() != null));
	}

	@Test
	void historicoDeConversaInexistenteNaoEhEncontrado() {
		UUID id = UUID.randomUUID();
		when(conversas.findById(id)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> servico.listarMensagens(id));
	}
}
