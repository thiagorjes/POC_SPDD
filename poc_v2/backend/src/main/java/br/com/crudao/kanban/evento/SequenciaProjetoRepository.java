package br.com.crudao.kanban.evento;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SequenciaProjetoRepository extends JpaRepository<SequenciaProjeto, UUID> {

  /**
   * Incremento serializado dentro da transacao de escrita. O lock pessimista garante monotonicidade
   * do {@code seq} mesmo com multiplos pods escrevendo no mesmo projeto.
   */
  @Modifying(clearAutomatically = true)
  @Query(
      value =
          "UPDATE sequencia_projeto SET ultimo_seq = ultimo_seq + 1 WHERE projeto_id = :projetoId",
      nativeQuery = true)
  int incrementar(@Param("projetoId") UUID projetoId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM SequenciaProjeto s WHERE s.projetoId = :projetoId")
  Optional<SequenciaProjeto> buscarParaAtualizar(@Param("projetoId") UUID projetoId);
}
