package br.com.wxnchatbot.backend.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "mensagem")
public class Mensagem {

	@Id
	private UUID id = UUID.randomUUID();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversa_id", nullable = false)
	private Conversa conversa;

	@Column(name = "remetente_id", nullable = false)
	private UUID remetenteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_remetente", nullable = false)
	private TipoRemetente tipoRemetente;

	@Transient
	private Remetente remetente;

	@Column(name = "conteudo", nullable = false, columnDefinition = "text")
	private String conteudo;

	@Column(name = "enviada_em", nullable = false)
	private Instant enviadaEm = Instant.now();

	protected Mensagem() {
	}

	Mensagem(Conversa conversa, Remetente remetente, String conteudo) {
		this.conversa = Objects.requireNonNull(conversa, "conversa é obrigatória");
		this.remetente = Objects.requireNonNull(remetente, "remetente é obrigatório");
		this.remetenteId = remetente.getId();
		this.tipoRemetente = remetente.getTipo();
		this.conteudo = Validacao.textoObrigatorio(conteudo, "conteúdo");
	}

	public UUID getId() {
		return id;
	}

	public Remetente getRemetente() {
		if (remetente == null) {
			remetente = conversa.resolverRemetente(tipoRemetente);
		}
		return remetente;
	}

	public TipoRemetente getTipoRemetente() {
		return tipoRemetente;
	}

	public String getConteudo() {
		return conteudo;
	}

	public Instant getEnviadaEm() {
		return enviadaEm;
	}
}
