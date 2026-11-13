package br.com.wxnchatbot.backend.chatbot;

import java.util.Locale;
import java.util.regex.Pattern;

import br.com.wxnchatbot.backend.llm.PromptDaLily;

/**
 * Confere a resposta livre da LLM antes de ela ir para o cliente: não pode vazar o prompt de sistema nem prometer
 * uma ação que a Lily não faz sozinha.
 */
public final class FiltroDeSaida {

	private static final int TAMANHO_MINIMO_DA_LINHA = 30;

	private static final Pattern PROMESSA = Pattern.compile(
			"\\b(abri|criei|registrei|cancelei|alterei|atualizei|encerrei)\\b.{0,30}\\b(chamado|demanda|ticket|protocolo)\\b"
					+ "|\\bvou (abrir|criar|cancelar|alterar|atualizar)\\b",
			Pattern.CASE_INSENSITIVE);

	private FiltroDeSaida() {
	}

	public static boolean seguro(String texto) {
		return !vazaOPrompt(texto) && !fazPromessa(texto);
	}

	/** Vaza se a resposta contém uma linha inteira do prompt (as linhas curtas são ignoradas). */
	static boolean vazaOPrompt(String texto) {
		String minusculo = texto.toLowerCase(Locale.ROOT);
		for (String linha : PromptDaLily.TEXTO.split("\\R")) {
			String trecho = linha.strip().replaceFirst("^- ", "").toLowerCase(Locale.ROOT);
			if (trecho.length() >= TAMANHO_MINIMO_DA_LINHA && minusculo.contains(trecho)) {
				return true;
			}
		}
		return false;
	}

	static boolean fazPromessa(String texto) {
		return PROMESSA.matcher(texto).find();
	}
}