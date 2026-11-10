package br.com.wxnchatbot.backend.chatbot;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

/** Ferramentas para comparar mensagens de cliente sem se preocupar com acento, maiúscula ou pontuação. */
final class Texto {

	private static final List<String> NEGACOES = List.of("nao", "sem", "nem", "nunca");

	private Texto() {
	}

	/** Minúsculas, sem acento, pontuação trocada por espaço, com um espaço no começo e outro no fim. */
	static String normalizar(String texto) {
		if (texto == null) {
			return "  ";
		}
		String semAcento = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		String simples = semAcento.replaceAll("[^a-z0-9 ]", " ").replaceAll(" +", " ").strip();
		return " " + simples + " ";
	}

	/** Diz se a frase (já normalizada, sem espaços nas pontas) aparece inteira no texto. */
	static boolean contemFrase(String normalizado, String frase) {
		return normalizado.contains(" " + frase + " ");
	}

	/** Diz se a frase (já normalizada, sem espaços nas pontas) aparece inteira, sem uma negação nas 3 palavras antes. */
	static boolean contemSemNegar(String normalizado, String frase) {
		String busca = " " + frase + " ";
		int posicao = normalizado.indexOf(busca);
		while (posicao != -1) {
			if (!temNegacaoAntes(normalizado, posicao)) {
				return true;
			}
			posicao = normalizado.indexOf(busca, posicao + 1);
		}
		return false;
	}

	static boolean contemAlguma(String normalizado, List<String> frases) {
		return frases.stream().anyMatch(frase -> contemSemNegar(normalizado, frase));
	}

	static boolean contemPalavra(String normalizado, String palavra) {
		return normalizado.contains(" " + palavra + " ");
	}

	static int quantidadeDePalavras(String normalizado) {
		String simples = normalizado.strip();
		return simples.isEmpty() ? 0 : simples.split(" ").length;
	}

	private static boolean temNegacaoAntes(String normalizado, int posicao) {
		String antes = normalizado.substring(0, posicao).strip();
		if (antes.isEmpty()) {
			return false;
		}
		String[] palavras = antes.split(" ");
		return Arrays.stream(palavras, Math.max(0, palavras.length - 3), palavras.length).anyMatch(NEGACOES::contains);
	}
}
