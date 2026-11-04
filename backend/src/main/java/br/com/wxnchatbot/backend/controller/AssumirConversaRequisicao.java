package br.com.wxnchatbot.backend.controller;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssumirConversaRequisicao(@NotNull(message = "é obrigatório") UUID atendenteId) {
}
