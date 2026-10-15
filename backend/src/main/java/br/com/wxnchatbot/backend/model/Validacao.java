package br.com.wxnchatbot.backend.model;

final class Validacao {

	private Validacao() {
	}

	static String textoObrigatorio(String valor, String campo) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException(campo + " é obrigatório");
		}
		return valor.strip();
	}
}
