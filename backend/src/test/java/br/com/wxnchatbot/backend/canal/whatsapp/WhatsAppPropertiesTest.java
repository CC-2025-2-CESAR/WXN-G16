package br.com.wxnchatbot.backend.canal.whatsapp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class WhatsAppPropertiesTest {

	private static WhatsAppProperties com(String verificacao, String segredo, String acesso, String numero) {
		return new WhatsAppProperties(true, verificacao, segredo, acesso, numero, "v23.0", "https://graph.facebook.com",
				Duration.ofSeconds(1), Duration.ofSeconds(1));
	}

	@Test
	void configuracaoCompletaEAceita() {
		assertDoesNotThrow(() -> com("v", "s", "a", "123").exigirConfiguracaoCompleta());
	}

	@Test
	void faltandoQualquerSegredoAAplicacaoNaoSobeEDizQualVariavelFalta() {
		assertTrue(assertThrows(IllegalStateException.class, () -> com("", "s", "a", "123").exigirConfiguracaoCompleta())
				.getMessage().contains("WHATSAPP_TOKEN_DE_VERIFICACAO"));
		assertTrue(assertThrows(IllegalStateException.class, () -> com("v", " ", "a", "123").exigirConfiguracaoCompleta())
				.getMessage().contains("WHATSAPP_SEGREDO_DO_APP"));
		assertTrue(assertThrows(IllegalStateException.class, () -> com("v", "s", null, "123").exigirConfiguracaoCompleta())
				.getMessage().contains("WHATSAPP_TOKEN_DE_ACESSO"));
		assertTrue(assertThrows(IllegalStateException.class, () -> com("v", "s", "a", "").exigirConfiguracaoCompleta())
				.getMessage().contains("WHATSAPP_ID_DO_NUMERO"));
	}
}
