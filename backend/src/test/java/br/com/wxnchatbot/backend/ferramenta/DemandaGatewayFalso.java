package br.com.wxnchatbot.backend.ferramenta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.wxnchatbot.backend.integracao.wxn.Chamado;
import br.com.wxnchatbot.backend.integracao.wxn.Demanda;
import br.com.wxnchatbot.backend.integracao.wxn.DemandaGateway;
import br.com.wxnchatbot.backend.integracao.wxn.DemandaIndisponivelException;
import br.com.wxnchatbot.backend.integracao.wxn.NovoChamado;
import br.com.wxnchatbot.backend.integracao.wxn.StatusDemanda;

class DemandaGatewayFalso implements DemandaGateway {

	record Escrita(UUID empresaId, NovoChamado chamado, UUID chave) {
	}

	final Map<UUID, Map<String, Demanda>> dados = new HashMap<>();
	final List<UUID> empresasConsultadas = new ArrayList<>();
	final List<String> protocolosConsultados = new ArrayList<>();
	final List<Escrita> tentativasDeEscrita = new ArrayList<>();
	final List<Escrita> escritas = new ArrayList<>();
	boolean indisponivel;

	void cadastrar(UUID empresaId, Demanda demanda) {
		dados.computeIfAbsent(empresaId, id -> new HashMap<>()).put(demanda.protocolo(), demanda);
	}

	@Override
	public Optional<Demanda> consultarPorProtocolo(UUID empresaId, String protocolo) {
		empresasConsultadas.add(empresaId);
		protocolosConsultados.add(protocolo);
		if (indisponivel) {
			throw new DemandaIndisponivelException("fora do ar");
		}
		return Optional.ofNullable(dados.getOrDefault(empresaId, Map.of()).get(protocolo));
	}

	@Override
	public Chamado abrirChamado(UUID empresaId, NovoChamado chamado, UUID chaveIdempotencia) {
		Escrita escrita = new Escrita(empresaId, chamado, chaveIdempotencia);
		tentativasDeEscrita.add(escrita);
		if (indisponivel) {
			throw new DemandaIndisponivelException("fora do ar");
		}
		escritas.add(escrita);
		return new Chamado("WXN-2026-5000", chamado.titulo(), StatusDemanda.ABERTA, Instant.parse("2026-10-22T12:00:00Z"));
	}
}
