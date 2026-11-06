package br.com.wxnchatbot.backend.chatbot;

import java.util.Objects;

/**
 * A intenção escolhida e o quanto o classificador confia nela (de 0 a 1). Para ABRIR_CHAMADO, um classificador que
 * entende o texto também diz se o cliente já descreveu o problema com detalhe suficiente; null quer dizer que ele não
 * opinou (as regras, por exemplo).
 */
public record ResultadoIntencao(Intencao intencao, double confianca, Boolean descricaoSuficiente) {

	public ResultadoIntencao {
		Objects.requireNonNull(intencao, "intenção é obrigatória");
		if (Double.isNaN(confianca) || confianca < 0 || confianca > 1) {
			throw new IllegalArgumentException("confiança deve estar entre 0 e 1");
		}
	}

	public ResultadoIntencao(Intencao intencao, double confianca) {
		this(intencao, confianca, null);
	}

	public static ResultadoIntencao certa(Intencao intencao) {
		return new ResultadoIntencao(intencao, 1.0);
	}
}
