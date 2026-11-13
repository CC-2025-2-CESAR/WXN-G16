package br.com.wxnchatbot.backend.chatbot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.chatbot.DecisaoDoTurno.Encaminhar;
import br.com.wxnchatbot.backend.chatbot.DecisaoDoTurno.Responder;
import br.com.wxnchatbot.backend.chatbot.InterpretadorDeConfirmacao.Resposta;
import br.com.wxnchatbot.backend.ferramenta.ConfirmacaoChamadoServico;
import br.com.wxnchatbot.backend.ferramenta.ConsultaDemandaFerramenta;
import br.com.wxnchatbot.backend.ferramenta.PedidoDeConfirmacao;
import br.com.wxnchatbot.backend.ferramenta.ResultadoAbertura;
import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda;
import br.com.wxnchatbot.backend.ferramenta.SolicitacaoChamadoFerramenta;
import br.com.wxnchatbot.backend.integracao.wxn.NovoChamado;
import br.com.wxnchatbot.backend.llm.LlmClient;
import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.llm.MensagemLlm.Papel;
import br.com.wxnchatbot.backend.llm.PromptDaLily;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Mensagem;
import br.com.wxnchatbot.backend.model.TipoRemetente;

/**
 * Escolhe o caminho de cada mensagem: descobre a intenção e chama a ferramenta certa. O que mexe em dados (consultar
 * a demanda, abrir o chamado) é decidido aqui, em código. As palavras de cada resposta passam pelo {@link Redator}: com
 * uma LLM configurada é ela que escreve (sem mudar os fatos), sem LLM valem os textos prontos abaixo.
 */
@Component
public class RoteadorDeIntencao {

	static final String PEDIR_PROTOCOLO = "Claro! Qual é o protocolo da demanda? Ele tem o formato WXN-2026-0001.";
	static final String PEDIR_DETALHES = "Posso abrir um chamado para você. Me conta o que está acontecendo, com o máximo de detalhes que puder.";
	static final String PEDIR_SIM_OU_NAO = "Responda sim para eu abrir o chamado ou não para cancelar.";
	static final String FORA_DE_ESCOPO = "Eu só consigo ajudar com o status de demandas, a abertura de chamados e dúvidas sobre o atendimento da WXN. Se preferir, chamo um atendente.";
	static final String CONSULTA_INDISPONIVEL = "Não consegui consultar a demanda agora. Tente de novo em instantes ou peça para falar com um atendente.";
	static final String CHAMADO_INDISPONIVEL = "Não consegui abrir o chamado agora. Responda sim para eu tentar de novo em instantes ou peça para falar com um atendente.";
	static final String PEDIDO_EXPIROU = "Não encontrei um pedido de chamado esperando confirmação. Se quiser, me conte de novo o que está acontecendo.";
	static final String CHAMADO_CANCELADO = "Tudo bem, não abri o chamado. Posso ajudar com mais alguma coisa?";
	static final String ENCAMINHANDO = "Claro! Vou chamar um atendente para te ajudar. Você já está na fila e logo alguém assume a conversa.";
	static final String ENCAMINHANDO_SEM_PROTOCOLO = "Sem o protocolo eu não consigo localizar a demanda com segurança. Vou chamar um atendente para buscar pelo seu cadastro.";
	static final String ENCAMINHANDO_POR_DUVIDA = "Não tenho certeza de que consigo ajudar com isso, então vou chamar um atendente para você.";

	private static final int TITULO_USADO_MAXIMO = 100;

	/** O que a Lily acabou de pedir ao cliente: a próxima mensagem dele é a resposta a esse pedido. */
	private enum Pendencia {
		PROTOCOLO, DETALHES
	}

	/** O caminho escolhido para a mensagem e, para ABRIR_CHAMADO, o que o classificador achou dos detalhes. */
	private record Rota(Intencao intencao, Boolean descricaoSuficiente) {
	}

	private final ClassificadorDeIntencao classificador;
	private final ExtratorDeProtocolo extrator;
	private final InterpretadorDeConfirmacao interpretador;
	private final ConsultaDemandaFerramenta consultaDemanda;
	private final SolicitacaoChamadoFerramenta solicitacaoChamado;
	private final ConfirmacaoChamadoServico confirmacaoChamado;
	private final LlmClient llm;
	private final BaseDeConhecimento conhecimento;
	private final Redator redator;
	private final double limiarDeConfianca;
	/**
	 * Em memória, como os pedidos de chamado esperando confirmação. Antes o pedido era reconhecido pelo texto pronto da
	 * Lily na conversa, o que não vale mais quando é a LLM que escreve.
	 */
	private final Map<UUID, Pendencia> pendencias = new ConcurrentHashMap<>();

