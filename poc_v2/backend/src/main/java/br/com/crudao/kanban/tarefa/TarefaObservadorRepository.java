package br.com.crudao.kanban.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaObservadorRepository
    extends JpaRepository<TarefaObservador, TarefaObservadorId> {

  List<TarefaObservador> findByIdTarefaId(UUID tarefaId);

  boolean existsByIdTarefaIdAndIdUsuarioId(UUID tarefaId, UUID usuarioId);

  void deleteByIdTarefaId(UUID tarefaId);
}
