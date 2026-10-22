package br.com.wxnchatbot.backend.integracao.wxn.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.wxnchatbot.backend.integracao.wxn.CabecalhosWxn;

class MockWxnControllerTest {

	private static final long ATRASO_MS = 300;

	private MockMvc mvc;

	@BeforeEach
	void preparar() {
		mvc = MockMvcBuilders.standaloneSetup(new MockWxnController(new MockWxnDados(), ATRASO_MS)).build();
	}

	private MockHttpServletResponse consultar(UUID empresa, String protocolo) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.get("/mock/wxn/demandas/{p}", protocolo)
				.header(CabecalhosWxn.EMPRESA_ID, empresa.toString())).andReturn().getResponse();
	}

	private MockHttpServletResponse abrir(UUID empresa, String chave, String json) throws Exception {
		MockHttpServletRequestBuilder requisicao = MockMvcRequestBuilders.post("/mock/wxn/chamados")
				.header(CabecalhosWxn.EMPRESA_ID, empresa.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
		if (chave != null) {
			requisicao.header(CabecalhosWxn.CHAVE_IDEMPOTENCIA, chave);
		}
		return mvc.perform(requisicao).andReturn().getResponse();
	}

	@Test
	void demandaExistenteDevolve200ComOsDados() throws Exception {
		MockHttpServletResponse resposta = consultar(MockWxnDados.EMPRESA_DEMO, "WXN-2026-0001");

		assertEquals(200, resposta.getStatus());
		String corpo = resposta.getContentAsString();
		assertTrue(corpo.contains("\"protocolo\":\"WXN-2026-0001\""), corpo);
		assertTrue(corpo.contains("\"status\":\"EM_ANDAMENTO\""), corpo);
	}

	@Test
	void protocoloInexistenteDevolve404ComCodigoDeNaoEncontrada() throws Exception {
		MockHttpServletResponse resposta = consultar(MockWxnDados.EMPRESA_DEMO, "WXN-2026-7777");

		assertEquals(404, resposta.getStatus());
		assertTrue(resposta.getContentAsString().contains("DEMANDA_NAO_ENCONTRADA"));
	}

	@Test
	void protocoloDeOutraEmpresaNaoApareceParaEstaEmpresa() throws Exception {
		assertEquals(200, consultar(MockWxnDados.EMPRESA_OUTRA, "WXN-2026-9001").getStatus());
		assertEquals(404, consultar(MockWxnDados.EMPRESA_DEMO, "WXN-2026-9001").getStatus());
		assertEquals(404, consultar(UUID.randomUUID(), "WXN-2026-0001").getStatus());
	}

	@Test
	void protocoloComFormatoInvalidoDevolve400() throws Exception {
		MockHttpServletResponse resposta = consultar(MockWxnDados.EMPRESA_DEMO, "wxn 2026");

		assertEquals(400, resposta.getStatus());
		assertTrue(resposta.getContentAsString().contains("PROTOCOLO_INVALIDO"));
	}

	@Test
	void protocoloDeErroSimuladoDevolve500() throws Exception {
		assertEquals(500, consultar(MockWxnDados.EMPRESA_DEMO, MockWxnController.PROTOCOLO_ERRO).getStatus());
	}

	@Test
	void protocoloLentoSoRespondeDepoisDoAtrasoConfigurado() throws Exception {
		long inicio = System.nanoTime();
		MockHttpServletResponse resposta = consultar(MockWxnDados.EMPRESA_DEMO, MockWxnController.PROTOCOLO_LENTO);
		long decorridoMs = (System.nanoTime() - inicio) / 1_000_000;

		assertEquals(200, resposta.getStatus());
		assertTrue(decorridoMs >= ATRASO_MS - 20, "respondeu rápido demais: " + decorridoMs + " ms");
	}

	@Test
	void cabecalhoDeEmpresaAusenteOuInvalidoDevolve400() throws Exception {
		MockHttpServletResponse semCabecalho = mvc
				.perform(MockMvcRequestBuilders.get("/mock/wxn/demandas/WXN-2026-0001")).andReturn().getResponse();
		MockHttpServletResponse cabecalhoInvalido = mvc.perform(MockMvcRequestBuilders
				.get("/mock/wxn/demandas/WXN-2026-0001").header(CabecalhosWxn.EMPRESA_ID, "nao-e-uuid")).andReturn()
				.getResponse();

		assertEquals(400, semCabecalho.getStatus());
		assertEquals(400, cabecalhoInvalido.getStatus());
		assertTrue(cabecalhoInvalido.getContentAsString().contains("REQUISICAO_INVALIDA"));
	}

	@Test
	void chamadoAbertoDevolve201EPassaAExistirParaConsulta() throws Exception {
		MockHttpServletResponse criado = abrir(MockWxnDados.EMPRESA_DEMO, null,
				"{\"titulo\":\"Erro ao emitir nota\",\"descricao\":\"Trava na assinatura\"}");

		assertEquals(201, criado.getStatus());
		String corpo = criado.getContentAsString();
		String protocolo = corpo.replaceAll(".*\"protocolo\":\"([^\"]+)\".*", "$1");
		assertTrue(protocolo.startsWith("WXN-2026-"), corpo);
		assertEquals(200, consultar(MockWxnDados.EMPRESA_DEMO, protocolo).getStatus());
		assertEquals(404, consultar(MockWxnDados.EMPRESA_OUTRA, protocolo).getStatus(),
				"chamado de uma empresa não aparece para outra");
	}

	@Test
	void mesmaChaveDeIdempotenciaNaoAbreDoisChamados() throws Exception {
		String chave = UUID.randomUUID().toString();
		String json = "{\"titulo\":\"Erro ao emitir nota\",\"descricao\":\"\"}";

		String primeiro = abrir(MockWxnDados.EMPRESA_DEMO, chave, json).getContentAsString();
		String repetido = abrir(MockWxnDados.EMPRESA_DEMO, chave, json).getContentAsString();
		String outraChave = abrir(MockWxnDados.EMPRESA_DEMO, UUID.randomUUID().toString(), json).getContentAsString();

		assertEquals(primeiro, repetido);
		assertNotEquals(primeiro, outraChave);
	}

	@Test
	void chamadoComDadosInvalidosDevolve400() throws Exception {
		assertEquals(400, abrir(MockWxnDados.EMPRESA_DEMO, null, "{\"titulo\":\"\",\"descricao\":\"x\"}").getStatus());
		assertEquals(400, abrir(MockWxnDados.EMPRESA_DEMO, null, "isto nao e json").getStatus());
	}
}
