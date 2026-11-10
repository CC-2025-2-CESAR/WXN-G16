package br.com.wxnchatbot.backend.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.llm.LlmClient;
import br.com.wxnchatbot.backend.llm.LlmIndisponivelException;
import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.llm.MensagemLlm.Papel;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ClassificadorComLlmTest {

	@Mock
	private LlmClient llm;

	private ClassificadorComLlm classificador;

	@BeforeEach
	void preparar() {
		classificador = new ClassificadorComLlm(llm, new ClassificadorPorRegras(new ExtratorDeProtocolo()),
				JsonMapper.builder().build());
	}

	private static List<MensagemLlm> conversa(String ultima) {
		return List.of(new MensagemLlm(Papel.USUARIO, ultima));
	}

	@Test
	void usaAIntencaoEAConfiancaQueALlmDevolve() {
		when(llm.responderEmJson(any(), any(), any())).thenReturn("{\"intencao\":\"FORA_DE_ESCOPO\",\"confianca\":0.92}");

		ResultadoIntencao resultado = classificador.classificar(conversa("Me conta uma piada"));

		assertEquals(new ResultadoIntencao(Intencao.FORA_DE_ESCOPO, 0.92), resultado);
	}

	@Test
	void mandaOPromptOEsquemaEPoucoContexto() {
		when(llm.responderEmJson(any(), any(), any())).thenReturn("{\"intencao\":\"DUVIDA\",\"confianca\":1}");
		List<MensagemLlm> longa = new ArrayList<>();
		for (int i = 1; i <= 10; i++) {
			longa.add(new MensagemLlm(i % 2 == 1 ? Papel.USUARIO : Papel.ASSISTENTE, "mensagem " + i));
		}

		classificador.classificar(longa);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<MensagemLlm>> enviado = ArgumentCaptor.forClass(List.class);
		verify(llm).responderEmJson(eq(ClassificadorComLlm.PROMPT), enviado.capture(), eq(ClassificadorComLlm.ESQUEMA));
		assertEquals(6, enviado.getValue().size());
		assertEquals("mensagem 10", enviado.getValue().get(5).conteudo());
		for (Intencao intencao : Intencao.values()) {
			assertTrue(ClassificadorComLlm.PROMPT.contains(intencao.name()));
		}
	}

	@Test
	void pedidoDeAtendenteNemChegaALlm() {
		ResultadoIntencao resultado = classificador.classificar(conversa("Quero falar com um atendente"));

		assertEquals(ResultadoIntencao.certa(Intencao.FALAR_COM_HUMANO), resultado);
		verifyNoInteractions(llm);
	}

	@Test
	void protocoloNaMensagemNemChegaALlm() {
		ResultadoIntencao resultado = classificador.classificar(conversa("WXN-2026-0001"));

		assertEquals(Intencao.CONSULTAR_DEMANDA, resultado.intencao());
		verifyNoInteractions(llm);
	}

	@Test
	void llmForaDoArUsaAsRegras() {
		when(llm.responderEmJson(any(), any(), any())).thenThrow(new LlmIndisponivelException("429", null));

		ResultadoIntencao resultado = classificador.classificar(conversa("Boa tarde"));

		assertEquals(ResultadoIntencao.certa(Intencao.DUVIDA), resultado);
	}

	@Test
	void respostaInvalidaDaLlmUsaAsRegras() {
		when(llm.responderEmJson(any(), any(), any())).thenReturn("isso não é json");

		assertEquals(Intencao.DUVIDA, classificador.classificar(conversa("Boa tarde")).intencao());
	}

	@Test
	void intencaoQueNaoExisteUsaAsRegras() {
		when(llm.responderEmJson(any(), any(), any())).thenReturn("{\"intencao\":\"COMPRAR_PIZZA\",\"confianca\":1}");

		assertEquals(Intencao.DUVIDA, classificador.classificar(conversa("Boa tarde")).intencao());
	}

	@Test
	void leOQueALlmAchouDosDetalhesDoChamado() {
		assertEquals(Boolean.TRUE, classificador
				.interpretar("{\"intencao\":\"ABRIR_CHAMADO\",\"confianca\":0.9,\"descricaoSuficiente\":true}").descricaoSuficiente());
		assertEquals(Boolean.FALSE, classificador
				.interpretar("{\"intencao\":\"ABRIR_CHAMADO\",\"confianca\":0.9,\"descricaoSuficiente\":false}").descricaoSuficiente());
	}

	@Test
	void semOuComValorInvalidoOsDetalhesFicamSemOpiniao() {
		assertNull(classificador.interpretar("{\"intencao\":\"ABRIR_CHAMADO\",\"confianca\":0.9}").descricaoSuficiente());
		assertNull(classificador.interpretar("{\"intencao\":\"ABRIR_CHAMADO\",\"confianca\":0.9,\"descricaoSuficiente\":\"sim\"}")
				.descricaoSuficiente());
	}

	@Test
	void oPromptEOEsquemaPedemAAvaliacaoDosDetalhes() {
		assertTrue(ClassificadorComLlm.PROMPT.contains("descricaoSuficiente"));
		@SuppressWarnings("unchecked")
		Map<String, Object> propriedades = (Map<String, Object>) ClassificadorComLlm.ESQUEMA.get("properties");
		assertTrue(propriedades.containsKey("descricaoSuficiente"));
		assertEquals(List.of("intencao", "confianca"), ClassificadorComLlm.ESQUEMA.get("required"));
	}

	@Test
	void confiancaForaDaFaixaFicaEntreZeroEUm() {
		assertEquals(1.0, classificador.interpretar("{\"intencao\":\"duvida\",\"confianca\":7}").confianca());
		assertEquals(0.0, classificador.interpretar("{\"intencao\":\"DUVIDA\",\"confianca\":-2}").confianca());
		assertEquals(0.0, classificador.interpretar("{\"intencao\":\"DUVIDA\"}").confianca());
	}
}
