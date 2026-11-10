package br.com.wxnchatbot.backend.llm;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

	public GeminiConfig(@Value("${chatbot.llm:fake}") String llm) {
		if (!Set.of("fake", "gemini").contains(llm)) {
			throw new IllegalStateException("chatbot.llm deve ser 'fake' ou 'gemini', mas é '" + llm + "'");
		}
	}

	@Bean
	@ConditionalOnProperty(name = "chatbot.llm", havingValue = "gemini")
	public LlmClient llmClientGemini(GeminiProperties propriedades, JsonMapper json) {
		propriedades.exigirChave();
		return new LlmClientGemini(propriedades, json);
	}
}
