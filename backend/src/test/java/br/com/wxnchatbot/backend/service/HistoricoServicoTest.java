package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;

@ExtendWith(MockitoExtension.class)
class HistoricoServicoTest {
	@Mock
	private ConversaRepositorio conversas;
	private HistoricoServico servico;

	@BeforeEach
	void preparar() {
		servico = new HistoricoServico(conversas);
	}

	private Conversa conversaEncerrada() {
		Empresa empresa = new Empresa("Empresa Demo");
		Cliente cliente = new Cliente(empresa, "Maria Souza", "81999990001");
		Conversa conversa = new Conversa(cliente, new Chatbot("Lily", "Oi!"), OrigemConversa.SIMULADOR);
		conversa.adicionarMensagem(cliente, "Relatório WXN-2026-0003 está divergente");
		conversa.solicitarAtendente("Relatório divergente");
		Atendente atendente = new Atendente("Ana Martins", "ana@wxn.com.br");
		conversa.atribuirAtendente(atendente);
		conversa.adicionarMensagem(atendente, "Vou verificar o relatório.");
		conversa.encerrar("Cliente orientado a refazer a consulta.");
		return conversa;
	}

	@Test
	void listaComFiltrosEContagemSemCarregarMensagensDeCadaItem() {
		Conversa conversa = conversaEncerrada();
		UUID atendenteId = conversa.getAtendente().orElseThrow().getId();
		var pagina = PageRequest.of(0, 20);
		Instant inicio = Instant.parse("2026-11-26T03:00:00Z");
		Instant fim = Instant.parse("2026-11-27T03:00:00Z");
		when(conversas.buscarHistorico("%relatório%", null, inicio, fim, atendenteId,
				OrigemConversa.SIMULADOR, pagina))
				.thenReturn(new PageImpl<>(List.of(conversa), pagina, 1));
		when(conversas.contarMensagens(List.of(conversa.getId())))
				.thenReturn(List.<Object[]>of(new Object[] { conversa.getId(), 2L }));

		PaginaHistorico resultado = servico.listar("  Relatório  ", LocalDate.of(2026, 11, 26),
				LocalDate.of(2026, 11, 26), atendenteId, OrigemConversa.SIMULADOR, 0, 20);

		assertEquals(1, resultado.total());
		assertEquals(2, resultado.itens().getFirst().totalMensagens());
		assertEquals("Maria Souza", resultado.itens().getFirst().clienteNome());
		assertEquals("Relatório divergente", resultado.itens().getFirst().motivo());
		assertEquals("Ana Martins", resultado.itens().getFirst().atendenteNome());
		assertEquals("Cliente orientado a refazer a consulta.", resultado.itens().getFirst().resumoEncerramento());
		verify(conversas).contarMensagens(List.of(conversa.getId()));
	}

	@Test
	void detalheTrazMensagensEmOrdemESomenteSeEncerrada() {
		Conversa encerrada = conversaEncerrada();
		when(conversas.findById(encerrada.getId())).thenReturn(Optional.of(encerrada));

		DetalheHistorico detalhe = servico.detalhar(encerrada.getId());
		assertEquals(2, detalhe.totalMensagens());
		assertEquals("Relatório WXN-2026-0003 está divergente", detalhe.mensagens().getFirst().conteudo());
		assertEquals("Vou verificar o relatório.", detalhe.mensagens().get(1).conteudo());
		assertTrue(detalhe.encerradaEm() != null);

		Conversa ativa = new Conversa(new Cliente(new Empresa("Outra"), "João", "81999990002"),
				new Chatbot("Lily", "Oi!"));
		when(conversas.findById(ativa.getId())).thenReturn(Optional.of(ativa));
		assertThrows(RecursoNaoEncontradoException.class, () -> servico.detalhar(ativa.getId()));
	}

	@Test
	void filtrosInvalidosNaoConsultamRepositorio() {
		assertThrows(IllegalArgumentException.class,
				() -> servico.listar(null, null, null, null, null, -1, 20));
		assertThrows(IllegalArgumentException.class,
				() -> servico.listar(null, null, null, null, null, 0, 101));
		assertThrows(IllegalArgumentException.class,
				() -> servico.listar(null, LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 26),
						null, null, 0, 20));
		verifyNoInteractions(conversas);
	}
}
