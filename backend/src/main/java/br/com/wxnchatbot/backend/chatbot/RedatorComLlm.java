package br.com.wxnchatbot.backend.chatbot;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.llm.LlmClient;
import br.com.wxnchatbot.backend.llm.LlmIndisponivelException;
import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.llm.PromptDaLily;

/**
 * Pede à LLM que escreva a mensagem da Lily a partir do rascunho do código. A LLM muda as palavras, mas os fatos não
 * podem mudar: se o texto dela perder um protocolo, uma data, um status ou um título do rascunho, ou citar um protocolo
 * que não está nele, vale o rascunho. A LLM fora do ar também cai no rascunho, então a conversa nunca para por isso.
 */
@Component
@ConditionalOnProperty(name = "chatbot.llm", havingValue = "gemini")
public class RedatorComLlm implements Redator {

	private static final Logger log = LoggerFactory.getLogger(RedatorComLlm.class);

	static final int TAMANHO_MAXIMO = 800;
	private static final int MENSAGENS_DE_CONTEXTO = 8;

	private static final Pattern PROTOCOLO = Pattern.compile("\\bWXN-\\d{4}-\\d{1,6}\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern DATA = Pattern.compile("\\b\\d{2}/\\d{2}/\\d{4}\\b");
	private static final Pattern ENTRE_ASPAS = Pattern.compile("\"([^\"]+)\"");
	private static final Pattern STATUS = Pattern.compile("Status: ([^.]+)\\.");

	private final LlmClient llm;

	public RedatorComLlm(LlmClient llm) {
		this.llm = llm;
	}

	@Override
	public String escrever(Situacao situacao, String rascunho, List<MensagemLlm> historico) {
		try {
			String texto = llm.responder(PromptDaLily.paraRedigir(situacao.descricao(), rascunho), recortar(historico)).strip();
			if (fiel(rascunho, texto)) {
				return texto;
			}
			log.warn("A LLM mudou os fatos da mensagem ({}); usando o texto pronto.", situacao);
		} catch (LlmIndisponivelException e) {
			log.warn("LLM indisponível ao escrever a mensagem ({}), usando o texto pronto: {}", situacao, e.getMessage());
		}
		return rascunho;
	}

	/** O texto da LLM serve se tem tamanho razoável e mantém todos os fatos do rascunho sem inventar protocolos. */
	static boolean fiel(String rascunho, String texto) {
		if (texto.isBlank() || texto.length() > TAMANHO_MAXIMO) {
			return false;
		}
		String minusculo = texto.toLowerCase(Locale.ROOT);
		for (String fato : fatosDe(rascunho)) {
			if (!minusculo.contains(fato.toLowerCase(Locale.ROOT))) {
				return false;
			}
		}
		if (rascunho.contains("?") && !texto.contains("?")) {
			return false;
		}
		return protocolosDe(rascunho).containsAll(protocolosDe(texto));
	}

	/** O que tem de aparecer igual no texto final: protocolos, datas, status e tudo o que vem entre aspas. */
	private static Set<String> fatosDe(String rascunho) {
		Set<String> fatos = new HashSet<>(protocolosDe(rascunho));
		coletar(DATA, rascunho, 0, fatos);
		coletar(ENTRE_ASPAS, rascunho, 1, fatos);
		coletar(STATUS, rascunho, 1, fatos);
		return fatos;
	}

	private static Set<String> protocolosDe(String texto) {
		Set<String> protocolos = new HashSet<>();
		Matcher encontrado = PROTOCOLO.matcher(texto);
		while (encontrado.find()) {
			protocolos.add(encontrado.group().toUpperCase(Locale.ROOT));
		}
		return protocolos;
	}

	private static void coletar(Pattern padrao, String texto, int grupo, Set<String> destino) {
		Matcher encontrado = padrao.matcher(texto);
		while (encontrado.find()) {
			destino.add(encontrado.group(grupo).strip());
		}
	}

	private static List<MensagemLlm> recortar(List<MensagemLlm> historico) {
		int inicio = Math.max(0, historico.size() - MENSAGENS_DE_CONTEXTO);
		return historico.subList(inicio, historico.size());
	}
}
