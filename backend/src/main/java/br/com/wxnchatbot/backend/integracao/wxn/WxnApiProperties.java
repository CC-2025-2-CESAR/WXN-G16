package br.com.wxnchatbot.backend.integracao.wxn;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "wxn.api")
public record WxnApiProperties(
		String baseUrl,
		@DefaultValue("2s") Duration timeoutConexao,
		@DefaultValue("3s") Duration timeoutLeitura) {
}
