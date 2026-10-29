package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.llm.LlmClient;
import br.com.wxnchatbot.backend.llm.LlmIndisponivelException;
import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.llm.MensagemLlm.Papel;
import br.com.wxnchatbot.backend.llm.PromptDaLily;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;

@ExtendWith(MockitoExtension.class)
class OrquestradorConversaServicoTest {

	private static final Empresa EMPRESA = new Empresa("Empresa Demo", "12345678000199");

	@Mock
	private ConversaRepositorio conversas;

	@Mock
	private LlmClient llm;

	@Captor
	private ArgumentCaptor<List<MensagemLlm>> historico;

	private OrquestradorConversaServico orquestrador;

	@BeforeEach
	void preparar() {
		orquestrador = new OrquestradorConversaServico(conversas, llm);
	}

	private Conversa conversaComChatbot() {
		Conversa conversa = new Conversa(new Chatbot("Lily", "Oi, sou a Lily!"));
		conversa.vincularCliente(new Cliente(EMPRESA, "Maria Souza", "81999990001"));
		return conversa;
	}

	@Test
	void conversaComALilyGuardaAPerguntaEARespostaEDevolveOTexto() {
		Conversa conversa = conversaComChatbot();
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));
		when(llm.responder(any(), any())).thenReturn("Já verifico para você");

		ResultadoTurno resultado = orquestrador.processarTurno(conversa.getId(), "Quero saber do chamado");

		assertEquals("Já verifico para você", resultado.resposta());
		assertEquals(StatusConversa.COM_CHATBOT, resultado.status());
		assertEquals(2, conversa.getMensagens().size());
		assertEquals(TipoRemetente.CLIENTE, conversa.getMensagens().get(0).getTipoRemetente());
		assertEquals(TipoRemetente.CHATBOT, conversa.getMensagens().get(1).getTipoRemetente());
		verify(conversas).save(conversa);
	}

	@Test
	void mandaOPromptDaLilyEOHistoricoNoFormatoDaLlm() {
		Conversa conversa = conversaComChatbot();
		conversa.adicionarMensagem(conversa.getChatbot(), "Oi, sou a Lily!");
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));
		when(llm.responder(any(), any())).thenReturn("ok");

		orquestrador.processarTurno(conversa.getId(), "Preciso de ajuda");

		verify(llm).responder(eq(PromptDaLily.TEXTO), historico.capture());
		assertEquals(List.of(
				new MensagemLlm(Papel.ASSISTENTE, "Oi, sou a Lily!"),
				new MensagemLlm(Papel.USUARIO, "Preciso de ajuda")), historico.getValue());
	}

	@Test
	void historicoEnviadoNaoPassaDeVinteMensagens() {
		Conversa conversa = conversaComChatbot();
		for (int i = 1; i <= 25; i++) {
			conversa.adicionarMensagem(conversa.getCliente().orElseThrow(), "Mensagem " + i);
		}
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));
		when(llm.responder(any(), any())).thenReturn("ok");

		orquestrador.processarTurno(conversa.getId(), "Última");

		verify(llm).responder(eq(PromptDaLily.TEXTO), historico.capture());
		assertEquals(20, historico.getValue().size());
		assertEquals("Última", historico.getValue().get(19).conteudo());
	}

	@Test
	void conversaNaFilaGuardaSoAPerguntaENaoChamaALlm() {
		Conversa conversa = conversaComChatbot();
		conversa.adicionarMensagem(conversa.getCliente().orElseThrow(), "Quero falar com uma pessoa");
		conversa.solicitarAtendente("Pediu atendimento humano");
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));

		ResultadoTurno resultado = orquestrador.processarTurno(conversa.getId(), "Alguém pode me atender?");

		assertNull(resultado.resposta());
		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, resultado.status());
		assertEquals(2, conversa.getMensagens().size());
		verifyNoInteractions(llm);
		verify(conversas).save(conversa);
	}

	@Test
	void conversaInexistenteDaNaoEncontrado() {
		UUID inexistente = UUID.randomUUID();
		when(conversas.findById(inexistente)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> orquestrador.processarTurno(inexistente, "Oi"));
		verifyNoInteractions(llm);
		verify(conversas, never()).save(any());
	}

	@Test
	void conversaEncerradaNaoAceitaMensagemENaoGrava() {
		Conversa conversa = conversaComChatbot();
		conversa.encerrar();
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));

		assertThrows(IllegalStateException.class, () -> orquestrador.processarTurno(conversa.getId(), "Oi de novo"));
		verifyNoInteractions(llm);
		verify(conversas, never()).save(any());
	}

    @Test
	void llmForaDoArRespondeMensagemPadraoEGuardaNaConversa() {
		Conversa conversa = conversaComChatbot();
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));
		when(llm.responder(any(), any())).thenThrow(new LlmIndisponivelException("fora do ar", null));

		ResultadoTurno resultado = orquestrador.processarTurno(conversa.getId(), "Oi");

		assertNotNull(resultado.resposta());
		assertEquals(StatusConversa.COM_CHATBOT, resultado.status());
		assertEquals(2, conversa.getMensagens().size());
		verify(conversas).save(conversa);
	}

	@Test
	void erroInesperadoDaLlmNaoEEscondido() {
		Conversa conversa = conversaComChatbot();
		when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));
		when(llm.responder(any(), any())).thenThrow(new IllegalStateException("bug"));

		assertThrows(IllegalStateException.class, () -> orquestrador.processarTurno(conversa.getId(), "Oi"));
		verify(conversas, never()).save(any());
	}
}