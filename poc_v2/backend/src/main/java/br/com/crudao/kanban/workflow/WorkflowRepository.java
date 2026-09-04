package br.com.crudao.kanban.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

  Optional<Workflow> findByProjetoIdAndAtivoTrue(UUID projetoId);

  List<Workflow> findByProjetoIdOrderByNomeAsc(UUID projetoId);

  long countByProjetoId(UUID projetoId);
}
