package br.com.wxnchatbot.backend.canal.twilio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class TwilioPropertiesTest {

	private static final String SID = "AC_SID_FALSO_PARA_TESTES";
	private static final String TOKEN = "token-secreto-do-twilio";
	private static final String URL = "https://wxn-g16-backend.azurewebsites.net/webhook/twilio/whatsapp";

	private static TwilioProperties com(String sid, String token, String numero, String url) {
		return new TwilioProperties(true, sid, token, numero, url, "https://api.twilio.com", Duration.ofSeconds(1),
				Duration.ofSeconds(1));
	}

	@Test
	void configuracaoCompletaEAceita() {
		assertDoesNotThrow(() -> com(SID, TOKEN, "+14155238886", URL).exigirConfiguracaoCompleta());
		assertDoesNotThrow(() -> com(SID, TOKEN, "whatsapp:+14155238886", URL).exigirConfiguracaoCompleta());
		assertDoesNotThrow(() -> com(SID, TOKEN, "14155238886", "http://localhost:8080/webhook/twilio/whatsapp")
				.exigirConfiguracaoCompleta());
	}

	@Test
	void faltandoQualquerValorAAplicacaoNaoSobeEDizQualVariavelFalta() {
		assertTrue(mensagem(com("", TOKEN, "+14155238886", URL)).contains("TWILIO_ACCOUNT_SID"));
		assertTrue(mensagem(com(SID, null, "+14155238886", URL)).contains("TWILIO_AUTH_TOKEN"));
		assertTrue(mensagem(com(SID, TOKEN, " ", URL)).contains("TWILIO_NUMERO_DE_ORIGEM"));
		assertTrue(mensagem(com(SID, TOKEN, "+14155238886", "")).contains("TWILIO_URL_DO_WEBHOOK"));
	}

	@Test
	void urlDoWebhookSemHttpEhRecusada() {
		assertTrue(mensagem(com(SID, TOKEN, "+14155238886", "wxn-g16-backend.azurewebsites.net/webhook")).contains("https://"));
	}

	@Test
	void numeroDeOrigemQueNaoEhUmNumeroEhRecusadoSemRevelarOToken() {
		String erro = mensagem(com(SID, TOKEN, "meu-numero", URL));

		assertTrue(erro.contains("TWILIO_NUMERO_DE_ORIGEM"));
		assertFalse(erro.contains(TOKEN));
	}

	@Test
	void oRemetenteSempreSaiNoFormatoDoTwilio() {
		assertEquals("whatsapp:+14155238886", com(SID, TOKEN, "+14155238886", URL).remetente());
		assertEquals("whatsapp:+14155238886", com(SID, TOKEN, "whatsapp:+14155238886", URL).remetente());
		assertEquals("whatsapp:+14155238886", com(SID, TOKEN, "WhatsApp:+14155238886", URL).remetente());
		assertEquals("whatsapp:+14155238886", com(SID, TOKEN, " 14155238886 ", URL).remetente());
	}

	private static String mensagem(TwilioProperties propriedades) {
		return assertThrows(IllegalStateException.class, propriedades::exigirConfiguracaoCompleta).getMessage();
	}
}
