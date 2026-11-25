package br.com.wxnchatbot.backend.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.wxnchatbot.backend.model.Atendente;

public interface AtendenteRepositorio extends JpaRepository<Atendente, UUID> {
    boolean existsByDisponivelTrue();

	Optional<Atendente> findByEmailIgnoreCase(String email);

	boolean existsBySenhaHashIsNotNull();
}
