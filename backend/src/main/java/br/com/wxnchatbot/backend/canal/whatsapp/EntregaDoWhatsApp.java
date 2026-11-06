package br.com.wxnchatbot.backend.canal.whatsapp;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** O corpo que a Meta envia ao webhook; só os campos que a Lily usa. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EntregaDoWhatsApp(List<Entrada> entry) {

	/** Uma mensagem de texto ou de outro tipo, já com o nome que o cliente tem no WhatsApp. */
	public record MensagemRecebida(String id, String de, String nome, String tipo, String texto) {

		public boolean ehTexto() {
			return "text".equals(tipo) && texto != null && !texto.isBlank();
		}
	}

	/** As mensagens da entrega na ordem em que vieram; avisos de status (enviada, lida) não têm mensagens. */
	public List<MensagemRecebida> mensagens() {
		List<MensagemRecebida> recebidas = new ArrayList<>();
		for (Entrada entrada : orVazia(entry)) {
			for (Mudanca mudanca : orVazia(entrada.changes())) {
				Valor valor = mudanca.value();
				if (valor == null) {
					continue;
				}
				for (Mensagem mensagem : orVazia(valor.messages())) {
					recebidas.add(new MensagemRecebida(mensagem.id(), mensagem.from(), nomeDe(valor, mensagem.from()),
							mensagem.type(), mensagem.text() == null ? null : mensagem.text().body()));
				}
			}
		}
		return recebidas;
	}

	private static String nomeDe(Valor valor, String waId) {
		for (Contato contato : orVazia(valor.contacts())) {
			if (contato.waId() != null && contato.waId().equals(waId) && contato.profile() != null) {
				return contato.profile().name();
			}
		}
		return null;
	}

	private static <T> List<T> orVazia(List<T> lista) {
		return lista == null ? List.of() : lista;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Entrada(List<Mudanca> changes) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Mudanca(String field, Valor value) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Valor(List<Contato> contacts, List<Mensagem> messages) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Contato(@JsonProperty("wa_id") String waId, Perfil profile) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Perfil(String name) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Mensagem(String id, String from, String type, Texto text) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Texto(String body) {
	}
}
