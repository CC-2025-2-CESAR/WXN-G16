package br.com.wxnchatbot.backend.canal.whatsapp;

public interface WhatsAppClient {

	/** Envia um texto ao número (o wa_id que a Meta entregou no webhook), dividindo-o se passar do limite da Meta. */
	void enviarTexto(String para, String texto);
}
