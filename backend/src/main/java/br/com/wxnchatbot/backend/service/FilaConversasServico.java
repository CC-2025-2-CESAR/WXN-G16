package br.com.wxnchatbot.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.repository.AtendenteRepositorio;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;

@Service
public class FilaConversasServico {

	private final ConversaRepositorio conversaRepositorio;
	private final AtendenteRepositorio atendenteRepositorio;

	public FilaConversasServico(ConversaRepositorio conversaRepositorio, AtendenteRepositorio atendenteRepositorio) {
		this.conversaRepositorio = conversaRepositorio;
		this.atendenteRepositorio = atendenteRepositorio;
	}

	@Transactional(readOnly = true)
	public List<ConversaNaFila> visualizarFila() {
		List<Conversa> fila = conversaRepositorio.buscarFilaAguardando();
		List<ConversaNaFila> itens = new ArrayList<>(fila.size());
		for (int i = 0; i < fila.size(); i++) {
			itens.add(ConversaNaFila.de(fila.get(i), i + 1));
		}
		return itens;
	}

	@Transactional(readOnly = true)
	public DetalheConversaFila abrirConversaDaFila(UUID conversaId) {
		Conversa conversa = buscarConversa(conversaId);
		if (conversa.getStatus() != StatusConversa.AGUARDANDO_ATENDENTE) {
			throw new IllegalStateException("A conversa não está na fila de atendimento");
		}
		return DetalheConversaFila.de(conversa);
	}

	@Transactional
	public ConversaNaFila solicitarAtendente(UUID conversaId, String motivo) {
		Conversa conversa = buscarConversa(conversaId);
		conversa.solicitarAtendente(motivo);
		conversaRepositorio.save(conversa);
		return ConversaNaFila.de(conversa, posicaoNaFila(conversaId));
	}

	@Transactional
	public void atribuirAtendente(UUID conversaId, UUID atendenteId) {
		Conversa conversa = conversaRepositorio.buscarPorIdParaAtualizar(conversaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Conversa", conversaId));
		Atendente atendente = atendenteRepositorio.findById(atendenteId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Atendente", atendenteId));
		conversa.atribuirAtendente(atendente);
		conversaRepositorio.save(conversa);
	}

	@Transactional
	public void encerrar(UUID conversaId) {
		Conversa conversa = buscarConversa(conversaId);
		conversa.encerrar();
		conversaRepositorio.save(conversa);
	}

	private Conversa buscarConversa(UUID conversaId) {
		return conversaRepositorio.findById(conversaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Conversa", conversaId));
	}

	private int posicaoNaFila(UUID conversaId) {
		List<Conversa> fila = conversaRepositorio.buscarFilaAguardando();
		for (int i = 0; i < fila.size(); i++) {
			if (fila.get(i).getId().equals(conversaId)) {
				return i + 1;
			}
		}
		throw new IllegalStateException("A conversa não apareceu na fila após solicitar atendente: " + conversaId);
	}
}