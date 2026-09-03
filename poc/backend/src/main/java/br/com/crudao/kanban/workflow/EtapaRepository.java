package br.com.crudao.kanban.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EtapaRepository extends JpaRepository<Etapa, UUID> {

  List<Etapa> findByWorkflowIdOrderByOrdemAsc(UUID workflowId);

  Optional<Etapa> findFirstByWorkflowIdOrderByOrdemAsc(UUID workflowId);

  Optional<Etapa> findByWorkflowIdAndEtapaFinalTrue(UUID workflowId);

  @Query("SELECT COALESCE(MAX(e.ordem), -1) FROM Etapa e WHERE e.workflowId = :workflowId")
  int findMaiorOrdem(@Param("workflowId") UUID workflowId);

  long countByWorkflowId(UUID workflowId);
}
