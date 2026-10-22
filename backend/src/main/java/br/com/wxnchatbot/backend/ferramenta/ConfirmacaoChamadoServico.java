package br.com.wxnchatbot.backend.ferramenta;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.wxnchatbot.backend.ferramenta.PedidosDeChamadoPendentes.Pedido;
import br.com.wxnchatbot.backend.ferramenta.ResultadoAbertura.Aberto;
import br.com.wxnchatbot.backend.ferramenta.ResultadoAbertura.Indisponivel;
import br.com.wxnchatbot.backend.ferramenta.ResultadoAbertura.SemPedidoPendente;
import br.com.wxnchatbot.backend.integracao.wxn.Chamado;
import br.com.wxnchatbot.backend.integracao.wxn.DemandaGateway;
import br.com.wxnchatbot.backend.integracao.wxn.DemandaIndisponivelException;

// Único ponto que grava na API da WXN; não expor à LLM.
@Service
public class ConfirmacaoChamadoServico {

	private static final Logger log = LoggerFactory.getLogger(ConfirmacaoChamadoServico.class);

	private final PedidosDeChamadoPendentes pendentes;
	private final DemandaGateway gateway;

	public ConfirmacaoChamadoServico(PedidosDeChamadoPendentes pendentes, DemandaGateway gateway) {
		this.pendentes = pendentes;
		this.gateway = gateway;
	}

	public ResultadoAbertura confirmar(UUID empresaId, UUID conversaId) {
		Objects.requireNonNull(empresaId, "empresaId é obrigatório");
		Objects.requireNonNull(conversaId, "conversaId é obrigatório");
		Optional<Pedido> pedido = pendentes.buscarValido(empresaId, conversaId);
		if (pedido.isEmpty()) {
			return new SemPedidoPendente();
		}
		try {
			Chamado chamado = gateway.abrirChamado(empresaId, pedido.get().chamado(), pedido.get().chaveIdempotencia());
			pendentes.consumir(conversaId, pedido.get());
			return new Aberto(chamado);
		} catch (DemandaIndisponivelException e) {
			log.warn("API da WXN indisponível ao abrir chamado da conversa {}: {}", conversaId, e.getMessage());
			return new Indisponivel();
		}
	}

	public void cancelar(UUID conversaId) {
		Objects.requireNonNull(conversaId, "conversaId é obrigatório");
		pendentes.descartar(conversaId);
	}
}
