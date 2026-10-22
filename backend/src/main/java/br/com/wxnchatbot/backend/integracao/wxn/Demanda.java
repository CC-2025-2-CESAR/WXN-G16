package br.com.wxnchatbot.backend.integracao.wxn;

import java.time.Instant;

public record Demanda(
		String protocolo,
		String titulo,
		StatusDemanda status,
		Instant atualizadaEm,
		String observacao) {
}
