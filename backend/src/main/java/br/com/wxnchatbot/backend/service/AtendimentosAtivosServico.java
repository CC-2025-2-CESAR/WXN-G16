package br.com.wxnchatbot.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.repository.ConversaRepositorio;

@Service
public class AtendimentosAtivosServico {

	private final ConversaRepositorio conversas;

	public AtendimentosAtivosServico(ConversaRepositorio conversas) {
		this.conversas = conversas;
	}

	@Transactional(readOnly = true)
	public List<AtendimentoAtivo> listar(UUID atendenteId) {
		return conversas.buscarAtendimentosAtivos(atendenteId).stream()
				.map(AtendimentoAtivo::de)
				.toList();
	}
}
