package br.com.wxnchatbot.backend.autenticacao;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.repository.AtendenteRepositorio;

@ExtendWith(MockitoExtension.class)
class ProvisionamentoInicialTest {
	@Mock AtendenteRepositorio atendentes;

	@Test
	void provisionaOutroFuncionarioMesmoQueJaHajaContasComSenha() {
		String senha = "senha-de-teste-segura";
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		when(atendentes.findByEmailIgnoreCase("paulo@wxn.example")).thenReturn(Optional.empty());
		new ProvisionamentoInicial(atendentes, encoder, "Paulo Mendes", "paulo@wxn.example", senha).provisionar();

		ArgumentCaptor<Atendente> captor = ArgumentCaptor.forClass(Atendente.class);
		verify(atendentes).save(captor.capture());
		assertTrue(encoder.matches(senha, captor.getValue().getSenhaHash()));
	}

	@Test
	void naoTrocaSenhaDeContaExistente() {
		Atendente conta = new Atendente("Ana Martins", "ana@wxn.example");
		conta.definirSenhaHash("hash-ja-existente");
		when(atendentes.findByEmailIgnoreCase("ana@wxn.example")).thenReturn(Optional.of(conta));
		new ProvisionamentoInicial(atendentes, new BCryptPasswordEncoder(), "Ana Martins", "ana@wxn.example",
				"outra-senha-segura").provisionar();
		verify(atendentes, never()).save(any());
	}
}
