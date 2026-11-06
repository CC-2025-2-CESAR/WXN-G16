package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TelefoneBrTest {

	@Test
	void tiraOPaisEAFormatacao() {
		assertEquals("81999990101", TelefoneBr.nacional("5581999990101"));
		assertEquals("81999990101", TelefoneBr.nacional("+55 (81) 99999-0101"));
		assertEquals("81999990101", TelefoneBr.nacional("81 99999-0101"));
	}

	@Test
	void numeroDeOutroPaisFicaComoEsta() {
		assertEquals("14155550123", TelefoneBr.nacional("14155550123"));
	}

	@Test
	void variantesCobremComESemNonoDigitoEComESemPais() {
		List<String> variantes = TelefoneBr.variantes("558199990101");

		assertTrue(variantes.containsAll(List.of("8199990101", "81999990101", "558199990101", "5581999990101")));
	}

	@Test
	void numeroComNonoDigitoTambemProcuraSemEle() {
		List<String> variantes = TelefoneBr.variantes("5581999990101");

		assertTrue(variantes.contains("81999990101"));
		assertTrue(variantes.contains("8199990101"));
	}

	@Test
	void nuloViraVazio() {
		assertEquals("", TelefoneBr.nacional(null));
	}
}
