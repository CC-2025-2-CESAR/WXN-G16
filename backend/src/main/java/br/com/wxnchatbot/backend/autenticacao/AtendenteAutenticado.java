package br.com.wxnchatbot.backend.autenticacao;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AtendenteAutenticado {

	public UUID id() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao == null || !autenticacao.isAuthenticated()
				|| !(autenticacao.getPrincipal() instanceof AtendentePrincipal principal)) {
			throw new AccessDeniedException("Faça login como atendente para continuar.");
		}
		return principal.id();
	}
}
