package br.com.wxnchatbot.backend.canal.telegram;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfig {

	@Bean
	@ConditionalOnProperty(name = "telegram.habilitado", havingValue = "true")
	public TelegramClient telegramClient(TelegramProperties propriedades, JsonMapper json) {
		propriedades.exigirConfiguracaoCompleta();
		return new TelegramClientHttp(propriedades, json);
	}
}
