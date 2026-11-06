package br.com.wxnchatbot.backend.chatbot;

import java.util.List;

import br.com.wxnchatbot.backend.llm.MensagemLlm;

public interface ClassificadorDeIntencao {

	/** Classifica a última mensagem da conversa (a do cliente), usando as anteriores como contexto. */
	ResultadoIntencao classificar(List<MensagemLlm> conversa);
}
