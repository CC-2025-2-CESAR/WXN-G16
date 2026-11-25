package br.com.wxnchatbot.backend.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.autenticacao.AtendenteAutenticado;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;
import br.com.wxnchatbot.backend.service.FilaConversasServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;

@WebMvcTest(ConversaController.class)
class ConversaControllerTest {

	private static final UUID CONVERSA = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ATENDENTE = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private FilaConversasServico servico;

	@MockitoBean
	private ConversaRepositorio conversas;
	@MockitoBean
	private AtendenteAutenticado autenticado;

	@BeforeEach
	void prepararIdentidade() {
		lenient().when(autenticado.id()).thenReturn(ATENDENTE);
	}

	@Test
	void listarDevolveListaVaziaQuandoNaoHaConversas() throws Exception {
		when(conversas.findAll()).thenReturn(List.of());

		mvc.perform(get("/api/conversas"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void listarDevolveResumoDasConversasEncontradas() throws Exception {
		// 1. Criamos um mock do Cliente
		Cliente clienteMock = mock(Cliente.class);
		when(clienteMock.getNome()).thenReturn("João Silva");

		// 2. Criamos um mock da Conversa e ligamos o Cliente
		Conversa conversaMock = mock(Conversa.class);
		when(conversaMock.getId()).thenReturn(CONVERSA);
		when(conversaMock.getCliente()).thenReturn(clienteMock);
		when(conversaMock.getStatus()).thenReturn(StatusConversa.COM_CHATBOT);

		when(conversas.findAll()).thenReturn(List.of(conversaMock));

		mvc.perform(get("/api/conversas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].clienteNome").value("João Silva"))
				.andExpect(jsonPath("$[0].status").value("COM_CHATBOT"));
	}

	@Test
	void detalharDevolveConversaComMensagens() throws Exception {
		// 1. Criamos um mock do Cliente
		Cliente clienteMock = mock(Cliente.class);
		when(clienteMock.getNome()).thenReturn("João Silva");

		// 2. Criamos um mock da Conversa 
		Conversa conversaMock = mock(Conversa.class);
		when(conversaMock.getId()).thenReturn(CONVERSA);
		when(conversaMock.getCliente()).thenReturn(clienteMock);
		when(conversaMock.getStatus()).thenReturn(StatusConversa.COM_CHATBOT);
		when(conversaMock.getMensagens()).thenReturn(List.of()); // Histórico vazio para simplificar o teste

		when(conversas.findById(CONVERSA)).thenReturn(Optional.of(conversaMock));

		mvc.perform(get("/api/conversas/{id}", CONVERSA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clienteNome").value("João Silva"))
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

		verify(servico).encerrarComoAtendente(CONVERSA, ATENDENTE);
	}

	@Test
	void encerrarConversaInexistenteDevolve404() throws Exception {
		doThrow(new RecursoNaoEncontradoException("Conversa", CONVERSA)).when(servico).encerrarComoAtendente(CONVERSA, ATENDENTE);

		mvc.perform(post("/api/conversas/{id}/encerrar", CONVERSA))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void encerrarConversaJaEncerradaDevolve409() throws Exception {
		doThrow(new IllegalStateException("A conversa já está encerrada")).when(servico).encerrarComoAtendente(CONVERSA, ATENDENTE);

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
