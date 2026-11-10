package br.com.wxnchatbot.backend.chatbot;

import java.util.List;

import br.com.wxnchatbot.backend.llm.MensagemLlm;

/**
 * Dá a forma final à mensagem da Lily. Quem decide o que dizer, e quais são os fatos (protocolos, status, títulos), é o
 * código: ele passa um rascunho já correto, e o redator só pode mudar o jeito de dizer.
 */
public interface Redator {

	/**
	 * @param situacao em que ponto da conversa a Lily está
	 * @param rascunho a mensagem correta, com todos os fatos; é o que vale quando não há LLM
	 * @param historico a conversa até a última mensagem do cliente, para a resposta acompanhar o assunto
	 */
	String escrever(Situacao situacao, String rascunho, List<MensagemLlm> historico);
}
