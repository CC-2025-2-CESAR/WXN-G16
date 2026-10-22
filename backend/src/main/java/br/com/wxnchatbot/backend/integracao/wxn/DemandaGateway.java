package br.com.wxnchatbot.backend.integracao.wxn;

import java.util.Optional;
import java.util.UUID;

public interface DemandaGateway {

	// Vazio só quando a API confirma que a demanda não existe; qualquer outra falha lança DemandaIndisponivelException.
	Optional<Demanda> consultarPorProtocolo(UUID empresaId, String protocolo);

	// A mesma chave de idempotência nunca abre dois chamados.
	Chamado abrirChamado(UUID empresaId, NovoChamado chamado, UUID chaveIdempotencia);
}
