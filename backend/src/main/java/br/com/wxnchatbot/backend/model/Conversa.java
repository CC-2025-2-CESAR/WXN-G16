package br.com.wxnchatbot.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Conversa {

	private final UUID id = UUID.randomUUID();
	private final Cliente cliente;
	private final Chatbot chatbot;
	private final List<Mensagem> mensagens = new ArrayList<>();
	private final Instant iniciadaEm = Instant.now();
	private StatusConversa status = StatusConversa.COM_CHATBOT;
	private Atendente atendente;
	private Ticket ticket;
	private Instant encerradaEm;

	public Conversa(Cliente cliente, Chatbot chatbot) {
		this.cliente = Objects.requireNonNull(cliente, "cliente é obrigatório");
		this.chatbot = Objects.requireNonNull(chatbot, "chatbot é obrigatório");
		if (!chatbot.isAtivo()) {
			throw new IllegalStateException("O chatbot está desativado");
		}
	}

	public Mensagem adicionarMensagem(Remetente remetente, String conteudo) {
		Objects.requireNonNull(remetente, "remetente é obrigatório");
		if (status == StatusConversa.ENCERRADA) {
			throw new IllegalStateException("Conversa encerrada não aceita novas mensagens");
		}
		if (!podeEnviar(remetente)) {
			throw new IllegalStateException(remetente.getNome() + " não pode enviar mensagem nesta conversa agora");
		}
		Mensagem mensagem = new Mensagem(remetente, conteudo);
		mensagens.add(mensagem);
		return mensagem;
	}

	public Ticket solicitarAtendente(String motivo) {
		if (status != StatusConversa.COM_CHATBOT) {
			throw new IllegalStateException("Só é possível solicitar um atendente enquanto a conversa está com o chatbot");
		}
		this.ticket = new Ticket(this, motivo);
		this.status = StatusConversa.AGUARDANDO_ATENDENTE;
		return ticket;
	}

	public void atribuirAtendente(Atendente atendente) {
		Objects.requireNonNull(atendente, "atendente é obrigatório");
		if (status != StatusConversa.AGUARDANDO_ATENDENTE) {
			throw new IllegalStateException("A conversa não está aguardando atendente");
		}
		if (!atendente.isDisponivel()) {
			throw new IllegalStateException("O atendente não está disponível");
		}
		this.atendente = atendente;
		this.status = StatusConversa.COM_ATENDENTE;
		ticket.iniciarAtendimento(atendente);
	}

	public void encerrar() {
		if (status == StatusConversa.ENCERRADA) {
			throw new IllegalStateException("A conversa já está encerrada");
		}
		if (ticket != null) {
			ticket.resolver();
		}
		this.status = StatusConversa.ENCERRADA;
		this.encerradaEm = Instant.now();
	}

	private boolean podeEnviar(Remetente remetente) {
		return switch (remetente.getTipo()) {
			case CLIENTE -> remetente.equals(cliente);
			case CHATBOT -> remetente.equals(chatbot) && status != StatusConversa.COM_ATENDENTE;
			case ATENDENTE -> remetente.equals(atendente) && status == StatusConversa.COM_ATENDENTE;
		};
	}

	public UUID getId() {
		return id;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public Chatbot getChatbot() {
		return chatbot;
	}

	public Optional<Atendente> getAtendente() {
		return Optional.ofNullable(atendente);
	}

	public Optional<Ticket> getTicket() {
		return Optional.ofNullable(ticket);
	}

	public List<Mensagem> getMensagens() {
		return Collections.unmodifiableList(mensagens);
	}

	public StatusConversa getStatus() {
		return status;
	}

	public Instant getIniciadaEm() {
		return iniciadaEm;
	}

	public Optional<Instant> getEncerradaEm() {
		return Optional.ofNullable(encerradaEm);
	}
}
