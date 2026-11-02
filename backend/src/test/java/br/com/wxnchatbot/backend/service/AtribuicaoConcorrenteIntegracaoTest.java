package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.StatusTicket;
import br.com.wxnchatbot.backend.model.Ticket;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import br.com.wxnchatbot.backend.model.Empresa;

@SpringBootTest
class AtribuicaoConcorrenteIntegracaoTest {

	private static final int ATENDENTES = 5;

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
	private UUID conversaId;
	private final List<UUID> atendentesIds = new ArrayList<>();

	@BeforeEach
	void criarDados() {
		transacao = new TransactionTemplate(transacoes);
		transacao.executeWithoutResult(status -> {
			Chatbot lily = new Chatbot("Lily", "Oi, sou a Lily!");
			em.persist(lily);
			Empresa empresa = new Empresa("Empresa Demo");
   			em.persist(empresa);
   			empresaId = empresa.getId();
			Cliente cliente = new Cliente(empresa, "Maria Souza", "81999990001");
			em.persist(cliente);
			Conversa conversa = new Conversa(cliente, lily);
			em.persist(conversa);
			conversa.solicitarAtendente("Quer falar com uma pessoa");
			for (int i = 1; i <= ATENDENTES; i++) {
				Atendente atendente = new Atendente("Atendente " + i, "atendente" + i + "@wxn.com.br");
				em.persist(atendente);
				atendentesIds.add(atendente.getId());
			}
			chatbotId = lily.getId();
			clienteId = cliente.getId();
			conversaId = conversa.getId();
		});
	}

	@AfterEach
	void limpar() {
		transacao.executeWithoutResult(status -> {
			remover(Conversa.class, conversaId);
			atendentesIds.forEach(id -> remover(Atendente.class, id));
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

	private void conferirQueSoOVencedorFicouComAConversa(UUID vencedorId) {
		transacao.executeWithoutResult(status -> {
			Conversa conversa = em.find(Conversa.class, conversaId);
			assertEquals(StatusConversa.COM_ATENDENTE, conversa.getStatus());
			assertEquals(vencedorId, conversa.getAtendente().orElseThrow().getId());
			Ticket ticket = conversa.getTicket().orElseThrow();
			assertEquals(StatusTicket.EM_ATENDIMENTO, ticket.getStatus());
			assertEquals(vencedorId, ticket.getAtendente().orElseThrow().getId());
		});
	}

	@Test
	void segundoAtendenteEsperaOPrimeiroTerminarENaoTomaAConversa() throws Exception {
		UUID primeiroId = atendentesIds.get(0);
		UUID segundoId = atendentesIds.get(1);
		CountDownLatch primeiroJaAssumiu = new CountDownLatch(1);
		CountDownLatch liberarPrimeiro = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<?> primeiro = pool.submit(() -> new TransactionTemplate(transacoes).executeWithoutResult(status -> {
				servico.atribuirAtendente(conversaId, primeiroId);
				primeiroJaAssumiu.countDown();
				aguardar(liberarPrimeiro);
			}));
			assertTrue(primeiroJaAssumiu.await(15, TimeUnit.SECONDS));

			Future<?> segundo = pool.submit(() -> servico.atribuirAtendente(conversaId, segundoId));
			Thread.sleep(800);
			assertFalse(segundo.isDone(), "o segundo atendente deveria esperar o primeiro terminar");

			liberarPrimeiro.countDown();
			primeiro.get(15, TimeUnit.SECONDS);
			ExecutionException erro = assertThrows(ExecutionException.class, () -> segundo.get(15, TimeUnit.SECONDS));
			assertInstanceOf(IllegalStateException.class, erro.getCause());
		} finally {
			liberarPrimeiro.countDown();
			pool.shutdownNow();
		}

		conferirQueSoOVencedorFicouComAConversa(primeiroId);
	}

	@Test
	void varioAtendentesAoMesmoTempoSoUmAssume() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(ATENDENTES);
		CyclicBarrier largada = new CyclicBarrier(ATENDENTES);
		List<Future<UUID>> tentativas = new ArrayList<>();
		try {
			for (UUID atendenteId : atendentesIds) {
				tentativas.add(pool.submit(() -> {
					largada.await(15, TimeUnit.SECONDS);
					try {
						servico.atribuirAtendente(conversaId, atendenteId);
						return atendenteId;
					} catch (IllegalStateException e) {
						return null;
					}
				}));
			}
			List<UUID> vencedores = new ArrayList<>();
			for (Future<UUID> tentativa : tentativas) {
				UUID resultado = tentativa.get(30, TimeUnit.SECONDS);
				if (resultado != null) {
					vencedores.add(resultado);
				}
			}

			assertEquals(1, vencedores.size(), "só um atendente pode assumir: " + vencedores);
			conferirQueSoOVencedorFicouComAConversa(vencedores.get(0));
		} finally {
			pool.shutdownNow();
		}
	}

	private static void aguardar(CountDownLatch trava) {
		try {
			trava.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
