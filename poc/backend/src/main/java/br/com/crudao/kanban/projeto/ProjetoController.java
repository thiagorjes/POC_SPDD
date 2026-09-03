package br.com.crudao.kanban.projeto;

import br.com.crudao.kanban.projeto.dto.AtualizarProjetoRequest;
import br.com.crudao.kanban.projeto.dto.CriarProjetoRequest;
import br.com.crudao.kanban.projeto.dto.PermissoesEfetivasResponse;
import br.com.crudao.kanban.projeto.dto.ProjetoResponse;
import br.com.crudao.kanban.rbac.Permissoes;
import br.com.crudao.kanban.security.ExigePermissao;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projetos")
@Validated
@RequiredArgsConstructor
public class ProjetoController {

  private final ProjetoService projetoService;
  private final ProjetoMapper projetoMapper;
  private final UsuarioAtualProvider usuarioAtualProvider;

  /** Lista apenas os projetos dos quais o usuario participa (RF-008). */
  @GetMapping
  public ResponseEntity<List<ProjetoResponse>> listar() {
    return ResponseEntity.ok(
        projetoMapper.paraResponse(
            projetoService.listarDoUsuario(usuarioAtualProvider.obrigatorio())));
  }

  /**
   * Criacao nao tem projeto ao qual escopar a permissao: a autorizacao e feita dentro do service,
   * restrita a {@code adminGlobal} (ADR-007). Por isso este handler nao usa {@code @ExigePermissao}.
   */
  @PostMapping
  public ResponseEntity<ProjetoResponse> criar(@RequestBody @Valid CriarProjetoRequest request) {
    Projeto projeto = projetoService.criar(request, usuarioAtualProvider.obrigatorio());
    return ResponseEntity.created(URI.create("/api/projetos/" + projeto.getId()))
        .body(projetoMapper.paraResponse(projeto));
  }

  @GetMapping("/{id}")
  @ExigePermissao(
      valor = Permissoes.PROJETO_VISUALIZAR,
      escopoProjeto = "#resolvedor.deProjeto(#id)",
      escrita = false)
  public ResponseEntity<ProjetoResponse> buscar(@PathVariable UUID id) {
    return ResponseEntity.ok(projetoMapper.paraResponse(projetoService.buscar(id)));
  }

  @PutMapping("/{id}")
  @ExigePermissao(valor = Permissoes.PROJETO_ADMINISTRAR, escopoProjeto = "#resolvedor.deProjeto(#id)")
  public ResponseEntity<ProjetoResponse> atualizar(
      @PathVariable UUID id, @RequestBody @Valid AtualizarProjetoRequest request) {
    return ResponseEntity.ok(projetoMapper.paraResponse(projetoService.atualizar(id, request)));
  }

  /**
   * Finalizar torna o projeto somente leitura para todos (RN-015). Nao usa {@code escrita = true}:
   * a propria transicao de status precisa ocorrer com o projeto ainda ativo, e o service e quem
   * garante a idempotencia.
   */
  @PostMapping("/{id}/finalizar")
  @ExigePermissao(
      valor = Permissoes.PROJETO_ADMINISTRAR,
      escopoProjeto = "#resolvedor.deProjeto(#id)",
      escrita = false)
  public ResponseEntity<ProjetoResponse> finalizar(@PathVariable UUID id) {
    return ResponseEntity.ok(projetoMapper.paraResponse(projetoService.finalizar(id)));
  }

  @PostMapping("/{id}/reabrir")
  @ExigePermissao(
      valor = Permissoes.PROJETO_ADMINISTRAR,
      escopoProjeto = "#resolvedor.deProjeto(#id)",
      escrita = false)
  public ResponseEntity<ProjetoResponse> reabrir(@PathVariable UUID id) {
    return ResponseEntity.ok(projetoMapper.paraResponse(projetoService.reabrir(id)));
  }

  @DeleteMapping("/{id}")
  @ExigePermissao(valor = Permissoes.PROJETO_ADMINISTRAR, escopoProjeto = "#resolvedor.deProjeto(#id)")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    projetoService.excluir(id);
    return ResponseEntity.noContent().build();
  }

  /** Alimenta a UI condicional. Nunca e a autoridade de autorizacao (RNF-003). */
  @GetMapping("/{id}/permissoes")
  public ResponseEntity<PermissoesEfetivasResponse> permissoes(@PathVariable UUID id) {
    return ResponseEntity.ok(projetoService.permissoesEfetivas(id));
  }
}
