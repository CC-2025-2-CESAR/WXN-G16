package br.com.wxnchatbot.backend.autenticacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.repository.AtendenteRepositorio;

/** Provisiona uma conta por e-mail a partir de segredos do ambiente, sem redefinir senhas existentes. */
@Component
public class ProvisionamentoInicial {

	private static final Logger log = LoggerFactory.getLogger(ProvisionamentoInicial.class);

	private final AtendenteRepositorio atendentes;
	private final PasswordEncoder encoder;
	private final String nome;
	private final String email;
	private final String senha;

	public ProvisionamentoInicial(AtendenteRepositorio atendentes, PasswordEncoder encoder,
			@Value("${ATENDENTE_PROVISIONAR_NOME:${ATENDENTE_INICIAL_NOME:}}") String nome,
			@Value("${ATENDENTE_PROVISIONAR_EMAIL:${ATENDENTE_INICIAL_EMAIL:}}") String email,
			@Value("${ATENDENTE_PROVISIONAR_SENHA:${ATENDENTE_INICIAL_SENHA:}}") String senha) {
		this.atendentes = atendentes;
		this.encoder = encoder;
		this.nome = nome;
		this.email = email;
		this.senha = senha;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void provisionar() {
		boolean algumValor = !nome.isBlank() || !email.isBlank() || !senha.isBlank();
		if (!algumValor) {
			if (!atendentes.existsBySenhaHashIsNotNull()) {
				log.warn("Nenhum atendente tem senha. Configure ATENDENTE_PROVISIONAR_NOME, EMAIL e SENHA para abrir o painel.");
			}
			return;
		}
		if (nome.isBlank() || email.isBlank() || senha.isBlank()) {
			throw new IllegalStateException("Configure as três variáveis ATENDENTE_PROVISIONAR_NOME, EMAIL e SENHA juntas.");
		}
		if (senha.length() < 12) {
			throw new IllegalStateException("ATENDENTE_PROVISIONAR_SENHA deve ter pelo menos 12 caracteres.");
		}
		Atendente conta = atendentes.findByEmailIgnoreCase(email.trim())
				.orElseGet(() -> new Atendente(nome.trim(), email.trim()));
		if (conta.getSenhaHash() != null) return; // Reiniciar nunca troca a senha da conta.
		conta.definirSenhaHash(encoder.encode(senha));
		atendentes.save(conta);
		log.info("Conta de atendente provisionada para {}.", conta.getEmail());
	}
}
