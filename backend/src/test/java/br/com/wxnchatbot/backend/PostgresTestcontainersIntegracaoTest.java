package br.com.wxnchatbot.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class PostgresTestcontainersIntegracaoTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void usaOPostgresDoContainer() {
		String versao = jdbc.queryForObject("select version()", String.class);

		assertTrue(versao.startsWith("PostgreSQL 16"), versao);
	}

	@Test
	void aplicaTodasAsMigrationsDoFlywayNoBancoVazio() throws Exception {
		int arquivos = new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/V*.sql").length;

		Integer aplicadas = jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class);
		Integer comFalha = jdbc.queryForObject("select count(*) from flyway_schema_history where not success", Integer.class);

		assertEquals(arquivos, aplicadas);
		assertEquals(0, comFalha);
	}
}
