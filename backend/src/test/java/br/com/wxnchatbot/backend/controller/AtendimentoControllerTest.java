package br.com.wxnchatbot.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.autenticacao.AtendenteAutenticado;
import br.com.wxnchatbot.backend.service.AtendimentoServico;
import br.com.wxnchatbot.backend.service.DetalheConversaFila.MensagemResumo;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;
import br.com.wxnchatbot.backend.service.ResultadoDoEnvio;

@WebMvcTest(AtendimentoController.class)
class AtendimentoControllerTest {

	private static final UUID CONVERSA = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ATENDENTE = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final Instant AGORA = Instant.parse("2026-11-19T12:00:00Z");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private AtendimentoServico servico;
	@MockitoBean
	private AtendenteAutenticado autenticado;

	@BeforeEach
	void prepararIdentidade() {
		lenient().when(autenticado.id()).thenReturn(ATENDENTE);
	}

	private ResultActions enviar(String json) throws Exception {
		return mvc.perform(post("/api/conversas/{id}/atendimento/mensagens", CONVERSA).contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	@Test
	void historicoSaiEmOrdemComTipoConteudoEHora() throws Exception {
		when(servico.listarMensagens(CONVERSA, ATENDENTE)).thenReturn(List.of(
				new MensagemResumo(TipoRemetente.CLIENTE, "Preciso de ajuda", AGORA),
				new MensagemResumo(TipoRemetente.ATENDENTE, "Já vou ver", AGORA.plusSeconds(30))));

		mvc.perform(get("/api/conversas/{id}/atendimento/mensagens", CONVERSA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].tipoRemetente").value("CLIENTE"))
				.andExpect(jsonPath("$[0].conteudo").value("Preciso de ajuda"))
				.andExpect(jsonPath("$[0].enviadaEm").value("2026-11-19T12:00:00Z"))
				.andExpect(jsonPath("$[1].tipoRemetente").value("ATENDENTE"));
	}

	@Test
	void historicoDeConversaInexistenteDevolve404() throws Exception {
		when(servico.listarMensagens(CONVERSA, ATENDENTE)).thenThrow(new RecursoNaoEncontradoException("Conversa", CONVERSA));

		mvc.perform(get("/api/conversas/{id}/atendimento/mensagens", CONVERSA)).andExpect(status().isNotFound());
	}

	@Test
	void idQueNaoEhUuidDevolve400() throws Exception {
		mvc.perform(get("/api/conversas/{id}/atendimento/mensagens", "abc")).andExpect(status().isBadRequest());

		verifyNoInteractions(servico);
	}

	@Test
	void envioEntregueDevolveAMensagemEOAvisoNulo() throws Exception {
		when(servico.enviarMensagem(CONVERSA, "Oi, sou o Carlos", ATENDENTE)).thenReturn(
				new ResultadoDoEnvio(new MensagemResumo(TipoRemetente.ATENDENTE, "Oi, sou o Carlos", AGORA), true, null));

		enviar("{\"texto\":\"Oi, sou o Carlos\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entregue").value(true))
				.andExpect(jsonPath("$.aviso").doesNotExist())
				.andExpect(jsonPath("$.mensagem.tipoRemetente").value("ATENDENTE"))
				.andExpect(jsonPath("$.mensagem.conteudo").value("Oi, sou o Carlos"))
				.andExpect(jsonPath("$.mensagem.enviadaEm").value("2026-11-19T12:00:00Z"));
	}

	@Test
	void envioNaoEntregueDevolve200ComOAviso() throws Exception {
		when(servico.enviarMensagem(CONVERSA, "Oi", ATENDENTE)).thenReturn(new ResultadoDoEnvio(
				new MensagemResumo(TipoRemetente.ATENDENTE, "Oi", AGORA), false, "O cliente bloqueou o bot"));

		enviar("{\"texto\":\"Oi\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entregue").value(false))
				.andExpect(jsonPath("$.aviso").value("O cliente bloqueou o bot"))
				.andExpect(jsonPath("$.mensagem.conteudo").value("Oi"));
	}

	@Test
	void textoVazioEmBrancoAusenteOuGrandeDemaisDevolve400SemChamarOServico() throws Exception {
		enviar("{\"texto\":\"\"}").andExpect(status().isBadRequest());
		enviar("{\"texto\":\"   \"}").andExpect(status().isBadRequest());
		enviar("{}").andExpect(status().isBadRequest());
		enviar("{\"texto\":\"" + "a".repeat(4001) + "\"}").andExpect(status().isBadRequest());
		enviar("isto nao e json").andExpect(status().isBadRequest());

		verifyNoInteractions(servico);
	}

	@Test
	void textoDeExatamente4000CaracteresEhAceito() throws Exception {
		String texto = "a".repeat(4000);
		when(servico.enviarMensagem(CONVERSA, texto, ATENDENTE)).thenReturn(
				new ResultadoDoEnvio(new MensagemResumo(TipoRemetente.ATENDENTE, texto, AGORA), true, null));

		enviar("{\"texto\":\"" + texto + "\"}").andExpect(status().isOk());

		verify(servico).enviarMensagem(CONVERSA, texto, ATENDENTE);
	}

	@Test
	void conversaForaDoAtendimentoDevolve409ComOMotivo() throws Exception {
		when(servico.enviarMensagem(CONVERSA, "Oi", ATENDENTE)).thenThrow(new IllegalStateException("A conversa já foi encerrada"));

		enviar("{\"texto\":\"Oi\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("A conversa já foi encerrada"));
	}

	@Test
	void envioParaConversaInexistenteDevolve404() throws Exception {
		when(servico.enviarMensagem(CONVERSA, "Oi", ATENDENTE)).thenThrow(new RecursoNaoEncontradoException("Conversa", CONVERSA));

		enviar("{\"texto\":\"Oi\"}").andExpect(status().isNotFound());
	}
}
