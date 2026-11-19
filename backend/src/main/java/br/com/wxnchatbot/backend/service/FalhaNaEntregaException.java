package br.com.wxnchatbot.backend.service;

/**
 * O canal não conseguiu entregar a mensagem do atendente ao cliente. A mensagem da exceção é um aviso em português, pronto
 * para aparecer na tela do atendente, e nunca traz segredos (token do canal, endereço de chamada).
 */
public class FalhaNaEntregaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FalhaNaEntregaException(String aviso) {
		super(aviso);
	}
}
