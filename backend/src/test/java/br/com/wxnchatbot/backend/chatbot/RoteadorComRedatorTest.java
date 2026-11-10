package br.com.wxnchatbot.backend.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.chatbot.DecisaoDoTurno.Encaminhar;
import br.com.wxnchatbot.backend.chatbot.DecisaoDoTurno.Responder;
import br.com.wxnchatbot.backend.ferramenta.ConfirmacaoChamadoServico;
import br.com.wxnchatbot.backend.ferramenta.ConsultaDemandaFerramenta;
import br.com.wxnchatbot.backend.ferramenta.PedidoDeConfirmacao;
import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda;
import br.com.wxnchatbot.backend.ferramenta.SolicitacaoChamadoFerramenta;
import br.com.wxnchatbot.backend.integracao.wxn.Demanda;
import br.com.wxnchatbot.backend.integracao.wxn.StatusDemanda;
import br.com.wxnchatbot.backend.llm.LlmClient;
import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.llm.MensagemLlm.Papel;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;

/**
 * O roteador com um redator no lugar dos textos prontos: o que a Lily diz passa a ser escrito pela LLM, então o roteador
 * não pode mais reconhecer "o que ela acabou de pedir" pelo texto da conversa.
 */
@ExtendWith(MockitoExtension.class)
class RoteadorComRedatorTest {

	private static final Empresa EMPRESA = new Empresa("Empresa Demo");

	@Mock
	private ClassificadorDeIntencao classificador;
	@Mock
	private ConsultaDemandaFerramenta consultaDemanda;
	@Mock
	private SolicitacaoChamadoFerramenta solicitacaoChamado;
	@Mock
	private ConfirmacaoChamadoServico confirmacaoChamado;
	@Mock
	private LlmClient llm;
	@Mock
	private Redator redator;

	private RoteadorDeIntencao roteador;
	private Conversa conversa;

	@BeforeEach
	void preparar() {
		lenient().when(redator.escrever(any(), anyString(), any())).thenAnswer(i -> "IA: " + i.getArgument(1));
		roteador = new RoteadorDeIntencao(classificador, new ExtratorDeProtocolo(), new InterpretadorDeConfirmacao(),
				consultaDemanda, solicitacaoChamado, confirmacaoChamado, llm, new BaseDeConhecimento("Base"), redator, 0.7);
		conversa = new Conversa(new Cliente(EMPRESA, "Maria Souza", "81999990001"), new Chatbot("Lily", "Oi, sou a Lily!"));
	}

	private DecisaoDoTurno turno(String texto, ResultadoIntencao classificacao) {
		conversa.adicionarMensagem(conversa.getCliente(), texto);
		when(confirmacaoChamado.temPedidoPendente(EMPRESA.getId(), conversa.getId())).thenReturn(false);
		when(classificador.classificar(any())).thenReturn(classificacao);
		return roteador.decidir(conversa, List.of(new MensagemLlm(Papel.USUARIO, texto)));
	}

	/** A Lily "fala" com palavras que nada têm a ver com o texto pronto, como a LLM faria. */
	private void lilyDisse(String texto) {
		conversa.adicionarMensagem(conversa.getChatbot(), texto);
	}

	@Test
	void aRespostaPassaPeloRedatorComASituacaoEOTextoPronto() {
		DecisaoDoTurno decisao = turno("Queria saber da minha demanda", ResultadoIntencao.certa(Intencao.CONSULTAR_DEMANDA));

		verify(redator).escrever(eq(Situacao.PEDIR_PROTOCOLO), eq(RoteadorDeIntencao.PEDIR_PROTOCOLO), any());
		assertEquals(new Responder("IA: " + RoteadorDeIntencao.PEDIR_PROTOCOLO), decisao);
	}

