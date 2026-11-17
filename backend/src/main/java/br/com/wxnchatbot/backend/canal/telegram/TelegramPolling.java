package br.com.wxnchatbot.backend.canal.telegram;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Modo {@code polling}: em vez de esperar o Telegram chamar um endereço nosso, o backend pergunta a ele, em loop, se há
 * mensagens. Serve para testar na máquina, sem endereço público. O Telegram não deixa usar polling e webhook ao mesmo
 * tempo no mesmo bot, então use um bot só para testes locais.
 */
@Component
@ConditionalOnExpression("${telegram.habilitado:false} && '${telegram.modo:webhook}'.equalsIgnoreCase('polling')")
public class TelegramPolling {

	private static final Logger log = LoggerFactory.getLogger(TelegramPolling.class);
	private static final long PAUSA_APOS_ERRO_MS = 5_000;
	private static final long PAUSA_APOS_CONFLITO_MS = 30_000;

	private final TelegramClient telegram;
	private final ProcessadorDeAtualizacoesTelegram processador;
	private final TelegramProperties propriedades;
	private final long pausaAposErro;
	private final long pausaAposConflito;

	private volatile boolean ativo;
	private Thread fio;
	private long proximo;

	@Autowired
	public TelegramPolling(TelegramClient telegram, ProcessadorDeAtualizacoesTelegram processador,
			TelegramProperties propriedades) {
		this(telegram, processador, propriedades, PAUSA_APOS_ERRO_MS, PAUSA_APOS_CONFLITO_MS);
	}

	TelegramPolling(TelegramClient telegram, ProcessadorDeAtualizacoesTelegram processador, TelegramProperties propriedades,
			long pausaAposErro, long pausaAposConflito) {
		this.telegram = telegram;
		this.processador = processador;
		this.propriedades = propriedades;
		this.pausaAposErro = pausaAposErro;
		this.pausaAposConflito = pausaAposConflito;
	}

	@PostConstruct
	void iniciar() {
		ativo = true;
		fio = Thread.ofVirtual().name("telegram-polling").start(this::repetir);
		log.info("Telegram em modo polling: perguntando ao Telegram por mensagens novas");
	}

	@PreDestroy
	void parar() {
		ativo = false;
		if (fio != null) {
			fio.interrupt();
		}
	}

	private void repetir() {
		while (ativo) {
			try {
				rodada();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	/**
	 * Uma rodada: busca, entrega ao processador e confirma (o próximo pedido começa depois da última atualização).
	 * Depois de um erro, espera um pouco antes de tentar de novo. Devolve quantas atualizações vieram.
	 */
	int rodada() throws InterruptedException {
		try {
			List<AtualizacaoDoTelegram> atualizacoes = telegram.buscarAtualizacoes(proximo, propriedades.esperaDoPolling());
			for (AtualizacaoDoTelegram atualizacao : atualizacoes) {
				processador.aceitar(atualizacao);
				proximo = Math.max(proximo, atualizacao.updateId() + 1);
			}
			return atualizacoes.size();
		} catch (FalhaNoTelegramException e) {
			log.error("Falha no polling do Telegram: {}", e.getMessage());
			Thread.sleep(pausaApos(e));
			return 0;
		}
	}

	/** O conflito (409) quase sempre é um webhook ativo ou outro processo lendo o bot: insistir a cada poucos segundos só enche o log. */
	long pausaApos(FalhaNoTelegramException falha) {
		return falha.status() == 409 ? pausaAposConflito : pausaAposErro;
	}
}
