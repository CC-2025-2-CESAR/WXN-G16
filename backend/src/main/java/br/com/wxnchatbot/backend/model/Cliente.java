package br.com.wxnchatbot.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.regex.Pattern;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "cliente")
public class Cliente extends Pessoa {

	/**
	 * O Telegram não entrega o telefone do cliente, só o id do chat com o bot. Esse id fica na coluna do telefone como
	 * {@code tg:<id>}: é a chave que identifica o cliente no canal, e nunca coincide com um telefone (que só tem dígitos).
	 */
	private static final Pattern IDENTIFICADOR_DO_TELEGRAM = Pattern.compile("tg:\\d{1,20}");

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column(name = "telefone", nullable = false)
	private String telefone;

	protected Cliente() {
	}

	public Cliente(Empresa empresa, String nome, String telefone) {
		super(nome);
		this.empresa = Objects.requireNonNull(empresa, "empresa é obrigatória");
		this.telefone = normalizarTelefone(telefone);
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = normalizarTelefone(telefone);
	}

	@Override
	public TipoRemetente getTipo() {
		return TipoRemetente.CLIENTE;
	}

	/** Verdade para {@code tg:<id>}, como o cliente do Telegram é identificado no lugar do telefone. */
	public static boolean ehIdentificadorDoTelegram(String identificador) {
		return identificador != null && IDENTIFICADOR_DO_TELEGRAM.matcher(identificador.strip()).matches();
	}

	private static String normalizarTelefone(String telefone) {
		String texto = Validacao.textoObrigatorio(telefone, "telefone");
		if (ehIdentificadorDoTelegram(texto)) {
			return texto;
		}
		String digitos = texto.replaceAll("\\D", "");
		if (digitos.length() < 10 || digitos.length() > 15) {
			throw new IllegalArgumentException("telefone deve ter entre 10 e 15 dígitos");
		}
		return digitos;
	}
	
	public Empresa getEmpresa() {
		return empresa;
	}
}
