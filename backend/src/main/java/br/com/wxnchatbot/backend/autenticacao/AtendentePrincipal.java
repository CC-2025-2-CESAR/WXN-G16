package br.com.wxnchatbot.backend.autenticacao;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.com.wxnchatbot.backend.model.Atendente;

public record AtendentePrincipal(UUID id, String nome, String email, String senhaHash, boolean disponivel)
		implements UserDetails, Serializable {

	private static final long serialVersionUID = 1L;

	public static AtendentePrincipal de(Atendente atendente) {
		return new AtendentePrincipal(atendente.getId(), atendente.getNome(), atendente.getEmail(),
				atendente.getSenhaHash(), atendente.isDisponivel());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_ATENDENTE"));
	}

	@Override
	public String getPassword() {
		return senhaHash;
	}

	@Override
	public String getUsername() {
		return email;
	}
}
