package br.com.wxnchatbot.backend.ferramenta;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda.Encontrada;
import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda.Indisponivel;
import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda.NaoEncontrada;
import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda.ProtocoloInvalido;
import br.com.wxnchatbot.backend.integracao.wxn.DemandaGateway;
import br.com.wxnchatbot.backend.integracao.wxn.DemandaIndisponivelException;

@Service
public class ConsultaDemandaFerramenta {

	private static final Logger log = LoggerFactory.getLogger(ConsultaDemandaFerramenta.class);
	private static final Pattern PADRAO_PROTOCOLO = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{2,39}$");

	private final DemandaGateway gateway;

	public ConsultaDemandaFerramenta(DemandaGateway gateway) {
		this.gateway = gateway;
	}

	// empresaId vem do contexto do servidor; o único dado que vem do usuário ou da LLM é o protocolo.
	public ResultadoConsultaDemanda consultarStatus(UUID empresaId, String protocoloInformado) {
		Objects.requireNonNull(empresaId, "empresaId é obrigatório");
		String protocolo = normalizar(protocoloInformado);
		if (protocolo == null) {
			return new ProtocoloInvalido();
		}
		try {
			return gateway.consultarPorProtocolo(empresaId, protocolo)
					.<ResultadoConsultaDemanda>map(Encontrada::new)
					.orElseGet(() -> new NaoEncontrada(protocolo));
		} catch (DemandaIndisponivelException e) {
			log.warn("API da WXN indisponível ao consultar o protocolo {}: {}", protocolo, e.getMessage());
			return new Indisponivel();
		}
	}

	private static String normalizar(String informado) {
		if (informado == null) {
			return null;
		}
		String protocolo = informado.strip().toUpperCase(Locale.ROOT);
		return PADRAO_PROTOCOLO.matcher(protocolo).matches() ? protocolo : null;
	}
}
