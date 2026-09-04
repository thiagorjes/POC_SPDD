package br.com.crudao.kanban.leadtime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodoEtapaRepository extends JpaRepository<PeriodoEtapa, UUID> {

  Optional<PeriodoEtapa> findByTarefaIdAndEncerradoEmIsNull(UUID tarefaId);

  List<PeriodoEtapa> findByTarefaId(UUID tarefaId);
}
