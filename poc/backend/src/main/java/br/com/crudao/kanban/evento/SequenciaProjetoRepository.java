package br.com.crudao.kanban.evento;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SequenciaProjetoRepository extends JpaRepository<SequenciaProjeto, UUID> {

  /**
   * Incremento atomico com retorno, executado dentro da transacao de escrita. Garante {@code seq}
   * monotonico por projeto mesmo com N pods concorrentes (ADR-004).
   */
  @Query(
      value = "UPDATE sequencia_projeto SET ultimo_seq = ultimo_seq + 1 "
          + "WHERE projeto_id = :projetoId RETURNING ultimo_seq",
      nativeQuery = true)
  Long incrementarERetornar(@Param("projetoId") UUID projetoId);

  @Modifying
  @Query(
      value = "INSERT INTO sequencia_projeto (projeto_id, ultimo_seq) VALUES (:projetoId, 0) "
          + "ON CONFLICT (projeto_id) DO NOTHING",
      nativeQuery = true)
  void criarSeAusente(@Param("projetoId") UUID projetoId);
}
