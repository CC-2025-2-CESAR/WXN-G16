package br.com.wxnchatbot.backend.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Ticket {

	private final UUID id = UUID.randomUUID();
	private final Conversa conversa;
	private final String motivo;
	private final Instant abertoEm = Instant.now();
	private StatusTicket status = StatusTicket.ABERTO;
	private Atendente atendente;
	private Instant resolvidoEm;

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
		return conversa.getCliente();
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
