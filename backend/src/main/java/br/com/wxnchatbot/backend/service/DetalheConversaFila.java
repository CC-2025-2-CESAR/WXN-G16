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
		
		String nome = conversa.getCliente().map(Cliente::getNome).orElse("Não identificado");
		String telefone = conversa.getCliente().map(Cliente::getTelefone).orElse("Não informado");

		List<MensagemResumo> mensagens = conversa.getMensagens().stream()
				.map(m -> new MensagemResumo(m.getTipoRemetente(), m.getConteudo(), m.getEnviadaEm()))
				.toList();

		return new DetalheConversaFila(conversa.getId(), nome, telefone,
				ticket.getMotivo(), ticket.getAbertoEm(), mensagens);
	}
}