package br.com.wxnchatbot.backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmpresaTest {

	@Test
	void empresaSemCnpjContinuaValendoParaQuemJaExiste() {
		Empresa empresa = new Empresa("WXN Tecnologia");

		assertTrue(empresa.getCnpj().isEmpty());
	}

	@Test
	void guardaOCnpjSoComOsDigitos() {
		assertEquals("12345678000199", new Empresa("WXN", "12345678000199").getCnpj().orElseThrow());
		assertEquals("12345678000199", new Empresa("WXN", "12.345.678/0001-99").getCnpj().orElseThrow());
		assertEquals("12345678000199", new Empresa("WXN", "  12 345 678 0001 99 ").getCnpj().orElseThrow());
	}

	@ParameterizedTest
	@ValueSource(strings = { "1234567800019", "123456780001999", "abc", "12.345.678/0001" })
	void recusaCnpjSemQuatorzeDigitos(String cnpj) {
		IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> new Empresa("WXN", cnpj));

		assertEquals("cnpj deve ter 14 dígitos", erro.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   " })
	void recusaCnpjEmBranco(String cnpj) {
		assertThrows(IllegalArgumentException.class, () -> new Empresa("WXN", cnpj));
	}

	@Test
	void recusaCnpjNulo() {
		assertThrows(IllegalArgumentException.class, () -> new Empresa("WXN", null));
	}

	@Test
	void nomeContinuaObrigatorioComCnpj() {
		assertThrows(IllegalArgumentException.class, () -> new Empresa(" ", "12345678000199"));
	}

	@Test
	void normalizarServeParaBuscarEmpresaPeloMesmoFormato() {
		assertEquals("12345678000199", Empresa.normalizarCnpj("12.345.678/0001-99"));
		assertThrows(IllegalArgumentException.class, () -> Empresa.normalizarCnpj("123"));
	}
}
