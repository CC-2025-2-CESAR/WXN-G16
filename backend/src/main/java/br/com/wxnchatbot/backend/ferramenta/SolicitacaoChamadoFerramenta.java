package br.com.wxnchatbot.backend.ferramenta;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.wxnchatbot.backend.integracao.wxn.NovoChamado;

// Seguro para expor à LLM: só registra o pedido e não acessa o DemandaGateway.
@Service
public class SolicitacaoChamadoFerramenta {

	private final PedidosDeChamadoPendentes pendentes;

	public SolicitacaoChamadoFerramenta(PedidosDeChamadoPendentes pendentes) {
		this.pendentes = pendentes;
	}

	public PedidoDeConfirmacao solicitarAbertura(UUID empresaId, UUID conversaId, String titulo, String descricao) {
		Objects.requireNonNull(empresaId, "empresaId é obrigatório");
		Objects.requireNonNull(conversaId, "conversaId é obrigatório");
		NovoChamado chamado = new NovoChamado(titulo, descricao);
		PedidosDeChamadoPendentes.Pedido pedido = pendentes.registrar(empresaId, conversaId, chamado);
		return new PedidoDeConfirmacao(conversaId, chamado.titulo(), chamado.descricao(), pedido.expiraEm());
	}
}
