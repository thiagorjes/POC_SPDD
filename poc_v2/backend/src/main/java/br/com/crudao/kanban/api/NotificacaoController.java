package br.com.crudao.kanban.api;

import br.com.crudao.kanban.notificacao.NotificacaoMapper;
import br.com.crudao.kanban.notificacao.NotificacaoService;
import br.com.crudao.kanban.notificacao.dto.NotificacaoResponse;
import br.com.crudao.kanban.security.UsuarioAtualProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Notificacoes internas do usuario autenticado (RF-005). */
@RestController
@RequestMapping("/api/notificacoes")
@Validated
@RequiredArgsConstructor
public class NotificacaoController {

  private final NotificacaoService notificacaoService;
  private final NotificacaoMapper notificacaoMapper;
  private final UsuarioAtualProvider usuarioAtualProvider;

  @GetMapping
  public ResponseEntity<Page<NotificacaoResponse>> listar(
      @RequestParam(defaultValue = "false") boolean apenasNaoLidas,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        notificacaoService
            .listar(usuarioAtualProvider.atual(), apenasNaoLidas, pageable)
            .map(notificacaoMapper::paraResponse));
  }

  @PatchMapping("/{id}/lida")
  public ResponseEntity<Void> marcarLida(@PathVariable UUID id) {
    notificacaoService.marcarLida(id, usuarioAtualProvider.atual());
    return ResponseEntity.noContent().build();
  }
}
