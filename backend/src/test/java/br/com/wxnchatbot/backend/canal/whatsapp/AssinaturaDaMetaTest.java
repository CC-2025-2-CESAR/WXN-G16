package br.com.wxnchatbot.backend.canal.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AssinaturaDaMetaTest {

	private static final String SEGREDO = "segredo-do-app";

	private static byte[] bytes(String texto) {
		return texto.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void hmacSha256BateComOValorConhecido() {
		assertEquals("sha256=88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b",
				AssinaturaDaMeta.assinar(bytes("hello"), "secret"));
	}

	@Test
	void aceitaAAssinaturaCalculadaComOMesmoSegredo() {
		byte[] corpo = bytes("{\"object\":\"whatsapp_business_account\"}");

		assertTrue(AssinaturaDaMeta.confere(corpo, AssinaturaDaMeta.assinar(corpo, SEGREDO), SEGREDO));
	}

	@Test
	void aceitaHexEmMaiusculas() {
		byte[] corpo = bytes("{}");
		String assinatura = AssinaturaDaMeta.assinar(corpo, SEGREDO);
		String maiuscula = "sha256=" + assinatura.substring("sha256=".length()).toUpperCase();

		assertTrue(AssinaturaDaMeta.confere(corpo, maiuscula, SEGREDO));
	}

	@Test
	void recusaCorpoAlteradoOuSegredoErrado() {
		byte[] corpo = bytes("{\"texto\":\"ola\"}");
		String assinatura = AssinaturaDaMeta.assinar(corpo, SEGREDO);

		assertFalse(AssinaturaDaMeta.confere(bytes("{\"texto\":\"ola!\"}"), assinatura, SEGREDO));
		assertFalse(AssinaturaDaMeta.confere(corpo, assinatura, "outro-segredo"));
	}

	@Test
	void recusaCabecalhoAusenteSemPrefixoOuComHexInvalido() {
		byte[] corpo = bytes("{}");
		String hex = AssinaturaDaMeta.assinar(corpo, SEGREDO).substring("sha256=".length());

		assertFalse(AssinaturaDaMeta.confere(corpo, null, SEGREDO));
		assertFalse(AssinaturaDaMeta.confere(corpo, hex, SEGREDO));
		assertFalse(AssinaturaDaMeta.confere(corpo, "sha256=", SEGREDO));
		assertFalse(AssinaturaDaMeta.confere(corpo, "sha256=zzzz", SEGREDO));
		assertFalse(AssinaturaDaMeta.confere(corpo, "sha1=" + hex, SEGREDO));
	}

	@Test
	void semSegredoConfiguradoNadaConfere() {
		byte[] corpo = bytes("{}");

		assertFalse(AssinaturaDaMeta.confere(corpo, AssinaturaDaMeta.assinar(corpo, "x"), ""));
		assertFalse(AssinaturaDaMeta.confere(corpo, AssinaturaDaMeta.assinar(corpo, "x"), null));
	}

	@Test
	void usaOsBytesExatosDoCorpoInclusiveComAcentosEscapadosComoAMetaEnvia() {
		byte[] escapado = bytes("{\"body\":\"ol\\u00e1, tudo bem?\"}");
		byte[] cru = bytes("{\"body\":\"olá, tudo bem?\"}");
		String assinaturaDoEscapado = AssinaturaDaMeta.assinar(escapado, SEGREDO);

		assertTrue(AssinaturaDaMeta.confere(escapado, assinaturaDoEscapado, SEGREDO));
		assertFalse(AssinaturaDaMeta.confere(cru, assinaturaDoEscapado, SEGREDO));
	}
}
