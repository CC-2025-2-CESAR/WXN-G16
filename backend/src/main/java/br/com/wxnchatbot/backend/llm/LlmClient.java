package br.com.wxnchatbot.backend.llm;

import java.util.List;
import java.util.Map;

public interface LlmClient {

	/** Devolve a resposta da Lily; a última mensagem de "conversa" é a pergunta atual do usuário. */
	String responder(String promptDoSistema, List<MensagemLlm> conversa);

	/**
	 * Igual a {@link #responder}, mas a resposta é um JSON no formato do esquema informado (usado para decisões
	 * estruturadas, como classificar a intenção). Devolve o texto JSON sem interpretar.
	 */
	String responderEmJson(String promptDoSistema, List<MensagemLlm> conversa, Map<String, Object> esquema);
}
