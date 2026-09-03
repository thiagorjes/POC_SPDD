package br.com.crudao.kanban.rbac;

import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.projeto.ProjetoToggleService;
import br.com.crudao.kanban.projeto.dto.AtualizarTogglesRequest;
import br.com.crudao.kanban.rbac.dto.AssociarPapelRequest;
import br.com.crudao.kanban.rbac.dto.MembroProjetoResponse;
import br.com.crudao.kanban.rbac.dto.PapelResponse;
import br.com.crudao.kanban.security.ExigePermissao;
import br.com.crudao.kanban.security.UsuarioAtualProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administracao de papeis e toggles do projeto (RF-015, RF-016). */
@RestController
@RequestMapping("/api/projetos/{projetoId}")
@Validated
@RequiredArgsConstructor
public class AdminPapelController {

  private final UsuarioProjetoPapelService usuarioProjetoPapelService;
  private final ProjetoToggleService projetoToggleService;
  private final UsuarioAtualProvider usuarioAtualProvider;

  @GetMapping("/usuarios")
  @ExigePermissao(
      valor = Permissoes.USUARIO_ASSOCIAR,
      escopoProjeto = "#resolvedor.deProjeto(#projetoId)",
      escrita = false)
  public ResponseEntity<List<MembroProjetoResponse>> listarMembros(@PathVariable UUID projetoId) {
    return ResponseEntity.ok(usuarioProjetoPapelService.listarMembros(projetoId));
  }

  @GetMapping("/papeis")
  @ExigePermissao(
      valor = Permissoes.USUARIO_ASSOCIAR,
      escopoProjeto = "#resolvedor.deProjeto(#projetoId)",
      escrita = false)
  public ResponseEntity<List<PapelResponse>> listarPapeis(@PathVariable UUID projetoId) {
    return ResponseEntity.ok(usuarioProjetoPapelService.listarPapeis());
  }

  @PostMapping("/usuarios")
  @ExigePermissao(
      valor = Permissoes.USUARIO_ASSOCIAR, escopoProjeto = "#resolvedor.deProjeto(#projetoId)")
  public ResponseEntity<Void> associar(
      @PathVariable UUID projetoId, @RequestBody @Valid AssociarPapelRequest request) {
    usuarioProjetoPapelService.associar(
        projetoId, request.usuarioId(), request.codigoPapel(), usuarioAtualProvider.obrigatorio());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/usuarios/{usuarioId}")
  @ExigePermissao(
      valor = Permissoes.USUARIO_ASSOCIAR, escopoProjeto = "#resolvedor.deProjeto(#projetoId)")
  public ResponseEntity<Void> desassociar(
      @PathVariable UUID projetoId,
      @PathVariable UUID usuarioId,
      @RequestParam(required = false) String codigoPapel) {
    usuarioProjetoPapelService.desassociar(
        projetoId, usuarioId, codigoPapel, usuarioAtualProvider.obrigatorio());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/toggles")
  @ExigePermissao(
      valor = Permissoes.PROJETO_ADMINISTRAR,
      escopoProjeto = "#resolvedor.deProjeto(#projetoId)",
      escrita = false)
  public ResponseEntity<Map<ChaveToggle, Boolean>> obterToggles(@PathVariable UUID projetoId) {
    return ResponseEntity.ok(projetoToggleService.obter(projetoId));
  }

  @PutMapping("/toggles")
  @ExigePermissao(
      valor = Permissoes.PROJETO_ADMINISTRAR, escopoProjeto = "#resolvedor.deProjeto(#projetoId)")
  public ResponseEntity<Map<ChaveToggle, Boolean>> atualizarToggles(
      @PathVariable UUID projetoId, @RequestBody @Valid AtualizarTogglesRequest request) {
    projetoToggleService.atualizar(projetoId, request.valores());
    return ResponseEntity.ok(projetoToggleService.obter(projetoId));
  }
}
