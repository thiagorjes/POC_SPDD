package br.com.crudao.kanban.workflow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransicaoRepository extends JpaRepository<Transicao, UUID> {

  List<Transicao> findByWorkflowId(UUID workflowId);

  List<Transicao> findByEtapaOrigemId(UUID etapaOrigemId);

  List<Transicao> findByEtapaDestinoId(UUID etapaDestinoId);

  boolean existsByEtapaOrigemIdAndEtapaDestinoId(UUID etapaOrigemId, UUID etapaDestinoId);

  boolean existsByWorkflowIdAndEtapaOrigemIdAndEtapaDestinoId(
      UUID workflowId, UUID etapaOrigemId, UUID etapaDestinoId);

  void deleteByEtapaOrigemIdOrEtapaDestinoId(UUID etapaOrigemId, UUID etapaDestinoId);
}
