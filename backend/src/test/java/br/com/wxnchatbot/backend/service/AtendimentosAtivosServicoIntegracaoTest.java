package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
@Transactional
class AtendimentosAtivosServicoIntegracaoTest {

	@Autowired
	private AtendimentosAtivosServico servico;

	@PersistenceContext
	private EntityManager em;

	@Test
	void listaApenasConversasAtivasDoAtendenteDepoisDeRecarregarDoBanco() {
		Empresa empresa = new Empresa("Empresa Demo");
		Chatbot lily = new Chatbot("Lily", "Oi, sou a Lily!");
		Atendente ana = new Atendente("Ana Martins", "ana@wxn.com.br");
		Atendente paulo = new Atendente("Paulo Mendes", "paulo@wxn.com.br");
		em.persist(empresa);
		em.persist(lily);
		em.persist(ana);
		em.persist(paulo);

		Conversa minha = conversa(empresa, lily, "Maria Souza", "81999990001", "Nota fiscal");
		minha.atribuirAtendente(ana);
		minha.adicionarMensagem(ana, "Vou verificar para você.");
		Conversa deOutro = conversa(empresa, lily, "João Pedro", "81999990002", "Acesso bloqueado");
		deOutro.atribuirAtendente(paulo);
		Conversa encerrada = conversa(empresa, lily, "Lia Santos", "81999990003", "Cancelamento");
		encerrada.atribuirAtendente(ana);
		encerrada.encerrar();
		em.flush();
		em.clear();

		List<AtendimentoAtivo> resultado = servico.listar(ana.getId());

		assertEquals(1, resultado.size());
		AtendimentoAtivo ativo = resultado.getFirst();
		assertEquals(minha.getId(), ativo.detalhe().conversaId());
		assertEquals("Maria Souza", ativo.detalhe().clienteNome());
		assertEquals("Nota fiscal", ativo.detalhe().motivo());
		assertEquals(2, ativo.detalhe().mensagens().size());
		assertEquals(ana.getId(), ativo.atendente().id());
		assertNotNull(ativo.assumidaEm());
	}

	private Conversa conversa(Empresa empresa, Chatbot lily, String nome, String telefone, String motivo) {
		Cliente cliente = new Cliente(empresa, nome, telefone);
		em.persist(cliente);
		Conversa conversa = new Conversa(cliente, lily);
		em.persist(conversa);
		conversa.adicionarMensagem(cliente, "Preciso de ajuda");
		conversa.solicitarAtendente(motivo);
		return conversa;
	}
}
