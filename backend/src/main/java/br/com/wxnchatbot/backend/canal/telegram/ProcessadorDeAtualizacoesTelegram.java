package br.com.wxnchatbot.backend.canal.telegram;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.canal.telegram.AtualizacaoDoTelegram.MensagemDoTelegram;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.service.ChatServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;
import br.com.wxnchatbot.backend.service.RespostaChat;
import jakarta.annotation.PreDestroy;

/**
 * Atende as mensagens do Telegram fora da requisição: o Telegram quer o 200 logo, e a Lily pode levar segundos (LLM,
 * API da WXN). O cliente é identificado pelo id do chat com o bot ({@code tg:<id>}): o Telegram não entrega o telefone.
 */
@Component
@ConditionalOnProperty(name = "telegram.habilitado", havingValue = "true")
public class ProcessadorDeAtualizacoesTelegram {

	static final String BOAS_VINDAS = "Oi! Eu sou a Lily, assistente virtual da WXN Tecnologia. Posso consultar o andamento de uma demanda pelo protocolo, abrir um chamado ou chamar um atendente. Como posso ajudar?";
	static final String SO_TEXTO = "Por enquanto eu só entendo mensagens de texto. Pode escrever o que você precisa?";
	static final String SEM_CADASTRO = "Não consegui liberar o seu atendimento por aqui. Fale com a equipe da WXN.";
	static final String FALHA = "Desculpe, tive um problema para processar sua mensagem. Tente de novo em instantes.";
	private static final int ATUALIZACOES_LEMBRADAS = 2000;

	private static final Logger log = LoggerFactory.getLogger(ProcessadorDeAtualizacoesTelegram.class);

	private final ChatServico chat;
	private final TelegramClient telegram;
	private final Executor executor;
	private final AtualizacoesJaProcessadas vistas = new AtualizacoesJaProcessadas(ATUALIZACOES_LEMBRADAS);

	@Autowired
	public ProcessadorDeAtualizacoesTelegram(ChatServico chat, TelegramClient telegram) {
		this(chat, telegram, Executors.newVirtualThreadPerTaskExecutor());
	}

	ProcessadorDeAtualizacoesTelegram(ChatServico chat, TelegramClient telegram, Executor executor) {
		this.chat = chat;
		this.telegram = telegram;
		this.executor = executor;
	}

	/** Devolve na hora; a resposta sai pelo Telegram quando ficar pronta. Uma atualização repetida é ignorada. */
	public void aceitar(AtualizacaoDoTelegram atualizacao) {
		if (vistas.registrar(atualizacao.updateId())) {
			executor.execute(() -> tratar(atualizacao));
		}
	}

	void tratar(AtualizacaoDoTelegram atualizacao) {
		MensagemDoTelegram mensagem = atualizacao.message();
		if (mensagem == null || mensagem.chat() == null || !mensagem.chat().privado()) {
			return;
		}
		if (mensagem.from() != null && mensagem.from().bot()) {
			return;
		}
		long chatId = mensagem.chat().id();
		try {
			if (mensagem.text() == null || mensagem.text().isBlank()) {
				telegram.enviarTexto(chatId, SO_TEXTO);
				return;
			}
			if (ehStart(mensagem.text())) {
				telegram.enviarTexto(chatId, BOAS_VINDAS);
				return;
			}
			String nome = mensagem.from() == null ? null : mensagem.from().nomeCompleto();
			RespostaChat resposta = chat.receber(OrigemConversa.TELEGRAM, "tg:" + chatId, nome, mensagem.text());
			if (resposta.resposta() != null) {
				telegram.enviarTexto(chatId, resposta.resposta());
			}
		} catch (RecursoNaoEncontradoException e) {
			avisar(chatId, atualizacao, SEM_CADASTRO);
		} catch (RuntimeException e) {
			log.error("Falha ao atender a atualização {} do Telegram", atualizacao.updateId(), e);
			avisar(chatId, atualizacao, FALHA);
		}
	}

	/** O botão "Iniciar" do Telegram manda "/start" (às vezes "/start texto" ou "/start@NomeDoBot"). */
	private static boolean ehStart(String texto) {
		String limpo = texto.strip();
		return limpo.equals("/start") || limpo.startsWith("/start ") || limpo.startsWith("/start@");
	}

	private void avisar(long chatId, AtualizacaoDoTelegram atualizacao, String texto) {
		try {
			telegram.enviarTexto(chatId, texto);
		} catch (RuntimeException e) {
			log.error("Não foi possível avisar o cliente da atualização {} pelo Telegram", atualizacao.updateId(), e);
		}
	}

	@PreDestroy
	void encerrar() {
		if (executor instanceof ExecutorService servico) {
			servico.shutdown();
		}
	}
}