	@Test
	void odadoDaDemandaVaiNoRascunhoParaARedacao() {
		Demanda demanda = new Demanda("WXN-2026-0001", "Ajuste no relatório", StatusDemanda.EM_ANDAMENTO,
				Instant.parse("2026-10-20T14:30:00Z"), "Em desenvolvimento");
		when(consultaDemanda.consultarStatus(EMPRESA.getId(), "WXN-2026-0001"))
				.thenReturn(new ResultadoConsultaDemanda.Encontrada(demanda));

		DecisaoDoTurno decisao = turno("Qual o andamento da WXN-2026-0001?", ResultadoIntencao.certa(Intencao.CONSULTAR_DEMANDA));

		verify(redator).escrever(eq(Situacao.DEMANDA_ENCONTRADA), eq(new ResultadoConsultaDemanda.Encontrada(demanda).resumoParaCliente()), any());
		assertTrue(assertInstanceOf(Responder.class, decisao).texto().startsWith("IA: Demanda WXN-2026-0001"));
	}

	@Test
	void lembraQueAcabouDePedirOProtocoloMesmoComTextoEscritoPelaIa() {
		turno("Queria saber da minha demanda", ResultadoIntencao.certa(Intencao.CONSULTAR_DEMANDA));
		lilyDisse("Me passa o número do protocolo, por favor? Ele é parecido com WXN-2026-0001.");

		DecisaoDoTurno decisao = turno("Não lembro, pedi por telefone", ResultadoIntencao.certa(Intencao.DUVIDA));

		Encaminhar encaminhar = assertInstanceOf(Encaminhar.class, decisao);
		assertEquals("O cliente quer consultar uma demanda, mas não tem o protocolo", encaminhar.motivo());
		verify(redator).escrever(eq(Situacao.ENCAMINHANDO_SEM_PROTOCOLO), eq(RoteadorDeIntencao.ENCAMINHANDO_SEM_PROTOCOLO), any());
		verifyNoInteractions(consultaDemanda);
	}

	@Test
	void opedidoDeProtocoloValeSoParaAProximaMensagem() {
		turno("Queria saber da minha demanda", ResultadoIntencao.certa(Intencao.CONSULTAR_DEMANDA));
		lilyDisse("Qual é o protocolo?");
		turno("Não lembro", ResultadoIntencao.certa(Intencao.DUVIDA));
		lilyDisse("Vou chamar um atendente.");
		when(llm.responder(anyString(), any())).thenReturn("Claro! Posso ajudar com status, chamados e dúvidas.");

		DecisaoDoTurno decisao = turno("E vocês atendem aos sábados?", ResultadoIntencao.certa(Intencao.DUVIDA));

		assertEquals(new Responder("Claro! Posso ajudar com status, chamados e dúvidas."), decisao);
	}

	@Test
	void lembraQueAcabouDePedirDetalhesMesmoComTextoEscritoPelaIa() {
		turno("Quero abrir um chamado", ResultadoIntencao.certa(Intencao.ABRIR_CHAMADO));
		lilyDisse("Claro! Me conta o que está acontecendo?");
		PedidoDeConfirmacao pedido = new PedidoDeConfirmacao(conversa.getId(), "o relatório", "", Instant.now());
		when(solicitacaoChamado.solicitarAbertura(any(), any(), anyString(), anyString())).thenReturn(pedido);

		DecisaoDoTurno decisao = turno("o relatório", new ResultadoIntencao(Intencao.ABRIR_CHAMADO, 0.9, false));

		verify(solicitacaoChamado).solicitarAbertura(any(), any(), anyString(), anyString());
		verify(redator).escrever(eq(Situacao.PEDIR_CONFIRMACAO), eq(pedido.resumoParaCliente()), any());
		assertInstanceOf(Responder.class, decisao);
	}

	@Test
	void duvidaContinuaSendoRespondidaPelaLlmSemPassarPeloRedator() {
		when(llm.responder(anyString(), any())).thenReturn("O protocolo serve para acompanhar o andamento.");

		DecisaoDoTurno decisao = turno("Para que serve esse protocolo?", ResultadoIntencao.certa(Intencao.DUVIDA));

		assertEquals(new Responder("O protocolo serve para acompanhar o andamento."), decisao);
		verifyNoInteractions(redator);
		verify(consultaDemanda, never()).consultarStatus(any(), anyString());
	}
}
