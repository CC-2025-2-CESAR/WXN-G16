package br.com.wxnchatbot.backend.llm;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configuração do Gemini; a chave vem só da variável de ambiente GEMINI_API_KEY, nunca do repositório. */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
		@DefaultValue("") String apiKey,
		@DefaultValue("gemini-3.5-flash-lite") String modelo,
		@DefaultValue("low") String nivelDePensamento,
		@DefaultValue("https://generativelanguage.googleapis.com/v1beta") String urlBase,
		@DefaultValue("3s") Duration timeoutConexao,
		@DefaultValue("20s") Duration timeoutLeitura) {

	/** Com chatbot.llm=gemini, faltar a chave derruba a subida em vez de falhar na primeira mensagem. */
	public void exigirChave() {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("chatbot.llm=gemini exige a variável de ambiente GEMINI_API_KEY");
		}
	}
}
