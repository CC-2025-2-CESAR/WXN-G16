package br.com.wxnchatbot.backend.chatbot;

/** O que a Lily faz com a mensagem do cliente: responder por conta própria ou passar a conversa para um atendente. */
public sealed interface DecisaoDoTurno {

	record Responder(String texto) implements DecisaoDoTurno {
	}

	/** Responde ao cliente e coloca a conversa na fila de atendimento, com o motivo para o atendente ler. */
	record Encaminhar(String motivo, String texto) implements DecisaoDoTurno {
	}
}
