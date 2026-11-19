package br.com.wxnchatbot.backend.canal.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.service.CanalDeSaida;
import br.com.wxnchatbot.backend.service.FalhaNaEntregaException;

/**
 * Entrega ao cliente, pelo Telegram, a mensagem que o atendente escreveu no dashboard. O cliente do Telegram é
 * identificado por {@code tg:<id do chat>}, e o id do chat é onde o {@code sendMessage} responde.
 */
@Component
@ConditionalOnProperty(name = "telegram.habilitado", havingValue = "true")
public class CanalDeSaidaTelegram implements CanalDeSaida {

	private static final Logger log = LoggerFactory.getLogger(CanalDeSaidaTelegram.class);
	private static final String PREFIXO = "tg:";

	private final TelegramClient telegram;

	public CanalDeSaidaTelegram(TelegramClient telegram) {
		this.telegram = telegram;
	}

	@Override
	public OrigemConversa origem() {
		return OrigemConversa.TELEGRAM;
	}

	@Override
	public void entregar(String identificador, String texto) {
		long chatId = chatDe(identificador);
		try {
			telegram.enviarTexto(chatId, texto);
		} catch (FalhaNoTelegramException e) {
			// A mensagem da falha já vem sem o token (veja FalhaNoTelegramException).
			log.warn("O Telegram recusou a entrega (HTTP {}): {}", e.status(), e.getMessage());
			throw new FalhaNaEntregaException(e.status() == 403
					? "O Telegram não aceitou a entrega (o cliente pode ter bloqueado o bot). A mensagem foi salva no histórico."
					: "Não foi possível entregar pelo Telegram agora. A mensagem foi salva no histórico; tente enviar de novo.");
		}
	}

	private static long chatDe(String identificador) {
		if (identificador == null || !Cliente.ehIdentificadorDoTelegram(identificador)) {
			throw new FalhaNaEntregaException(
					"Este cliente não tem um chat do Telegram registrado. A mensagem foi salva no histórico, mas não foi entregue.");
		}
		try {
			return Long.parseLong(identificador.substring(PREFIXO.length()));
		} catch (NumberFormatException e) {
			throw new FalhaNaEntregaException(
					"O identificador do cliente no Telegram é inválido. A mensagem foi salva no histórico, mas não foi entregue.");
		}
	}
}
