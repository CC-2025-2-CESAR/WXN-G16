package br.com.wxnchatbot.backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.service.ChatServico;
import br.com.wxnchatbot.backend.service.OrquestradorConversaServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;
import br.com.wxnchatbot.backend.service.RespostaChat;
import br.com.wxnchatbot.backend.service.ResultadoTurno;

@WebMvcTest(controllers = ChatController.class, properties = "chat.api.habilitada=true")
class ChatControllerTest {

	private static final UUID CONVERSA = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ChatServico chat;

	@MockitoBean
	private OrquestradorConversaServico orquestrador;

	@Test
	void mensagemPorTelefoneChamaOChatEDevolveARespostaDaLily() throws Exception {
		when(chat.receber(OrigemConversa.SIMULADOR, "5581999990001", "Maria", "Oi")).thenReturn(new RespostaChat(CONVERSA, "Olá!", StatusConversa.COM_CHATBOT));

		mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON)
				.content("{\"telefone\":\"5581999990001\",\"nome\":\"Maria\",\"texto\":\"Oi\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.conversaId").value(CONVERSA.toString()))
				.andExpect(jsonPath("$.resposta").value("Olá!"))
				.andExpect(jsonPath("$.status").value("COM_CHATBOT"));
	}

	@Test
	void nomeEOpcional() throws Exception {
		when(chat.receber(OrigemConversa.SIMULADOR, "81999990001", null, "Oi")).thenReturn(new RespostaChat(CONVERSA, "Olá!", StatusConversa.COM_CHATBOT));

		mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON)
				.content("{\"telefone\":\"81999990001\",\"texto\":\"Oi\"}")).andExpect(status().isOk());
	}

	@Test
	void conversaComUmAtendenteDevolveRespostaNula() throws Exception {
		when(chat.receber(OrigemConversa.SIMULADOR, "81999990001", null, "Oi")).thenReturn(new RespostaChat(CONVERSA, null, StatusConversa.COM_ATENDENTE));

		mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON)
				.content("{\"telefone\":\"81999990001\",\"texto\":\"Oi\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resposta").doesNotExist())
				.andExpect(jsonPath("$.status").value("COM_ATENDENTE"));
	}

	@Test
	void textoEmBrancoDevolve400SemChamarOChat() throws Exception {
		mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON)
				.content("{\"telefone\":\"81999990001\",\"texto\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("texto")));

		verifyNoInteractions(chat);
	}

	@Test
	void textoMuitoGrandeDevolve400() throws Exception {
		String enorme = "a".repeat(4001);

		mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON)
				.content("{\"telefone\":\"81999990001\",\"texto\":\"" + enorme + "\"}")).andExpect(status().isBadRequest());
	}

	@Test
	void telefoneEmBrancoDevolve400() throws Exception {
		mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON)
				.content("{\"telefone\":\"\",\"texto\":\"Oi\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("telefone")));
	}

	@Test
	void telefoneSemCadastroDevolve404() throws Exception {
		when(chat.receber(OrigemConversa.SIMULADOR, "81999990001", null, "Oi")).thenThrow(new RecursoNaoEncontradoException("Não há cadastro para este telefone"));

		mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON)
				.content("{\"telefone\":\"81999990001\",\"texto\":\"Oi\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Não há cadastro para este telefone"));
	}

	@Test
	void corpoMalformadoDevolve400() throws Exception {
		mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON).content("isso não é json"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void mensagemNumaConversaChamaOOrquestrador() throws Exception {
		when(orquestrador.processarTurno(CONVERSA, "Tudo bem?")).thenReturn(new ResultadoTurno("Tudo!", StatusConversa.COM_CHATBOT));

		mvc.perform(post("/api/conversas/{id}/mensagens", CONVERSA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"Tudo bem?\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.conversaId").value(CONVERSA.toString()))
				.andExpect(jsonPath("$.resposta").value("Tudo!"));

		verify(orquestrador).processarTurno(CONVERSA, "Tudo bem?");
	}

	@Test
	void conversaInexistenteDevolve404() throws Exception {
		when(orquestrador.processarTurno(CONVERSA, "Oi")).thenThrow(new RecursoNaoEncontradoException("Conversa", CONVERSA));

		mvc.perform(post("/api/conversas/{id}/mensagens", CONVERSA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"Oi\"}")).andExpect(status().isNotFound());
	}

	@Test
	void conversaEncerradaDevolve409() throws Exception {
		when(orquestrador.processarTurno(CONVERSA, "Oi")).thenThrow(new IllegalStateException("Conversa encerrada não aceita novas mensagens"));

		mvc.perform(post("/api/conversas/{id}/mensagens", CONVERSA).contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"Oi\"}")).andExpect(status().isConflict());
	}
}
