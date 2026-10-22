package br.com.wxnchatbot.backend.notificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class NotificationGatewayFakeTest {

	private static final Instant AGORA = Instant.parse("2026-10-22T12:00:00Z");

	@Test
	void guardaAConfirmacaoEmMemoriaSemEnviarNada() {
		NotificationGatewayFake gateway = new NotificationGatewayFake();
		ConfirmacaoDeSolicitacao confirmacao = new ConfirmacaoDeSolicitacao("maria@exemplo.com", "WXN-2026-5000",
				"Erro ao emitir nota", AGORA);

		gateway.enviarConfirmacaoDeSolicitacao(confirmacao);

		assertEquals(1, gateway.enviadas().size());
		assertEquals(confirmacao, gateway.enviadas().get(0));
	}

	@Test
	void comecaVazioEAListaExpostaNaoPodeSerAlterada() {
		NotificationGatewayFake gateway = new NotificationGatewayFake();

		assertTrue(gateway.enviadas().isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> gateway.enviadas().add(null));
	}

	@Test
	void confirmacaoRejeitaDadosInvalidos() {
		assertThrows(IllegalArgumentException.class, () -> new ConfirmacaoDeSolicitacao("sem-arroba", "WXN-1", "x", AGORA));
		assertThrows(IllegalArgumentException.class, () -> new ConfirmacaoDeSolicitacao(null, "WXN-1", "x", AGORA));
		assertThrows(IllegalArgumentException.class, () -> new ConfirmacaoDeSolicitacao("a@b.com", " ", "x", AGORA));
		assertThrows(NullPointerException.class, () -> new ConfirmacaoDeSolicitacao("a@b.com", "WXN-1", "x", null));
	}
}
