package br.com.wxnchatbot.backend.canal.twilio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.service.CanalDeSaida;
import br.com.wxnchatbot.backend.service.FalhaNaEntregaException;

/**
 * Entrega ao cliente, pelo WhatsApp do Twilio, a mensagem que o atendente escreveu no dashboard. O telefone do cliente
 * fica no cadastro sem o 55 do Brasil (ou, às vezes, com ele); o Twilio quer o número completo, com DDI.
 */
@Component
@ConditionalOnProperty(name = "twilio.habilitado", havingValue = "true")
public class CanalDeSaidaTwilio implements CanalDeSaida {

	private static final Logger log = LoggerFactory.getLogger(CanalDeSaidaTwilio.class);
	private static final String DDI_DO_BRASIL = "55";

	/** O Twilio recusa texto livre fora das 24 horas depois da última mensagem do cliente (erro 63016). */
	static final int FORA_DA_JANELA = 63016;
	/** No sandbox do Twilio só recebe quem mandou "join <código>" ao número de teste (erro 63015). */
	static final int FORA_DO_SANDBOX = 63015;

	private final TwilioClient twilio;

	public CanalDeSaidaTwilio(TwilioClient twilio) {
		this.twilio = twilio;
	}

	@Override
	public OrigemConversa origem() {
		return OrigemConversa.WHATSAPP;
	}

	@Override
	public void entregar(String identificador, String texto) {
		String para = enderecoDe(identificador);
		try {
			twilio.enviarTexto(para, texto);
		} catch (FalhaNoTwilioException e) {
			// A mensagem da falha já vem sem as credenciais (veja FalhaNoTwilioException).
			log.warn("O Twilio recusou a entrega (HTTP {}, código {}): {}", e.status(), e.codigo(), e.getMessage());
			throw new FalhaNaEntregaException(avisoDe(e));
		}
	}

	private static String avisoDe(FalhaNoTwilioException e) {
		if (e.codigo() == FORA_DA_JANELA) {
			return "A janela de 24 horas do WhatsApp fechou: o cliente precisa escrever de novo antes de você poder responder. "
					+ "A mensagem foi salva no histórico.";
		}
		if (e.codigo() == FORA_DO_SANDBOX) {
			return "O cliente ainda não entrou no sandbox do Twilio (ele precisa mandar \"join <código>\" ao número de teste). "
					+ "A mensagem foi salva no histórico.";
		}
		if (e.status() == 401) {
			return "O Twilio recusou as credenciais do backend. Avise a equipe técnica. A mensagem foi salva no histórico.";
		}
		return "Não foi possível entregar pelo WhatsApp agora. A mensagem foi salva no histórico; tente enviar de novo.";
	}

	/** {@code whatsapp:+55<DDD><número>} a partir do telefone do cadastro. */
	private static String enderecoDe(String identificador) {
		String digitos = identificador == null ? "" : identificador.replaceAll("\\D", "");
		if (identificador == null || identificador.startsWith("tg:") || digitos.length() < 10 || digitos.length() > 15) {
			throw new FalhaNaEntregaException(
					"Este cliente não tem um número de WhatsApp válido no cadastro. A mensagem foi salva no histórico, mas não foi entregue.");
		}
		boolean semPais = digitos.length() == 10 || digitos.length() == 11;
		return "whatsapp:+" + (semPais ? DDI_DO_BRASIL + digitos : digitos);
	}
}
