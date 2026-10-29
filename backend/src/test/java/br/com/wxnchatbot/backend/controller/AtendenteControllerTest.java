package br.com.wxnchatbot.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.wxnchatbot.backend.service.AtendenteResumo;
import br.com.wxnchatbot.backend.service.AtendenteServico;

@WebMvcTest(AtendenteController.class)
class AtendenteControllerTest {

	private static final UUID ANA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID PAULO = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private AtendenteServico servico;

	@Test
	void listaOsAtendentesNaOrdemDoServicoSoComOsCamposDoContrato() throws Exception {
		when(servico.listar()).thenReturn(List.of(
				new AtendenteResumo(ANA, "Ana Martins", "ana@wxn.example", true),
				new AtendenteResumo(PAULO, "Paulo Mendes", "paulo@wxn.example", false)));

		mvc.perform(get("/api/atendentes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].*", hasSize(4)))
				.andExpect(jsonPath("$[0].id").value(ANA.toString()))
				.andExpect(jsonPath("$[0].nome").value("Ana Martins"))
				.andExpect(jsonPath("$[0].email").value("ana@wxn.example"))
				.andExpect(jsonPath("$[0].disponivel").value(true))
				.andExpect(jsonPath("$[1].id").value(PAULO.toString()))
				.andExpect(jsonPath("$[1].disponivel").value(false));
	}

	@Test
	void semAtendentesDevolve200ComListaVazia() throws Exception {
		when(servico.listar()).thenReturn(List.of());

		mvc.perform(get("/api/atendentes"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}
}
