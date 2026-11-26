package br.com.wxnchatbot.backend.autenticacao;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.repository.AtendenteRepositorio;

@WebMvcTest(AutenticacaoController.class)
@Import({ AutenticacaoConfig.class, AtendenteAutenticado.class })
class AutenticacaoHttpTest {

	private static final String EMAIL = "ana@wxn.example";
	private static final String SENHA = "senha-de-teste-segura";

	@Autowired MockMvc mvc;
	@Autowired PasswordEncoder encoder;
	@MockitoBean AtendenteRepositorio atendentes;

	private Atendente conta() {
		Atendente conta = new Atendente("Ana Martins", EMAIL);
		conta.definirSenhaHash(encoder.encode(SENHA));
		when(atendentes.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(conta));
		when(atendentes.findById(conta.getId())).thenReturn(Optional.of(conta));
		return conta;
	}

	private MvcResult token() throws Exception {
		return mvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andReturn();
	}

	private String valor(MvcResult resultado) throws Exception {
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	@Test
	void acessoExigeLoginECsrf() throws Exception {
		mvc.perform(get("/api/fila")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + EMAIL + "\",\"senha\":\"" + SENHA + "\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void loginPersisteSessaoELogoutRevogaAcesso() throws Exception {
		Atendente conta = conta();
		MvcResult csrf = token();
		MockHttpSession sessao = (MockHttpSession) csrf.getRequest().getSession(false);
		String valor = valor(csrf);

		mvc.perform(post("/api/auth/login").session(sessao).header("X-XSRF-TOKEN", valor)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"" + EMAIL + "\",\"senha\":\"" + SENHA + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(conta.getId().toString()))
				.andExpect(jsonPath("$.email").value(EMAIL));

		mvc.perform(get("/api/auth/me").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana Martins"));

		mvc.perform(post("/api/auth/logout").session(sessao).header("X-XSRF-TOKEN", valor))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isUnauthorized());
	}

	@Test
	void senhaIncorretaNaoAbreSessaoAutenticada() throws Exception {
		conta();
		MvcResult csrf = token();
		MockHttpSession sessao = (MockHttpSession) csrf.getRequest().getSession(false);
		mvc.perform(post("/api/auth/login").session(sessao).header("X-XSRF-TOKEN", valor(csrf))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"" + EMAIL + "\",\"senha\":\"errada\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isUnauthorized());
	}
}
