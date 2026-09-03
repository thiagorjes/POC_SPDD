package br.com.crudao.kanban.tarefa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarefaObservadorRepository
    extends JpaRepository<TarefaObservador, TarefaObservador.Id> {

  List<TarefaObservador> findByIdTarefaId(UUID tarefaId);

  Optional<TarefaObservador> findByIdTarefaIdAndIdUsuarioId(UUID tarefaId, UUID usuarioId);

  @Query("SELECT o.id.usuarioId FROM TarefaObservador o WHERE o.id.tarefaId = :tarefaId")
  List<UUID> findUsuarioIds(@Param("tarefaId") UUID tarefaId);

  void deleteByIdTarefaId(UUID tarefaId);
}
