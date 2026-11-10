package br.com.wxnchatbot.backend.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.chatbot.DecisaoDoTurno.Encaminhar;
import br.com.wxnchatbot.backend.chatbot.DecisaoDoTurno.Responder;
import br.com.wxnchatbot.backend.ferramenta.ConfirmacaoChamadoServico;
import br.com.wxnchatbot.backend.ferramenta.ConsultaDemandaFerramenta;
import br.com.wxnchatbot.backend.ferramenta.PedidoDeConfirmacao;
import br.com.wxnchatbot.backend.ferramenta.ResultadoAbertura;
import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda;
import br.com.wxnchatbot.backend.ferramenta.SolicitacaoChamadoFerramenta;
import br.com.wxnchatbot.backend.integracao.wxn.Chamado;
import br.com.wxnchatbot.backend.integracao.wxn.Demanda;
import br.com.wxnchatbot.backend.integracao.wxn.StatusDemanda;
import br.com.wxnchatbot.backend.llm.LlmClient;
import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.llm.MensagemLlm.Papel;
import br.com.wxnchatbot.backend.llm.PromptDaLily;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;

@ExtendWith(MockitoExtension.class)
class RoteadorDeIntencaoTest {

	private static final Empresa EMPRESA = new Empresa("Empresa Demo");
	private static final String BASE = "Base de teste da WXN";

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

	private RoteadorDeIntencao roteador;
	private Conversa conversa;

	@BeforeEach
	void preparar() {
		roteador = new RoteadorDeIntencao(classificador, new ExtratorDeProtocolo(), new InterpretadorDeConfirmacao(),
				consultaDemanda, solicitacaoChamado, confirmacaoChamado, llm, new BaseDeConhecimento(BASE), 0.7);
		conversa = new Conversa(new Cliente(EMPRESA, "Maria Souza", "81999990001"), new Chatbot("Lily", "Oi, sou a Lily!"));
	}

	private DecisaoDoTurno turno(String texto, ResultadoIntencao classificacao) {
		conversa.adicionarMensagem(conversa.getCliente(), texto);
		when(confirmacaoChamado.temPedidoPendente(EMPRESA.getId(), conversa.getId())).thenReturn(false);
		when(classificador.classificar(any())).thenReturn(classificacao);
		return roteador.decidir(conversa, List.of(new MensagemLlm(Papel.USUARIO, texto)));
	}

	private void lilyDisse(String texto) {
		conversa.adicionarMensagem(conversa.getChatbot(), texto);
	}

	private static Demanda demanda() {
		return new Demanda("WXN-2026-0001", "Ajuste no relatório", StatusDemanda.EM_ANDAMENTO,
				Instant.parse("2026-10-20T14:30:00Z"), "Em desenvolvimento");
	}

	@Test
	void consultaComProtocoloResponderComOResumoDaDemanda() {
		when(consultaDemanda.consultarStatus(EMPRESA.getId(), "WXN-2026-0001"))
				.thenReturn(new ResultadoConsultaDemanda.Encontrada(demanda()));

		DecisaoDoTurno decisao = turno("Qual o status da WXN-2026-0001?", ResultadoIntencao.certa(Intencao.CONSULTAR_DEMANDA));

		Responder resposta = assertInstanceOf(Responder.class, decisao);
		assertTrue(resposta.texto().contains("WXN-2026-0001"));
		assertTrue(resposta.texto().contains("em andamento"));
		verifyNoInteractions(llm);
	}

	@Test
	void consultaSemProtocoloPedeOProtocolo() {
		DecisaoDoTurno decisao = turno("Queria saber do andamento da minha demanda", ResultadoIntencao.certa(Intencao.CONSULTAR_DEMANDA));

		assertEquals(new Responder(RoteadorDeIntencao.PEDIR_PROTOCOLO), decisao);
		verifyNoInteractions(consultaDemanda);
	}

	@Test
	void clienteSemProtocoloDepoisDoPedidoVaiParaUmAtendente() {
		lilyDisse(RoteadorDeIntencao.PEDIR_PROTOCOLO);

		DecisaoDoTurno decisao = turno("Não lembro, pedi por telefone", ResultadoIntencao.certa(Intencao.DUVIDA));

		Encaminhar encaminhar = assertInstanceOf(Encaminhar.class, decisao);
		assertEquals(RoteadorDeIntencao.ENCAMINHANDO_SEM_PROTOCOLO, encaminhar.texto());
		verifyNoInteractions(llm, consultaDemanda);
	}

