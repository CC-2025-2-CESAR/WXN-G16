package br.com.wxnchatbot.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.wxnchatbot.backend.model.Chatbot;

public interface ChatbotRepositorio extends JpaRepository<Chatbot, UUID> {

	Optional<Chatbot> findFirstByAtivoTrueOrderByIdAsc();
}
