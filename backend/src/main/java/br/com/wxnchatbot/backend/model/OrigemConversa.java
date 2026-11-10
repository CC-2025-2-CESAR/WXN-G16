package br.com.wxnchatbot.backend.model;

/**
 * De onde a conversa veio. Serve para o atendente (e o grupo) saberem de relance se estão olhando dados fictícios, um
 * teste feito por pessoas do projeto ou um cliente de verdade.
 */
public enum OrigemConversa {

	/** Dado fictício criado pelo perfil `demo` (a "conversa mocada"), ou por testes automáticos. */
	DEMONSTRACAO,

	/** Conversa de teste, escrita por uma pessoa pelo simulador de WhatsApp ou pela API de chat. */
	SIMULADOR,

	/** Mensagem de um cliente pelo WhatsApp de verdade (Cloud API da Meta). */
	WHATSAPP
}
