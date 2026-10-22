package br.com.wxnchatbot.backend.notificacao;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Quando existir a implementação real (SMTP), marque esta classe com @Profile("!prod").
@Component
public class NotificationGatewayFake implements NotificationGateway {

	private static final Logger log = LoggerFactory.getLogger(NotificationGatewayFake.class);

	private final List<ConfirmacaoDeSolicitacao> enviadas = new CopyOnWriteArrayList<>();

	@Override
	public void enviarConfirmacaoDeSolicitacao(ConfirmacaoDeSolicitacao confirmacao) {
		enviadas.add(confirmacao);
		log.warn("E-mail de confirmação do protocolo {} NÃO foi enviado: implementação fake, sem SMTP configurado",
				confirmacao.protocolo());
	}

	public List<ConfirmacaoDeSolicitacao> enviadas() {
		return List.copyOf(enviadas);
	}
}
