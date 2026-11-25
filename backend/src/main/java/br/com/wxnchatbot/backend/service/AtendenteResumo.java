package br.com.wxnchatbot.backend.service;

import java.util.UUID;

import br.com.wxnchatbot.backend.model.Atendente;

public record AtendenteResumo(UUID id, String nome, String email, boolean disponivel) {

	public static AtendenteResumo de(Atendente atendente) {
		return new AtendenteResumo(atendente.getId(), atendente.getNome(), atendente.getEmail(),
				atendente.isDisponivel());
	}
}
