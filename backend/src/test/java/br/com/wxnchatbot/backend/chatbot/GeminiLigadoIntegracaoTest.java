package br.com.wxnchatbot.backend.chatbot;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import br.com.wxnchatbot.backend.llm.LlmClient;
import br.com.wxnchatbot.backend.llm.LlmClientGemini;

/** Com chatbot.llm=gemini a aplicação sobe com o cliente do Gemini e o classificador por LLM, e não com os de mentira. */
@SpringBootTest(properties = { "chatbot.llm=gemini", "gemini.api-key=chave-so-de-teste",
		"gemini.url-base=http://127.0.0.1:1/v1beta" })
class GeminiLigadoIntegracaoTest {

	@Autowired
	private LlmClient llm;

	@Autowired
	private ClassificadorDeIntencao classificador;

	@Test
	void usaOGeminiEOClassificadorComLlm() {
		assertInstanceOf(LlmClientGemini.class, llm);
		assertInstanceOf(ClassificadorComLlm.class, classificador);
	}
}
