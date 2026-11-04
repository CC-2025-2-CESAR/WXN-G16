package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.StatusTicket;
import br.com.wxnchatbot.backend.model.Ticket;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class AssumirEEncerrarConcorrentesIntegracaoTest {

	private static final int RODADAS = 40;

	@Autowired
	private FilaConversasServico servico;

	@Autowired
	private PlatformTransactionManager transacoes;

	@PersistenceContext
	private EntityManager em;

	private TransactionTemplate transacao;
	private UUID chatbotId;
	private UUID empresaId;
	private UUID clienteId;
	private UUID atendenteId;
	private final List<UUID> conversasIds = new ArrayList<>();

	@BeforeEach
	void criarDados() {
		transacao = new TransactionTemplate(transacoes);
		transacao.executeWithoutResult(status -> {
			Chatbot lily = new Chatbot("Lily", "Oi, sou a Lily!");
			em.persist(lily);
			Empresa empresa = new Empresa("Empresa Demo", "12345678000199");
			em.persist(empresa);
			Cliente cliente = new Cliente(empresa, "Maria Souza", "81999990001");
			em.persist(cliente);
			Atendente atendente = new Atendente("Carlos Lima", "carlos@wxn.com.br");
			em.persist(atendente);
			chatbotId = lily.getId();
			empresaId = empresa.getId();
			clienteId = cliente.getId();
			atendenteId = atendente.getId();
		});
	}

	@AfterEach
	void limpar() {
		transacao.executeWithoutResult(status -> {
			conversasIds.forEach(id -> remover(Conversa.class, id));
			remover(Atendente.class, atendenteId);
			remover(Cliente.class, clienteId);
			remover(Empresa.class, empresaId);
			remover(Chatbot.class, chatbotId);
		});
	}

	private <T> void remover(Class<T> tipo, UUID id) {
		T entidade = em.find(tipo, id);
		if (entidade != null) {
			em.remove(entidade);
		}
	}

	private UUID novaConversaNaFila() {
		return transacao.execute(status -> {
			Conversa conversa = new Conversa(em.find(Chatbot.class, chatbotId));
			conversa.vincularCliente(em.find(Cliente.class, clienteId));
			em.persist(conversa);
			conversa.solicitarAtendente("Quer falar com uma pessoa");
			conversasIds.add(conversa.getId());
			return conversa.getId();
		});
	}

	private void conferirEstadoFinal(UUID conversaId, boolean assumiu, int rodada) {
		transacao.executeWithoutResult(status -> {
			Conversa conversa = em.find(Conversa.class, conversaId);
			Ticket ticket = conversa.getTicket().orElseThrow();
			String contexto = "rodada " + rodada + ", assumir " + (assumiu ? "aceito" : "recusado") + ", conversa "
					+ conversa.getStatus() + ", ticket " + ticket.getStatus();
			UUID esperado = assumiu ? atendenteId : null;
			assertEquals(StatusConversa.ENCERRADA, conversa.getStatus(), contexto);
			assertEquals(StatusTicket.RESOLVIDO, ticket.getStatus(), contexto);
			assertTrue(conversa.getEncerradaEm().isPresent(), contexto);
			assertTrue(ticket.getResolvidoEm().isPresent(), contexto);
			assertEquals(esperado, conversa.getAtendente().map(Atendente::getId).orElse(null), contexto);
			assertEquals(esperado, ticket.getAtendente().map(Atendente::getId).orElse(null), contexto);
		});
	}

	@Test
	void assumirEEncerrarAoMesmoTempoDeixamSempreUmEstadoCoerente() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			for (int rodada = 0; rodada < RODADAS; rodada++) {
				UUID conversaId = novaConversaNaFila();
				CyclicBarrier largada = new CyclicBarrier(2);
				long atraso = (rodada % 6) * 250_000L;
				boolean atrasaAssumir = rodada % 2 == 0;

				Future<Boolean> assumir = pool.submit(() -> {
					largada.await(15, TimeUnit.SECONDS);
					if (atrasaAssumir) {
						LockSupport.parkNanos(atraso);
					}
					try {
						servico.atribuirAtendente(conversaId, atendenteId);
						return true;
					} catch (IllegalStateException e) {
						if (!e.getMessage().contains("aguardando atendente")) {
							throw e;
						}
						return false;
					}
				});
				Future<Boolean> encerrar = pool.submit(() -> {
					largada.await(15, TimeUnit.SECONDS);
					if (!atrasaAssumir) {
						LockSupport.parkNanos(atraso);
					}
					servico.encerrar(conversaId);
					return true;
				});

				boolean assumiu = assumir.get(30, TimeUnit.SECONDS);
				encerrar.get(30, TimeUnit.SECONDS);
				conferirEstadoFinal(conversaId, assumiu, rodada);
			}
		} finally {
			pool.shutdownNow();
		}
	}
}