	@Test
	void respostaAoPedidoDeProtocoloConsultaMesmoSemPalavrasDeConsulta() {
		lilyDisse(RoteadorDeIntencao.PEDIR_PROTOCOLO);
		when(consultaDemanda.consultarStatus(EMPRESA.getId(), "WXN-2026-0002"))
				.thenReturn(new ResultadoConsultaDemanda.NaoEncontrada("WXN-2026-0002"));

		DecisaoDoTurno decisao = turno("é a wxn-2026-0002", ResultadoIntencao.certa(Intencao.DUVIDA));

		Responder resposta = assertInstanceOf(Responder.class, decisao);
		assertTrue(resposta.texto().contains("Não encontrei a demanda WXN-2026-0002"));
	}

	@Test
	void consultaComApiDaWxnForaDoArAvisaSemEncaminhar() {
		when(consultaDemanda.consultarStatus(any(), anyString())).thenReturn(new ResultadoConsultaDemanda.Indisponivel());

		DecisaoDoTurno decisao = turno("status da WXN-2026-0001", ResultadoIntencao.certa(Intencao.CONSULTAR_DEMANDA));

		assertEquals(new Responder(RoteadorDeIntencao.CONSULTA_INDISPONIVEL), decisao);
	}

	@Test
	void abrirChamadoComMensagemCurtaPedeDetalhes() {
		DecisaoDoTurno decisao = turno("Quero abrir um chamado", ResultadoIntencao.certa(Intencao.ABRIR_CHAMADO));

		assertEquals(new Responder(RoteadorDeIntencao.PEDIR_DETALHES), decisao);
		verifyNoInteractions(solicitacaoChamado);
	}

	@Test
	void descricaoDepoisDoPedidoDeDetalhesPedeConfirmacaoSemAbrirAinda() {
		lilyDisse(RoteadorDeIntencao.PEDIR_DETALHES);
		String texto = "O financeiro não abre depois que troquei a senha. Os outros módulos abrem normal.";
		PedidoDeConfirmacao pedido = new PedidoDeConfirmacao(conversa.getId(), "O financeiro não abre depois que troquei a senha",
				texto, Instant.now());
		when(solicitacaoChamado.solicitarAbertura(eq(EMPRESA.getId()), eq(conversa.getId()), anyString(), anyString())).thenReturn(pedido);

		DecisaoDoTurno decisao = turno(texto, ResultadoIntencao.certa(Intencao.DUVIDA));

		Responder resposta = assertInstanceOf(Responder.class, decisao);
		assertEquals(pedido.resumoParaCliente(), resposta.texto());
		ArgumentCaptor<String> titulo = ArgumentCaptor.forClass(String.class);
		verify(solicitacaoChamado).solicitarAbertura(eq(EMPRESA.getId()), eq(conversa.getId()), titulo.capture(), eq(texto));
		assertEquals("O financeiro não abre depois que troquei a senha", titulo.getValue());
		verify(confirmacaoChamado, never()).confirmar(any(), any());
	}

	@Test
	void mensagemLongaJaViraPedidoDeConfirmacaoSemPerguntarDetalhes() {
		String texto = "Preciso de um chamado: a nota fiscal trava na assinatura desde ontem";
		when(solicitacaoChamado.solicitarAbertura(any(), any(), anyString(), anyString()))
				.thenReturn(new PedidoDeConfirmacao(conversa.getId(), "t", "d", Instant.now()));

		DecisaoDoTurno decisao = turno(texto, ResultadoIntencao.certa(Intencao.ABRIR_CHAMADO));

		assertInstanceOf(Responder.class, decisao);
		verify(solicitacaoChamado).solicitarAbertura(any(), any(), anyString(), anyString());
	}

	@Test
	void mensagemVagaPedeDetalhesMesmoSendoLonga() {
		DecisaoDoTurno decisao = turno("Quero abrir um chamado para aquele problema do relatório",
				ResultadoIntencao.certa(Intencao.ABRIR_CHAMADO));

		assertEquals(new Responder(RoteadorDeIntencao.PEDIR_DETALHES), decisao);
		verifyNoInteractions(solicitacaoChamado);
	}

	@Test
	void classificadorQueAchaADescricaoInsuficientePedeDetalhesMesmoComPalavrasDeSobra() {
		DecisaoDoTurno decisao = turno("Preciso de um chamado: a nota fiscal trava na assinatura desde ontem",
				new ResultadoIntencao(Intencao.ABRIR_CHAMADO, 0.9, false));

		assertEquals(new Responder(RoteadorDeIntencao.PEDIR_DETALHES), decisao);
		verifyNoInteractions(solicitacaoChamado);
	}

	@Test
	void classificadorQueAchaADescricaoSuficienteNaoLiberaMensagemVaga() {
		DecisaoDoTurno decisao = turno("Quero abrir um chamado para aquele problema do relatório",
				new ResultadoIntencao(Intencao.ABRIR_CHAMADO, 0.9, true));

		assertEquals(new Responder(RoteadorDeIntencao.PEDIR_DETALHES), decisao);
	}

