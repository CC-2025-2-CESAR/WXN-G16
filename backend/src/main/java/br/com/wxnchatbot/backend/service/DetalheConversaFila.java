package br.com.wxnchatbot.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Ticket;
import br.com.wxnchatbot.backend.model.TipoRemetente;

public record DetalheConversaFila(
		UUID conversaId,
		String clienteNome,
		String clienteTelefone,
		String motivo,
		Instant abertoEm,
		List<MensagemResumo> mensagens) {

	public record MensagemResumo(TipoRemetente tipoRemetente, String conteudo, Instant enviadaEm) {
	}

	static DetalheConversaFila de(Conversa conversa) {
		Ticket ticket = conversa.getTicket()
				.orElseThrow(() -> new IllegalStateException("Conversa na fila sem ticket: " + conversa.getId()));
		Cliente cliente = conversa.getCliente().orElseThrow(() -> new IllegalStateException("Conversa sem cliente vinculado"));
		List<MensagemResumo> mensagens = conversa.getMensagens().stream()
				.map(m -> new MensagemResumo(m.getTipoRemetente(), m.getConteudo(), m.getEnviadaEm()))
				.toList();
		return new DetalheConversaFila(conversa.getId(), cliente.getNome(), cliente.getTelefone(),
				ticket.getMotivo(), ticket.getAbertoEm(), mensagens);
	}
}