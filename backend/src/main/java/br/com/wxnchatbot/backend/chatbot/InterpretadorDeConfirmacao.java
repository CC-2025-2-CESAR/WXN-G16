package br.com.wxnchatbot.backend.chatbot;

import java.util.Arrays;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Entende o "sim" ou o "não" do cliente depois que a Lily pergunta se pode abrir o chamado. Fica de fora da LLM de
 * propósito: abrir um chamado grava na API da WXN, então só um sim claro, feito apenas de palavras de concordância,
 * confirma. Qualquer outra coisa não abre o chamado.
 */
@Component
public class InterpretadorDeConfirmacao {

	public enum Resposta {
		SIM, NAO, INDEFINIDA
	}

	private static final Set<String> NEGATIVAS = Set.of("nao", "cancela", "cancelar", "cancele", "deixa", "desisto",
			"desistir", "nunca");

	private static final Set<String> AFIRMATIVAS = Set.of("sim", "s", "pode", "confirmo", "confirma", "confirmar",
			"isso", "ok", "okay", "certo", "claro", "quero", "beleza", "positivo", "uhum", "aham", "fechado");

	private static final Set<String> ENFEITES = Set.of("por", "favor", "pf", "pfv", "abrir", "abre", "abra", "mesmo",
			"com", "certeza", "tudo", "bem", "obrigado", "obrigada", "valeu", "o", "a", "e", "pode", "sim");

	public Resposta interpretar(String mensagem) {
		String[] palavras = Texto.normalizar(mensagem).strip().split(" ");
		if (Arrays.stream(palavras).anyMatch(NEGATIVAS::contains)) {
			return Resposta.NAO;
		}
		boolean soConcordancia = Arrays.stream(palavras).allMatch(p -> AFIRMATIVAS.contains(p) || ENFEITES.contains(p));
		boolean afirma = Arrays.stream(palavras).anyMatch(AFIRMATIVAS::contains);
		return soConcordancia && afirma ? Resposta.SIM : Resposta.INDEFINIDA;
	}
}
