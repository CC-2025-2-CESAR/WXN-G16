package br.com.wxnchatbot.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.model.Empresa;

@SpringBootTest
@Transactional
class EmpresaRepositorioIntegracaoTest {

	@Autowired
	private EmpresaRepositorio empresas;

	@Test
	void achaAEmpresaPeloCnpj() {
		Empresa salva = empresas.saveAndFlush(new Empresa("Empresa do CNPJ", "11.222.333/0001-44"));

		assertEquals(salva.getId(), empresas.findByCnpj("11222333000144").orElseThrow().getId());
		assertEquals(salva.getId(), empresas.findByCnpj(Empresa.normalizarCnpj("11.222.333/0001-44")).orElseThrow().getId());
	}

	@Test
	void cnpjQueNaoExisteNaoAchaNada() {
		assertTrue(empresas.findByCnpj("99999999000199").isEmpty());
	}

	@Test
	void duasEmpresasNaoPodemTerOMesmoCnpj() {
		empresas.saveAndFlush(new Empresa("Primeira", "55666777000188"));

		assertThrows(DataIntegrityViolationException.class, () -> empresas.saveAndFlush(new Empresa("Segunda", "55666777000188")));
	}

	@Test
	void quantasEmpresasSemCnpjForemPrecisoNaoSeChocam() {
		long antes = empresas.count();

		empresas.saveAndFlush(new Empresa("Sem CNPJ um"));
		empresas.saveAndFlush(new Empresa("Sem CNPJ dois"));

		assertEquals(antes + 2, empresas.count());
	}
}
