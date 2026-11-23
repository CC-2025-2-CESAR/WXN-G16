package br.com.wxnchatbot.backend.canal.twilio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class CanalDeSaidaTwilioTest {

	@Mock
	private TwilioClient twilio;

	private CanalDeSaidaTwilio canal;

	@BeforeEach
	void preparar() {
		canal = new CanalDeSaidaTwilio(twilio);
	}

	private void falhaDoTwilio(int status, int codigo) {
		doThrow(new FalhaNoTwilioException("HTTP " + status + " do Twilio, detalhe interno", status, codigo)).when(twilio)
				.enviarTexto(anyString(), anyString());
	}

	@Test
	void atendeAsConversasDoWhatsApp() {
		assertEquals(OrigemConversa.WHATSAPP, canal.origem());
	}

	@Test
	void completaOEnderecoComODdiDoBrasilQuandoOCadastroNaoTemOPais() {
		canal.entregar("81999990001", "Oi, sou o Carlos");
		canal.entregar("8133334444", "Oi de novo");
		canal.entregar("5581999990001", "Já tem o país");

		verify(twilio).enviarTexto("whatsapp:+5581999990001", "Oi, sou o Carlos");
		verify(twilio).enviarTexto("whatsapp:+558133334444", "Oi de novo");
		verify(twilio).enviarTexto("whatsapp:+5581999990001", "Já tem o país");
	}

	@Test
	void telefoneComMascaraEhLimpoAntesDeEnviar() {
		canal.entregar("+55 (81) 99999-0001", "Oi");

		verify(twilio).enviarTexto("whatsapp:+5581999990001", "Oi");
	}

	@Test
	void clienteSemNumeroDeWhatsAppValidoNaoEhEntregue() {
		for (String identificador : new String[] { "tg:1111111", "123", "", null, "1".repeat(16) }) {
			FalhaNaEntregaException erro = assertThrows(FalhaNaEntregaException.class,
					() -> canal.entregar(identificador, "Oi"), String.valueOf(identificador));
			assertTrue(erro.getMessage().contains("WhatsApp"));
		}
		verifyNoInteractions(twilio);
	}

	@Test
	void foraDaJanelaDe24HorasOAtendenteRecebeUmAvisoEspecifico() {
		falhaDoTwilio(400, CanalDeSaidaTwilio.FORA_DA_JANELA);

		FalhaNaEntregaException erro = assertThrows(FalhaNaEntregaException.class, () -> canal.entregar("81999990001", "Oi"));

		assertTrue(erro.getMessage().contains("24 horas"));
		assertFalse(erro.getMessage().contains("detalhe interno"));
	}

	@Test
	void clienteForaDoSandboxRecebeOAvisoDeJoin() {
		falhaDoTwilio(400, CanalDeSaidaTwilio.FORA_DO_SANDBOX);

		FalhaNaEntregaException erro = assertThrows(FalhaNaEntregaException.class, () -> canal.entregar("81999990001", "Oi"));

		assertTrue(erro.getMessage().contains("sandbox"));
		assertTrue(erro.getMessage().contains("join"));
	}

	@Test
	void credenciaisRecusadasViramAvisoParaAEquipeTecnica() {
		falhaDoTwilio(401, 20003);

		FalhaNaEntregaException erro = assertThrows(FalhaNaEntregaException.class, () -> canal.entregar("81999990001", "Oi"));

		assertTrue(erro.getMessage().contains("credenciais"));
	}

	@Test
	void outraFalhaViraAvisoGenericoSemRepetirOErroOriginal() {
		falhaDoTwilio(500, 0);

		FalhaNaEntregaException erro = assertThrows(FalhaNaEntregaException.class, () -> canal.entregar("81999990001", "Oi"));

		assertTrue(erro.getMessage().contains("Não foi possível entregar pelo WhatsApp"));
		assertFalse(erro.getMessage().contains("detalhe interno"));
	}
}
