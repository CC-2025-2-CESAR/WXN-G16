package br.com.wxnchatbot.backend.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextoRequisicao(
		@NotBlank(message = "é obrigatório") @Size(max = 4000, message = "deve ter no máximo 4000 caracteres") String texto) {
}
