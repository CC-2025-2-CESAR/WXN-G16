package br.com.wxnchatbot.backend.canal.telegram;

/**
 * O Telegram recusou o pedido ou não respondeu. A mensagem nunca traz o token do bot: ele vai no endereço da chamada, e
 * as exceções do cliente HTTP costumam repetir o endereço inteiro.
 */
public class FalhaNoTelegramException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;

	public FalhaNoTelegramException(String mensagem, int status) {
		super(mensagem);
		this.status = status;
	}

	/** O código HTTP que o Telegram devolveu, ou 0 se a falha foi de rede. */
	public int status() {
		return status;
	}
}
