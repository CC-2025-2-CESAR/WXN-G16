package br.com.wxnchatbot.backend.repository;

import br.com.wxnchatbot.backend.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmpresaRepositorio extends JpaRepository<Empresa, UUID> {
    Optional<Empresa> findByCnpj(String cnpj);
}
