package br.com.wxnchatbot.backend.controller;

import java.util.UUID;

import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.StatusConversa;

public record ConversaResumo(
        UUID id,
        String clienteNome,
        StatusConversa status
) {
    public ConversaResumo(Conversa conversa) {
        this(
                conversa.getId(),
                conversa.getCliente().isPresent() ? conversa.getCliente().get().getNome() : "Anónimo",
                conversa.getStatus()
        );
    }
}