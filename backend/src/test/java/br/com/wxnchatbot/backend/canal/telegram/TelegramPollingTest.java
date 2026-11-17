package br.com.wxnchatbot.backend.canal.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.canal.telegram.AtualizacaoDoTelegram.ChatDoTelegram;
import br.com.wxnchatbot.backend.canal.telegram.AtualizacaoDoTelegram.MensagemDoTelegram;

@ExtendWith(MockitoExtension.class)
class TelegramPollingTest {

	@Mock
	private TelegramClient telegram;

	@Mock
	private ProcessadorDeAtualizacoesTelegram processador;

	private TelegramPolling polling;

	@BeforeEach
	void preparar() {
		TelegramProperties propriedades = new TelegramProperties(true, "polling", "123:ABC", "", "https://api.telegram.org",
				Duration.ofSeconds(1), Duration.ofSeconds(1), 25);
		polling = new TelegramPolling(telegram, processador, propriedades);
	}

	private static AtualizacaoDoTelegram atualizacao(long id) {
		return new AtualizacaoDoTelegram(id, new MensagemDoTelegram(id, null, new ChatDoTelegram(1, "private"), "oi"));
	}

	@Test
	void entregaCadaAtualizacaoAoProcessadorEConfirmaPedindoOProximoId() throws Exception {
		when(telegram.buscarAtualizacoes(0, 25)).thenReturn(List.of(atualizacao(41), atualizacao(42)));
		when(telegram.buscarAtualizacoes(43, 25)).thenReturn(List.of());

		assertEquals(2, polling.rodada());
		assertEquals(0, polling.rodada());

		InOrder ordem = inOrder(processador, telegram);
		ordem.verify(telegram).buscarAtualizacoes(0, 25);
		ordem.verify(processador).aceitar(atualizacao(41));
		ordem.verify(processador).aceitar(atualizacao(42));
		ordem.verify(telegram).buscarAtualizacoes(43, 25);
	}

	@Test
	void oIdPedidoNuncaVoltaParaTras() throws Exception {
		when(telegram.buscarAtualizacoes(0, 25)).thenReturn(List.of(atualizacao(50)));
		when(telegram.buscarAtualizacoes(51, 25)).thenReturn(List.of(atualizacao(49)), List.of());

		polling.rodada();
		polling.rodada();
		polling.rodada();

		verify(telegram, times(1)).buscarAtualizacoes(0, 25);
		verify(telegram, times(2)).buscarAtualizacoes(51, 25);
		verify(telegram, never()).buscarAtualizacoes(eq(50L), anyInt());
	}

	@Test
	void falhaDoTelegramNaoPropagaEDevolveZero() throws Exception {
		when(telegram.buscarAtualizacoes(anyLong(), anyInt())).thenThrow(new FalhaNoTelegramException("fora do ar", 0));

		assertEquals(0, pacienciaCurta().rodada());

		verify(processador, never()).aceitar(any());
	}

	@Test
	void conflitoComWebhookEsperaMaisQueUmErroQualquer() {
		assertEquals(5_000, polling.pausaApos(new FalhaNoTelegramException("rede", 0)));
		assertEquals(5_000, polling.pausaApos(new FalhaNoTelegramException("erro", 500)));
		assertEquals(30_000, polling.pausaApos(new FalhaNoTelegramException("conflito", 409)));
	}

	/** O mesmo polling, só que esperando 1 ms depois de um erro, para o teste não demorar. */
	private TelegramPolling pacienciaCurta() {
		return new TelegramPolling(telegram, processador, new TelegramProperties(true, "polling", "123:ABC", "",
				"https://api.telegram.org", Duration.ofSeconds(1), Duration.ofSeconds(1), 25), 1, 1);
	}
}
