package br.com.wxnchatbot.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;

import br.com.wxnchatbot.backend.service.FilaConversasServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;

@WebMvcTest(FilaConversasController.class)
class TratadorDeErrosTest {

	private static final UUID CONVERSA = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private FilaConversasServico servico;

	@Test
	void naoEncontradoVira404EmProblemDetailEmPortugues() throws Exception {
		when(servico.abrirConversaDaFila(CONVERSA)).thenThrow(new RecursoNaoEncontradoException("Conversa", CONVERSA));

		mvc.perform(get("/api/fila/{id}", CONVERSA))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Não encontrado"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("Conversa não encontrado(a): " + CONVERSA));
	}

	@Test
	void regraDeNegocioViolada409() throws Exception {
		when(servico.abrirConversaDaFila(CONVERSA)).thenThrow(new IllegalStateException("A conversa não está na fila de atendimento"));

		mvc.perform(get("/api/fila/{id}", CONVERSA))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Conflito"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("A conversa não está na fila de atendimento"));
	}

	@Test
	void argumentoInvalidoDoDominioVira400() throws Exception {
		when(servico.abrirConversaDaFila(CONVERSA)).thenThrow(new IllegalArgumentException("motivo é obrigatório"));

		mvc.perform(get("/api/fila/{id}", CONVERSA))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Requisição inválida"))
				.andExpect(jsonPath("$.detail").value("motivo é obrigatório"));
	}

	@Test
	void erroInesperadoContinua500SemVazarMensagemNemPilha() throws Exception {
		when(servico.abrirConversaDaFila(CONVERSA)).thenThrow(new RuntimeException("senha do banco: segredo"));

		mvc.perform(get("/api/fila/{id}", CONVERSA))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Erro interno"))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.detail").value("Não foi possível concluir a operação. Tente novamente em instantes."))
				.andExpect(jsonPath("$.trace").doesNotExist())
				.andExpect(content().string(not(containsString("segredo"))))
				.andExpect(content().string(not(containsString("br.com.wxnchatbot"))));
	}

	@Test
	void estadoInvalidoLancadoForaDoProjetoNaoViraConflito() throws Exception {
		when(servico.abrirConversaDaFila(CONVERSA)).thenAnswer(chamada -> {
			Assert.state(false, "estado interno do framework");
			return null;
		});

		mvc.perform(get("/api/fila/{id}", CONVERSA))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string(not(containsString("framework"))));
	}

	@Test
	void argumentoInvalidoLancadoForaDoProjetoNaoViraRequisicaoInvalida() throws Exception {
		when(servico.abrirConversaDaFila(CONVERSA)).thenAnswer(chamada -> {
			Assert.isTrue(false, "argumento interno do framework");
			return null;
		});

		mvc.perform(get("/api/fila/{id}", CONVERSA))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string(not(containsString("framework"))));
	}

	@Test
	void corpoIlegivelVira400ComDetalheEmPortugues() throws Exception {
		mvc.perform(post("/api/fila/{id}/assumir", CONVERSA).contentType(MediaType.APPLICATION_JSON).content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Requisição inválida"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("O corpo da requisição está ausente ou malformado"));
	}

	@Test
	void campoInvalidoVira400ComONomeDoCampo() throws Exception {
		mvc.perform(post("/api/fila/{id}/assumir", CONVERSA).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Dados inválidos: atendenteId é obrigatório"));
	}

	@Test
	void identificadorMalformadoVira400ComONomeDoParametro() throws Exception {
		mvc.perform(get("/api/fila/{id}", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("O valor informado para 'conversaId' está em formato inválido"));
	}

	@Test
	void metodoNaoPermitidoVira405EmPortuguesMantendoOCabecalhoAllow() throws Exception {
		mvc.perform(get("/api/fila/{id}/assumir", CONVERSA))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(header().string("Allow", containsString("POST")))
				.andExpect(jsonPath("$.title").value("Método não permitido"))
				.andExpect(jsonPath("$.detail").value("Método não permitido para este recurso"));
	}

	@Test
	void tipoDeConteudoNaoSuportadoVira415EmPortugues() throws Exception {
		mvc.perform(post("/api/fila/{id}/assumir", CONVERSA).contentType(MediaType.TEXT_PLAIN).content("x"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.title").value("Tipo de conteúdo não suportado"))
				.andExpect(jsonPath("$.detail").value("Tipo de conteúdo não suportado"));
	}

	@Test
	void rotaInexistenteVira404EmPortugues() throws Exception {
		mvc.perform(get("/api/nao-existe"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Não encontrado"))
				.andExpect(jsonPath("$.detail").value("O recurso solicitado não existe"));
	}
}
