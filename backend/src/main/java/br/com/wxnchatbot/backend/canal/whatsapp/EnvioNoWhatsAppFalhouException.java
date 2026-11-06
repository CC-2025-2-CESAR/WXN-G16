package br.com.wxnchatbot.backend.canal.whatsapp;

public class EnvioNoWhatsAppFalhouException extends RuntimeException {

	public EnvioNoWhatsAppFalhouException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
