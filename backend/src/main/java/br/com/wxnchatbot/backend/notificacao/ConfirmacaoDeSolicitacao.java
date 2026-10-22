package br.com.wxnchatbot.backend.notificacao;

import java.time.Instant;
import java.util.Objects;

public record ConfirmacaoDeSolicitacao(String emailDestino, String protocolo, String resumo, Instant registradaEm) {

	public ConfirmacaoDeSolicitacao {
		emailDestino = emailDestino == null ? "" : emailDestino.strip();
		if (!emailDestino.contains("@")) {
			throw new IllegalArgumentException("e-mail de destino inválido");
		}
		if (protocolo == null || protocolo.isBlank()) {
			throw new IllegalArgumentException("protocolo é obrigatório");
		}
		resumo = resumo == null ? "" : resumo.strip();
		Objects.requireNonNull(registradaEm, "data de registro é obrigatória");
	}
}
