package br.com.crudao.kanban.auditoria;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<AuditoriaTarefa, UUID> {

  Page<AuditoriaTarefa> findByTarefaIdOrderByOcorridoEmDesc(UUID tarefaId, Pageable pageable);
}
