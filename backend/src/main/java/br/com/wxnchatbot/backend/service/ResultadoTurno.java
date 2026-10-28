package br.com.wxnchatbot.backend.service;

import br.com.wxnchatbot.backend.model.StatusConversa;

/** Resposta da Lily (nula quando a conversa está com um humano) e o status da conversa depois do turno. */
public record ResultadoTurno(String resposta, StatusConversa status) {
}