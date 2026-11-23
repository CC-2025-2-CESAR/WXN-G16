package br.com.wxnchatbot.backend.service;

import java.time.Instant;
import java.util.UUID;

import br.com.wxnchatbot.backend.model.NivelUrgencia;

public record TicketAberto(UUID ticketId, UUID conversaId, NivelUrgencia urgencia, String motivo, Instant abertoEm) {
}