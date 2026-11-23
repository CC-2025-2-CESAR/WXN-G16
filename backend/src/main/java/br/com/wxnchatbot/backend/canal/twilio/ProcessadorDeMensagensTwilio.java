package br.com.wxnchatbot.backend.canal.twilio;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.service.ChatServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;
import br.com.wxnchatbot.backend.service.RespostaChat;
import jakarta.annotation.PreDestroy;

/**
 * Atende as mensagens do WhatsApp que chegam pelo Twilio fora da requisição: o Twilio quer a resposta do webhook logo,
 * e a Lily pode levar segundos (LLM, API da WXN). A resposta sai por uma chamada à API do Twilio, não pela resposta
 * do webhook. O cliente é identificado pelo telefone, como no WhatsApp da Meta.
 */
@Component
@ConditionalOnProperty(name = "twilio.habilitado", havingValue = "true")
public class ProcessadorDeMensagensTwilio {

	static final String SO_TEXTO = "Por enquanto eu só entendo mensagens de texto. Pode escrever o que você precisa?";
	static final String SEM_CADASTRO = "Não consegui liberar o seu atendimento por aqui. Fale com a equipe da WXN.";
	static final String FALHA = "Desculpe, tive um problema para processar sua mensagem. Tente de novo em instantes.";
	private static final int MENSAGENS_LEMBRADAS = 2000;

	private static final Logger log = LoggerFactory.getLogger(ProcessadorDeMensagensTwilio.class);

	private final ChatServico chat;
	private final TwilioClient twilio;
	private final Executor executor;
	private final MensagensVistas vistas = new MensagensVistas(MENSAGENS_LEMBRADAS);

	@Autowired
	public ProcessadorDeMensagensTwilio(ChatServico chat, TwilioClient twilio) {
		this(chat, twilio, Executors.newVirtualThreadPerTaskExecutor());
	}

	ProcessadorDeMensagensTwilio(ChatServico chat, TwilioClient twilio, Executor executor) {
		this.chat = chat;
		this.twilio = twilio;
		this.executor = executor;
	}

	/** Devolve na hora; a resposta sai pelo WhatsApp quando ficar pronta. Uma mensagem repetida é ignorada. */
	public void aceitar(MensagemDoTwilio mensagem) {
		if (mensagem.id() == null || vistas.registrar(mensagem.id())) {
			executor.execute(() -> tratar(mensagem));
		}
	}

	void tratar(MensagemDoTwilio mensagem) {
		if (!mensagem.ehWhatsApp() || mensagem.telefone().isEmpty()) {
			log.warn("Mensagem {} do Twilio sem remetente de WhatsApp, ignorada", mensagem.id());
			return;
		}
		try {
			if (!mensagem.temTexto()) {
				twilio.enviarTexto(mensagem.de(), SO_TEXTO);
				return;
			}
			RespostaChat resposta = chat.receber(OrigemConversa.WHATSAPP, mensagem.telefone(), mensagem.nome(),
					mensagem.texto());
			if (resposta.resposta() != null) {
				twilio.enviarTexto(mensagem.de(), resposta.resposta());
			}
		} catch (RecursoNaoEncontradoException e) {
			avisar(mensagem, SEM_CADASTRO);
		} catch (RuntimeException e) {
			log.error("Falha ao atender a mensagem {} do Twilio", mensagem.id(), e);
			avisar(mensagem, FALHA);
		}
	}

	private void avisar(MensagemDoTwilio mensagem, String texto) {
		try {
			twilio.enviarTexto(mensagem.de(), texto);
		} catch (RuntimeException e) {
			log.error("Não foi possível avisar o cliente da mensagem {} pelo Twilio", mensagem.id(), e);
		}
	}

	@PreDestroy
	void encerrar() {
		if (executor instanceof ExecutorService servico) {
			servico.shutdown();
		}
	}
}