	@Autowired
	public RoteadorDeIntencao(ClassificadorDeIntencao classificador, ExtratorDeProtocolo extrator,
			InterpretadorDeConfirmacao interpretador, ConsultaDemandaFerramenta consultaDemanda,
			SolicitacaoChamadoFerramenta solicitacaoChamado, ConfirmacaoChamadoServico confirmacaoChamado, LlmClient llm,
			BaseDeConhecimento conhecimento, Redator redator,
			@Value("${chatbot.intencao.limiar-confianca:0.7}") double limiarDeConfianca) {
		this.classificador = classificador;
		this.extrator = extrator;
		this.interpretador = interpretador;
		this.consultaDemanda = consultaDemanda;
		this.solicitacaoChamado = solicitacaoChamado;
		this.confirmacaoChamado = confirmacaoChamado;
		this.llm = llm;
		this.conhecimento = conhecimento;
		this.redator = redator;
		this.limiarDeConfianca = limiarDeConfianca;
	}

	/** Igual ao principal, com os textos prontos da Lily (sem LLM escrevendo as respostas). */
	public RoteadorDeIntencao(ClassificadorDeIntencao classificador, ExtratorDeProtocolo extrator,
			InterpretadorDeConfirmacao interpretador, ConsultaDemandaFerramenta consultaDemanda,
			SolicitacaoChamadoFerramenta solicitacaoChamado, ConfirmacaoChamadoServico confirmacaoChamado, LlmClient llm,
			BaseDeConhecimento conhecimento, double limiarDeConfianca) {
		this(classificador, extrator, interpretador, consultaDemanda, solicitacaoChamado, confirmacaoChamado, llm,
				conhecimento, new RedatorPorModelos(), limiarDeConfianca);
	}

	/** Decide o que fazer com a última mensagem do histórico (a do cliente, já gravada na conversa). */
	public DecisaoDoTurno decidir(Conversa conversa, List<MensagemLlm> historico) {
		String texto = ultimaDoCliente(historico);
		UUID empresaId = conversa.getEmpresa().getId();
		Pendencia pendente = pendencias.remove(conversa.getId());
		if (confirmacaoChamado.temPedidoPendente(empresaId, conversa.getId())) {
			return tratarConfirmacao(empresaId, conversa, historico, texto);
		}
		String anterior = ultimaDoChatbot(conversa).orElse("");
		boolean esperaProtocolo = pendente == Pendencia.PROTOCOLO || anterior.equals(PEDIR_PROTOCOLO);
		boolean esperaDetalhes = pendente == Pendencia.DETALHES || anterior.equals(PEDIR_DETALHES);
		Rota rota = rotaDe(historico, esperaProtocolo, esperaDetalhes);
		return switch (rota.intencao()) {
			case CONSULTAR_DEMANDA -> consultar(empresaId, conversa.getId(), texto, esperaProtocolo, historico);
			case ABRIR_CHAMADO -> abrirChamado(empresaId, conversa.getId(), texto, esperaDetalhes, rota.descricaoSuficiente(),
					historico);
			case DUVIDA -> responderDuvida(historico);
			case FALAR_COM_HUMANO -> encaminhar("O cliente pediu para falar com um atendente", Situacao.ENCAMINHANDO,
					ENCAMINHANDO, historico);
			case FORA_DE_ESCOPO -> responder(Situacao.FORA_DE_ESCOPO, FORA_DE_ESCOPO, historico);
			case INCERTO -> encaminhar("A Lily não conseguiu entender o pedido do cliente", Situacao.ENCAMINHANDO_POR_DUVIDA,
					ENCAMINHANDO_POR_DUVIDA, historico);
		};
	}

	private Responder responder(Situacao situacao, String rascunho, List<MensagemLlm> historico) {
		return new Responder(redator.escrever(situacao, rascunho, historico));
	}

