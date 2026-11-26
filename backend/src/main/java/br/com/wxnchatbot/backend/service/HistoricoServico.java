package br.com.wxnchatbot.backend.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;

@Service
public class HistoricoServico {
	private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
	private static final int TAMANHO_MAXIMO = 100;
	private final ConversaRepositorio conversas;

	public HistoricoServico(ConversaRepositorio conversas) {
		this.conversas = conversas;
	}

	@Transactional(readOnly = true)
	public PaginaHistorico listar(String busca, LocalDate dataInicial, LocalDate dataFinal, UUID atendenteId,
			OrigemConversa origem, int pagina, int tamanho) {
		if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
			throw new IllegalArgumentException("A página deve ser positiva e o tamanho deve estar entre 1 e 100");
		}
		if (dataInicial != null && dataFinal != null && dataFinal.isBefore(dataInicial)) {
			throw new IllegalArgumentException("A data final deve ser igual ou posterior à data inicial");
		}
		String termo = prepararBusca(busca);
		UUID idBusca = interpretarId(busca);
		var inicio = dataInicial == null ? null : dataInicial.atStartOfDay(FUSO).toInstant();
		var fim = dataFinal == null ? null : dataFinal.plusDays(1).atStartOfDay(FUSO).toInstant();
		Page<Conversa> resultado = conversas.buscarHistorico(termo, idBusca, inicio, fim, atendenteId, origem,
				PageRequest.of(pagina, tamanho));
		if (resultado.isEmpty()) return new PaginaHistorico(java.util.List.of(), resultado.getTotalElements(), pagina, tamanho);
		var ids = resultado.getContent().stream().map(Conversa::getId).toList();
		Map<UUID, Long> contagens = conversas.contarMensagens(ids).stream()
				.collect(Collectors.toMap(linha -> (UUID) linha[0], linha -> ((Number) linha[1]).longValue()));
		var itens = resultado.getContent().stream()
				.map(conversa -> ItemHistorico.de(conversa, contagens.getOrDefault(conversa.getId(), 0L)))
				.toList();
		return new PaginaHistorico(itens, resultado.getTotalElements(), pagina, tamanho);
	}

	@Transactional(readOnly = true)
	public DetalheHistorico detalhar(UUID conversaId) {
		Conversa conversa = conversas.findById(conversaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Conversa", conversaId));
		if (conversa.getStatus() != StatusConversa.ENCERRADA) {
			throw new RecursoNaoEncontradoException("Conversa", conversaId);
		}
		return DetalheHistorico.de(conversa);
	}

	private static String prepararBusca(String busca) {
		if (busca == null || busca.isBlank()) return null;
		String texto = busca.strip().toLowerCase(Locale.ROOT);
		if (texto.length() > 120) throw new IllegalArgumentException("A busca deve ter até 120 caracteres");
		return "%" + texto.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
	}

	private static UUID interpretarId(String busca) {
		if (busca == null) return null;
		try {
			return UUID.fromString(busca.strip());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
