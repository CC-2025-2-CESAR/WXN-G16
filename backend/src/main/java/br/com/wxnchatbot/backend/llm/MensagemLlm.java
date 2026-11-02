package br.com.wxnchatbot.backend.llm;

import java.util.Objects;

public record MensagemLlm(Papel papel, String conteudo) {

	public enum Papel {
		USUARIO, ASSISTENTE
	}

	public MensagemLlm {
		Objects.requireNonNull(papel, "papel é obrigatório");
		Objects.requireNonNull(conteudo, "conteúdo é obrigatório");
	}
}