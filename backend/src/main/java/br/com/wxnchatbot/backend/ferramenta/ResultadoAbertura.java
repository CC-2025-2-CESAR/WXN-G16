package br.com.wxnchatbot.backend.ferramenta;

import br.com.wxnchatbot.backend.integracao.wxn.Chamado;

public sealed interface ResultadoAbertura {

	record Aberto(Chamado chamado) implements ResultadoAbertura {
	}

	record SemPedidoPendente() implements ResultadoAbertura {
	}

	record Indisponivel() implements ResultadoAbertura {
	}
}
