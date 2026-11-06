package br.com.wxnchatbot.backend.canal.whatsapp;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configuração da WhatsApp Cloud API da Meta; os segredos vêm só de variáveis de ambiente, nunca do repositório. */
@ConfigurationProperties(prefix = "whatsapp")
public record WhatsAppProperties(
		@DefaultValue("false") boolean habilitado,
		@DefaultValue("") String tokenDeVerificacao,
		@DefaultValue("") String segredoDoApp,
		@DefaultValue("") String tokenDeAcesso,
		@DefaultValue("") String idDoNumero,
		@DefaultValue("v26.0") String versaoDaApi,
		@DefaultValue("https://graph.facebook.com") String urlBase,
		@DefaultValue("3s") Duration timeoutConexao,
		@DefaultValue("10s") Duration timeoutLeitura) {

	/** Com o WhatsApp ligado, falta de configuração derruba a subida em vez de falhar na primeira mensagem. */
	public void exigirConfiguracaoCompleta() {
		exigir(tokenDeVerificacao, "WHATSAPP_TOKEN_DE_VERIFICACAO");
		exigir(segredoDoApp, "WHATSAPP_SEGREDO_DO_APP");
		exigir(tokenDeAcesso, "WHATSAPP_TOKEN_DE_ACESSO");
		exigir(idDoNumero, "WHATSAPP_ID_DO_NUMERO");
	}

	private static void exigir(String valor, String variavel) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalStateException("whatsapp.habilitado=true exige a variável de ambiente " + variavel);
		}
	}
}
