package br.com.wxnchatbot.backend.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket")
public class Ticket {

	@Id
	private UUID id = UUID.randomUUID();

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversa_id", nullable = false, unique = true)
	private Conversa conversa;

	@Column(name = "motivo", nullable = false)
	private String motivo;

	@Column(name = "aberto_em", nullable = false)
	private Instant abertoEm = Instant.now();

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private StatusTicket status = StatusTicket.ABERTO;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atendente_id")
	private Atendente atendente;

	@Column(name = "resolvido_em")
	private Instant resolvidoEm;

	protected Ticket() {
	}

	Ticket(Conversa conversa, String motivo) {
		this.conversa = Objects.requireNonNull(conversa, "conversa é obrigatória");
		this.motivo = Validacao.textoObrigatorio(motivo, "motivo");
	}

	void iniciarAtendimento(Atendente atendente) {
		if (status != StatusTicket.ABERTO) {
			throw new IllegalStateException("Só é possível iniciar o atendimento de um ticket aberto");
		}
		this.atendente = Objects.requireNonNull(atendente, "atendente é obrigatório");
		this.status = StatusTicket.EM_ATENDIMENTO;
	}

	void resolver() {
		if (status == StatusTicket.RESOLVIDO) {
			throw new IllegalStateException("O ticket já foi resolvido");
		}
		this.status = StatusTicket.RESOLVIDO;
		this.resolvidoEm = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public Conversa getConversa() {
		return conversa;
	}

public Cliente getCliente() {
		return conversa.getCliente()
				.orElseThrow(() -> new IllegalStateException("Conversa sem cliente vinculado ao ticket"));
	}

	public Optional<Atendente> getAtendente() {
		return Optional.ofNullable(atendente);
	}

	public String getMotivo() {
		return motivo;
	}

	public StatusTicket getStatus() {
		return status;
	}

	public Instant getAbertoEm() {
		return abertoEm;
	}

	public Optional<Instant> getResolvidoEm() {
		return Optional.ofNullable(resolvidoEm);
	}
}
