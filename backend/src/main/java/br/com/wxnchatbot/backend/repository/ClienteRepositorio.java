package br.com.wxnchatbot.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.wxnchatbot.backend.model.Cliente;

public interface ClienteRepositorio extends JpaRepository<Cliente, UUID> {

	List<Cliente> findByTelefoneIn(Collection<String> telefones);
}
