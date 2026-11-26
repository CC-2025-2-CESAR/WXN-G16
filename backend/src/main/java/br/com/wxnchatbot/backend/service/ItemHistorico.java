package br.com.wxnchatbot.backend.service;

import java.time.Instant;
import java.util.UUID;

import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.NivelUrgencia;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.Ticket;

public record ItemHistorico(
		UUID conversaId,
		String clienteNome,
		String clienteTelefone,
		String motivo,
		OrigemConversa origem,
		NivelUrgencia urgencia,
		UUID atendenteId,
		String atendenteNome,
		Instant iniciadaEm,
		Instant assumidaEm,
		Instant encerradaEm,
		String resumoEncerramento,
		long totalMensagens) {

	static ItemHistorico de(Conversa conversa, long totalMensagens) {
		Ticket ticket = conversa.getTicket().orElse(null);
		var atendente = conversa.getAtendente().orElse(null);
		return new ItemHistorico(
				conversa.getId(),
				conversa.getCliente().getNome(),
				conversa.getCliente().getTelefone(),
				ticket == null ? "Conversa com a Lily" : ticket.getMotivo(),
				conversa.getOrigem(),
				ticket == null ? null : ticket.getUrgencia(),
				atendente == null ? null : atendente.getId(),
				atendente == null ? null : atendente.getNome(),
				conversa.getIniciadaEm(),
				conversa.getAssumidaEm().orElse(null),
				conversa.getEncerradaEm().orElse(null),
				conversa.getResumoEncerramento().orElse(null),
				totalMensagens);
	}
}
