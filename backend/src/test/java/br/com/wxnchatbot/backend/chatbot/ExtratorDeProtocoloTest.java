package br.com.wxnchatbot.backend.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ExtratorDeProtocoloTest {

	private final ExtratorDeProtocolo extrator = new ExtratorDeProtocolo();

	@Test
	void achaOProtocoloNoMeioDaFraseEPoeEmMaiusculas() {
		assertEquals(Optional.of("WXN-2026-0001"), extrator.extrair("Qual o status da wxn-2026-0001, por favor?"));
	}

	@Test
	void aceitaProtocoloComSublinhado() {
		assertEquals(Optional.of("WXN_LENTO-1"), extrator.extrair("consulta wxn_lento-1"));
	}

	@Test
	void ignoraTelefoneDataEPalavrasComHifen() {
		assertEquals(Optional.empty(), extrator.extrair("Meu telefone é 81-99999-0101"));
		assertEquals(Optional.empty(), extrator.extrair("Falei por e-mail ontem"));
		assertEquals(Optional.empty(), extrator.extrair("Foi dia 05-11-2026"));
	}

	@Test
	void semTextoNaoHaProtocolo() {
		assertEquals(Optional.empty(), extrator.extrair(null));
		assertEquals(Optional.empty(), extrator.extrair(""));
	}

	@Test
	void ficaComOPrimeiroQuandoHaMaisDeUm() {
		assertEquals(Optional.of("WXN-2026-0001"), extrator.extrair("WXN-2026-0001 ou WXN-2026-0002?"));
	}
}
