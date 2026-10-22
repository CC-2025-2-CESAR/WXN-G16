package br.com.wxnchatbot.backend.notificacao;

public interface NotificationGateway {

	void enviarConfirmacaoDeSolicitacao(ConfirmacaoDeSolicitacao confirmacao);
}