	@Test
	void descricaoSuficienteParaOClassificadorEParaAsPalavrasViraPedidoDeConfirmacao() {
		when(solicitacaoChamado.solicitarAbertura(any(), any(), anyString(), anyString()))
				.thenReturn(new PedidoDeConfirmacao(conversa.getId(), "t", "d", Instant.now()));

		DecisaoDoTurno decisao = turno("O relatório de vendas não carrega desde a atualização de ontem",
				new ResultadoIntencao(Intencao.ABRIR_CHAMADO, 0.95, true));

		assertInstanceOf(Responder.class, decisao);
		verify(solicitacaoChamado).solicitarAbertura(any(), any(), anyString(), anyString());
	}

	@Test
	void depoisDePedirDetalhesAceitaATentativaDoClienteMesmoVaga() {
		lilyDisse(RoteadorDeIntencao.PEDIR_DETALHES);
		when(solicitacaoChamado.solicitarAbertura(any(), any(), anyString(), anyString()))
				.thenReturn(new PedidoDeConfirmacao(conversa.getId(), "t", "d", Instant.now()));

		DecisaoDoTurno decisao = turno("o relatório", new ResultadoIntencao(Intencao.ABRIR_CHAMADO, 0.9, false));

		assertInstanceOf(Responder.class, decisao);
		verify(solicitacaoChamado).solicitarAbertura(any(), any(), anyString(), anyString());
	}

	@Test
	void simDepoisDoPedidoAbreOChamadoEDevolveOProtocolo() {
		conversa.adicionarMensagem(conversa.getCliente(), "sim");
		when(confirmacaoChamado.temPedidoPendente(EMPRESA.getId(), conversa.getId())).thenReturn(true);
		when(confirmacaoChamado.confirmar(EMPRESA.getId(), conversa.getId())).thenReturn(new ResultadoAbertura.Aberto(
				new Chamado("WXN-2026-0099", "Financeiro", StatusDemanda.ABERTA, Instant.now())));

		DecisaoDoTurno decisao = roteador.decidir(conversa, List.of(new MensagemLlm(Papel.USUARIO, "sim")));

		Responder resposta = assertInstanceOf(Responder.class, decisao);
		assertTrue(resposta.texto().contains("WXN-2026-0099"));
		verifyNoInteractions(classificador, llm);
	}

	@Test
	void naoDepoisDoPedidoCancelaSemAbrir() {
		conversa.adicionarMensagem(conversa.getCliente(), "não, deixa");
		when(confirmacaoChamado.temPedidoPendente(EMPRESA.getId(), conversa.getId())).thenReturn(true);

		DecisaoDoTurno decisao = roteador.decidir(conversa, List.of(new MensagemLlm(Papel.USUARIO, "não, deixa")));

		assertEquals(new Responder(RoteadorDeIntencao.CHAMADO_CANCELADO), decisao);
		verify(confirmacaoChamado).cancelar(conversa.getId());
		verify(confirmacaoChamado, never()).confirmar(any(), any());
	}

	@Test
	void respostaQueNaoESimNemNaoRepeteAPerguntaEMantemOPedido() {
		conversa.adicionarMensagem(conversa.getCliente(), "ok mas qual o prazo?");
		when(confirmacaoChamado.temPedidoPendente(EMPRESA.getId(), conversa.getId())).thenReturn(true);
		when(classificador.classificar(any())).thenReturn(ResultadoIntencao.certa(Intencao.DUVIDA));

		DecisaoDoTurno decisao = roteador.decidir(conversa, List.of(new MensagemLlm(Papel.USUARIO, "ok mas qual o prazo?")));

		assertEquals(new Responder(RoteadorDeIntencao.PEDIR_SIM_OU_NAO), decisao);
		verify(confirmacaoChamado, never()).confirmar(any(), any());
		verify(confirmacaoChamado, never()).cancelar(any());
	}

	@Test
	void pedirUmAtendenteNoMeioDaConfirmacaoDescartaOPedidoEEncaminha() {
		conversa.adicionarMensagem(conversa.getCliente(), "prefiro falar com uma pessoa");
		when(confirmacaoChamado.temPedidoPendente(EMPRESA.getId(), conversa.getId())).thenReturn(true);
		when(classificador.classificar(any())).thenReturn(ResultadoIntencao.certa(Intencao.FALAR_COM_HUMANO));

		DecisaoDoTurno decisao = roteador.decidir(conversa,
				List.of(new MensagemLlm(Papel.USUARIO, "prefiro falar com uma pessoa")));

		assertInstanceOf(Encaminhar.class, decisao);
		verify(confirmacaoChamado).cancelar(conversa.getId());
	}

