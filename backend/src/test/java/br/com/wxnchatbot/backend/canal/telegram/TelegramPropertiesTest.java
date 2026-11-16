package br.com.wxnchatbot.backend.canal.telegram;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class TelegramPropertiesTest {

	private static TelegramProperties com(String modo, String token, String segredo) {
		return new TelegramProperties(true, modo, token, segredo, "https://api.telegram.org", Duration.ofSeconds(1),
				Duration.ofSeconds(1), 25);
	}

	@Test
	void webhookCompletoEAceito() {
		assertDoesNotThrow(() -> com("webhook", "123:ABC", "segredo_Valido-01").exigirConfiguracaoCompleta());
	}

	@Test
	void pollingNaoPrecisaDoSegredoDoWebhook() {
		assertDoesNotThrow(() -> com("polling", "123:ABC", "").exigirConfiguracaoCompleta());
		assertTrue(com("POLLING", "123:ABC", "").polling());
		assertFalse(com("webhook", "123:ABC", "x").polling());
	}

	@Test
	void faltandoOTokenOuOSegredoAAplicacaoNaoSobeEDizQualVariavelFalta() {
		assertTrue(assertThrows(IllegalStateException.class, () -> com("webhook", "", "s").exigirConfiguracaoCompleta())
				.getMessage().contains("TELEGRAM_TOKEN_DO_BOT"));
		assertTrue(assertThrows(IllegalStateException.class, () -> com("polling", null, "").exigirConfiguracaoCompleta())
				.getMessage().contains("TELEGRAM_TOKEN_DO_BOT"));
		assertTrue(assertThrows(IllegalStateException.class, () -> com("webhook", "123:ABC", " ").exigirConfiguracaoCompleta())
				.getMessage().contains("TELEGRAM_SEGREDO_DO_WEBHOOK"));
	}

	@Test
	void segredoComCaracterQueOTelegramNaoAceitaEhRecusadoSemRevelarOValor() {
		IllegalStateException erro = assertThrows(IllegalStateException.class,
				() -> com("webhook", "123:ABC", "tem espaço!").exigirConfiguracaoCompleta());

		assertTrue(erro.getMessage().contains("TELEGRAM_SEGREDO_DO_WEBHOOK"));
		assertFalse(erro.getMessage().contains("tem espaço"));
	}

	@Test
	void segredoMaiorQue256CaracteresEhRecusado() {
		assertThrows(IllegalStateException.class, () -> com("webhook", "123:ABC", "a".repeat(257)).exigirConfiguracaoCompleta());
		assertDoesNotThrow(() -> com("webhook", "123:ABC", "a".repeat(256)).exigirConfiguracaoCompleta());
	}

	@Test
	void modoDesconhecidoEhRecusado() {
		IllegalStateException erro = assertThrows(IllegalStateException.class,
				() -> com("longo", "123:ABC", "s").exigirConfiguracaoCompleta());

		assertEquals("telegram.modo (TELEGRAM_MODO) deve ser webhook ou polling", erro.getMessage());
	}
}
