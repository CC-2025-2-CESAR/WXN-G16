package br.com.wxnchatbot.backend.service;

import br.com.wxnchatbot.backend.service.DetalheConversaFila.MensagemResumo;

/**
 * O que aconteceu com a mensagem do atendente: ela sempre fica no histórico; {@code entregue} diz se também chegou ao
 * cliente. Quando não chegou, {@code aviso} explica por quê (em português, para mostrar ao atendente).
 */
public record ResultadoDoEnvio(MensagemResumo mensagem, boolean entregue, String aviso) {
}
