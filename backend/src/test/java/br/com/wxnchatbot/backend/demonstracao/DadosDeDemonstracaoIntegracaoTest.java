package br.com.wxnchatbot.backend.demonstracao;

import static br.com.wxnchatbot.backend.demonstracao.DadosDeDemonstracao.DOMINIO_EMAIL;
import static br.com.wxnchatbot.backend.demonstracao.DadosDeDemonstracao.NOME_EMPRESA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.StatusTicket;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.service.ConversaNaFila;
import br.com.wxnchatbot.backend.service.DetalheConversaFila;
import br.com.wxnchatbot.backend.service.FilaConversasServico;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class DadosDeDemonstracaoIntegracaoTest {

	@Autowired
	private FilaConversasServico fila;

	@Autowired
	private ApplicationContext contexto;

	@PersistenceContext
	private EntityManager em;

	private DadosDeDemonstracao demonstracao;

	@BeforeEach
	void prepararBancoSemDemonstracao() {
		consultar("select c from Conversa c where c.empresa.nome = :nome", Conversa.class).forEach(em::remove);
		em.flush();
		consultar("select c from Cliente c where c.empresa.nome = :nome", Cliente.class).forEach(em::remove);
		em.createQuery("select a from Atendente a where a.email like :email", Atendente.class)
				.setParameter("email", "%" + DOMINIO_EMAIL).getResultList().forEach(em::remove);
		consultar("select e from Empresa e where e.nome = :nome", Empresa.class).forEach(em::remove);
		em.flush();
		em.clear();
		demonstracao = new DadosDeDemonstracao(em, fila);
	}

	private <T> List<T> consultar(String jpql, Class<T> tipo) {
		return em.createQuery(jpql, tipo).setParameter("nome", NOME_EMPRESA).getResultList();
	}

	private List<Conversa> conversasDaDemonstracao() {
		return consultar("select c from Conversa c where c.empresa.nome = :nome", Conversa.class);
	}

	private void criar() {
		demonstracao.run();
		em.flush();
		em.clear();
	}

	@Test
	void semOPerfilDemoOsDadosNaoSaoCriados() {
		assertTrue(contexto.getBeansOfType(DadosDeDemonstracao.class).isEmpty());
		assertTrue(conversasDaDemonstracao().isEmpty());
	}

	@Test
	void criaSeteConversasNaFilaDuasComOChatbotEUmaEncerrada() {
		criar();

		List<Conversa> conversas = conversasDaDemonstracao();
		assertEquals(10, conversas.size());
		assertTrue(conversas.stream().allMatch(c -> c.getOrigem() == OrigemConversa.DEMONSTRACAO));
		assertEquals(7, conversas.stream().filter(c -> c.getStatus() == StatusConversa.AGUARDANDO_ATENDENTE).count());
		assertEquals(2, conversas.stream().filter(c -> c.getStatus() == StatusConversa.COM_CHATBOT).count());
		Conversa encerrada = conversas.stream().filter(c -> c.getStatus() == StatusConversa.ENCERRADA).findFirst()
				.orElseThrow();
		assertEquals(StatusTicket.RESOLVIDO, encerrada.getTicket().orElseThrow().getStatus());
		assertEquals("Ana Martins", encerrada.getAtendente().orElseThrow().getNome());
		assertTrue(encerrada.getEncerradaEm().orElseThrow().isAfter(encerrada.getIniciadaEm()));
		assertTrue(encerrada.getEncerradaEm().orElseThrow().isBefore(Instant.now()));
	}

	@Test
	void conversasNaFilaTemInstantesDiferentesMotivoEMensagensEmOrdem() {
		criar();

		List<ConversaNaFila> naFila = fila.visualizarFila().stream()
				.filter(item -> item.clienteTelefone().startsWith("8199999010")).toList();

		assertEquals(7, naFila.size());
		assertEquals(7, naFila.stream().map(ConversaNaFila::abertoEm).distinct().count());
		assertEquals(naFila.stream().map(ConversaNaFila::abertoEm).sorted().toList(),
				naFila.stream().map(ConversaNaFila::abertoEm).toList());
		assertTrue(naFila.stream().allMatch(item -> item.motivo() != null && !item.motivo().isBlank()));
		assertEquals("Marcos Albuquerque", naFila.get(0).clienteNome());
		for (ConversaNaFila item : naFila) {
			DetalheConversaFila detalhe = fila.abrirConversaDaFila(item.conversaId());
			List<DetalheConversaFila.MensagemResumo> mensagens = detalhe.mensagens();
			assertEquals(TipoRemetente.CHATBOT, mensagens.get(0).tipoRemetente());
			assertTrue(mensagens.size() >= 4);
			assertEquals(mensagens.stream().map(DetalheConversaFila.MensagemResumo::enviadaEm).sorted().toList(),
					mensagens.stream().map(DetalheConversaFila.MensagemResumo::enviadaEm).toList());
			assertTrue(mensagens.get(mensagens.size() - 1).enviadaEm().isBefore(item.abertoEm()));
			assertTrue(mensagens.stream().anyMatch(m -> m.tipoRemetente() == TipoRemetente.CLIENTE));
		}
	}

	@Test
	void criaTresAtendentesComUmIndisponivel() {
		criar();

		List<Atendente> atendentes = em.createQuery("select a from Atendente a where a.email like :email", Atendente.class)
				.setParameter("email", "%" + DOMINIO_EMAIL).getResultList().stream()
				.sorted(Comparator.comparing(Atendente::getNome)).toList();

		assertEquals(List.of("Ana Martins", "Carla Nogueira", "Paulo Mendes"),
				atendentes.stream().map(Atendente::getNome).toList());
		assertEquals(List.of(true, false, true), atendentes.stream().map(Atendente::isDisponivel).toList());
	}

	@Test
	void rodarDuasVezesNaoDuplicaOsDados() {
		criar();
		criar();

		assertEquals(10, conversasDaDemonstracao().size());
		assertEquals(10, consultar("select c from Cliente c where c.empresa.nome = :nome", Cliente.class).size());
		assertEquals(1, consultar("select e from Empresa e where e.nome = :nome", Empresa.class).size());
		assertEquals(3, em.createQuery("select a from Atendente a where a.email like :email", Atendente.class)
				.setParameter("email", "%" + DOMINIO_EMAIL).getResultList().size());
		assertFalse(fila.visualizarFila().stream().filter(item -> item.clienteTelefone().startsWith("8199999010"))
				.toList().isEmpty());
	}
}
