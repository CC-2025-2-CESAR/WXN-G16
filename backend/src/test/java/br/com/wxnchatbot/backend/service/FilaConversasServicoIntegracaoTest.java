package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.StatusTicket;
import br.com.wxnchatbot.backend.model.Ticket;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import br.com.wxnchatbot.backend.model.Empresa;

@SpringBootTest
@Transactional
class FilaConversasServicoIntegracaoTest {

	@Autowired
	private FilaConversasServico servico;

	@Autowired
	private ConversaRepositorio conversas;

	@PersistenceContext
	private EntityManager em;

	private Empresa empresa;
	private Chatbot lily;

	@BeforeEach
	void criarChatbot() {
		empresa = new Empresa("Empresa Demo", "12345678000199");
   		em.persist(empresa);
		lily = new Chatbot("Lily", "Oi, sou a Lily!");
		em.persist(lily);
	}

	private Conversa novaConversa(String nome, String telefone) {
		Cliente cliente = new Cliente(empresa, nome, telefone);
		em.persist(cliente);
		Conversa conversa = new Conversa(lily);
		conversa.vincularCliente(cliente);
		em.persist(conversa);
		return conversa;
	}

	private Atendente novoAtendente(String nome, String email) {
		Atendente atendente = new Atendente(nome, email);
		em.persist(atendente);
		return atendente;
	}

	private void sincronizar() {
		em.flush();
		em.clear();
	}

