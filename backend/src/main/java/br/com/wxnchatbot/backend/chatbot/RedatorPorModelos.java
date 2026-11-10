package br.com.wxnchatbot.backend.chatbot;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.llm.MensagemLlm;

/** Sem LLM de verdade (chatbot.llm=fake, o padrão e o dos testes), a Lily fala com os textos prontos do código. */
@Component
@ConditionalOnProperty(name = "chatbot.llm", havingValue = "fake", matchIfMissing = true)
public class RedatorPorModelos implements Redator {

	@Override
	public String escrever(Situacao situacao, String rascunho, List<MensagemLlm> historico) {
		return rascunho;
	}
}
