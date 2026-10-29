package br.com.wxnchatbot.experimento;

import java.util.List;

public record Pergunta(int numero, String categoria, String texto, Esperado esperado, List<String> deveConter,
		List<String> deveConterAlgum, List<String> naoDeveConter) {

	public enum Esperado {
		CONSULTAR_STATUS,
		SOLICITAR_ABERTURA,
		SEM_FERRAMENTA,
		INDIFERENTE
	}

	public static Pergunta de(int numero, String categoria, String texto, Esperado esperado) {
		return new Pergunta(numero, categoria, texto, esperado, List.of(), List.of(), List.of());
	}

	public Pergunta conter(String... termos) {
		return new Pergunta(numero, categoria, texto, esperado, List.of(termos), deveConterAlgum, naoDeveConter);
	}

	public Pergunta contemAlgum(String... termos) {
		return new Pergunta(numero, categoria, texto, esperado, deveConter, List.of(termos), naoDeveConter);
	}

	public Pergunta semConter(String... termos) {
		return new Pergunta(numero, categoria, texto, esperado, deveConter, deveConterAlgum, List.of(termos));
	}
}
