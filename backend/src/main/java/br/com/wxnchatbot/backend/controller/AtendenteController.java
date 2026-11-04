package br.com.wxnchatbot.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.wxnchatbot.backend.service.AtendenteResumo;
import br.com.wxnchatbot.backend.service.AtendenteServico;

@RestController
@RequestMapping("/api/atendentes")
public class AtendenteController {

	private final AtendenteServico servico;

	public AtendenteController(AtendenteServico servico) {
		this.servico = servico;
	}

	@GetMapping
	public List<AtendenteResumo> listar() {
		return servico.listar();
	}
}
