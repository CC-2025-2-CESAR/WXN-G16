package br.com.wxnchatbot.backend.canal.telegram;

import java.time.Duration;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração do canal do Telegram (Bot API). Os segredos vêm só de variáveis de ambiente, nunca do repositório.
 *
 * <p>Dois modos de receber as mensagens, que o Telegram não deixa usar ao mesmo tempo no mesmo bot:
 * {@code webhook} (o Telegram chama um endereço HTTPS nosso; é o do Azure) e {@code polling} (o backend pergunta ao
 * Telegram se há mensagens; serve para testar na máquina, sem endereço público).
 */
@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(
		@DefaultValue("false") boolean habilitado,
		@DefaultValue("webhook") String modo,
		@DefaultValue("") String tokenDoBot,
		@DefaultValue("") String segredoDoWebhook,
		@DefaultValue("https://api.telegram.org") String urlBase,
		@DefaultValue("3s") Duration timeoutConexao,
		@DefaultValue("10s") Duration timeoutLeitura,
		@DefaultValue("25") int esperaDoPolling) {

	/** O que o Telegram aceita no segredo do webhook: de 1 a 256 caracteres entre letras, dígitos, "_" e "-". */
	private static final Pattern SEGREDO_VALIDO = Pattern.compile("[A-Za-z0-9_-]{1,256}");

	public boolean polling() {
		return "polling".equalsIgnoreCase(modo);
	}

	/** Com o Telegram ligado, falta de configuração derruba a subida em vez de falhar na primeira mensagem. */
	public void exigirConfiguracaoCompleta() {
		exigir(tokenDoBot, "TELEGRAM_TOKEN_DO_BOT");
		if (!polling() && !"webhook".equalsIgnoreCase(modo)) {
			throw new IllegalStateException("telegram.modo (TELEGRAM_MODO) deve ser webhook ou polling");
		}
		if (!polling()) {
			exigir(segredoDoWebhook, "TELEGRAM_SEGREDO_DO_WEBHOOK");
			if (!SEGREDO_VALIDO.matcher(segredoDoWebhook).matches()) {
				throw new IllegalStateException(
						"TELEGRAM_SEGREDO_DO_WEBHOOK deve ter de 1 a 256 caracteres, só com letras, dígitos, _ e -");
			}
		}
	}

	private static void exigir(String valor, String variavel) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalStateException("telegram.habilitado=true exige a variável de ambiente " + variavel);
		}
	}
}
