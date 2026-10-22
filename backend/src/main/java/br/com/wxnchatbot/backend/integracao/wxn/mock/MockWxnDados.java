package br.com.wxnchatbot.backend.integracao.wxn.mock;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.integracao.wxn.Chamado;
import br.com.wxnchatbot.backend.integracao.wxn.Demanda;
import br.com.wxnchatbot.backend.integracao.wxn.NovoChamado;
import br.com.wxnchatbot.backend.integracao.wxn.StatusDemanda;

@Component
@Profile("mock")
public class MockWxnDados {

	public static final UUID EMPRESA_DEMO = UUID.fromString("00000000-0000-0000-0000-000000000001");
	public static final UUID EMPRESA_OUTRA = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private final Map<UUID, Map<String, Demanda>> demandasPorEmpresa = new ConcurrentHashMap<>();
	private final Map<String, Chamado> chamadosPorChave = new ConcurrentHashMap<>();
	private final AtomicInteger sequencia = new AtomicInteger(5000);

	public MockWxnDados() {
		semear(EMPRESA_DEMO, new Demanda("WXN-2026-0001", "Ajuste no relatório de vendas do ERP",
				StatusDemanda.EM_ANDAMENTO, Instant.parse("2026-10-20T14:30:00Z"),
				"Em desenvolvimento pelo time de integrações"));
		semear(EMPRESA_DEMO, new Demanda("WXN-2026-0002", "Integração de emissão de notas fiscais",
				StatusDemanda.CONCLUIDA, Instant.parse("2026-10-14T18:05:00Z"), "Entregue e validada pelo cliente"));
		semear(EMPRESA_DEMO, new Demanda("WXN-2026-0003", "Cadastro de novos centros de custo",
				StatusDemanda.AGUARDANDO_CLIENTE, Instant.parse("2026-10-21T09:12:00Z"),
				"Aguardando o cliente enviar a planilha de centros de custo"));
		semear(EMPRESA_DEMO, new Demanda("WXN-LENTO", "Demanda usada para simular lentidão da API",
				StatusDemanda.EM_ANDAMENTO, Instant.parse("2026-10-19T10:00:00Z"), null));
		semear(EMPRESA_OUTRA, new Demanda("WXN-2026-9001", "Migração da base de clientes", StatusDemanda.ABERTA,
				Instant.parse("2026-10-18T16:45:00Z"), null));
	}

	public Optional<Demanda> buscar(UUID empresaId, String protocolo) {
		return Optional.ofNullable(demandasPorEmpresa.getOrDefault(empresaId, Map.of()).get(protocolo));
	}

	public Chamado abrir(UUID empresaId, NovoChamado novo, String chaveIdempotencia) {
		if (chaveIdempotencia == null || chaveIdempotencia.isBlank()) {
			return criar(empresaId, novo);
		}
		return chamadosPorChave.computeIfAbsent(empresaId + ":" + chaveIdempotencia, chave -> criar(empresaId, novo));
	}

	private Chamado criar(UUID empresaId, NovoChamado novo) {
		Instant agora = Instant.now();
		String protocolo = "WXN-2026-" + sequencia.getAndIncrement();
		semear(empresaId, new Demanda(protocolo, novo.titulo(), StatusDemanda.ABERTA, agora, null));
		return new Chamado(protocolo, novo.titulo(), StatusDemanda.ABERTA, agora);
	}

	private void semear(UUID empresaId, Demanda demanda) {
		demandasPorEmpresa.computeIfAbsent(empresaId, id -> new ConcurrentHashMap<>()).put(demanda.protocolo(), demanda);
	}
}
