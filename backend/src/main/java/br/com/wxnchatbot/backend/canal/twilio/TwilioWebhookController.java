package br.com.wxnchatbot.backend.canal.twilio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook do WhatsApp pelo Twilio: o Twilio faz um POST (formulário) a cada mensagem do cliente. Só aceita o que traz uma
 * assinatura {@code X-Twilio-Signature} válida; responde logo com um TwiML vazio e atende a mensagem em segundo plano
 * (a resposta ao cliente sai pela API REST do Twilio).
 */
@RestController
@ConditionalOnProperty(name = "twilio.habilitado", havingValue = "true")
@RequestMapping("/webhook/twilio/whatsapp")
public class TwilioWebhookController {

	private static final Logger log = LoggerFactory.getLogger(TwilioWebhookController.class);
	private static final String TWIML_VAZIO = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response/>";

	private final TwilioProperties propriedades;
	private final ProcessadorDeMensagensTwilio processador;

	public TwilioWebhookController(TwilioProperties propriedades, ProcessadorDeMensagensTwilio processador) {
		this.propriedades = propriedades;
		this.processador = processador;
	}

	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<String> receber(@RequestParam MultiValueMap<String, String> parametros,
			@RequestHeader(name = "X-Twilio-Signature", required = false) String assinatura) {
		if (!AssinaturaDoTwilio.confere(propriedades.urlDoWebhook().strip(), parametros, assinatura,
				propriedades.authToken())) {
			log.warn("Webhook do Twilio recusado: assinatura ausente ou inválida (confira TWILIO_URL_DO_WEBHOOK e TWILIO_AUTH_TOKEN)");
			return ResponseEntity.status(403).build();
		}
		processador.aceitar(MensagemDoTwilio.de(parametros));
		return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(TWIML_VAZIO);
	}
}
