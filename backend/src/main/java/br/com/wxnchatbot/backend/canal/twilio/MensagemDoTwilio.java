package br.com.wxnchatbot.backend.canal.twilio;

import java.util.List;
import java.util.Map;

/**
 * Os campos do formulário que o Twilio envia ao webhook e que a Lily usa. {@code de} vem no formato do Twilio
 * ({@code whatsapp:+5581999990001}); mensagem sem texto (foto, áudio, figurinha) chega com o corpo vazio e
 * {@code NumMedia} maior que zero.
 */
public record MensagemDoTwilio(String id, String de, String nome, String texto) {

	private static final String PREFIXO = "whatsapp:";

	static MensagemDoTwilio de(Map<String, List<String>> parametros) {
		return new MensagemDoTwilio(primeiro(parametros, "MessageSid"), primeiro(parametros, "From"),
				primeiro(parametros, "ProfileName"), primeiro(parametros, "Body"));
	}

	/** Verdade quando o remetente é um endereço de WhatsApp (e não um SMS que caiu no mesmo número). */
	boolean ehWhatsApp() {
		return de != null && de.startsWith(PREFIXO);
	}

	/** Só os dígitos do número, sem o {@code whatsapp:} nem o {@code +}: é como a Lily identifica o cliente. */
	String telefone() {
		return de == null ? "" : de.substring(Math.min(de.length(), PREFIXO.length())).replaceAll("\\D", "");
	}

	boolean temTexto() {
		return texto != null && !texto.isBlank();
	}

	private static String primeiro(Map<String, List<String>> parametros, String nome) {
		List<String> valores = parametros.get(nome);
		return valores == null || valores.isEmpty() ? null : valores.get(0);
	}
}
