package br.com.crudao.kanban.auditoria;

import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.tarefa.Tarefa;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Log append-only de alteracoes de responsavel, titulo, etapa e impedimento (RN-016, RF-017).
 * Nunca atualiza nem remove; o timestamp vem do banco.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

  private final AuditoriaRepository auditoriaRepository;

  /** Registra a alteracao na mesma transacao da escrita. No-op quando o valor nao mudou. */
  @Transactional
  public void registrar(
      Tarefa tarefa, CampoAuditado campo, String anterior, String novo, Usuario autor) {
    if (Objects.equals(anterior, novo)) {
      return;
    }
    auditoriaRepository.save(
        AuditoriaTarefa.builder()
            .tarefaId(tarefa.getId())
            .projetoId(tarefa.getProjetoId())
            .autorId(autor.getId())
            .campo(campo)
            .valorAnterior(anterior)
            .valorNovo(novo)
            .build());
  }

  @Transactional(readOnly = true)
  public Page<AuditoriaTarefa> historico(UUID tarefaId, Pageable pageable) {
    return auditoriaRepository.findByTarefaIdOrderByOcorridoEmDesc(tarefaId, pageable);
  }
}
