package br.com.wxnchatbot.backend.canal.twilio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Guarda os ids das últimas mensagens recebidas. O Twilio repete a chamada quando o webhook demora a responder (e o
 * Azure F1 leva uns 30 s para acordar), então a mesma mensagem pode chegar duas vezes. Fica só em memória: com mais de
 * uma instância do backend, ou depois de uma reinicialização, uma repetição ainda pode passar.
 */
final class MensagensVistas {

	private final Set<String> ids;

	MensagensVistas(int limite) {
		this.ids = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>(16, 0.75f, false) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Boolean> maisAntiga) {
				return size() > limite;
			}
		});
	}

	/** Devolve verdadeiro se é a primeira vez que o id aparece. */
	synchronized boolean registrar(String id) {
		return ids.add(id);
	}
}
