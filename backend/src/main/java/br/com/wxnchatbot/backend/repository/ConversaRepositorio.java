package br.com.wxnchatbot.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.wxnchatbot.backend.model.Conversa;
import jakarta.persistence.LockModeType;

public interface ConversaRepositorio extends JpaRepository<Conversa, UUID> {

	@Query("""
			select c from Conversa c
			join fetch c.cliente
			join fetch c.ticket t
			where c.status = br.com.wxnchatbot.backend.model.StatusConversa.AGUARDANDO_ATENDENTE
			order by t.abertoEm asc, c.id asc
			""")
	List<Conversa> buscarFilaAguardando();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Conversa c where c.id = :id")
	Optional<Conversa> buscarPorIdParaAtualizar(@Param("id") UUID id);
}