package br.com.wxnchatbot.backend.service;

import br.com.wxnchatbot.backend.model.OrigemConversa;

/**
 * Por onde uma mensagem do atendente chega ao cliente. Cada canal que sabe falar com o cliente (hoje o Telegram)
 * implementa esta interface; as conversas de uma origem sem canal (simulador, demonstração, WhatsApp) só guardam a
 * mensagem no histórico.
 */
public interface CanalDeSaida {

	/** A origem das conversas que este canal atende. */
	OrigemConversa origem();

	/**
	 * Entrega o texto ao cliente. {@code identificador} é o que identifica o cliente no canal (a coluna do telefone: um
	 * telefone ou, no Telegram, {@code tg:<id>}).
	 *
	 * @throws FalhaNaEntregaException quando o canal não conseguiu entregar; a mensagem já é um aviso pronto para o atendente
	 */
	void entregar(String identificador, String texto);
}
