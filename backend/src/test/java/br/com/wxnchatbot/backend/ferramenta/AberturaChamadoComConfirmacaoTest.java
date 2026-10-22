package br.com.wxnchatbot.backend.ferramenta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.wxnchatbot.backend.ferramenta.ResultadoAbertura.Aberto;
import br.com.wxnchatbot.backend.ferramenta.ResultadoAbertura.Indisponivel;
import br.com.wxnchatbot.backend.ferramenta.ResultadoAbertura.SemPedidoPendente;

class AberturaChamadoComConfirmacaoTest {

	private final UUID empresa = UUID.randomUUID();
	private final UUID conversa = UUID.randomUUID();

	private DemandaGatewayFalso gateway;
	private RelogioFalso relogio;
	private SolicitacaoChamadoFerramenta solicitacao;
	private ConfirmacaoChamadoServico confirmacao;

	@BeforeEach
	void preparar() {
		gateway = new DemandaGatewayFalso();
		relogio = new RelogioFalso();
		PedidosDeChamadoPendentes pendentes = new PedidosDeChamadoPendentes(relogio);
		solicitacao = new SolicitacaoChamadoFerramenta(pendentes);
		confirmacao = new ConfirmacaoChamadoServico(pendentes, gateway);
	}

	@Test
	void solicitarNuncaGravaNadaNoSistemaDoCliente() {
		solicitacao.solicitarAbertura(empresa, conversa, "Erro ao emitir nota", "Trava na assinatura");
		solicitacao.solicitarAbertura(empresa, conversa, "Outro erro", "");
		solicitacao.solicitarAbertura(empresa, UUID.randomUUID(), "Mais um", "");

		assertTrue(gateway.tentativasDeEscrita.isEmpty(), "sem confirmação, a escrita nunca acontece");
	}

	@Test
	void solicitarDevolveOResumoParaOClienteConfirmar() {
		PedidoDeConfirmacao pedido = solicitacao.solicitarAbertura(empresa, conversa, "  Erro ao emitir nota ",
				"Trava na assinatura");

		assertEquals("Erro ao emitir nota", pedido.titulo());
		assertEquals("Vou abrir um chamado com o título \"Erro ao emitir nota\" e a descrição \"Trava na assinatura\". "
				+ "Posso confirmar?", pedido.resumoParaCliente());
	}

	@Test
	void confirmarSemPedidoNaoGrava() {
		assertInstanceOf(SemPedidoPendente.class, confirmacao.confirmar(empresa, conversa));
		assertTrue(gateway.tentativasDeEscrita.isEmpty());
	}

	@Test
	void confirmarDepoisDeSolicitarAbreOChamadoUmaUnicaVez() {
		solicitacao.solicitarAbertura(empresa, conversa, "Erro ao emitir nota", "Trava na assinatura");

		Aberto aberto = assertInstanceOf(Aberto.class, confirmacao.confirmar(empresa, conversa));

		assertEquals("WXN-2026-5000", aberto.chamado().protocolo());
		assertEquals(1, gateway.escritas.size());
		assertEquals(empresa, gateway.escritas.get(0).empresaId());
		assertInstanceOf(SemPedidoPendente.class, confirmacao.confirmar(empresa, conversa),
				"confirmar de novo não abre outro chamado");
		assertEquals(1, gateway.tentativasDeEscrita.size());
	}

	@Test
	void pedidoDeUmaConversaNaoPodeSerConfirmadoPorOutra() {
		solicitacao.solicitarAbertura(empresa, conversa, "Erro ao emitir nota", "");

		assertInstanceOf(SemPedidoPendente.class, confirmacao.confirmar(empresa, UUID.randomUUID()));
		assertTrue(gateway.tentativasDeEscrita.isEmpty());
	}

	@Test
	void pedidoNaoPodeSerConfirmadoNoContextoDeOutraEmpresa() {
		solicitacao.solicitarAbertura(empresa, conversa, "Erro ao emitir nota", "");

		assertInstanceOf(SemPedidoPendente.class, confirmacao.confirmar(UUID.randomUUID(), conversa));
		assertTrue(gateway.tentativasDeEscrita.isEmpty());
	}

	@Test
	void pedidoExpiradoNaoGrava() {
		solicitacao.solicitarAbertura(empresa, conversa, "Erro ao emitir nota", "");
		relogio.avancar(PedidosDeChamadoPendentes.VALIDADE.plusSeconds(1));

		assertInstanceOf(SemPedidoPendente.class, confirmacao.confirmar(empresa, conversa));
		assertTrue(gateway.tentativasDeEscrita.isEmpty());
	}

	@Test
	void pedidoDentroDoPrazoAindaPodeSerConfirmado() {
		solicitacao.solicitarAbertura(empresa, conversa, "Erro ao emitir nota", "");
		relogio.avancar(PedidosDeChamadoPendentes.VALIDADE.minusSeconds(1));

		assertInstanceOf(Aberto.class, confirmacao.confirmar(empresa, conversa));
	}

	@Test
	void cancelarDescartaOPedido() {
		solicitacao.solicitarAbertura(empresa, conversa, "Erro ao emitir nota", "");

		confirmacao.cancelar(conversa);

		assertInstanceOf(SemPedidoPendente.class, confirmacao.confirmar(empresa, conversa));
		assertTrue(gateway.tentativasDeEscrita.isEmpty());
	}

	@Test
	void novoPedidoSubstituiOAnteriorDaMesmaConversa() {
		solicitacao.solicitarAbertura(empresa, conversa, "Primeiro título", "");
		solicitacao.solicitarAbertura(empresa, conversa, "Segundo título", "");

		confirmacao.confirmar(empresa, conversa);

		assertEquals(1, gateway.escritas.size());
		assertEquals("Segundo título", gateway.escritas.get(0).chamado().titulo());
	}

	@Test
	void apiForaDoArNaConfirmacaoMantemOPedidoEReusaAMesmaChaveDeIdempotencia() {
		solicitacao.solicitarAbertura(empresa, conversa, "Erro ao emitir nota", "");
		gateway.indisponivel = true;

		assertInstanceOf(Indisponivel.class, confirmacao.confirmar(empresa, conversa));
		gateway.indisponivel = false;
		assertInstanceOf(Aberto.class, confirmacao.confirmar(empresa, conversa));

		assertEquals(2, gateway.tentativasDeEscrita.size());
		assertEquals(gateway.tentativasDeEscrita.get(0).chave(), gateway.tentativasDeEscrita.get(1).chave());
		assertEquals(1, gateway.escritas.size());
	}

	@Test
	void dadosInvalidosNaSolicitacaoSaoRejeitadosENadaFicaPendente() {
		assertThrows(IllegalArgumentException.class, () -> solicitacao.solicitarAbertura(empresa, conversa, " ", "x"));
		assertThrows(IllegalArgumentException.class,
				() -> solicitacao.solicitarAbertura(empresa, conversa, "ok titulo", "x".repeat(1001)));

		assertInstanceOf(SemPedidoPendente.class, confirmacao.confirmar(empresa, conversa));
	}

	@Test
	void pedidosExpiradosDeOutrasConversasSaoLimpos() {
		UUID abandonada = UUID.randomUUID();
		solicitacao.solicitarAbertura(empresa, abandonada, "Pedido abandonado", "");
		relogio.avancar(Duration.ofMinutes(30));

		solicitacao.solicitarAbertura(empresa, conversa, "Pedido novo", "");

		assertInstanceOf(SemPedidoPendente.class, confirmacao.confirmar(empresa, abandonada));
		assertInstanceOf(Aberto.class, confirmacao.confirmar(empresa, conversa));
	}
}
