package br.com.wxnchatbot.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.wxnchatbot.backend.service.AtendimentoServico;
import br.com.wxnchatbot.backend.service.DetalheConversaFila.MensagemResumo;
import br.com.wxnchatbot.backend.service.ResultadoDoEnvio;
import jakarta.validation.Valid;

/** A conversa do atendente com o cliente: ler o histórico ao vivo e responder. */
@RestController
@RequestMapping("/api/conversas/{conversaId}/atendimento/mensagens")
public class AtendimentoController {

	private final AtendimentoServico servico;

	public AtendimentoController(AtendimentoServico servico) {
		this.servico = servico;
	}

	/** Todas as mensagens da conversa, da mais antiga para a mais nova. O dashboard consulta isto a cada poucos segundos. */
	@GetMapping
	public List<MensagemResumo> listar(@PathVariable UUID conversaId) {
		return servico.listarMensagens(conversaId);
	}

	/** Resposta do atendente que assumiu a conversa. 409 se a conversa não está em atendimento. */
	@PostMapping
	public ResultadoDoEnvio enviar(@PathVariable UUID conversaId, @Valid @RequestBody TextoRequisicao requisicao) {
		return servico.enviarMensagem(conversaId, requisicao.texto());
	}
}
