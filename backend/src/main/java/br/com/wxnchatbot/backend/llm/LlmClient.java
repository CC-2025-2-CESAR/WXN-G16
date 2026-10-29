package br.com.wxnchatbot.backend.llm;

import java.util.List;

public interface LlmClient {

	/** Devolve a resposta da Lily; a última mensagem de "conversa" é a pergunta atual do usuário. */
	String responder(String promptDoSistema, List<MensagemLlm> conversa);
}