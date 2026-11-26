package br.com.wxnchatbot.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.wxnchatbot.backend.autenticacao.AtendenteAutenticado;
import br.com.wxnchatbot.backend.service.AtendimentoAtivo;
import br.com.wxnchatbot.backend.service.AtendimentosAtivosServico;

@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentosAtivosController {

	private final AtendimentosAtivosServico servico;
	private final AtendenteAutenticado atendenteAutenticado;

	public AtendimentosAtivosController(AtendimentosAtivosServico servico, AtendenteAutenticado atendenteAutenticado) {
		this.servico = servico;
		this.atendenteAutenticado = atendenteAutenticado;
	}

	@GetMapping("/ativos")
	public List<AtendimentoAtivo> listarAtivos() {
		return servico.listar(atendenteAutenticado.id());
	}
}
