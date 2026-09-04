package br.com.crudao.kanban.api;

import br.com.crudao.kanban.rbac.UsuarioProjetoPapelService;
import br.com.crudao.kanban.rbac.dto.AssociarPapelRequest;
import br.com.crudao.kanban.rbac.dto.MembroProjetoResponse;
import br.com.crudao.kanban.security.UsuarioAtualProvider;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Associacao usuario x papel dentro do projeto (RF-015). Papeis sao acumulaveis (BDR-001). */
@RestController
@RequestMapping("/api/projetos/{projetoId}/usuarios")
@Validated
@RequiredArgsConstructor
public class AdminPapelController {

  private final UsuarioProjetoPapelService usuarioProjetoPapelService;
  private final UsuarioAtualProvider usuarioAtualProvider;

  @GetMapping
  public ResponseEntity<List<MembroProjetoResponse>> listar(@PathVariable UUID projetoId) {
    return ResponseEntity.ok(usuarioProjetoPapelService.listarMembros(projetoId));
  }

  @PostMapping
  public ResponseEntity<Void> associar(
      @PathVariable UUID projetoId, @RequestBody @Valid AssociarPapelRequest request) {
    usuarioProjetoPapelService.associar(
        projetoId, request.usuarioId(), request.codigoPapel(), usuarioAtualProvider.atual());
    return ResponseEntity.created(
            URI.create("/api/projetos/" + projetoId + "/usuarios/" + request.usuarioId()))
        .build();
  }

  @DeleteMapping("/{usuarioId}")
  public ResponseEntity<Void> desassociar(
      @PathVariable UUID projetoId,
      @PathVariable UUID usuarioId,
      @RequestParam String codigoPapel) {
    usuarioProjetoPapelService.desassociar(projetoId, usuarioId, codigoPapel);
    return ResponseEntity.noContent().build();
  }
}
