package br.com.wxnchatbot.backend.integracao.wxn;

import java.util.Optional;
import java.util.UUID;

public interface DemandaGateway {

	Optional<Demanda> consultarPorProtocolo(UUID empresaId, String protocolo);

	Chamado abrirChamado(UUID empresaId, NovoChamado chamado, UUID chaveIdempotencia);
}
