package br.com.wxnchatbot.backend.canal.twilio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AssinaturaDoTwilioTest {

	private static final String URL = "https://wxn-g16-backend.azurewebsites.net/webhook/twilio/whatsapp";
	private static final String TOKEN = "token-de-teste-1234567890";

	private static Map<String, List<String>> parametros(String... pares) {
		Map<String, List<String>> mapa = new LinkedHashMap<>();
		for (int i = 0; i < pares.length; i += 2) {
			mapa.computeIfAbsent(pares[i], chave -> new java.util.ArrayList<>()).add(pares[i + 1]);
		}
		return mapa;
	}

	private static Map<String, List<String>> mensagem() {
		return parametros("MessageSid", "SM_MENSAGEM_FALSA_PARA_TESTES", "From", "whatsapp:+5581999990001", "To",
				"whatsapp:+14155238886", "Body", "Oi, tudo bem?", "ProfileName", "Maria Souza", "NumMedia", "0", "WaId",
				"5581999990001");
	}

	@Test
	void reproduzOExemploCalculadoAMaoNaDocumentacaoOficialDoTwilio() {
		// O exemplo "Explore the algorithm yourself" de https://www.twilio.com/docs/usage/security (token 12345).
		Map<String, List<String>> parametros = parametros("Digits", "1234", "To", "+18005551212", "From", "+14158675310",
				"Caller", "+14158675310", "CallSid", "CA1234567890ABCDE");

		String assinatura = AssinaturaDoTwilio.assinar("https://example.com/myapp.php?foo=1&bar=2", parametros, "12345");

		assertEquals("L/OH5YylLD5NRKLltdqwSvS0BnU=", assinatura);
	}

	@Test
	void concordaComUmSegundoProgramaIndependenteQueImplementaOMesmoAlgoritmo() {
		// Valor calculado em Python (hmac + hashlib + base64) a partir da descrição da documentação.
		assertEquals("hH1Pwk0Y+8RxYa5G4/pdxHvfyJ8=", AssinaturaDoTwilio.assinar(URL, mensagem(), TOKEN));
	}

	@Test
	void aOrdemDosParametrosEPorCodigoDeCaractereMaiusculasAntesDeMinusculas() {
		Map<String, List<String>> parametros = parametros("b", "2", "B", "1", "a", "3");

		assertEquals("fMGAnh5r8l+YHREScgCHkIMTgwo=", AssinaturaDoTwilio.assinar("https://exemplo.com/x", parametros, "segredo"));
	}

	@Test
	void aOrdemEmQueOsParametrosChegamNaoMudaAAssinatura() {
		Map<String, List<String>> embaralhado = parametros("WaId", "5581999990001", "Body", "Oi, tudo bem?", "From",
				"whatsapp:+5581999990001", "NumMedia", "0", "MessageSid", "SM_MENSAGEM_FALSA_PARA_TESTES",
				"ProfileName", "Maria Souza", "To", "whatsapp:+14155238886");

		assertEquals(AssinaturaDoTwilio.assinar(URL, mensagem(), TOKEN), AssinaturaDoTwilio.assinar(URL, embaralhado, TOKEN));
	}

	@Test
	void aceitaAAssinaturaCertaMesmoComEspacosNasPontas() {
		String assinatura = AssinaturaDoTwilio.assinar(URL, mensagem(), TOKEN);

		assertTrue(AssinaturaDoTwilio.confere(URL, mensagem(), assinatura, TOKEN));
		assertTrue(AssinaturaDoTwilio.confere(URL, mensagem(), "  " + assinatura + " ", TOKEN));
	}

	@Test
	void recusaQualquerCoisaQueNaoSejaExatamenteOQueFoiAssinado() {
		String assinatura = AssinaturaDoTwilio.assinar(URL, mensagem(), TOKEN);

		assertFalse(AssinaturaDoTwilio.confere(URL, mensagem(), assinatura, "outro-token"));
		assertFalse(AssinaturaDoTwilio.confere(URL.replace("https", "http"), mensagem(), assinatura, TOKEN));
		assertFalse(AssinaturaDoTwilio.confere(URL + "/", mensagem(), assinatura, TOKEN));

		Map<String, List<String>> adulterado = mensagem();
		adulterado.put("Body", List.of("Oi, tudo mal?"));
		assertFalse(AssinaturaDoTwilio.confere(URL, adulterado, assinatura, TOKEN));

		Map<String, List<String>> comExtra = mensagem();
		comExtra.put("Extra", List.of("x"));
		assertFalse(AssinaturaDoTwilio.confere(URL, comExtra, assinatura, TOKEN));

		Map<String, List<String>> semUm = mensagem();
		semUm.remove("ProfileName");
		assertFalse(AssinaturaDoTwilio.confere(URL, semUm, assinatura, TOKEN));
	}

	@Test
	void semCabecalhoOuSemConfiguracaoNaoHaAssinaturaValida() {
		String assinatura = AssinaturaDoTwilio.assinar(URL, mensagem(), TOKEN);

		assertFalse(AssinaturaDoTwilio.confere(URL, mensagem(), null, TOKEN));
		assertFalse(AssinaturaDoTwilio.confere(URL, mensagem(), "", TOKEN));
		assertFalse(AssinaturaDoTwilio.confere(URL, mensagem(), "   ", TOKEN));
		assertFalse(AssinaturaDoTwilio.confere(URL, mensagem(), "lixo", TOKEN));
		assertFalse(AssinaturaDoTwilio.confere(null, mensagem(), assinatura, TOKEN));
		assertFalse(AssinaturaDoTwilio.confere("", mensagem(), assinatura, TOKEN));
		assertFalse(AssinaturaDoTwilio.confere(URL, mensagem(), assinatura, null));
		assertFalse(AssinaturaDoTwilio.confere(URL, mensagem(), assinatura, ""));
	}
}