	private DecisaoDoTurno responderDuvida(List<MensagemLlm> historico) {
		String texto = llm.responder(PromptDaLily.comConhecimento(conhecimento.texto()), historico);
		if (FiltroDeSaida.seguro(texto)) {
			return new Responder(texto);
		}
		return encaminhar("A resposta da LLM foi barrada pelo filtro de saída", Situacao.ENCAMINHANDO_POR_DUVIDA,
				ENCAMINHANDO_POR_DUVIDA, historico);
	}

	private Encaminhar encaminhar(String motivo, Situacao situacao, String rascunho, List<MensagemLlm> historico) {
		return new Encaminhar(motivo, redator.escrever(situacao, rascunho, historico));
	}

	/** Classifica a mensagem; quando ela responde a uma pergunta da Lily, segue o assunto dessa pergunta. */
	private Rota rotaDe(List<MensagemLlm> historico, boolean esperaProtocolo, boolean esperaDetalhes) {
		ResultadoIntencao resultado = classificador.classificar(historico);
		Intencao intencao = resultado.confianca() < limiarDeConfianca ? Intencao.INCERTO : resultado.intencao();
		boolean semAssuntoClaro = intencao == Intencao.DUVIDA || intencao == Intencao.INCERTO;
		if (esperaProtocolo && semAssuntoClaro) {
			return new Rota(Intencao.CONSULTAR_DEMANDA, null);
		}
		if (esperaDetalhes && (semAssuntoClaro || intencao == Intencao.CONSULTAR_DEMANDA)) {
			return new Rota(Intencao.ABRIR_CHAMADO, null);
		}
		return new Rota(intencao, resultado.descricaoSuficiente());
	}

	private DecisaoDoTurno consultar(UUID empresaId, UUID conversaId, String texto, boolean esperaProtocolo,
			List<MensagemLlm> historico) {
		Optional<String> protocolo = extrator.extrair(texto);
		if (protocolo.isEmpty()) {
			if (esperaProtocolo) {
				return encaminhar("O cliente quer consultar uma demanda, mas não tem o protocolo",
						Situacao.ENCAMINHANDO_SEM_PROTOCOLO, ENCAMINHANDO_SEM_PROTOCOLO, historico);
			}
			return pedirProtocolo(conversaId, historico);
		}
		return switch (consultaDemanda.consultarStatus(empresaId, protocolo.get())) {
			case ResultadoConsultaDemanda.Encontrada encontrada ->
				responder(Situacao.DEMANDA_ENCONTRADA, encontrada.resumoParaCliente(), historico);
			case ResultadoConsultaDemanda.NaoEncontrada naoEncontrada -> responder(Situacao.DEMANDA_NAO_ENCONTRADA,
					"Não encontrei a demanda " + naoEncontrada.protocolo()
							+ ". Confira o número ou me diga se prefere falar com um atendente.",
					historico);
			case ResultadoConsultaDemanda.ProtocoloInvalido invalido -> pedirProtocolo(conversaId, historico);
			case ResultadoConsultaDemanda.Indisponivel indisponivel ->
				responder(Situacao.CONSULTA_INDISPONIVEL, CONSULTA_INDISPONIVEL, historico);
		};
	}

	private Responder pedirProtocolo(UUID conversaId, List<MensagemLlm> historico) {
		pendencias.put(conversaId, Pendencia.PROTOCOLO);
		return responder(Situacao.PEDIR_PROTOCOLO, PEDIR_PROTOCOLO, historico);
	}

	/**
	 * Só pede confirmação quando o cliente já contou o que está acontecendo; se a mensagem for vaga ("aquele problema do
	 * relatório"), pergunta primeiro. Na dúvida, pergunta: basta as palavras ou o classificador acharem pouco. Depois de
	 * ter perguntado, aceita o que vier.
	 */
	private DecisaoDoTurno abrirChamado(UUID empresaId, UUID conversaId, String texto, boolean esperaDetalhes,
			Boolean descricaoSuficiente, List<MensagemLlm> historico) {
		String limpo = texto.strip().replaceAll("\\s+", " ");
		boolean trouxeDetalhes = esperaDetalhes
				|| (DetalhesDoChamado.suficientes(limpo) && !Boolean.FALSE.equals(descricaoSuficiente));
		if (!trouxeDetalhes) {
			pendencias.put(conversaId, Pendencia.DETALHES);
			return responder(Situacao.PEDIR_DETALHES, PEDIR_DETALHES, historico);
		}
		String titulo = tituloDe(limpo);
		String descricao = limpo.equals(titulo) ? "" : abreviar(limpo, NovoChamado.DESCRICAO_MAXIMA);
		PedidoDeConfirmacao pedido = solicitacaoChamado.solicitarAbertura(empresaId, conversaId, titulo, descricao);
		return responder(Situacao.PEDIR_CONFIRMACAO, pedido.resumoParaCliente(), historico);
	}

