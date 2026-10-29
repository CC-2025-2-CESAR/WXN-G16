package br.com.wxnchatbot.backend.repository;

import br.com.wxnchatbot.backend.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    Optional<Cliente> findByTelefone(String telefone);
}
