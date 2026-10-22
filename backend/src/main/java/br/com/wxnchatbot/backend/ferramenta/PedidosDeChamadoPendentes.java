package br.com.wxnchatbot.backend.ferramenta;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.integracao.wxn.NovoChamado;

@Component
public class PedidosDeChamadoPendentes {

	static final Duration VALIDADE = Duration.ofMinutes(10);

	record Pedido(UUID empresaId, NovoChamado chamado, UUID chaveIdempotencia, Instant expiraEm) {
	}

	private final Map<UUID, Pedido> porConversa = new ConcurrentHashMap<>();
	private final Clock relogio;

	@Autowired
	public PedidosDeChamadoPendentes() {
		this(Clock.systemUTC());
	}

	PedidosDeChamadoPendentes(Clock relogio) {
		this.relogio = relogio;
	}

	Pedido registrar(UUID empresaId, UUID conversaId, NovoChamado chamado) {
		Instant agora = relogio.instant();
		porConversa.values().removeIf(pedido -> !agora.isBefore(pedido.expiraEm()));
		Pedido pedido = new Pedido(empresaId, chamado, UUID.randomUUID(), agora.plus(VALIDADE));
		porConversa.put(conversaId, pedido);
		return pedido;
	}

	Optional<Pedido> buscarValido(UUID empresaId, UUID conversaId) {
		Pedido pedido = porConversa.get(conversaId);
		if (pedido == null) {
			return Optional.empty();
		}
		if (!relogio.instant().isBefore(pedido.expiraEm())) {
			porConversa.remove(conversaId, pedido);
			return Optional.empty();
		}
		return pedido.empresaId().equals(empresaId) ? Optional.of(pedido) : Optional.empty();
	}

	void consumir(UUID conversaId, Pedido pedido) {
		porConversa.remove(conversaId, pedido);
	}

	void descartar(UUID conversaId) {
		porConversa.remove(conversaId);
	}
}
