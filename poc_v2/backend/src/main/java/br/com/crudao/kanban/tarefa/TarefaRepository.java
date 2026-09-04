package br.com.crudao.kanban.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

  List<Tarefa> findByProjetoId(UUID projetoId);

  /** Tarefa ativa = tarefa existente nao posicionada na etapa final (RN-005). */
  @Query(
      "SELECT COUNT(t) FROM Tarefa t JOIN Etapa e ON e.id = t.etapaId "
          + "WHERE t.projetoId = :projetoId AND e.etapaFinal = false")
  long contarAtivasPorProjeto(@Param("projetoId") UUID projetoId);

  @Query(
      "SELECT COUNT(t) FROM Tarefa t JOIN Etapa e ON e.id = t.etapaId "
          + "WHERE t.workflowId = :workflowId AND e.etapaFinal = false")
  long contarAtivasPorWorkflow(@Param("workflowId") UUID workflowId);

  @Query(
      "SELECT COUNT(t) FROM Tarefa t JOIN Etapa e ON e.id = t.etapaId "
          + "WHERE t.etapaId = :etapaId AND e.etapaFinal = false")
  long contarAtivasPorEtapa(@Param("etapaId") UUID etapaId);

  @Query(
      "SELECT COUNT(t) FROM Tarefa t JOIN Etapa e ON e.id = t.etapaId "
          + "WHERE t.raiaId = :raiaId AND e.etapaFinal = false")
  long contarAtivasPorRaia(@Param("raiaId") UUID raiaId);

  long countByRaiaId(UUID raiaId);
}
