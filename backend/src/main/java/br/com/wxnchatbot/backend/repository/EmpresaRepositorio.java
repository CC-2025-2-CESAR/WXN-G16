package br.com.wxnchatbot.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.wxnchatbot.backend.model.Empresa;

public interface EmpresaRepositorio extends JpaRepository<Empresa, UUID> {

	Optional<Empresa> findFirstByNomeOrderByIdAsc(String nome);
}
