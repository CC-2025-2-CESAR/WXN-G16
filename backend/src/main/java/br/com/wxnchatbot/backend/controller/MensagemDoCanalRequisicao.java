package br.com.wxnchatbot.backend.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mensagem de um cliente identificado pelo telefone; o nome é opcional e só vale na primeira mensagem. */
public record MensagemDoCanalRequisicao(
		@NotBlank(message = "é obrigatório") String telefone,
		@Size(max = 255, message = "deve ter no máximo 255 caracteres") String nome,
		@NotBlank(message = "é obrigatório") @Size(max = 4000, message = "deve ter no máximo 4000 caracteres") String texto) {
}
