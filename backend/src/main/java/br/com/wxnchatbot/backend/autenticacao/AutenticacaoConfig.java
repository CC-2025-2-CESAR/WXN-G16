package br.com.wxnchatbot.backend.autenticacao;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import br.com.wxnchatbot.backend.repository.AtendenteRepositorio;

@Configuration
@EnableWebSecurity
public class AutenticacaoConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService atendenteDetails(AtendenteRepositorio atendentes) {
		return email -> atendentes.findByEmailIgnoreCase(email.trim())
				.filter(atendente -> atendente.getSenhaHash() != null)
				.map(AtendentePrincipal::de)
				.orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
	}

	@Bean
	DaoAuthenticationProvider atendenteProvider(UserDetailsService atendenteDetails, PasswordEncoder encoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(atendenteDetails);
		provider.setPasswordEncoder(encoder);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(DaoAuthenticationProvider atendenteProvider) {
		return new ProviderManager(atendenteProvider);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
		repository.setHeaderName("X-XSRF-TOKEN");
		return repository;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider atendenteProvider,
			SecurityContextRepository contextos, CsrfTokenRepository csrfTokens) throws Exception {
		http
				.authenticationProvider(atendenteProvider)
				.cors(withDefaults())
				.csrf(csrf -> csrf.csrfTokenRepository(csrfTokens)
						.ignoringRequestMatchers("/webhook/**", "/mock/wxn/**"))
				.securityContext(contexto -> contexto.securityContextRepository(contextos))
				.authorizeHttpRequests(acesso -> acesso
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/health",
								"/actuator/health/**", "/webhook/**", "/mock/wxn/**").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(erros -> erros.authenticationEntryPoint((req, res, ex) -> {
					res.setStatus(HttpStatus.UNAUTHORIZED.value());
					res.setContentType("application/problem+json;charset=UTF-8");
					res.getWriter().write("{\"detail\":\"Faça login como atendente para continuar.\"}");
				}))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(logout -> logout.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));
		return http.build();
	}
}
