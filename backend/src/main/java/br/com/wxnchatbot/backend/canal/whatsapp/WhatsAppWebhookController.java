package br.com.wxnchatbot.backend.canal.whatsapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** O endereço que a Meta chama: GET para confirmar que o webhook é nosso, POST a cada mensagem recebida. */
@RestController
@RequestMapping("/webhook/whatsapp")
@ConditionalOnProperty(name = "whatsapp.habilitado", havingValue = "true")
public class WhatsAppWebhookController {

	private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

	private final WhatsAppProperties propriedades;
	private final ProcessadorDeMensagensWhatsApp processador;
	private final JsonMapper json;

	public WhatsAppWebhookController(WhatsAppProperties propriedades, ProcessadorDeMensagensWhatsApp processador, JsonMapper json) {
		this.propriedades = propriedades;
		this.processador = processador;
		this.json = json;
	}

	/** Verificação da Meta: devolve o desafio, em texto puro, se o modo e o token de verificação batem. */
	@GetMapping
	public ResponseEntity<String> verificar(
			@RequestParam(name = "hub.mode", required = false) String modo,
			@RequestParam(name = "hub.verify_token", required = false) String token,
			@RequestParam(name = "hub.challenge", required = false) String desafio) {
		boolean confere = "subscribe".equals(modo) && desafio != null && iguais(token, propriedades.tokenDeVerificacao());
		if (!confere) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(desafio);
	}

	/**
	 * Recebe as mensagens. Só aceita o que vem assinado pela Meta, e responde 200 logo: o atendimento acontece em
	 * segundo plano. Um corpo ilegível, mas assinado, também recebe 200, porque reenviar não o consertaria.
	 */
	@PostMapping
	public ResponseEntity<Void> receber(@RequestBody byte[] corpo,
			@RequestHeader(name = "X-Hub-Signature-256", required = false) String assinatura) {
		if (!AssinaturaDaMeta.confere(corpo, assinatura, propriedades.segredoDoApp())) {
			log.warn("Webhook do WhatsApp recusado: assinatura ausente ou inválida");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		try {
			processador.aceitar(json.readValue(corpo, EntregaDoWhatsApp.class).mensagens());
		} catch (JacksonException e) {
			log.error("Corpo assinado, mas ilegível, no webhook do WhatsApp: {}", e.getOriginalMessage());
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
