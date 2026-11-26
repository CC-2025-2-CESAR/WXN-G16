package br.com.wxnchatbot.backend.autenticacao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.wxnchatbot.backend.repository.AtendenteRepositorio;
import br.com.wxnchatbot.backend.service.AtendenteResumo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

	private final AuthenticationManager autenticador;
	private final SecurityContextRepository contextos;
	private final AtendenteAutenticado atual;
	private final AtendenteRepositorio atendentes;

	public AutenticacaoController(AuthenticationManager autenticador, SecurityContextRepository contextos,
			AtendenteAutenticado atual, AtendenteRepositorio atendentes) {
		this.autenticador = autenticador;
		this.contextos = contextos;
		this.atual = atual;
		this.atendentes = atendentes;
	}

	@GetMapping("/csrf")
	public TokenCsrf csrf(CsrfToken token) {
		return new TokenCsrf(token.getToken());
	}

	@PostMapping("/login")
	public AtendenteResumo login(@Valid @RequestBody Credenciais credenciais, HttpServletRequest requisicao,
			HttpServletResponse resposta) {
		Authentication autenticacao = autenticador.authenticate(UsernamePasswordAuthenticationToken
				.unauthenticated(credenciais.email().trim(), credenciais.senha()));
		// O token CSRF já abriu a sessão. Trocar seu identificador evita fixation.
		requisicao.getSession(true);
		requisicao.changeSessionId();
		SecurityContext contexto = SecurityContextHolder.createEmptyContext();
		contexto.setAuthentication(autenticacao);
		SecurityContextHolder.setContext(contexto);
		contextos.saveContext(contexto, requisicao, resposta);
		return resumo(((AtendentePrincipal) autenticacao.getPrincipal()).id());
	}

	@GetMapping("/me")
	public AtendenteResumo me() {
		return resumo(atual.id());
	}

	private AtendenteResumo resumo(java.util.UUID id) {
		return atendentes.findById(id).map(AtendenteResumo::de)
				.orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Conta não encontrada"));
	}

	@ExceptionHandler(AuthenticationException.class)
	public ProblemDetail credenciaisInvalidas(AuthenticationException erro) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
	}

	public record Credenciais(@NotBlank @Email String email, @NotBlank String senha) {
	}

	public record TokenCsrf(String token) {
	}
}
