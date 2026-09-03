package br.com.crudao.kanban.notificacao;

import br.com.crudao.kanban.notificacao.dto.NotificacaoResponse;
import br.com.crudao.kanban.security.UsuarioAtualProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Notificacoes internas do usuario autenticado (RF-005). Sem canal externo. */
@RestController
@RequestMapping("/api/notificacoes")
@Validated
@RequiredArgsConstructor
public class NotificacaoController {

  private final NotificacaoService notificacaoService;
  private final UsuarioAtualProvider usuarioAtualProvider;

  @GetMapping
  public ResponseEntity<List<NotificacaoResponse>> listar(
      @RequestParam(defaultValue = "false") boolean apenasNaoLidas, Pageable pageable) {
    return ResponseEntity.ok(
        notificacaoService
            .listar(usuarioAtualProvider.obrigatorio(), apenasNaoLidas, pageable)
            .map(NotificacaoResponse::de)
            .getContent());
  }

  /** O escopo e o proprio destinatario: o service rejeita id de outro usuario. */
  @PatchMapping("/{id}/lida")
  public ResponseEntity<Void> marcarLida(@PathVariable UUID id) {
    notificacaoService.marcarLida(id, usuarioAtualProvider.obrigatorio());
    return ResponseEntity.noContent().build();
  }
}
