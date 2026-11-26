package br.com.wxnchatbot.backend.autenticacao;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.UUID;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Uma chamada feita por um atendente logado, nos testes que passam pela segurança de verdade. */
public final class SessaoDeTeste {

	private SessaoDeTeste() {
	}

	/** Como o atendente com este id (o id é o que o backend usa para saber de quem é a conversa). */
	public static RequestPostProcessor comoAtendente(UUID id) {
		return user(new AtendentePrincipal(id, "Atendente de Teste", "atendente@teste.wxn", "hash-nao-usado", true));
	}
}
