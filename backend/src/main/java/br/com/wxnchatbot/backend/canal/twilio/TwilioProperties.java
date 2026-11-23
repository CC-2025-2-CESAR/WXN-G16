package br.com.wxnchatbot.backend.canal.twilio;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração do WhatsApp pelo Twilio (a alternativa à Meta, para o MVP). Os segredos vêm só de variáveis de ambiente,
 * nunca do repositório.
 *
 * @param numeroDeOrigem o número do Twilio que envia (no sandbox, o número compartilhado do sandbox), com ou sem o
 *        prefixo {@code whatsapp:}
 * @param urlDoWebhook a URL pública que o Twilio chama, exatamente como está no console do Twilio: a assinatura de cada
 *        chamada é calculada sobre ela, e atrás do Azure o backend não consegue adivinhá-la
 */
@ConfigurationProperties(prefix = "twilio")
public record TwilioProperties(
		@DefaultValue("false") boolean habilitado,
		@DefaultValue("") String accountSid,
		@DefaultValue("") String authToken,
		@DefaultValue("") String numeroDeOrigem,
		@DefaultValue("") String urlDoWebhook,
		@DefaultValue("https://api.twilio.com") String urlBase,
		@DefaultValue("3s") Duration timeoutConexao,
		@DefaultValue("10s") Duration timeoutLeitura) {

	private static final String PREFIXO = "whatsapp:";

	/** Com o Twilio ligado, falta de configuração derruba a subida em vez de falhar na primeira mensagem. */
	public void exigirConfiguracaoCompleta() {
		exigir(accountSid, "TWILIO_ACCOUNT_SID");
		exigir(authToken, "TWILIO_AUTH_TOKEN");
		exigir(numeroDeOrigem, "TWILIO_NUMERO_DE_ORIGEM");
		exigir(urlDoWebhook, "TWILIO_URL_DO_WEBHOOK");
		if (!urlDoWebhook.strip().startsWith("http://") && !urlDoWebhook.strip().startsWith("https://")) {
			throw new IllegalStateException("twilio.url-do-webhook (TWILIO_URL_DO_WEBHOOK) deve começar com https://");
		}
		if (!remetente().matches("whatsapp:\\+\\d{8,15}")) {
			throw new IllegalStateException(
					"twilio.numero-de-origem (TWILIO_NUMERO_DE_ORIGEM) deve ser um número com DDI, por exemplo +14155238886");
		}
	}

	/** O remetente no formato que a API do Twilio pede: {@code whatsapp:+14155238886}. */
	public String remetente() {
		String numero = numeroDeOrigem == null ? "" : numeroDeOrigem.strip();
		if (numero.regionMatches(true, 0, PREFIXO, 0, PREFIXO.length())) {
			numero = numero.substring(PREFIXO.length()).strip();
		}
		if (!numero.isEmpty() && !numero.startsWith("+")) {
			numero = "+" + numero;
		}
		return PREFIXO + numero;
	}

	private static void exigir(String valor, String variavel) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalStateException("twilio.habilitado=true exige a variável de ambiente " + variavel);
		}
	}
}
