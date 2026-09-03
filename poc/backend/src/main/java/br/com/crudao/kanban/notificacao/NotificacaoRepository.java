package br.com.crudao.kanban.notificacao;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

  Page<Notificacao> findByDestinatarioIdOrderByCriadaEmDesc(UUID destinatarioId, Pageable pageable);

  Page<Notificacao> findByDestinatarioIdAndLidaEmIsNullOrderByCriadaEmDesc(
      UUID destinatarioId, Pageable pageable);

  void deleteByTarefaId(UUID tarefaId);
}
