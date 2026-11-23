package br.com.wxnchatbot.backend.canal.twilio;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Confere o cabeçalho {@code X-Twilio-Signature}. Segundo a documentação do Twilio, a assinatura é o HMAC-SHA1, em
 * base64, de: a URL completa que o Twilio chamou, seguida de cada parâmetro do POST em ordem alfabética (maiúsculas
 * antes de minúsculas) como nome imediatamente seguido do valor, sem separadores; a chave é o Auth Token. Sem essa
 * conferência qualquer um que descobrisse a URL do webhook poderia se passar pelo Twilio e falar como se fosse um cliente.
 */
final class AssinaturaDoTwilio {

	private static final String ALGORITMO = "HmacSHA1";

	private AssinaturaDoTwilio() {
	}

	static boolean confere(String url, Map<String, List<String>> parametros, String cabecalho, String authToken) {
		if (url == null || url.isBlank() || cabecalho == null || cabecalho.isBlank() || authToken == null
				|| authToken.isEmpty()) {
			return false;
		}
		byte[] esperada = assinar(url, parametros, authToken).getBytes(StandardCharsets.US_ASCII);
		byte[] recebida = cabecalho.strip().getBytes(StandardCharsets.US_ASCII);
		return MessageDigest.isEqual(esperada, recebida);
	}

	static String assinar(String url, Map<String, List<String>> parametros, String authToken) {
		StringBuilder dados = new StringBuilder(url);
		// A ordem natural de String é a de código de caractere: "Unix-style case-sensitive", como o Twilio pede.
		Map<String, List<String>> ordenados = new TreeMap<>();
		if (parametros != null) {
			ordenados.putAll(parametros);
		}
		ordenados.forEach((nome, valores) -> {
			List<String> ordenadosDoNome = new ArrayList<>(valores == null ? List.of() : valores);
			Collections.sort(ordenadosDoNome);
			if (ordenadosDoNome.isEmpty()) {
				dados.append(nome);
			}
			for (String valor : ordenadosDoNome) {
				dados.append(nome).append(valor == null ? "" : valor);
			}
		});
		try {
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), ALGORITMO));
			return Base64.getEncoder().encodeToString(mac.doFinal(dados.toString().getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Não foi possível calcular o HMAC-SHA1", e);
		}
	}
}
