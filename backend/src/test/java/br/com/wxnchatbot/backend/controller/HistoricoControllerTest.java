package br.com.wxnchatbot.backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.service.DetalheHistorico;
import br.com.wxnchatbot.backend.service.HistoricoServico;
import br.com.wxnchatbot.backend.service.ItemHistorico;
import br.com.wxnchatbot.backend.service.PaginaHistorico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;

@WebMvcTest(HistoricoController.class)
class HistoricoControllerTest {
	private static final UUID CONVERSA = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ATENDENTE = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired
	private MockMvc mvc;
	@MockitoBean
	private HistoricoServico servico;

	@Test
	void listaEncerradasComFiltrosEPaginacao() throws Exception {
		LocalDate data = LocalDate.of(2026, 11, 26);
		Instant instante = Instant.parse("2026-11-26T15:00:00Z");
		ItemHistorico item = new ItemHistorico(CONVERSA, "Maria Souza", "81999990001", "Relatório",
				OrigemConversa.SIMULADOR, null, ATENDENTE, "Ana Martins", instante, instante,
				instante, "Resolvido", 3);
		when(servico.listar("Maria", data, data, ATENDENTE, OrigemConversa.SIMULADOR, 1, 10))
				.thenReturn(new PaginaHistorico(List.of(item), 11, 1, 10));

		mvc.perform(get("/api/historico")
				.param("busca", "Maria")
				.param("dataInicial", "2026-11-26")
				.param("dataFinal", "2026-11-26")
				.param("atendenteId", ATENDENTE.toString())
				.param("origem", "SIMULADOR")
				.param("pagina", "1")
				.param("tamanho", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(11))
				.andExpect(jsonPath("$.itens[0].clienteNome").value("Maria Souza"))
				.andExpect(jsonPath("$.itens[0].resumoEncerramento").value("Resolvido"))
				.andExpect(jsonPath("$.itens[0].totalMensagens").value(3));
		verify(servico).listar("Maria", data, data, ATENDENTE, OrigemConversa.SIMULADOR, 1, 10);
	}

	@Test
	void detalheRetornaMensagensEConversaAusenteRetorna404() throws Exception {
		Instant instante = Instant.parse("2026-11-26T15:00:00Z");
		DetalheHistorico detalhe = new DetalheHistorico(CONVERSA, "Maria Souza", "81999990001",
				"Relatório", OrigemConversa.SIMULADOR, null, ATENDENTE, "Ana Martins",
				instante, instante, instante, "Resolvido", 1,
				List.of(new DetalheHistorico.MensagemHistorico(
						br.com.wxnchatbot.backend.model.TipoRemetente.CLIENTE, "Oi", instante)));
		when(servico.detalhar(CONVERSA)).thenReturn(detalhe);

		mvc.perform(get("/api/historico/{id}", CONVERSA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensagens[0].conteudo").value("Oi"));

		UUID ausente = UUID.randomUUID();
		when(servico.detalhar(ausente)).thenThrow(new RecursoNaoEncontradoException("Conversa", ausente));
		mvc.perform(get("/api/historico/{id}", ausente))
				.andExpect(status().isNotFound());
	}
}
