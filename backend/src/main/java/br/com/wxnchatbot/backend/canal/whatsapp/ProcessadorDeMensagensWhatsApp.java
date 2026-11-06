package br.com.wxnchatbot.backend.canal.whatsapp;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.canal.whatsapp.EntregaDoWhatsApp.MensagemRecebida;
import br.com.wxnchatbot.backend.service.ChatServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;
import br.com.wxnchatbot.backend.service.RespostaChat;
import jakarta.annotation.PreDestroy;

/**
 * Atende as mensagens de uma entrega do webhook fora da requisição: a Meta quer o 200 logo, e a Lily pode levar
 * segundos (LLM, API da WXN). As mensagens da mesma entrega são atendidas em ordem.
 */
@Component
@ConditionalOnProperty(name = "whatsapp.habilitado", havingValue = "true")
public class ProcessadorDeMensagensWhatsApp {

	static final String SO_TEXTO = "Por enquanto eu só entendo mensagens de texto. Pode escrever o que você precisa?";
	static final String SEM_CADASTRO = "Não encontrei o seu número no cadastro da WXN. Fale com a equipe da WXN para liberar o atendimento por aqui.";
	static final String FALHA = "Desculpe, tive um problema para processar sua mensagem. Tente de novo em instantes.";
	private static final int MENSAGENS_LEMBRADAS = 2000;

	private static final Logger log = LoggerFactory.getLogger(ProcessadorDeMensagensWhatsApp.class);

	private final ChatServico chat;
	private final WhatsAppClient whatsApp;
	private final Executor executor;
	private final MensagensJaProcessadas vistas = new MensagensJaProcessadas(MENSAGENS_LEMBRADAS);

	@Autowired
	public ProcessadorDeMensagensWhatsApp(ChatServico chat, WhatsAppClient whatsApp) {
		this(chat, whatsApp, Executors.newVirtualThreadPerTaskExecutor());
	}

	ProcessadorDeMensagensWhatsApp(ChatServico chat, WhatsAppClient whatsApp, Executor executor) {
		this.chat = chat;
		this.whatsApp = whatsApp;
		this.executor = executor;
	}

	/** Devolve na hora; as respostas saem pelo WhatsApp quando ficarem prontas. */
	public void aceitar(List<MensagemRecebida> mensagens) {
		List<MensagemRecebida> novas = mensagens.stream().filter(m -> m.id() == null || vistas.registrar(m.id())).toList();
		if (!novas.isEmpty()) {
			executor.execute(() -> novas.forEach(this::tratar));
		}
	}

	private void tratar(MensagemRecebida mensagem) {
		try {
			if (mensagem.de() == null || mensagem.de().isBlank()) {
				log.warn("Mensagem {} do WhatsApp sem remetente, ignorada", mensagem.id());
				return;
			}
			if (!mensagem.ehTexto()) {
				whatsApp.enviarTexto(mensagem.de(), SO_TEXTO);
				return;
			}
			RespostaChat resposta = chat.receber(mensagem.de(), mensagem.nome(), mensagem.texto());
			if (resposta.resposta() != null) {
				whatsApp.enviarTexto(mensagem.de(), resposta.resposta());
			}
		} catch (RecursoNaoEncontradoException e) {
			avisar(mensagem, SEM_CADASTRO);
		} catch (RuntimeException e) {
			log.error("Falha ao atender a mensagem {} do WhatsApp", mensagem.id(), e);
			avisar(mensagem, FALHA);
		}
	}

	private void avisar(MensagemRecebida mensagem, String texto) {
		try {
			whatsApp.enviarTexto(mensagem.de(), texto);
		} catch (RuntimeException e) {
			log.error("Não foi possível avisar o cliente da mensagem {} pelo WhatsApp", mensagem.id(), e);
		}
	}

	@PreDestroy
	void encerrar() {
		if (executor instanceof ExecutorService servico) {
			servico.shutdown();
		}
	}
}
