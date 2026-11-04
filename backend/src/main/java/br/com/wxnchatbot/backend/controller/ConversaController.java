package br.com.wxnchatbot.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.wxnchatbot.backend.service.FilaConversasServico;

@RestController
@RequestMapping("/api/conversas")
public class ConversaController {

	private final FilaConversasServico servico;

	public ConversaController(FilaConversasServico servico) {
		this.servico = servico;
	}

	@PostMapping("/{conversaId}/encerrar")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void encerrar(@PathVariable UUID conversaId) {
		servico.encerrar(conversaId);
	}
}
