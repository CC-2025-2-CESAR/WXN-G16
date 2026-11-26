package br.com.wxnchatbot.backend.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.service.DetalheHistorico;
import br.com.wxnchatbot.backend.service.HistoricoServico;
import br.com.wxnchatbot.backend.service.PaginaHistorico;

@RestController
@RequestMapping("/api/historico")
public class HistoricoController {
	private final HistoricoServico servico;

	public HistoricoController(HistoricoServico servico) {
		this.servico = servico;
	}

	@GetMapping
	public PaginaHistorico listar(@RequestParam(required = false) String busca,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
			@RequestParam(required = false) UUID atendenteId,
			@RequestParam(required = false) OrigemConversa origem,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanho) {
		return servico.listar(busca, dataInicial, dataFinal, atendenteId, origem, pagina, tamanho);
	}

	@GetMapping("/{id}")
	public DetalheHistorico detalhar(@PathVariable UUID id) {
		return servico.detalhar(id);
	}
}
