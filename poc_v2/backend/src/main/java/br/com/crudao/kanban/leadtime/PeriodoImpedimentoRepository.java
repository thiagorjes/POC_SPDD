package br.com.crudao.kanban.leadtime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodoImpedimentoRepository extends JpaRepository<PeriodoImpedimento, UUID> {

  Optional<PeriodoImpedimento> findByTarefaIdAndEncerradoEmIsNull(UUID tarefaId);

  List<PeriodoImpedimento> findByTarefaId(UUID tarefaId);
}
