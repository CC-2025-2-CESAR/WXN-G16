package br.com.wxnchatbot.backend.canal.telegram;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Guarda os ids das últimas atualizações recebidas. O Telegram repete a entrega do webhook quando não ouve um 200 a
 * tempo (e o Azure F1 leva uns 30 s para acordar), e o polling reenvia o que não foi confirmado. Fica só em memória:
 * depois de uma reinicialização, uma repetição ainda pode passar.
 */
final class AtualizacoesJaProcessadas {

	private final Set<Long> ids;

	AtualizacoesJaProcessadas(int limite) {
		this.ids = Collections.newSetFromMap(new LinkedHashMap<Long, Boolean>(16, 0.75f, false) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<Long, Boolean> maisAntiga) {
				return size() > limite;
			}
		});
	}

	/** Devolve verdadeiro se é a primeira vez que o id aparece. */
	synchronized boolean registrar(long id) {
		return ids.add(id);
	}
}
