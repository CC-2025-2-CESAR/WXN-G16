package br.com.wxnchatbot.backend.canal.telegram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * O endereço que o Telegram chama a cada mensagem (modo webhook). O Telegram manda o segredo combinado no cabeçalho
 * {@code X-Telegram-Bot-Api-Secret-Token}; sem ele, ou com outro, o corpo é recusado.
 */
@RestController
@RequestMapping("/webhook/telegram")
@ConditionalOnExpression("${telegram.habilitado:false} && !'${telegram.modo:webhook}'.equalsIgnoreCase('polling')")
public class TelegramWebhookController {

	static final String CABECALHO_DO_SEGREDO = "X-Telegram-Bot-Api-Secret-Token";

	private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

	private final TelegramProperties propriedades;
	private final ProcessadorDeAtualizacoesTelegram processador;
	private final JsonMapper json;

	public TelegramWebhookController(TelegramProperties propriedades, ProcessadorDeAtualizacoesTelegram processador,
			JsonMapper json) {
		this.propriedades = propriedades;
		this.processador = processador;
		this.json = json;
	}

	/**
	 * Responde 200 logo: o atendimento acontece em segundo plano. Um corpo ilegível, mas com o segredo certo, também
	 * recebe 200, porque o Telegram repetiria a entrega e ela continuaria ilegível.
	 */
	@PostMapping
	public ResponseEntity<Void> receber(@RequestBody byte[] corpo,
			@RequestHeader(name = CABECALHO_DO_SEGREDO, required = false) String segredo) {
		if (!iguais(segredo, propriedades.segredoDoWebhook())) {
			log.warn("Webhook do Telegram recusado: segredo ausente ou inválido");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		try {
			processador.aceitar(json.readValue(corpo, AtualizacaoDoTelegram.class));
		} catch (JacksonException e) {
			log.error("Corpo com o segredo certo, mas ilegível, no webhook do Telegram: {}", e.getOriginalMessage());
		}
		return ResponseEntity.ok().build();
	}

	private static boolean iguais(String recebido, String esperado) {
		if (recebido == null || esperado == null || esperado.isEmpty()) {
			return false;
		}
		return MessageDigest.isEqual(recebido.getBytes(StandardCharsets.UTF_8), esperado.getBytes(StandardCharsets.UTF_8));
	}
}
