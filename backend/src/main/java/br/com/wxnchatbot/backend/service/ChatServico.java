package br.com.wxnchatbot.backend.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.repository.ChatbotRepositorio;
import br.com.wxnchatbot.backend.repository.ClienteRepositorio;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;
import br.com.wxnchatbot.backend.repository.EmpresaRepositorio;

/**
 * Porta de entrada dos canais (simulador de chat, WhatsApp): recebe o texto de um telefone, acha ou abre a conversa
 * dele e entrega ao orquestrador. Mensagens do mesmo telefone são atendidas uma de cada vez.
 */
@Service
public class ChatServico {

	private static final int TRANCAS = 64;
	private static final String BOAS_VINDAS = "Oi, eu sou a Lily, assistente virtual da WXN. Como posso ajudar?";

	private final ClienteRepositorio clientes;
	private final EmpresaRepositorio empresas;
	private final ChatbotRepositorio chatbots;
	private final ConversaRepositorio conversas;
	private final OrquestradorConversaServico orquestrador;
	private final TransactionTemplate transacao;
	private final String empresaPadrao;
	private final UUID empresaPadraoId;
	private final ReentrantLock[] trancas = new ReentrantLock[TRANCAS];

	public ChatServico(ClienteRepositorio clientes, EmpresaRepositorio empresas, ChatbotRepositorio chatbots,
			ConversaRepositorio conversas, OrquestradorConversaServico orquestrador,
			PlatformTransactionManager transacoes, @Value("${chat.empresa-padrao:}") String empresaPadrao,
			@Value("${chat.empresa-padrao-id:}") String empresaPadraoId) {
		this.clientes = clientes;
		this.empresas = empresas;
		this.chatbots = chatbots;
		this.conversas = conversas;
		this.orquestrador = orquestrador;
		this.transacao = new TransactionTemplate(transacoes);
		this.empresaPadrao = empresaPadrao == null ? "" : empresaPadrao.strip();
		this.empresaPadraoId = lerId(empresaPadraoId, this.empresaPadrao);
		for (int i = 0; i < TRANCAS; i++) {
			trancas[i] = new ReentrantLock();
		}
	}

	private static UUID lerId(String texto, String nomeDaEmpresa) {
		if (texto == null || texto.isBlank()) {
			return null;
		}
		if (nomeDaEmpresa.isEmpty()) {
			throw new IllegalStateException("chat.empresa-padrao-id exige chat.empresa-padrao");
		}
		try {
			return UUID.fromString(texto.strip());
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("chat.empresa-padrao-id não é um UUID válido: " + texto, e);
		}
	}

	/**
	 * Recebe uma mensagem de texto enviada pelo telefone. O cliente que ainda não existe é cadastrado na empresa
	 * padrão (chat.empresa-padrao); sem empresa padrão, só atende telefones já cadastrados. Quando
	 * chat.empresa-padrao-id está definido, a empresa padrão é a que tem esse id, e não a que tem esse nome. A origem
	 * (canal por onde a mensagem chegou) só marca a conversa quando ela é aberta agora: uma conversa que já estava aberta
	 * para o telefone continua com a origem que tinha.
	 */
	public RespostaChat receber(OrigemConversa origem, String telefone, String nome, String texto) {
		Objects.requireNonNull(texto, "texto é obrigatório");
		String nacional = TelefoneBr.nacional(telefone);
		ReentrantLock tranca = trancaDe(nacional);
		tranca.lock();
		try {
			UUID conversaId = transacao.execute(status -> localizarOuAbrirConversa(origem, telefone, nome));
			ResultadoTurno resultado = orquestrador.processarTurno(conversaId, texto);
			return new RespostaChat(conversaId, resultado.resposta(), resultado.status());
		} finally {
			tranca.unlock();
		}
	}

	private UUID localizarOuAbrirConversa(OrigemConversa origem, String telefone, String nome) {
		Cliente cliente = localizarOuCadastrarCliente(telefone, nome);
		return conversas.findFirstByClienteIdAndStatusNotOrderByIniciadaEmDesc(cliente.getId(), StatusConversa.ENCERRADA)
				.map(Conversa::getId)
				.orElseGet(() -> conversas.save(new Conversa(cliente, chatbotAtivo(), origem)).getId());
	}

	private Cliente localizarOuCadastrarCliente(String telefone, String nome) {
		List<Cliente> achados = clientes.findByTelefoneIn(TelefoneBr.variantes(telefone));
		if (achados.size() == 1) {
			return achados.get(0);
		}
		if (achados.size() > 1) {
			return achados.stream().filter(c -> ehDaEmpresaPadrao(c.getEmpresa())).findFirst()
					.orElseThrow(() -> new IllegalStateException("O telefone está cadastrado em mais de uma empresa"));
		}
		if (empresaPadrao.isEmpty()) {
			throw new RecursoNaoEncontradoException("Não há cadastro para este telefone");
		}
		return clientes.save(new Cliente(empresaPadrao(), nomeOuPadrao(nome, telefone), TelefoneBr.nacional(telefone)));
	}

	private boolean ehDaEmpresaPadrao(Empresa empresa) {
		return empresaPadraoId != null ? empresa.getId().equals(empresaPadraoId) : empresa.getNome().equals(empresaPadrao);
	}

	private Empresa empresaPadrao() {
		if (empresaPadraoId != null) {
			return empresas.findById(empresaPadraoId)
					.orElseGet(() -> empresas.save(new Empresa(empresaPadraoId, empresaPadrao)));
		}
		return empresas.findFirstByNomeOrderByIdAsc(empresaPadrao).orElseGet(() -> empresas.save(new Empresa(empresaPadrao)));
	}

	private Chatbot chatbotAtivo() {
		return chatbots.findFirstByAtivoTrueOrderByIdAsc().orElseGet(() -> chatbots.save(new Chatbot("Lily", BOAS_VINDAS)));
	}

	private static String nomeOuPadrao(String nome, String telefone) {
		if (nome != null && !nome.isBlank()) {
			return nome.strip();
		}
		String nacional = TelefoneBr.nacional(telefone);
		return "Cliente " + nacional.substring(Math.max(0, nacional.length() - 4));
	}

	private ReentrantLock trancaDe(String nacional) {
		return trancas[Math.floorMod(nacional.hashCode(), TRANCAS)];
	}
}
