package br.com.wxnchatbot.backend.llm;

public class LlmIndisponivelException extends RuntimeException {

	public LlmIndisponivelException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}