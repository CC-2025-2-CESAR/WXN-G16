package br.com.wxnchatbot.backend.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.chatbot.DecisaoDoTurno;
import br.com.wxnchatbot.backend.chatbot.DecisaoDoTurno.Encaminhar;
import br.com.wxnchatbot.backend.chatbot.DecisaoDoTurno.Responder;
import br.com.wxnchatbot.backend.chatbot.RoteadorDeIntencao;
import br.com.wxnchatbot.backend.llm.LlmIndisponivelException;
import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Mensagem;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;

@Service
public class OrquestradorConversaServico {

	private static final Logger log = LoggerFactory.getLogger(OrquestradorConversaServico.class);
	private static final String MENSAGEM_LLM_INDISPONIVEL = "Desculpe, não consegui processar sua mensagem agora. Tente de novo em instantes ou peça para falar com um atendente.";
	private static final int LIMITE_HISTORICO = 20;

	private final ConversaRepositorio conversaRepositorio;
	private final RoteadorDeIntencao roteador;
	private final TicketServico ticketServico;

	public OrquestradorConversaServico(ConversaRepositorio conversaRepositorio, RoteadorDeIntencao roteador,
			TicketServico ticketServico) {
		this.conversaRepositorio = conversaRepositorio;
		this.roteador = roteador;
		this.ticketServico = ticketServico;
	}

	/**
	 * Salva a mensagem do cliente e, se a conversa está com a Lily, descobre o que ele quer, responde e salva a
	 * resposta. Quando a Lily encaminha, a conversa entra na fila de atendimento com um ticket.
	 */
	@Transactional
	public ResultadoTurno processarTurno(UUID conversaId, String texto) {
		Conversa conversa = conversaRepositorio.findById(conversaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Conversa", conversaId));
		conversa.adicionarMensagem(conversa.getCliente(), texto);
		String resposta = null;
		if (conversa.getStatus() == StatusConversa.COM_CHATBOT) {
			DecisaoDoTurno decisao = decidir(conversa);
			resposta = textoDe(decisao);
			conversa.adicionarMensagem(conversa.getChatbot(), resposta);
			if (decisao instanceof Encaminhar encaminhar) {
				ticketServico.abrir(conversa.getId(), encaminhar.motivo());
			}
		}
		conversaRepositorio.save(conversa);
		return new ResultadoTurno(resposta, conversa.getStatus());
	}

	/** Pede a decisão ao roteador; se a LLM estiver fora do ar, responde a mensagem padrão em vez de falhar o turno. */
	private DecisaoDoTurno decidir(Conversa conversa) {
		try {
			return roteador.decidir(conversa, montarHistorico(conversa));
		} catch (LlmIndisponivelException e) {
			log.warn("LLM indisponível na conversa {}: {}", conversa.getId(), e.getMessage());
			return new Responder(MENSAGEM_LLM_INDISPONIVEL);
		}
	}

	private static String textoDe(DecisaoDoTurno decisao) {
		return switch (decisao) {
			case Responder responder -> responder.texto();
			case Encaminhar encaminhar -> encaminhar.texto();
		};
	}

	/** Converte as últimas mensagens da conversa para o formato da LLM. */
	private List<MensagemLlm> montarHistorico(Conversa conversa) {
		List<Mensagem> mensagens = conversa.getMensagens();
		int inicio = Math.max(0, mensagens.size() - LIMITE_HISTORICO);
		return mensagens.subList(inicio, mensagens.size()).stream()
				.map(m -> new MensagemLlm(papelDe(m.getTipoRemetente()), m.getConteudo()))
				.toList();
	}

	private MensagemLlm.Papel papelDe(TipoRemetente tipo) {
		return tipo == TipoRemetente.CLIENTE ? MensagemLlm.Papel.USUARIO : MensagemLlm.Papel.ASSISTENTE;
	}
}
