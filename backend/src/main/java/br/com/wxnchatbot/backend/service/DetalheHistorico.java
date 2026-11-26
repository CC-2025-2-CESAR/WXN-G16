package br.com.wxnchatbot.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.NivelUrgencia;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.TipoRemetente;

public record DetalheHistorico(
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
		long totalMensagens,
		List<MensagemHistorico> mensagens) {

	public record MensagemHistorico(TipoRemetente tipoRemetente, String conteudo, Instant enviadaEm) {
	}

	static DetalheHistorico de(Conversa conversa) {
		var mensagens = conversa.getMensagens().stream()
				.map(m -> new MensagemHistorico(m.getTipoRemetente(), m.getConteudo(), m.getEnviadaEm()))
				.toList();
		ItemHistorico item = ItemHistorico.de(conversa, mensagens.size());
		return new DetalheHistorico(item.conversaId(), item.clienteNome(), item.clienteTelefone(),
				item.motivo(), item.origem(), item.urgencia(), item.atendenteId(), item.atendenteNome(),
				item.iniciadaEm(), item.assumidaEm(), item.encerradaEm(), item.resumoEncerramento(),
				item.totalMensagens(), mensagens);
	}
}
