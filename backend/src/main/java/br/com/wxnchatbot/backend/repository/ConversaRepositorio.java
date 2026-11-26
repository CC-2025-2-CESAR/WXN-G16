package br.com.wxnchatbot.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import jakarta.persistence.LockModeType;

public interface ConversaRepositorio extends JpaRepository<Conversa, UUID> {

	@Query("""
			select c from Conversa c
			join fetch c.cliente
			join fetch c.ticket t
			where c.status = br.com.wxnchatbot.backend.model.StatusConversa.AGUARDANDO_ATENDENTE
			order by
   				case t.urgencia
   					when br.com.wxnchatbot.backend.model.NivelUrgencia.ALTA then 0
   					when br.com.wxnchatbot.backend.model.NivelUrgencia.MEDIA then 1
   					else 2
   				end,
   				t.abertoEm asc,
   				c.id asc
			""")
	List<Conversa> buscarFilaAguardando();

	/** Conversas assumidas pelo atendente, com cliente, ticket e responsável para retomar o painel. */
	@Query("""
			select c from Conversa c
			join fetch c.cliente
			join fetch c.ticket
			join fetch c.atendente
			where c.status = br.com.wxnchatbot.backend.model.StatusConversa.COM_ATENDENTE
			and c.atendente.id = :atendenteId
			order by case when c.assumidaEm is null then 1 else 0 end, c.assumidaEm desc, c.id
			""")
	List<Conversa> buscarAtendimentosAtivos(@Param("atendenteId") UUID atendenteId);

	/**
	 * Histórico paginado, sempre restrito a conversas encerradas. Um filtro vazio é decidido no Java (SpEL) e não com
	 * {@code :x is null}: o PostgreSQL não descobre o tipo de um parâmetro que só aparece nessa comparação e a consulta
	 * inteira falha (SQLState 42P18).
	 */
	@Query(value = """
			select c from Conversa c
			join fetch c.cliente cliente
			left join fetch c.atendente atendente
			left join fetch c.ticket ticket
			where c.status = br.com.wxnchatbot.backend.model.StatusConversa.ENCERRADA
			and (:#{#busca == null} = true or c.id = :idBusca
				or lower(cliente.nome) like :busca escape '!'
				or cliente.telefone like :busca escape '!'
				or lower(ticket.motivo) like :busca escape '!'
				or lower(ticket.resumo) like :busca escape '!'
				or lower(c.resumoEncerramento) like :busca escape '!'
				or exists (select mensagem.id from Mensagem mensagem
					where mensagem.conversa = c and lower(mensagem.conteudo) like :busca escape '!'))
			and (:#{#inicio == null} = true or c.encerradaEm >= :inicio)
			and (:#{#fim == null} = true or c.encerradaEm < :fim)
			and (:#{#atendenteId == null} = true or atendente.id = :atendenteId)
			and (:#{#origem == null} = true or c.origem = :origem)
			order by c.encerradaEm desc, c.id desc
			""", countQuery = """
			select count(c) from Conversa c
			join c.cliente cliente
			left join c.atendente atendente
			left join c.ticket ticket
			where c.status = br.com.wxnchatbot.backend.model.StatusConversa.ENCERRADA
			and (:#{#busca == null} = true or c.id = :idBusca
				or lower(cliente.nome) like :busca escape '!'
				or cliente.telefone like :busca escape '!'
				or lower(ticket.motivo) like :busca escape '!'
				or lower(ticket.resumo) like :busca escape '!'
				or lower(c.resumoEncerramento) like :busca escape '!'
				or exists (select mensagem.id from Mensagem mensagem
					where mensagem.conversa = c and lower(mensagem.conteudo) like :busca escape '!'))
			and (:#{#inicio == null} = true or c.encerradaEm >= :inicio)
			and (:#{#fim == null} = true or c.encerradaEm < :fim)
			and (:#{#atendenteId == null} = true or atendente.id = :atendenteId)
			and (:#{#origem == null} = true or c.origem = :origem)
			""")
	Page<Conversa> buscarHistorico(@Param("busca") String busca, @Param("idBusca") UUID idBusca,
			@Param("inicio") Instant inicio, @Param("fim") Instant fim,
			@Param("atendenteId") UUID atendenteId, @Param("origem") OrigemConversa origem,
			Pageable pageable);

	@Query("select mensagem.conversa.id, count(mensagem) from Mensagem mensagem where mensagem.conversa.id in :ids group by mensagem.conversa.id")
	List<Object[]> contarMensagens(@Param("ids") Collection<UUID> ids);

	/** A conversa mais recente do cliente que ainda não foi encerrada (a que continua quando ele escreve de novo). */
	Optional<Conversa> findFirstByClienteIdAndStatusNotOrderByIniciadaEmDesc(UUID clienteId, StatusConversa status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Conversa c where c.id = :id")
	Optional<Conversa> buscarPorIdParaAtualizar(@Param("id") UUID id);
}
