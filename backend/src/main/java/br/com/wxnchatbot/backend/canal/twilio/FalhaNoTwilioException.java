package br.com.wxnchatbot.backend.canal.twilio;

/**
 * O Twilio recusou o pedido ou não respondeu. A mensagem nunca traz o Auth Token nem o Account SID: o endereço da
 * chamada leva o SID, e as exceções do cliente HTTP costumam repetir o endereço inteiro.
 */
public class FalhaNoTwilioException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;
	private final int codigo;

	public FalhaNoTwilioException(String mensagem, int status, int codigo) {
		super(mensagem);
		this.status = status;
		this.codigo = codigo;
	}

	/** O código HTTP que o Twilio devolveu, ou 0 se a falha foi de rede. */
	public int status() {
		return status;
	}

	/** O código de erro do Twilio (por exemplo 63016, fora da janela de 24 horas), ou 0 se não veio um. */
	public int codigo() {
		return codigo;
	}
}
