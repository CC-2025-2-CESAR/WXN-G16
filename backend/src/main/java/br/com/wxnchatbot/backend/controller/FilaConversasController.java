package br.com.wxnchatbot.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.wxnchatbot.backend.service.ConversaNaFila;
import br.com.wxnchatbot.backend.service.DetalheConversaFila;
import br.com.wxnchatbot.backend.service.FilaConversasServico;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/fila")
public class FilaConversasController {

	private final FilaConversasServico servico;

	public FilaConversasController(FilaConversasServico servico) {
		this.servico = servico;
	}

	@GetMapping
	public List<ConversaNaFila> visualizarFila() {
		return servico.visualizarFila();
	}

	@GetMapping("/{conversaId}")
	public DetalheConversaFila abrirConversa(@PathVariable UUID conversaId) {
		return servico.abrirConversaDaFila(conversaId);
	}

	@PostMapping("/{conversaId}/assumir")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void assumir(@PathVariable UUID conversaId, @Valid @RequestBody AssumirConversaRequisicao requisicao) {
		servico.atribuirAtendente(conversaId, requisicao.atendenteId());
	}
}
