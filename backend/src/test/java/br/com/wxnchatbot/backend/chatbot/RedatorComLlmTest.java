package br.com.wxnchatbot.backend.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

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

@ExtendWith(MockitoExtension.class)
class RedatorComLlmTest {

	private static final String RESUMO = "Demanda WXN-2026-0001 - Ajuste no relatório de vendas. Status: em andamento. Última atualização em 20/10/2026.";
	private static final String PEDIDO = "Vou abrir um chamado com o título \"O financeiro não abre\" e a descrição \"Depois da troca de senha\". Posso confirmar?";
	private static final List<MensagemLlm> HISTORICO = List.of(new MensagemLlm(Papel.USUARIO, "e aquela demanda?"));

	@Mock
	private LlmClient llm;

	private RedatorComLlm redator;

	@BeforeEach
	void preparar() {
		redator = new RedatorComLlm(llm);
	}

	private String escrever(Situacao situacao, String rascunho, String respostaDaLlm) {
		when(llm.responder(anyString(), any())).thenReturn(respostaDaLlm);
		return redator.escrever(situacao, rascunho, HISTORICO);
	}

	@Test
	void usaOTextoDaLlmQuandoOsFatosSeMantem() {
		String texto = "Oi! A WXN-2026-0001 (ajuste no relatório de vendas) está em andamento, atualizada em 20/10/2026.";

		assertEquals(texto, escrever(Situacao.DEMANDA_ENCONTRADA, RESUMO, texto));
	}

	@Test
	void aceitaOProtocoloEmOutraCaixa() {
		String texto = "A wxn-2026-0001 está em andamento desde 20/10/2026.";

		assertEquals(texto, escrever(Situacao.DEMANDA_ENCONTRADA, RESUMO, texto));
	}

	@Test
	void usaOTextoProntoQuandoALlmPerdeOProtocolo() {
		assertEquals(RESUMO, escrever(Situacao.DEMANDA_ENCONTRADA, RESUMO, "Sua demanda está em andamento desde 20/10/2026."));
	}

	@Test
	void usaOTextoProntoQuandoALlmCitaOutroProtocolo() {
		String texto = "A WXN-2026-0001 está em andamento (20/10/2026), e a WXN-2026-0099 já foi concluída.";

		assertEquals(RESUMO, escrever(Situacao.DEMANDA_ENCONTRADA, RESUMO, texto));
	}

	@Test
	void usaOTextoProntoQuandoALlmMudaOStatus() {
		assertEquals(RESUMO, escrever(Situacao.DEMANDA_ENCONTRADA, RESUMO, "A WXN-2026-0001 já foi concluída em 20/10/2026."));
	}

	@Test
	void usaOTextoProntoQuandoALlmPerdeADataDoRascunho() {
		assertEquals(RESUMO, escrever(Situacao.DEMANDA_ENCONTRADA, RESUMO, "A WXN-2026-0001 está em andamento."));
	}

	@Test
	void usaOTextoProntoQuandoALlmMudaOTituloDoChamado() {
		String texto = "Vou abrir um chamado com o título \"Problema no financeiro\" e a descrição \"Depois da troca de senha\". Posso confirmar?";

		assertEquals(PEDIDO, escrever(Situacao.PEDIR_CONFIRMACAO, PEDIDO, texto));
	}

	@Test
	void mantemTituloEDescricaoEntreAspas() {
		String texto = "Certo! Vou abrir o chamado \"O financeiro não abre\", com a descrição \"Depois da troca de senha\". Pode confirmar?";

		assertEquals(texto, escrever(Situacao.PEDIR_CONFIRMACAO, PEDIDO, texto));
	}

	@Test
	void usaOTextoProntoQuandoOPedidoDeConfirmacaoDeixaDeSerUmaPergunta() {
		String texto = "Vou abrir um chamado com o título \"O financeiro não abre\" e a descrição \"Depois da troca de senha\".";

		assertEquals(PEDIDO, escrever(Situacao.PEDIR_CONFIRMACAO, PEDIDO, texto));
	}

	@Test
	void usaOTextoProntoQuandoALlmEstaForaDoAr() {
		when(llm.responder(anyString(), any())).thenThrow(new LlmIndisponivelException("429", null));

		assertEquals(RESUMO, redator.escrever(Situacao.DEMANDA_ENCONTRADA, RESUMO, HISTORICO));
	}

	@Test
	void usaOTextoProntoQuandoARespostaVemVaziaOuLongaDemais() {
		assertEquals(RESUMO, escrever(Situacao.DEMANDA_ENCONTRADA, RESUMO, "   "));
		assertEquals(RESUMO, escrever(Situacao.DEMANDA_ENCONTRADA, RESUMO,
				"A WXN-2026-0001 está em andamento (20/10/2026). " + "x".repeat(RedatorComLlm.TAMANHO_MAXIMO)));
	}

	@Test
	void semFatosNoRascunhoQualquerTextoSemProtocoloServe() {
		String texto = "Tudo bem! Não abri nada. Posso ajudar com mais alguma coisa?";

		assertEquals(texto, escrever(Situacao.CHAMADO_CANCELADO, RoteadorDeIntencao.CHAMADO_CANCELADO, texto));
	}

	@Test
	void naoDeixaALlmInventarProtocoloMesmoSemFatosNoRascunho() {
		String texto = "Tudo bem! Seu chamado WXN-2026-0042 continua aberto. Posso ajudar com mais alguma coisa?";

		assertEquals(RoteadorDeIntencao.CHAMADO_CANCELADO,
				escrever(Situacao.CHAMADO_CANCELADO, RoteadorDeIntencao.CHAMADO_CANCELADO, texto));
	}

	@Test
	void mandaASituacaoOTextoBaseEPoucoContextoParaALlm() {
		when(llm.responder(anyString(), any())).thenReturn("Tudo bem!");
		List<MensagemLlm> longo = new ArrayList<>();
		for (int i = 1; i <= 12; i++) {
			longo.add(new MensagemLlm(i % 2 == 1 ? Papel.USUARIO : Papel.ASSISTENTE, "mensagem " + i));
		}

		redator.escrever(Situacao.CHAMADO_CANCELADO, RoteadorDeIntencao.CHAMADO_CANCELADO, longo);

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<MensagemLlm>> enviado = ArgumentCaptor.forClass(List.class);
		verify(llm).responder(prompt.capture(), enviado.capture());
		assertTrue(prompt.getValue().contains(Situacao.CHAMADO_CANCELADO.descricao()));
		assertTrue(prompt.getValue().contains(RoteadorDeIntencao.CHAMADO_CANCELADO));
		assertEquals(8, enviado.getValue().size());
		assertEquals("mensagem 12", enviado.getValue().get(7).conteudo());
	}

	@Test
	void fielRecusaTextoVazio() {
		assertFalse(RedatorComLlm.fiel(RESUMO, ""));
	}
}
