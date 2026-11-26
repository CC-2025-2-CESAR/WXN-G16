package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Cada filtro do histórico vazio e preenchido, contra o PostgreSQL de verdade (o mesmo dos outros testes de integração,
 * sem Docker). Com {@code :filtro is null} na consulta, o PostgreSQL recusava a consulta inteira (SQLState 42P18) assim
 * que um filtro vinha vazio, ou seja, na tela de histórico ao abrir.
 */
@SpringBootTest
@Transactional
class HistoricoFiltrosVaziosIntegracaoTest {

	@Autowired
	private HistoricoServico servico;

	@PersistenceContext
	private EntityManager em;

	private Conversa encerrada(Atendente atendente) {
		Empresa empresa = new Empresa("Empresa do Historico Vazio");
		Chatbot lily = new Chatbot("Lily", "Oi!");
		Cliente cliente = new Cliente(empresa, "Cliente Do Filtro", "81955550001");
		em.persist(empresa);
		em.persist(lily);
		em.persist(cliente);
		Conversa conversa = new Conversa(cliente, lily, OrigemConversa.TELEGRAM);
		em.persist(conversa);
		conversa.adicionarMensagem(cliente, "Preciso de ajuda com a nota");
		conversa.solicitarAtendente("Nota fiscal");
		conversa.atribuirAtendente(atendente);
		conversa.encerrar("Resolvido");
		em.flush();
		return conversa;
	}

	private Atendente atendente() {
		Atendente atendente = new Atendente("Atendente Do Filtro", "atendente.filtro@wxn.com.br");
		em.persist(atendente);
		return atendente;
	}

	@Test
	void semNenhumFiltroAConsultaFuncionaETrazAConversaEncerrada() {
		Conversa conversa = encerrada(atendente());

		PaginaHistorico pagina = servico.listar(null, null, null, null, null, 0, 100);

		assertTrue(pagina.total() >= 1);
		assertTrue(pagina.itens().stream().anyMatch(item -> item.conversaId().equals(conversa.getId())));
	}

	@Test
	void cadaFiltroSozinhoFuncionaEOsOutrosFicamVazios() {
		Atendente atendente = atendente();
		Conversa conversa = encerrada(atendente);
		LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

		assertEquals(1, servico.listar("cliente do filtro", null, null, atendente.getId(), null, 0, 20).total());
		assertEquals(1, servico.listar(null, hoje, null, atendente.getId(), null, 0, 20).total());
		assertEquals(1, servico.listar(null, null, hoje, atendente.getId(), null, 0, 20).total());
		assertEquals(1, servico.listar(null, null, null, atendente.getId(), null, 0, 20).total());
		assertEquals(1, servico.listar(null, null, null, atendente.getId(), OrigemConversa.TELEGRAM, 0, 20).total());
		assertEquals(0, servico.listar(null, null, null, atendente.getId(), OrigemConversa.WHATSAPP, 0, 20).total());
		assertEquals(1, servico.listar(conversa.getId().toString(), null, null, null, null, 0, 20).total());
		assertEquals(0, servico.listar(null, hoje.plusDays(1), null, atendente.getId(), null, 0, 20).total());
	}

	@Test
	void buscaQueNaoAchaNadaDevolveListaVaziaSemErro() {
		assertEquals(0, servico.listar("zzz-nao-existe-100%_x", null, null, null, null, 0, 20).total());
	}
}
