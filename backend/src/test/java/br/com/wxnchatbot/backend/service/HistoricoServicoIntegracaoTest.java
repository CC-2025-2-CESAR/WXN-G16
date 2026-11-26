package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.PostgresTestcontainersConfig;
import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
@Transactional
class HistoricoServicoIntegracaoTest {
	@Autowired
	private HistoricoServico servico;
	@PersistenceContext
	private EntityManager em;

	@Test
	void historicoFiltraEncerradasPorConteudoDataAtendenteEOrigem() {
		Empresa empresa = new Empresa("Empresa Demo");
		Chatbot lily = new Chatbot("Lily", "Oi!");
		Atendente ana = new Atendente("Ana Martins", "ana@wxn.com.br");
		Atendente paulo = new Atendente("Paulo Mendes", "paulo@wxn.com.br");
		em.persist(empresa);
		em.persist(lily);
		em.persist(ana);
		em.persist(paulo);

		Conversa principal = conversa(empresa, lily, "Maria Souza", "81999990001", OrigemConversa.SIMULADOR);
		principal.adicionarMensagem(principal.getCliente(), "Protocolo WXN-2026-0003 está divergente");
		principal.solicitarAtendente("Relatório de vendas");
		principal.atribuirAtendente(ana);
		principal.adicionarMensagem(ana, "Vou conferir.");
		principal.encerrar("Relatório corrigido.");

		Conversa outra = conversa(empresa, lily, "João Pedro", "81999990002", OrigemConversa.WHATSAPP);
		outra.adicionarMensagem(outra.getCliente(), "Preciso de ajuda com acesso");
		outra.solicitarAtendente("Acesso bloqueado");
		outra.atribuirAtendente(paulo);
		outra.encerrar();

		Conversa ativa = conversa(empresa, lily, "Lia Santos", "81999990003", OrigemConversa.SIMULADOR);
		ativa.adicionarMensagem(ativa.getCliente(), "Preciso de ajuda");
		ativa.solicitarAtendente("Dúvida");
		ativa.atribuirAtendente(ana);
		em.flush();
		em.clear();

		LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		PaginaHistorico pagina = servico.listar("WXN-2026-0003", hoje, hoje, ana.getId(),
				OrigemConversa.SIMULADOR, 0, 20);
		assertEquals(1, pagina.total());
		assertEquals(principal.getId(), pagina.itens().getFirst().conversaId());
		assertEquals(2, pagina.itens().getFirst().totalMensagens());
		assertEquals("Relatório corrigido.", pagina.itens().getFirst().resumoEncerramento());

		DetalheHistorico detalhe = servico.detalhar(principal.getId());
		assertEquals(2, detalhe.mensagens().size());
		assertEquals("Ana Martins", detalhe.atendenteNome());
		assertThrows(RecursoNaoEncontradoException.class, () -> servico.detalhar(ativa.getId()));
		assertEquals(0, servico.listar(null, hoje.plusDays(1), hoje.plusDays(1), null, null, 0, 20).total());
	}

	private Conversa conversa(Empresa empresa, Chatbot lily, String nome, String telefone, OrigemConversa origem) {
		Cliente cliente = new Cliente(empresa, nome, telefone);
		em.persist(cliente);
		Conversa conversa = new Conversa(cliente, lily, origem);
		em.persist(conversa);
		return conversa;
	}
}