	private static void esperar() {
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private List<UUID> filaEntre(Conversa... minhas) {
		Set<UUID> ids = Arrays.stream(minhas).map(Conversa::getId).collect(Collectors.toSet());
		return servico.visualizarFila().stream().map(ConversaNaFila::conversaId).filter(ids::contains).toList();
	}

	@Test
	void conversaQueAindaEstaComOChatbotNaoApareceNaFila() {
		Conversa conversa = novaConversa("Maria Souza", "81999990001");
		sincronizar();

		assertTrue(filaEntre(conversa).isEmpty());
	}

	@Test
	void filaMostraOsPedidosDoMaisAntigoParaOMaisNovo() {
		Conversa primeira = novaConversa("Maria Souza", "81999990001");
		Conversa segunda = novaConversa("Joao Pedro", "81999990002");
		Conversa terceira = novaConversa("Ana Lima", "81999990003");

		servico.solicitarAtendente(primeira.getId(), "Primeiro pedido");
		esperar();
		servico.solicitarAtendente(segunda.getId(), "Segundo pedido");
		esperar();
		servico.solicitarAtendente(terceira.getId(), "Terceiro pedido");
		sincronizar();

		assertEquals(List.of(primeira.getId(), segunda.getId(), terceira.getId()),
				filaEntre(primeira, segunda, terceira));
	}

	@Test
	void solicitarAtendenteDevolveAPosicaoQueConstaNaFila() {
		Conversa primeira = novaConversa("Maria Souza", "81999990001");
		Conversa segunda = novaConversa("Joao Pedro", "81999990002");
		servico.solicitarAtendente(primeira.getId(), "Primeiro pedido");
		esperar();

		ConversaNaFila resultado = servico.solicitarAtendente(segunda.getId(), "Segundo pedido");

		List<UUID> fila = servico.visualizarFila().stream().map(ConversaNaFila::conversaId).toList();
		assertEquals(fila.indexOf(segunda.getId()) + 1, resultado.posicao());
		assertTrue(fila.indexOf(primeira.getId()) < fila.indexOf(segunda.getId()));
	}

	@Test
	void cadaItemDaFilaTrazOsDadosDoSeuClienteEOMotivo() {
		Conversa maria = novaConversa("Maria Souza", "81999990001");
		Conversa joao = novaConversa("Joao Pedro", "81999990002");
		servico.solicitarAtendente(maria.getId(), "Quer falar com uma pessoa");
		esperar();
		servico.solicitarAtendente(joao.getId(), "Problema com a nota fiscal");
		sincronizar();

		List<ConversaNaFila> meus = servico.visualizarFila().stream()
				.filter(i -> i.conversaId().equals(maria.getId()) || i.conversaId().equals(joao.getId())).toList();

		assertEquals(2, meus.size());
		assertEquals("Maria Souza", meus.get(0).clienteNome());
		assertEquals("81999990001", meus.get(0).clienteTelefone());
		assertEquals("Quer falar com uma pessoa", meus.get(0).motivo());
		assertEquals("Joao Pedro", meus.get(1).clienteNome());
		assertEquals("81999990002", meus.get(1).clienteTelefone());
		assertEquals("Problema com a nota fiscal", meus.get(1).motivo());
	}

	@Test
	void detalheTrazAsMensagensEmOrdemComOsRemetentes() {
		Conversa conversa = novaConversa("Maria Souza", "81999990001");
		lily.saudar(conversa);
		esperar();
		conversa.adicionarMensagem(conversa.getCliente().orElseThrow(), "Preciso de ajuda");
		esperar();
		conversa.adicionarMensagem(conversa.getCliente().orElseThrow(), "Podem me ligar?");
		servico.solicitarAtendente(conversa.getId(), "Quer falar com uma pessoa");
		sincronizar();

		DetalheConversaFila detalhe = servico.abrirConversaDaFila(conversa.getId());

		assertEquals("Maria Souza", detalhe.clienteNome());
		assertEquals("Quer falar com uma pessoa", detalhe.motivo());
		assertEquals(List.of(TipoRemetente.CHATBOT, TipoRemetente.CLIENTE, TipoRemetente.CLIENTE),
				detalhe.mensagens().stream().map(DetalheConversaFila.MensagemResumo::tipoRemetente).toList());
		assertEquals(List.of("Oi, sou a Lily!", "Preciso de ajuda", "Podem me ligar?"),
				detalhe.mensagens().stream().map(DetalheConversaFila.MensagemResumo::conteudo).toList());
	}

	@Test
	void conversaAssumidaSaiDaFilaEFicaComOAtendente() {
		Conversa primeira = novaConversa("Maria Souza", "81999990001");
		Conversa segunda = novaConversa("Joao Pedro", "81999990002");
		Atendente carlos = novoAtendente("Carlos Lima", "carlos@wxn.com.br");
		servico.solicitarAtendente(primeira.getId(), "Primeiro pedido");
		esperar();
		servico.solicitarAtendente(segunda.getId(), "Segundo pedido");
		sincronizar();

		servico.atribuirAtendente(primeira.getId(), carlos.getId());
		sincronizar();

		assertEquals(List.of(segunda.getId()), filaEntre(primeira, segunda));
		assertThrows(IllegalStateException.class, () -> servico.abrirConversaDaFila(primeira.getId()));
		Conversa recarregada = conversas.findById(primeira.getId()).orElseThrow();
		assertEquals(StatusConversa.COM_ATENDENTE, recarregada.getStatus());
		assertEquals(carlos.getId(), recarregada.getAtendente().orElseThrow().getId());
		Ticket ticket = recarregada.getTicket().orElseThrow();
		assertEquals(StatusTicket.EM_ATENDIMENTO, ticket.getStatus());
		assertEquals(carlos.getId(), ticket.getAtendente().orElseThrow().getId());
	}

	@Test
	void atendenteIndisponivelNaoAssumeEAConversaContinuaNaFila() {
		Conversa conversa = novaConversa("Maria Souza", "81999990001");
		Atendente carlos = novoAtendente("Carlos Lima", "carlos@wxn.com.br");
		carlos.marcarIndisponivel();
		servico.solicitarAtendente(conversa.getId(), "Quer falar com uma pessoa");
		sincronizar();

		assertThrows(IllegalStateException.class, () -> servico.atribuirAtendente(conversa.getId(), carlos.getId()));

		assertEquals(List.of(conversa.getId()), filaEntre(conversa));
	}

	@Test
	void atribuirComIdsQueNaoExistemDaNaoEncontrado() {
		Conversa conversa = novaConversa("Maria Souza", "81999990001");
		servico.solicitarAtendente(conversa.getId(), "Quer falar com uma pessoa");
		sincronizar();

		assertThrows(RecursoNaoEncontradoException.class,
				() -> servico.atribuirAtendente(UUID.randomUUID(), UUID.randomUUID()));
		assertThrows(RecursoNaoEncontradoException.class,
				() -> servico.atribuirAtendente(conversa.getId(), UUID.randomUUID()));
		assertThrows(RecursoNaoEncontradoException.class, () -> servico.abrirConversaDaFila(UUID.randomUUID()));
		assertThrows(RecursoNaoEncontradoException.class, () -> servico.encerrar(UUID.randomUUID()));
	}

	@Test
	void solicitarAtendenteDuasVezesDaErroEMantemOMotivoOriginal() {
		Conversa conversa = novaConversa("Maria Souza", "81999990001");
		servico.solicitarAtendente(conversa.getId(), "Primeiro pedido");
		sincronizar();

		assertThrows(IllegalStateException.class, () -> servico.solicitarAtendente(conversa.getId(), "Segundo pedido"));

		DetalheConversaFila detalhe = servico.abrirConversaDaFila(conversa.getId());
		assertEquals("Primeiro pedido", detalhe.motivo());
	}

	@Test
	void encerrarTiraDaFilaEPersisteOEncerramentoEOTicketResolvido() {
		Conversa conversa = novaConversa("Maria Souza", "81999990001");
		servico.solicitarAtendente(conversa.getId(), "Quer falar com uma pessoa");
		sincronizar();

		servico.encerrar(conversa.getId());
		sincronizar();

		assertTrue(filaEntre(conversa).isEmpty());
		Conversa recarregada = conversas.findById(conversa.getId()).orElseThrow();
		assertEquals(StatusConversa.ENCERRADA, recarregada.getStatus());
		assertTrue(recarregada.getEncerradaEm().isPresent());
		Ticket ticket = recarregada.getTicket().orElseThrow();
		assertEquals(StatusTicket.RESOLVIDO, ticket.getStatus());
		assertTrue(ticket.getResolvidoEm().isPresent());
		assertThrows(IllegalStateException.class, () -> servico.encerrar(conversa.getId()));
	}

	@Test
	void conversaPersistidaVoltaDoBancoComClienteChatbotMensagensERemetentes() {
		Conversa conversa = novaConversa("Maria Souza", "81999990001");
		lily.saudar(conversa);
		esperar();
		conversa.adicionarMensagem(conversa.getCliente().orElseThrow(), "Preciso de ajuda");
		sincronizar();

		Conversa recarregada = conversas.findById(conversa.getId()).orElseThrow();

		assertEquals("Maria Souza", recarregada.getCliente().orElseThrow().getNome());
		assertEquals("81999990001", recarregada.getCliente().orElseThrow().getTelefone());
		assertEquals("Lily", recarregada.getChatbot().getNome());
		assertEquals(StatusConversa.COM_CHATBOT, recarregada.getStatus());
		assertEquals(2, recarregada.getMensagens().size());
		assertEquals("Lily", recarregada.getMensagens().get(0).getRemetente().getNome());
		assertEquals("Maria Souza", recarregada.getMensagens().get(1).getRemetente().getNome());
		assertEquals(TipoRemetente.CLIENTE, recarregada.getMensagens().get(1).getTipoRemetente());
	}
}