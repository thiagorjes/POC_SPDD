package br.com.crudao.kanban.auditoria;

import br.com.crudao.kanban.common.ClockProvider;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.tarefa.Tarefa;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Log append-only de alteracoes de tarefa (RN-016, RF-017). Nunca atualiza nem remove registros.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

  private final AuditoriaRepository auditoriaRepository;
  private final ClockProvider clockProvider;

  /**
   * Registra a alteracao na mesma transacao da escrita que a originou (RN-016). Valores iguais nao
   * geram registro.
   */
  @Transactional
  public void registrar(
      Tarefa tarefa, CampoAuditado campo, String anterior, String novo, Usuario autor) {
    if (java.util.Objects.equals(anterior, novo)) {
      return;
    }
    auditoriaRepository.save(
        new AuditoriaTarefa(
            tarefa.getId(),
            tarefa.getProjetoId(),
            autor.getId(),
            campo,
            anterior,
            novo,
            clockProvider.agora()));
  }

  /** Historico da tarefa, mais recente primeiro (RF-017). */
  @Transactional(readOnly = true)
  public Page<AuditoriaTarefa> historico(UUID tarefaId, Pageable pageable) {
    return auditoriaRepository.findByTarefaIdOrderByOcorridoEmDesc(tarefaId, pageable);
  }
}
