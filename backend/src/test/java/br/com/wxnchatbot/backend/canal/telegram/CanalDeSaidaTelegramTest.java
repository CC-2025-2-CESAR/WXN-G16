package br.com.wxnchatbot.backend.canal.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.service.FalhaNaEntregaException;

@ExtendWith(MockitoExtension.class)
class CanalDeSaidaTelegramTest {

	@Mock
	private TelegramClient telegram;

	private CanalDeSaidaTelegram canal;

	@BeforeEach
	void preparar() {
		canal = new CanalDeSaidaTelegram(telegram);
	}

	@Test
	void atendeAsConversasDoTelegram() {
		assertEquals(OrigemConversa.TELEGRAM, canal.origem());
	}

	@Test
	void entregaOTextoNoChatDoClienteUsandoOIdGuardadoNoLugarDoTelefone() {
		canal.entregar("tg:1111111", "Oi, sou o Carlos");

		verify(telegram).enviarTexto(1111111L, "Oi, sou o Carlos");
	}

	@Test
	void aceitaIdDeChatGrande() {
		canal.entregar("tg:7700000001", "Oi");

		verify(telegram).enviarTexto(7_700_000_001L, "Oi");
	}

	@Test
	void clienteQueNaoEhDoTelegramNaoEhEntregue() {
		for (String identificador : new String[] { "81999990001", "tg:", "tg:abc", "TG:123", "", null }) {
			FalhaNaEntregaException erro = assertThrows(FalhaNaEntregaException.class,
					() -> canal.entregar(identificador, "Oi"), String.valueOf(identificador));
			assertTrue(erro.getMessage().contains("Telegram"));
		}
		verifyNoInteractions(telegram);
	}

	@Test
	void idQueEstouraOLongNaoEhEntregueENaoVazaOValor() {
		FalhaNaEntregaException erro = assertThrows(FalhaNaEntregaException.class,
				() -> canal.entregar("tg:99999999999999999999", "Oi"));

		assertFalse(erro.getMessage().contains("99999999999999999999"));
		verifyNoInteractions(telegram);
	}

	@Test
	void clienteQueBloqueouOBotRecebeUmAvisoEspecifico() {
		doThrow(new FalhaNoTelegramException("Forbidden: bot was blocked by the user", 403)).when(telegram)
				.enviarTexto(anyLong(), anyString());

		FalhaNaEntregaException erro = assertThrows(FalhaNaEntregaException.class, () -> canal.entregar("tg:1111111", "Oi"));

		assertTrue(erro.getMessage().contains("bloqueado"));
		assertFalse(erro.getMessage().contains("Forbidden"));
	}

	@Test
	void outraFalhaDoTelegramViraAvisoGenericoSemRepetirOErroOriginal() {
		doThrow(new FalhaNoTelegramException("HTTP 500 do Telegram", 500)).when(telegram).enviarTexto(anyLong(), anyString());
		doThrow(new FalhaNoTelegramException("sem rede", 0)).when(telegram).enviarTexto(2L, "rede");

		FalhaNaEntregaException servidor = assertThrows(FalhaNaEntregaException.class, () -> canal.entregar("tg:1", "Oi"));
		FalhaNaEntregaException rede = assertThrows(FalhaNaEntregaException.class, () -> canal.entregar("tg:2", "rede"));

		assertTrue(servidor.getMessage().contains("Não foi possível entregar pelo Telegram"));
		assertTrue(rede.getMessage().contains("Não foi possível entregar pelo Telegram"));
		assertFalse(servidor.getMessage().contains("HTTP 500"));
	}
}
