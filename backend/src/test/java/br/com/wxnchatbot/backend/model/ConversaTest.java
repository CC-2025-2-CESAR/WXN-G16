package br.com.wxnchatbot.backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversaTest {

	private Cliente cliente;
	private Chatbot lily;
	private Atendente atendente;
	private Conversa conversa;

	@BeforeEach
	void preparar() {
		cliente = new Cliente("Maria Souza", "+55 (81) 99999-1234");
		lily = new Chatbot("Lily", "Oi, sou a Lily, assistente virtual da WXN!");
		atendente = new Atendente("Carlos Lima", "carlos@wxn.com.br");
		conversa = new Conversa(cliente, lily);
	}

	@Test
	void conversaNovaComecaComOChatbotESemMensagens() {
		assertEquals(StatusConversa.COM_CHATBOT, conversa.getStatus());
		assertTrue(conversa.getMensagens().isEmpty());
		assertTrue(conversa.getAtendente().isEmpty());
		assertTrue(conversa.getTicket().isEmpty());
		assertTrue(conversa.getEncerradaEm().isEmpty());
	}

	@Test
	void chatbotSaudaEMensagemFicaRegistradaNaConversa() {
		Mensagem saudacao = lily.saudar(conversa);

		assertEquals(1, conversa.getMensagens().size());
		assertSame(saudacao, conversa.getMensagens().get(0));
		assertEquals(TipoRemetente.CHATBOT, saudacao.getTipoRemetente());
		assertEquals("Oi, sou a Lily, assistente virtual da WXN!", saudacao.getConteudo());
	}

	@Test
	void fluxoCompletoDoChatbotAoAtendenteHumanoAteEncerrar() {
		lily.saudar(conversa);
		conversa.adicionarMensagem(cliente, "Preciso falar com uma pessoa");

		Ticket ticket = conversa.solicitarAtendente("Cliente pediu atendimento humano");
		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, conversa.getStatus());
		assertEquals(StatusTicket.ABERTO, ticket.getStatus());
		assertSame(cliente, ticket.getCliente());

		conversa.atribuirAtendente(atendente);
		assertEquals(StatusConversa.COM_ATENDENTE, conversa.getStatus());
		assertEquals(StatusTicket.EM_ATENDIMENTO, ticket.getStatus());
		assertSame(atendente, ticket.getAtendente().orElseThrow());

		conversa.adicionarMensagem(atendente, "Olá Maria, como posso ajudar?");
		assertEquals(3, conversa.getMensagens().size());

		conversa.encerrar();
		assertEquals(StatusConversa.ENCERRADA, conversa.getStatus());
		assertEquals(StatusTicket.RESOLVIDO, ticket.getStatus());
		assertTrue(ticket.getResolvidoEm().isPresent());
		assertTrue(conversa.getEncerradaEm().isPresent());
	}

	@Test
	void listaDeMensagensExpostaNaoPodeSerAlteradaDeFora() {
		assertThrows(UnsupportedOperationException.class, () -> conversa.getMensagens().clear());
	}

	@Test
	void clienteDeOutraConversaNaoPodeEnviarMensagem() {
		Cliente intruso = new Cliente("Joao Pedro", "81988887777");

		assertThrows(IllegalStateException.class, () -> conversa.adicionarMensagem(intruso, "Oi"));
	}

	@Test
	void chatbotNaoEnviaMensagemDepoisQueOAtendenteAssume() {
		conversa.solicitarAtendente("Precisa de humano");
		conversa.atribuirAtendente(atendente);

		assertThrows(IllegalStateException.class, () -> conversa.adicionarMensagem(lily, "Posso ajudar?"));
	}

	@Test
	void atendenteNaoEnviaMensagemAntesDeAssumirAConversa() {
		assertThrows(IllegalStateException.class, () -> conversa.adicionarMensagem(atendente, "Oi"));
	}

	@Test
	void conversaEncerradaNaoAceitaMensagensNemNovoEncerramento() {
		conversa.encerrar();

		assertThrows(IllegalStateException.class, () -> conversa.adicionarMensagem(cliente, "Alguem ai?"));
		assertThrows(IllegalStateException.class, conversa::encerrar);
	}

	@Test
	void naoSeAtribuiAtendenteSemTerSolicitado() {
		assertThrows(IllegalStateException.class, () -> conversa.atribuirAtendente(atendente));
	}

	@Test
	void naoSeSolicitaAtendenteDuasVezes() {
		conversa.solicitarAtendente("Primeiro pedido");

		assertThrows(IllegalStateException.class, () -> conversa.solicitarAtendente("Segundo pedido"));
	}

	@Test
	void atendenteIndisponivelNaoAssumeConversa() {
		atendente.marcarIndisponivel();
		conversa.solicitarAtendente("Precisa de humano");

		assertThrows(IllegalStateException.class, () -> conversa.atribuirAtendente(atendente));
		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, conversa.getStatus());
	}

	@Test
	void encerrarComTicketAindaAbertoResolveOTicket() {
		Ticket ticket = conversa.solicitarAtendente("Ninguem assumiu");

		conversa.encerrar();

		assertEquals(StatusTicket.RESOLVIDO, ticket.getStatus());
		assertTrue(ticket.getAtendente().isEmpty());
	}

	@Test
	void chatbotDesativadoNaoIniciaConversa() {
		lily.desativar();

		assertThrows(IllegalStateException.class, () -> new Conversa(cliente, lily));
		assertFalse(lily.isAtivo());
	}

	@Test
	void mensagemVaziaEhRejeitada() {
		assertThrows(IllegalArgumentException.class, () -> conversa.adicionarMensagem(cliente, "   "));
		assertThrows(IllegalArgumentException.class, () -> conversa.adicionarMensagem(cliente, null));
	}

	@Test
	void cadaEntidadeNasceComIdProprio() {
		assertNotNull(cliente.getId());
		assertNotNull(lily.getId());
		assertNotNull(conversa.getId());
		assertFalse(cliente.getId().equals(atendente.getId()));
	}
}
