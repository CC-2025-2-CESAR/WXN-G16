package br.com.wxnchatbot.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.integracao.wxn.Chamado;
import br.com.wxnchatbot.backend.integracao.wxn.Demanda;
import br.com.wxnchatbot.backend.integracao.wxn.DemandaGateway;
import br.com.wxnchatbot.backend.integracao.wxn.NovoChamado;
import br.com.wxnchatbot.backend.integracao.wxn.mock.MockWxnDados;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.service.RespostaChat;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.databind.json.JsonMapper;

/**
 * Chat com a configuração padrão do perfil dev e os dados de exemplo da API simulada da WXN: a empresa que o chat
 * cria tem de ser a mesma que o mock conhece, senão a consulta de demanda nunca acha nada. Roda dentro de uma
 * transação que é desfeita no fim, então não deixa nada no banco.
 */
@SpringBootTest(properties = "chat.empresa-padrao=Empresa Teste da API Simulada")
@AutoConfigureMockMvc
@Transactional
class ConsultaComApiSimuladaIntegracaoTest {

	private static final String TELEFONE = "5581988880002";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JsonMapper json;

	@PersistenceContext
	private EntityManager em;

	@TestConfiguration
	static class GatewayQueLeOMock {

		@Bean
		@Primary
		DemandaGateway gatewayDoMock(MockWxnDados dados) {
			return new DemandaGateway() {
				@Override
				public Optional<Demanda> consultarPorProtocolo(UUID empresaId, String protocolo) {
					return dados.buscar(empresaId, protocolo);
				}

				@Override
				public Chamado abrirChamado(UUID empresaId, NovoChamado chamado, UUID chaveIdempotencia) {
					return dados.abrir(empresaId, chamado, chaveIdempotencia.toString());
				}
			};
		}
	}

	private RespostaChat enviar(String texto) throws Exception {
		String corpo = mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("telefone", TELEFONE, "nome", "Teste do Chat", "texto", texto))))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		return json.readValue(corpo, RespostaChat.class);
	}

	@Test
	void consultaAchaOProtocoloDeExemploDaEmpresaPadraoDoChat() throws Exception {
		RespostaChat resposta = enviar("Qual o status da WXN-2026-0001?");

		assertTrue(resposta.resposta().contains("Ajuste no relatório de vendas do ERP"), resposta.resposta());
		assertTrue(resposta.resposta().contains("em andamento"), resposta.resposta());
		em.flush();
		em.clear();
		assertEquals(MockWxnDados.EMPRESA_DEMO, em.find(Conversa.class, resposta.conversaId()).getEmpresa().getId());
	}

	@Test
	void protocoloDeOutraEmpresaDoMockContinuaInvisivelParaOChat() throws Exception {
		RespostaChat resposta = enviar("Qual o status da WXN-2026-9001?");

		assertTrue(resposta.resposta().contains("Não encontrei a demanda WXN-2026-9001"), resposta.resposta());
	}
}
