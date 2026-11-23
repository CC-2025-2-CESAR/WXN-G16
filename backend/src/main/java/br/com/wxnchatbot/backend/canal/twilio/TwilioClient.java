package br.com.wxnchatbot.backend.canal.twilio;

public interface TwilioClient {

	/**
	 * Envia um texto por WhatsApp, dividindo-o se passar do limite de 1600 caracteres do Twilio. {@code para} é o endereço
	 * do cliente no formato do Twilio: {@code whatsapp:+5581999990001}.
	 */
	void enviarTexto(String para, String texto);
}
