package br.com.wxnchatbot.backend.controller;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.wxnchatbot.backend.service.ChatServico;
import br.com.wxnchatbot.backend.service.OrquestradorConversaServico;
import br.com.wxnchatbot.backend.service.RespostaChat;
import br.com.wxnchatbot.backend.service.ResultadoTurno;
import jakarta.validation.Valid;

/**
 * Entrada do chatbot pela API, para o simulador de chat e para testes. Cada chamada gasta a cota da LLM e cria dados,
 * então só existe com chat.api.habilitada=true (ligado no perfil dev, desligado em produção).
 */
@RestController
@ConditionalOnProperty(name = "chat.api.habilitada", havingValue = "true")
public class ChatController {

	private final ChatServico chat;
	private final OrquestradorConversaServico orquestrador;

	public ChatController(ChatServico chat, OrquestradorConversaServico orquestrador) {
		this.chat = chat;
		this.orquestrador = orquestrador;
	}

	/** Mensagem de um telefone: acha ou abre a conversa dele e devolve a resposta da Lily. */
	@PostMapping("/api/chat/mensagens")
	public RespostaChat receber(@Valid @RequestBody MensagemDoCanalRequisicao requisicao) {
		return chat.receber(requisicao.telefone(), requisicao.nome(), requisicao.texto());
	}

	/** Mensagem do cliente numa conversa que já existe. */
	@PostMapping("/api/conversas/{conversaId}/mensagens")
	public RespostaChat enviar(@PathVariable UUID conversaId, @Valid @RequestBody TextoRequisicao requisicao) {
		ResultadoTurno resultado = orquestrador.processarTurno(conversaId, requisicao.texto());
		return new RespostaChat(conversaId, resultado.resposta(), resultado.status());
	}
}
