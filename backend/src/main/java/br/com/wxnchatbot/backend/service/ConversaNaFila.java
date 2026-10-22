package br.com.wxnchatbot.backend.service;

import java.time.Instant;
import java.util.UUID;

import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Ticket;

public record ConversaNaFila(
		int posicao,
		UUID conversaId,
		String clienteNome,
		String clienteTelefone,
		String motivo,
		Instant abertoEm) {

	static ConversaNaFila de(Conversa conversa, int posicao) {
		Ticket ticket = conversa.getTicket()
				.orElseThrow(() -> new IllegalStateException("Conversa na fila sem ticket: " + conversa.getId()));
		Cliente cliente = conversa.getCliente();
		return new ConversaNaFila(posicao, conversa.getId(), cliente.getNome(), cliente.getTelefone(),
				ticket.getMotivo(), ticket.getAbertoEm());
	}
}