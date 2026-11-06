package br.com.wxnchatbot.backend.chatbot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Texto de apoio que acompanha o prompt da Lily nas dúvidas. O manual inteiro da WXN é pequeno o bastante para ir
 * junto em toda chamada, então não há busca por trechos (RAG) por enquanto.
 */
@Component
public class BaseDeConhecimento {

	private final String texto;

	@Autowired
	public BaseDeConhecimento(@Value("${chatbot.base-de-conhecimento:classpath:conhecimento/base-wxn.md}") Resource recurso) {
		this.texto = ler(recurso);
	}

	public BaseDeConhecimento(String texto) {
		this.texto = texto == null ? "" : texto.strip();
	}

	public String texto() {
		return texto;
	}

	private static String ler(Resource recurso) {
		if (!recurso.exists()) {
			return "";
		}
		try {
			return new String(recurso.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
		} catch (IOException e) {
			throw new UncheckedIOException("Não foi possível ler a base de conhecimento " + recurso, e);
		}
	}
}
