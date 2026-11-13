package br.com.wxnchatbot.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.wxnchatbot.backend.repository.ConversaRepositorio;
import br.com.wxnchatbot.backend.service.FilaConversasServico;

@RestController
@RequestMapping("/api/conversas")
public class ConversaController {

	private final FilaConversasServico servico;
	private final ConversaRepositorio conversas;

	public ConversaController(FilaConversasServico servico, ConversaRepositorio conversas) {
		this.servico = servico;
		this.conversas = conversas;
	}

	@GetMapping
	public List<ConversaResumo> listar() {
		return conversas.findAll().stream()
				.map(ConversaResumo::new)
				.toList();
	}

	@GetMapping("/{id}")
	public ConversaDetalhe detalhar(@PathVariable UUID id) {
		return conversas.findById(id)
				.map(ConversaDetalhe::new)
				.orElseThrow(() -> new br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException("Conversa", id));
	}

	@PostMapping("/{conversaId}/encerrar")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void encerrar(@PathVariable UUID conversaId) {
		servico.encerrar(conversaId);
	}
}