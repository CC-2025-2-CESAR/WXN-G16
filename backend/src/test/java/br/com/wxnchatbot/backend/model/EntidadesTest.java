package br.com.wxnchatbot.backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EntidadesTest {

	@Test
	void clienteNormalizaTelefoneParaApenasDigitos() {
		Cliente cliente = new Cliente("  Maria Souza  ", "+55 (81) 99999-1234");

		assertEquals("5581999991234", cliente.getTelefone());
		assertEquals("Maria Souza", cliente.getNome());
		assertEquals(TipoRemetente.CLIENTE, cliente.getTipo());
	}

	@Test
	void clienteRejeitaDadosInvalidos() {
		assertThrows(IllegalArgumentException.class, () -> new Cliente("", "81999991234"));
		assertThrows(IllegalArgumentException.class, () -> new Cliente("Maria", null));
		assertThrows(IllegalArgumentException.class, () -> new Cliente("Maria", "12345"));
		assertThrows(IllegalArgumentException.class, () -> new Cliente("Maria", "1234567890123456"));
	}

	@Test
	void setNomeTambemValida() {
		Cliente cliente = new Cliente("Maria", "81999991234");

		assertThrows(IllegalArgumentException.class, () -> cliente.setNome(" "));
		assertEquals("Maria", cliente.getNome());
	}

	@Test
	void atendenteValidaEmailEComecaDisponivel() {
		Atendente atendente = new Atendente("Carlos", "carlos@wxn.com.br");

		assertTrue(atendente.isDisponivel());
		assertEquals(TipoRemetente.ATENDENTE, atendente.getTipo());
		assertThrows(IllegalArgumentException.class, () -> new Atendente("Carlos", "sem-arroba"));
		assertThrows(IllegalArgumentException.class, () -> atendente.setEmail(""));
	}

	@Test
	void atendenteAlternaDisponibilidade() {
		Atendente atendente = new Atendente("Carlos", "carlos@wxn.com.br");

		atendente.marcarIndisponivel();
		assertEquals(false, atendente.isDisponivel());

		atendente.marcarDisponivel();
		assertTrue(atendente.isDisponivel());
	}

	@Test
	void chatbotExigeNomeEMensagemDeBoasVindas() {
		assertThrows(IllegalArgumentException.class, () -> new Chatbot("", "Oi"));
		assertThrows(IllegalArgumentException.class, () -> new Chatbot("Lily", " "));

		Chatbot lily = new Chatbot("Lily", "Oi!");
		assertEquals(TipoRemetente.CHATBOT, lily.getTipo());
		assertThrows(IllegalArgumentException.class, () -> lily.setMensagemBoasVindas(null));
	}

	@Test
	void ticketSoEPodeSerCriadoPelaConversaEExigeMotivo() {
		Conversa conversa = new Conversa(new Cliente("Maria", "81999991234"), new Chatbot("Lily", "Oi!"));

		assertThrows(IllegalArgumentException.class, () -> conversa.solicitarAtendente(" "));
		assertTrue(conversa.getTicket().isEmpty());
	}
}
