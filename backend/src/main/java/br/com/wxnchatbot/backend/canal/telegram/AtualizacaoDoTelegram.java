package br.com.wxnchatbot.backend.canal.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Uma atualização (Update) da Bot API do Telegram; só os campos que a Lily usa, de mensagens de texto. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AtualizacaoDoTelegram(@JsonProperty("update_id") long updateId, MensagemDoTelegram message) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record MensagemDoTelegram(@JsonProperty("message_id") long messageId, UsuarioDoTelegram from,
			ChatDoTelegram chat, String text) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record UsuarioDoTelegram(long id, @JsonProperty("is_bot") boolean bot,
			@JsonProperty("first_name") String primeiroNome, @JsonProperty("last_name") String sobrenome,
			String username) {

		/** O nome como aparece no Telegram, ou {@code null} se o Telegram não mandou nenhum. */
		public String nomeCompleto() {
			String primeiro = primeiroNome == null ? "" : primeiroNome.strip();
			String ultimo = sobrenome == null ? "" : sobrenome.strip();
			String nome = (primeiro + " " + ultimo).strip();
			return nome.isEmpty() ? null : nome;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ChatDoTelegram(long id, String type) {

		/** Só conversa de um a um com o bot: grupos, supergrupos e canais não são atendidos. */
		public boolean privado() {
			return "private".equals(type);
		}
	}
}