	private DecisaoDoTurno tratarConfirmacao(UUID empresaId, Conversa conversa, List<MensagemLlm> historico, String texto) {
		Resposta resposta = interpretador.interpretar(texto);
		return switch (resposta) {
			case SIM -> switch (confirmacaoChamado.confirmar(empresaId, conversa.getId())) {
				case ResultadoAbertura.Aberto aberto -> responder(Situacao.CHAMADO_ABERTO,
						"Pronto! Abri o chamado " + aberto.chamado().protocolo()
								+ ". Guarde esse protocolo para acompanhar o andamento comigo.",
						historico);
				case ResultadoAbertura.SemPedidoPendente semPedido -> responder(Situacao.PEDIDO_EXPIROU, PEDIDO_EXPIROU, historico);
				case ResultadoAbertura.Indisponivel indisponivel ->
					responder(Situacao.CHAMADO_INDISPONIVEL, CHAMADO_INDISPONIVEL, historico);
			};
			case NAO -> {
				confirmacaoChamado.cancelar(conversa.getId());
				yield responder(Situacao.CHAMADO_CANCELADO, CHAMADO_CANCELADO, historico);
			}
			case INDEFINIDA -> pedirSimOuNaoOuEncaminhar(conversa, historico);
		};
	}

	/** O cliente não respondeu sim nem não: se ele quer uma pessoa, desiste do chamado; senão, repete a pergunta. */
	private DecisaoDoTurno pedirSimOuNaoOuEncaminhar(Conversa conversa, List<MensagemLlm> historico) {
		ResultadoIntencao resultado = classificador.classificar(historico);
		if (resultado.intencao() == Intencao.FALAR_COM_HUMANO && resultado.confianca() >= limiarDeConfianca) {
			confirmacaoChamado.cancelar(conversa.getId());
			return encaminhar("O cliente pediu para falar com um atendente", Situacao.ENCAMINHANDO, ENCAMINHANDO, historico);
		}
		return responder(Situacao.PEDIR_SIM_OU_NAO, PEDIR_SIM_OU_NAO, historico);
	}

	static String tituloDe(String texto) {
		int fim = indiceDoFimDaFrase(texto);
		String frase = (fim > 0 ? texto.substring(0, fim) : texto).strip();
		if (frase.length() > TITULO_USADO_MAXIMO) {
			String corte = frase.substring(0, TITULO_USADO_MAXIMO);
			int espaco = corte.lastIndexOf(' ');
			frase = (espaco > TITULO_USADO_MAXIMO / 2 ? corte.substring(0, espaco) : corte).strip();
		}
		return frase.length() >= NovoChamado.TITULO_MINIMO ? frase : "Chamado pedido pelo WhatsApp";
	}

	private static int indiceDoFimDaFrase(String texto) {
		for (int i = 0; i < texto.length(); i++) {
			char c = texto.charAt(i);
			if (c == '.' || c == '!' || c == '?') {
				return i;
			}
		}
		return -1;
	}

	private static String abreviar(String texto, int limite) {
		return texto.length() <= limite ? texto : texto.substring(0, limite).strip();
	}

	private static String ultimaDoCliente(List<MensagemLlm> historico) {
		for (int i = historico.size() - 1; i >= 0; i--) {
			if (historico.get(i).papel() == Papel.USUARIO) {
				return historico.get(i).conteudo();
			}
		}
		throw new IllegalArgumentException("histórico sem mensagem do cliente");
	}

	private static Optional<String> ultimaDoChatbot(Conversa conversa) {
		List<Mensagem> mensagens = conversa.getMensagens();
		for (int i = mensagens.size() - 1; i >= 0; i--) {
			if (mensagens.get(i).getTipoRemetente() == TipoRemetente.CHATBOT) {
				return Optional.of(mensagens.get(i).getConteudo());
			}
		}
		return Optional.empty();
	}
}
