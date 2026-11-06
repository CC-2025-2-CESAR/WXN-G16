package br.com.wxnchatbot.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.wxnchatbot.backend.service.ChatServico;
import br.com.wxnchatbot.backend.service.OrquestradorConversaServico;

/** Em produção a entrada do chatbot pela API fica desligada: as rotas simplesmente não existem. */
@WebMvcTest(controllers = ChatController.class, properties = "chat.api.habilitada=false")
class ChatControllerDesligadoTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ChatServico chat;

	@MockitoBean
	private OrquestradorConversaServico orquestrador;

	@Test
	void semAPropriedadeLigadaAsRotasNaoExistem() throws Exception {
		mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON)
				.content("{\"telefone\":\"81999990001\",\"texto\":\"Oi\"}")).andExpect(status().isNotFound());
		mvc.perform(post("/api/conversas/{id}/mensagens", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"Oi\"}")).andExpect(status().isNotFound());
	}
}
