package br.com.wxnchatbot.backend.canal.telegram;

import java.util.List;

public interface TelegramClient {

	/** Envia um texto ao chat, dividindo-o se passar do limite de 4096 caracteres do Telegram. */
	void enviarTexto(long chatId, String texto);

	/**
	 * Pergunta ao Telegram se há mensagens novas (long polling). {@code offset} é o id da primeira atualização
	 * desejada: pedir {@code ultimo + 1} confirma as anteriores. Espera até {@code esperaEmSegundos} por uma mensagem.
	 */
	List<AtualizacaoDoTelegram> buscarAtualizacoes(long offset, int esperaEmSegundos);
}
