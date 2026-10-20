package br.com.wxnchatbot.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversa")
public class Conversa {

	@Id
	private UUID id = UUID.randomUUID();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cliente_id", nullable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chatbot_id", nullable = false)
	private Chatbot chatbot;

	@OneToMany(mappedBy = "conversa", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("enviadaEm ASC")
	private List<Mensagem> mensagens = new ArrayList<>();

	@Column(name = "iniciada_em", nullable = false)
	private Instant iniciadaEm = Instant.now();

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private StatusConversa status = StatusConversa.COM_CHATBOT;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atendente_id")
	private Atendente atendente;

	@OneToOne(mappedBy = "conversa", cascade = CascadeType.ALL)
	private Ticket ticket;

	@Column(name = "encerrada_em")
	private Instant encerradaEm;

	protected Conversa() {
	}

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
		Mensagem mensagem = new Mensagem(this, remetente, conteudo);
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

	Remetente resolverRemetente(TipoRemetente tipo) {
		return switch (tipo) {
			case CLIENTE -> cliente;
			case CHATBOT -> chatbot;
			case ATENDENTE -> atendente;
		};
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
