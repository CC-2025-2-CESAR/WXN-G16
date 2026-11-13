package br.com.wxnchatbot.backend.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;
import br.com.wxnchatbot.backend.service.FilaConversasServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;

@WebMvcTest(ConversaController.class)
class ConversaControllerTest {

	private static final UUID CONVERSA = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private FilaConversasServico servico;

	@MockitoBean
	private ConversaRepositorio conversas;

	@Test
	void listarDevolveListaVaziaQuandoNaoHaConversas() throws Exception {
		when(conversas.findAll()).thenReturn(List.of());

		mvc.perform(get("/api/conversas"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void listarDevolveResumoDasConversasEncontradas() throws Exception {
		// Passando null como cliente para simular usuário anônimo
		Conversa conversaAnonima = new Conversa(null, new Chatbot("Lily", "Olá"));
		when(conversas.findAll()).thenReturn(List.of(conversaAnonima));

		mvc.perform(get("/api/conversas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].clienteNome").value("Anônimo"))
				.andExpect(jsonPath("$[0].status").value("COM_CHATBOT"));
	}

	@Test
	void detalharDevolveConversaComMensagens() throws Exception {
		// Passando null como cliente para simular usuário anônimo
		Conversa conversaAnonima = new Conversa(null, new Chatbot("Lily", "Olá"));
		when(conversas.findById(CONVERSA)).thenReturn(Optional.of(conversaAnonima));

		mvc.perform(get("/api/conversas/{id}", CONVERSA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clienteNome").value("Anônimo"))
				.andExpect(jsonPath("$.status").value("COM_CHATBOT"))
				.andExpect(jsonPath("$.mensagens").isArray());
	}

	@Test
	void detalharConversaInexistenteDevolve404() throws Exception {
		when(conversas.findById(CONVERSA)).thenReturn(Optional.empty());

		mvc.perform(get("/api/conversas/{id}", CONVERSA))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void encerrarDevolve204SemCorpoEChamaOServico() throws Exception {
		mvc.perform(post("/api/conversas/{id}/encerrar", CONVERSA))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(servico).encerrar(CONVERSA);
	}

	@Test
	void encerrarConversaInexistenteDevolve404() throws Exception {
		doThrow(new RecursoNaoEncontradoException("Conversa", CONVERSA)).when(servico).encerrar(CONVERSA);

		mvc.perform(post("/api/conversas/{id}/encerrar", CONVERSA))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void encerrarConversaJaEncerradaDevolve409() throws Exception {
		doThrow(new IllegalStateException("A conversa já está encerrada")).when(servico).encerrar(CONVERSA);

		mvc.perform(post("/api/conversas/{id}/encerrar", CONVERSA))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("A conversa já está encerrada"));
	}

	@Test
	void encerrarComIdentificadorMalformadoDevolve400() throws Exception {
		mvc.perform(post("/api/conversas/{id}/encerrar", "nao-e-uuid"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(servico);
	}
}