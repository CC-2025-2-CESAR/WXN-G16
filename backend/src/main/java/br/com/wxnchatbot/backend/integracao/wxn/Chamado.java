package br.com.wxnchatbot.backend.integracao.wxn;

import java.time.Instant;

public record Chamado(
		String protocolo,
		String titulo,
		StatusDemanda status,
		Instant criadoEm) {
}
