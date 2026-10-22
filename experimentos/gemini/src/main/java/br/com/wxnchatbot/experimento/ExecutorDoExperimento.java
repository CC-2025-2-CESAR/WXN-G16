package br.com.wxnchatbot.experimento;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class ExecutorDoExperimento implements CommandLineRunner, ExitCodeGenerator {

	private static final Logger log = LoggerFactory.getLogger(ExecutorDoExperimento.class);

	private final ChatClient.Builder construtor;
	private final ConfiguracaoExperimento configuracao;
	private final String nivelDePensamento;
	private final String chave;
	private int codigoDeSaida;

	public ExecutorDoExperimento(ChatClient.Builder construtor, ConfiguracaoExperimento configuracao,
			@Value("${spring.ai.google.genai.chat.thinking-level}") String nivelDePensamento,
			@Value("${spring.ai.google.genai.chat.include-thoughts}") String incluirPensamentos,
			@Value("${GEMINI_API_KEY:}") String chave) {
		this.construtor = construtor;
		this.configuracao = configuracao;
		this.nivelDePensamento = nivelDePensamento + " (include-thoughts=" + incluirPensamentos + ")";
		this.chave = chave;
	}

	@Override
	public int getExitCode() {
		return codigoDeSaida;
	}

	@Override
	public void run(String... args) throws Exception {
		if (chave.isBlank()) {
			log.error("A variável GEMINI_API_KEY não está definida. Crie a chave no Google AI Studio e defina a "
					+ "variável antes de rodar (veja experimentos/gemini/LEIA-ME.md).");
			codigoDeSaida = 1;
			return;
		}
		List<Pergunta> perguntas = configuracao.rapido() ? Perguntas.rapidas() : Perguntas.todas();
		ChatClient cliente = construtor.defaultSystem(PromptDaLily.TEXTO).build();
		List<Resultado> resultados = new ArrayList<>();
		int total = perguntas.size() * configuracao.modelos().size();

		for (String modelo : configuracao.modelos()) {
			for (Pergunta pergunta : perguntas) {
				Resultado resultado = executar(cliente, modelo, pergunta);
				resultados.add(resultado);
				log.info("[{}/{}] {} #{} {} em {} ms{}", resultados.size(), total, modelo, pergunta.numero(),
						resultado.verificacao().passou() ? "ok" : "falhou", resultado.latenciaMs(),
						resultado.erro() != null ? " - " + resultado.erro() : "");
				Thread.sleep(configuracao.pausa().toMillis());
			}
		}

		LocalDateTime agora = LocalDateTime.now();
		Path arquivo = Relatorio.gravar(Path.of(configuracao.pastaResultados()), agora,
				Relatorio.montar(agora, nivelDePensamento, resultados));
		log.info("Relatório gravado em {}", arquivo.toAbsolutePath());
		if (resultados.stream().allMatch(r -> r.erro() != null)) {
			log.error("Todas as chamadas falharam; confira a chave, os limites e a conexão.");
			codigoDeSaida = 1;
		}
	}

	private Resultado executar(ChatClient cliente, String modelo, Pergunta pergunta) {
		FerramentasDaLily ferramentas = new FerramentasDaLily();
		long inicio = System.nanoTime();
		try {
			ChatResponse resposta = cliente.prompt()
					.user(pergunta.texto())
					.tools(ferramentas)
					.options(GoogleGenAiChatOptions.builder().model(modelo).temperature(0.2))
					.call()
					.chatResponse();
			long latenciaMs = (System.nanoTime() - inicio) / 1_000_000;
			String texto = resposta.getResult() == null ? "" : resposta.getResult().getOutput().getText();
			Usage uso = resposta.getMetadata().getUsage();
			return new Resultado(modelo, pergunta, texto, ferramentas.chamadas(), latenciaMs, uso.getPromptTokens(),
					uso.getCompletionTokens(), null,
					Avaliador.avaliar(pergunta, texto, ferramentas.chamadas(), null));
		} catch (Exception e) {
			long latenciaMs = (System.nanoTime() - inicio) / 1_000_000;
			String erro = descrever(e);
			return new Resultado(modelo, pergunta, "", ferramentas.chamadas(), latenciaMs, null, null, erro,
					Avaliador.avaliar(pergunta, "", ferramentas.chamadas(), erro));
		}
	}

	static String descrever(Throwable erro) {
		Throwable raiz = erro;
		while (raiz.getCause() != null && raiz.getCause() != raiz) {
			raiz = raiz.getCause();
		}
		String principal = erro.getClass().getSimpleName() + ": " + abreviar(erro.getMessage());
		if (raiz == erro) {
			return principal;
		}
		return principal + " (causa: " + raiz.getClass().getSimpleName() + ": " + abreviar(raiz.getMessage()) + ")";
	}

	private static String abreviar(String mensagem) {
		if (mensagem == null) {
			return "";
		}
		String linha = mensagem.replaceAll("\\s+", " ").strip();
		return linha.length() > 300 ? linha.substring(0, 300) + "..." : linha;
	}
}
