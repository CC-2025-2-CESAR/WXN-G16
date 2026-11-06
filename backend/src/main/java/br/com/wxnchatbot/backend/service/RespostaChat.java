package br.com.wxnchatbot.backend.service;

import java.util.UUID;

import br.com.wxnchatbot.backend.model.StatusConversa;

/** O que o canal (simulador, WhatsApp) devolve ao cliente: a conversa, a resposta da Lily (nula se está com um humano) e o status. */
public record RespostaChat(UUID conversaId, String resposta, StatusConversa status) {
}
