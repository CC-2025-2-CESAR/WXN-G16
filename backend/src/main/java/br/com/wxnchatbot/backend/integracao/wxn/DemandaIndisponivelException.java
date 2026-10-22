package br.com.wxnchatbot.backend.integracao.wxn;

public class DemandaIndisponivelException extends RuntimeException {

	public DemandaIndisponivelException(String mensagem) {
		super(mensagem);
	}

	public DemandaIndisponivelException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