	@Test
	void chamadoComApiForaDoArMantemOPedidoParaTentarDeNovo() {
		conversa.adicionarMensagem(conversa.getCliente(), "sim");
		when(confirmacaoChamado.temPedidoPendente(EMPRESA.getId(), conversa.getId())).thenReturn(true);
		when(confirmacaoChamado.confirmar(any(), any())).thenReturn(new ResultadoAbertura.Indisponivel());

		DecisaoDoTurno decisao = roteador.decidir(conversa, List.of(new MensagemLlm(Papel.USUARIO, "sim")));

		assertEquals(new Responder(RoteadorDeIntencao.CHAMADO_INDISPONIVEL), decisao);
		verify(confirmacaoChamado, never()).cancelar(any());
	}

	@Test
	void pedidoExpiradoAvisaOCliente() {
		conversa.adicionarMensagem(conversa.getCliente(), "sim");
		when(confirmacaoChamado.temPedidoPendente(EMPRESA.getId(), conversa.getId())).thenReturn(true);
		when(confirmacaoChamado.confirmar(any(), any())).thenReturn(new ResultadoAbertura.SemPedidoPendente());

		DecisaoDoTurno decisao = roteador.decidir(conversa, List.of(new MensagemLlm(Papel.USUARIO, "sim")));

		assertEquals(new Responder(RoteadorDeIntencao.PEDIDO_EXPIROU), decisao);
	}

	@Test
	void pedidoDeHumanoEncaminhaComOMotivo() {
		DecisaoDoTurno decisao = turno("Quero falar com um atendente", ResultadoIntencao.certa(Intencao.FALAR_COM_HUMANO));

		Encaminhar encaminhar = assertInstanceOf(Encaminhar.class, decisao);
		assertEquals("O cliente pediu para falar com um atendente", encaminhar.motivo());
		assertEquals(RoteadorDeIntencao.ENCAMINHANDO, encaminhar.texto());
		verifyNoInteractions(llm);
	}

	@Test
	void foraDeEscopoRecusaComGentilezaSemChamarALlm() {
		DecisaoDoTurno decisao = turno("Me passa uma receita de bolo", ResultadoIntencao.certa(Intencao.FORA_DE_ESCOPO));

		assertEquals(new Responder(RoteadorDeIntencao.FORA_DE_ESCOPO), decisao);
		verifyNoInteractions(llm);
	}

	@Test
	void intencaoIncertaEncaminha() {
		DecisaoDoTurno decisao = turno("hmm", ResultadoIntencao.certa(Intencao.INCERTO));

		Encaminhar encaminhar = assertInstanceOf(Encaminhar.class, decisao);
		assertEquals(RoteadorDeIntencao.ENCAMINHANDO_POR_DUVIDA, encaminhar.texto());
	}

	@Test
	void confiancaAbaixoDoLimiarViraIncertaMesmoComIntencaoDefinida() {
		DecisaoDoTurno decisao = turno("acho que quero consultar algo", new ResultadoIntencao(Intencao.CONSULTAR_DEMANDA, 0.69));

		assertInstanceOf(Encaminhar.class, decisao);
		verifyNoInteractions(consultaDemanda);
	}

	@Test
	void confiancaNoLimiarAindaValeAIntencao() {
		DecisaoDoTurno decisao = turno("quero consultar uma demanda", new ResultadoIntencao(Intencao.CONSULTAR_DEMANDA, 0.7));

		assertEquals(new Responder(RoteadorDeIntencao.PEDIR_PROTOCOLO), decisao);
	}

	@Test
	void duvidaVaiParaALlmComOPromptEABaseDeConhecimento() {
		when(llm.responder(any(), any())).thenReturn("Eu consulto demandas e abro chamados.");

		DecisaoDoTurno decisao = turno("O que você faz?", ResultadoIntencao.certa(Intencao.DUVIDA));

		assertEquals(new Responder("Eu consulto demandas e abro chamados."), decisao);
		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(llm).responder(prompt.capture(), eq(List.of(new MensagemLlm(Papel.USUARIO, "O que você faz?"))));
		assertEquals(PromptDaLily.comConhecimento(BASE), prompt.getValue());
		assertTrue(prompt.getValue().contains(BASE));
	}

	@Test
	void tituloUsaAPrimeiraFraseEDescartaOQueExcedeOLimite() {
		assertEquals("O sistema parou", RoteadorDeIntencao.tituloDe("O sistema parou. Preciso de ajuda urgente"));
		assertEquals("Chamado pedido pelo WhatsApp", RoteadorDeIntencao.tituloDe("Oi."));
		String longo = "palavra ".repeat(30).strip();
		assertTrue(RoteadorDeIntencao.tituloDe(longo).length() <= 100);
		assertTrue(longo.startsWith(RoteadorDeIntencao.tituloDe(longo)));
	}
}
