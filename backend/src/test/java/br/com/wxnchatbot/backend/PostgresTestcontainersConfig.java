package br.com.wxnchatbot.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Sobe um PostgreSQL descartável num container Docker para os testes de integração.
 *
 * O {@code @ServiceConnection} faz o Spring usar esse banco no lugar do localhost e o
 * Flyway aplica as migrations nele do zero. Para usar, é só importar nos testes com
 * {@code @Import(PostgresTestcontainersConfig.class)}. Precisa do Docker rodando.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfig {

	// Mesma versão do Postgres usada no CI
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer("postgres:16");
	}
}
