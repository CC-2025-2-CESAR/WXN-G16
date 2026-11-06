package br.com.wxnchatbot.backend.canal.whatsapp;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Confere o cabeçalho X-Hub-Signature-256: "sha256=" mais o HMAC-SHA256 do corpo exatamente como chegou, calculado com
 * o segredo do app. Sem isso, qualquer um que descobrisse a URL poderia se passar pela Meta.
 */
final class AssinaturaDaMeta {

	private static final String PREFIXO = "sha256=";
	private static final String ALGORITMO = "HmacSHA256";

	private AssinaturaDaMeta() {
	}

	static boolean confere(byte[] corpo, String cabecalho, String segredo) {
		if (corpo == null || cabecalho == null || segredo == null || segredo.isEmpty() || !cabecalho.startsWith(PREFIXO)) {
			return false;
		}
		byte[] recebida;
		try {
			recebida = HexFormat.of().parseHex(cabecalho.substring(PREFIXO.length()).strip().toLowerCase());
		} catch (IllegalArgumentException e) {
			return false;
		}
		return MessageDigest.isEqual(calcular(corpo, segredo), recebida);
	}

	static String assinar(byte[] corpo, String segredo) {
		return PREFIXO + HexFormat.of().formatHex(calcular(corpo, segredo));
	}

	private static byte[] calcular(byte[] corpo, String segredo) {
		try {
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), ALGORITMO));
			return mac.doFinal(corpo);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Não foi possível calcular o HMAC-SHA256", e);
		}
	}
}
