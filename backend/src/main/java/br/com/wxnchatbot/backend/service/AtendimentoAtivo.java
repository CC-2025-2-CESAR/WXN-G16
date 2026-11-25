package br.com.wxnchatbot.backend.service;

import java.time.Instant;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Conversa;

/** Dados necessários para retomar um atendimento no dashboard após recarregar a página. */
public record AtendimentoAtivo(
		DetalheConversaFila detalhe,
		AtendenteResumo atendente,
		Instant assumidaEm) {

	static AtendimentoAtivo de(Conversa conversa) {
		Atendente responsavel = conversa.getAtendente()
				.orElseThrow(() -> new IllegalStateException("Conversa em atendimento sem atendente"));
		return new AtendimentoAtivo(
				DetalheConversaFila.de(conversa),
				AtendenteResumo.de(responsavel),
				conversa.getAssumidaEm().orElse(null));
	}
}
