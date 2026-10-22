package br.com.wxnchatbot.backend.ferramenta;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import br.com.wxnchatbot.backend.integracao.wxn.Demanda;
import br.com.wxnchatbot.backend.integracao.wxn.StatusDemanda;

public sealed interface ResultadoConsultaDemanda {

	record Encontrada(Demanda demanda) implements ResultadoConsultaDemanda {

		private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
				.withZone(ZoneId.of("America/Recife"));

		public String resumoParaCliente() {
			StringBuilder texto = new StringBuilder("Demanda ").append(demanda.protocolo()).append(" - ")
					.append(demanda.titulo()).append(". Status: ").append(rotulo(demanda.status()))
					.append(". Última atualização em ").append(DATA.format(demanda.atualizadaEm())).append(".");
			if (demanda.observacao() != null && !demanda.observacao().isBlank()) {
				texto.append(" Observação: ").append(demanda.observacao()).append(".");
			}
			return texto.toString();
		}

		private static String rotulo(StatusDemanda status) {
			return switch (status) {
				case ABERTA -> "aberta";
				case EM_ANDAMENTO -> "em andamento";
				case AGUARDANDO_CLIENTE -> "aguardando retorno do cliente";
				case CONCLUIDA -> "concluída";
				case CANCELADA -> "cancelada";
			};
		}
	}

	record NaoEncontrada(String protocolo) implements ResultadoConsultaDemanda {
	}

	record ProtocoloInvalido() implements ResultadoConsultaDemanda {
	}

	record Indisponivel() implements ResultadoConsultaDemanda {
	}
}
