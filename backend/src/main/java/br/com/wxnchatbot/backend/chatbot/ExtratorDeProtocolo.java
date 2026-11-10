package br.com.wxnchatbot.backend.chatbot;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/** Acha o protocolo de uma demanda (como WXN-2026-0001) no texto; quem escolhe o número é o código, nunca a LLM. */
@Component
public class ExtratorDeProtocolo {

	private static final Pattern CANDIDATO = Pattern.compile("(?<![A-Za-z0-9_-])[A-Za-z0-9]+(?:[-_][A-Za-z0-9]+)+(?![A-Za-z0-9_-])");

	public Optional<String> extrair(String texto) {
		if (texto == null) {
			return Optional.empty();
		}
		Matcher encontrado = CANDIDATO.matcher(texto);
		while (encontrado.find()) {
			String candidato = encontrado.group();
			boolean temNumero = candidato.chars().anyMatch(Character::isDigit);
			boolean temLetra = candidato.chars().anyMatch(Character::isLetter);
			if (temNumero && temLetra && candidato.length() >= 3 && candidato.length() <= 40) {
				return Optional.of(candidato.toUpperCase(Locale.ROOT));
			}
		}
		return Optional.empty();
	}
}
