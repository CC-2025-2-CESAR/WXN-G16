package br.com.wxnchatbot.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.service.ConversaNaFila;
import br.com.wxnchatbot.backend.service.DetalheConversaFila;
import br.com.wxnchatbot.backend.service.DetalheConversaFila.MensagemResumo;
import br.com.wxnchatbot.backend.service.FilaConversasServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;

@WebMvcTest(FilaConversasController.class)
class FilaConversasControllerTest {

	private static final UUID CONVERSA = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OUTRA_CONVERSA = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID ATENDENTE = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final Instant ABERTO_EM = Instant.parse("2026-10-29T14:00:00Z");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private FilaConversasServico servico;

	private ResultActions assumir(String conversaId, String json) throws Exception {
		return mvc.perform(post("/api/fila/{id}/assumir", conversaId).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions assumir(UUID conversaId, UUID atendenteId) throws Exception {
		return assumir(conversaId.toString(), "{\"atendenteId\":\"" + atendenteId + "\"}");
	}

	@Test
	void filaDevolve200ComOsItensNaOrdemDoServico() throws Exception {
		when(servico.visualizarFila()).thenReturn(List.of(
				new ConversaNaFila(1, CONVERSA, "Maria Souza", "81999990001", "Quer falar com uma pessoa", ABERTO_EM),
				new ConversaNaFila(2, OUTRA_CONVERSA, "Joao Pedro", "81999990002", "Nota fiscal",
						ABERTO_EM.plusSeconds(60))));

		mvc.perform(get("/api/fila"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].*", hasSize(6)))
				.andExpect(jsonPath("$[0].posicao").value(1))
				.andExpect(jsonPath("$[0].conversaId").value(CONVERSA.toString()))
				.andExpect(jsonPath("$[0].clienteNome").value("Maria Souza"))
				.andExpect(jsonPath("$[0].clienteTelefone").value("81999990001"))
				.andExpect(jsonPath("$[0].motivo").value("Quer falar com uma pessoa"))
				.andExpect(jsonPath("$[0].abertoEm").value("2026-10-29T14:00:00Z"))
				.andExpect(jsonPath("$[1].posicao").value(2))
				.andExpect(jsonPath("$[1].conversaId").value(OUTRA_CONVERSA.toString()));
	}

	@Test
	void filaVaziaDevolve200ComListaVazia() throws Exception {
		when(servico.visualizarFila()).thenReturn(List.of());

		mvc.perform(get("/api/fila"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void detalheDevolve200ComAsMensagensEmOrdem() throws Exception {
		when(servico.abrirConversaDaFila(CONVERSA)).thenReturn(new DetalheConversaFila(CONVERSA, "Maria Souza",
				"81999990001", "Quer falar com uma pessoa", ABERTO_EM,
				List.of(new MensagemResumo(TipoRemetente.CHATBOT, "Oi, sou a Lily!", ABERTO_EM.minusSeconds(60)),
						new MensagemResumo(TipoRemetente.CLIENTE, "Preciso de ajuda", ABERTO_EM.minusSeconds(30)))));

		mvc.perform(get("/api/fila/{id}", CONVERSA))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.*", hasSize(6)))
				.andExpect(jsonPath("$.conversaId").value(CONVERSA.toString()))
				.andExpect(jsonPath("$.clienteNome").value("Maria Souza"))
				.andExpect(jsonPath("$.clienteTelefone").value("81999990001"))
				.andExpect(jsonPath("$.motivo").value("Quer falar com uma pessoa"))
				.andExpect(jsonPath("$.abertoEm").value("2026-10-29T14:00:00Z"))
				.andExpect(jsonPath("$.mensagens", hasSize(2)))
				.andExpect(jsonPath("$.mensagens[0].*", hasSize(3)))
				.andExpect(jsonPath("$.mensagens[0].tipoRemetente").value("CHATBOT"))
				.andExpect(jsonPath("$.mensagens[0].conteudo").value("Oi, sou a Lily!"))
				.andExpect(jsonPath("$.mensagens[0].enviadaEm").value("2026-10-29T13:59:00Z"))
				.andExpect(jsonPath("$.mensagens[1].tipoRemetente").value("CLIENTE"));
	}

	@Test
	void detalheDeConversaInexistenteDevolve404() throws Exception {
		when(servico.abrirConversaDaFila(CONVERSA)).thenThrow(new RecursoNaoEncontradoException("Conversa", CONVERSA));

		mvc.perform(get("/api/fila/{id}", CONVERSA))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void detalheDeConversaForaDaFilaDevolve409() throws Exception {
		when(servico.abrirConversaDaFila(CONVERSA))
				.thenThrow(new IllegalStateException("A conversa não está na fila de atendimento"));

		mvc.perform(get("/api/fila/{id}", CONVERSA))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("A conversa não está na fila de atendimento"));
	}

	@Test
	void detalheComIdentificadorMalformadoDevolve400() throws Exception {
		mvc.perform(get("/api/fila/{id}", "nao-e-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(containsString("conversaId")));

		verifyNoInteractions(servico);
	}

	@Test
	void assumirDevolve204SemCorpoEChamaOServico() throws Exception {
		assumir(CONVERSA, ATENDENTE)
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(servico).atribuirAtendente(CONVERSA, ATENDENTE);
	}

	@Test
	void assumirConversaInexistenteDevolve404() throws Exception {
		doThrow(new RecursoNaoEncontradoException("Conversa", CONVERSA)).when(servico).atribuirAtendente(CONVERSA,
				ATENDENTE);

		assumir(CONVERSA, ATENDENTE)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value(containsString("Conversa")));
	}

	@Test
	void assumirComAtendenteInexistenteDevolve404() throws Exception {
		doThrow(new RecursoNaoEncontradoException("Atendente", ATENDENTE)).when(servico).atribuirAtendente(CONVERSA,
				ATENDENTE);

		assumir(CONVERSA, ATENDENTE)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value(containsString("Atendente")));
	}

	@Test
	void assumirQuandoARegraDeNegocioNaoPermiteDevolve409() throws Exception {
		doThrow(new IllegalStateException("O atendente não está disponível")).when(servico)
				.atribuirAtendente(CONVERSA, ATENDENTE);

		assumir(CONVERSA, ATENDENTE)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("O atendente não está disponível"));
	}

	@Test
	void assumirSemCorpoDevolve400() throws Exception {
		mvc.perform(post("/api/fila/{id}/assumir", CONVERSA).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/fila/{id}/assumir", CONVERSA))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(servico);
	}

	@Test
	void assumirComCorpoInvalidoDevolve400() throws Exception {
		assumir(CONVERSA.toString(), "{}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(containsString("atendenteId")));
		assumir(CONVERSA.toString(), "{\"atendenteId\":null}").andExpect(status().isBadRequest());
		assumir(CONVERSA.toString(), "{\"atendenteId\":\"nao-e-uuid\"}").andExpect(status().isBadRequest());
		assumir(CONVERSA.toString(), "{\"atendenteId\":").andExpect(status().isBadRequest());
		assumir(CONVERSA.toString(), "isto nao e json").andExpect(status().isBadRequest());

		verifyNoInteractions(servico);
	}

	@Test
	void assumirComIdentificadorDaConversaMalformadoDevolve400() throws Exception {
		assumir("nao-e-uuid", "{\"atendenteId\":\"" + ATENDENTE + "\"}")
				.andExpect(status().isBadRequest());

		verifyNoInteractions(servico);
	}
}
