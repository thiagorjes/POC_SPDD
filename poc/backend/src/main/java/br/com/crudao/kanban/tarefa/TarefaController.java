package br.com.crudao.kanban.tarefa;

import br.com.crudao.kanban.rbac.Permissoes;
import br.com.crudao.kanban.security.ExigePermissao;
import br.com.crudao.kanban.security.UsuarioAtualProvider;
import br.com.crudao.kanban.tarefa.dto.AtribuirResponsavelRequest;
import br.com.crudao.kanban.tarefa.dto.AtualizarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.AuditoriaResponse;
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.MarcarImpedimentoRequest;
import br.com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.TarefaDetalheResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
@RequiredArgsConstructor
public class TarefaController {

  private final TarefaService tarefaService;
  private final UsuarioAtualProvider usuarioAtualProvider;

  @PostMapping("/projetos/{projetoId}/tarefas")
  @ExigePermissao(
      valor = Permissoes.TAREFA_GERENCIAR, escopoProjeto = "#resolvedor.deProjeto(#projetoId)")
  public ResponseEntity<TarefaDetalheResponse> criar(
      @PathVariable UUID projetoId, @RequestBody @Valid CriarTarefaRequest request) {
    Tarefa tarefa = tarefaService.criar(projetoId, request, usuarioAtualProvider.obrigatorio());
    return ResponseEntity.created(URI.create("/api/tarefas/" + tarefa.getId()))
        .body(detalhe(tarefa.getId()));
  }

  @GetMapping("/tarefas/{id}")
  @ExigePermissao(
      valor = Permissoes.PROJETO_VISUALIZAR,
      escopoProjeto = "#resolvedor.deTarefa(#id)",
      escrita = false)
  public ResponseEntity<TarefaDetalheResponse> buscar(@PathVariable UUID id) {
    return ResponseEntity.ok(detalhe(id));
  }

  @PutMapping("/tarefas/{id}")
  @ExigePermissao(valor = Permissoes.TAREFA_GERENCIAR, escopoProjeto = "#resolvedor.deTarefa(#id)")
  public ResponseEntity<TarefaDetalheResponse> atualizar(
      @PathVariable UUID id, @RequestBody @Valid AtualizarTarefaRequest request) {
    tarefaService.atualizar(id, request, usuarioAtualProvider.obrigatorio());
    return ResponseEntity.ok(detalhe(id));
  }

  @DeleteMapping("/tarefas/{id}")
  @ExigePermissao(valor = Permissoes.TAREFA_GERENCIAR, escopoProjeto = "#resolvedor.deTarefa(#id)")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    tarefaService.excluir(id, usuarioAtualProvider.obrigatorio());
    return ResponseEntity.noContent().build();
  }

  /** Avancar, retroceder e desfinalizar usam este mesmo endpoint (DDR-002). */
  @PatchMapping("/tarefas/{id}/mover")
  @ExigePermissao(valor = Permissoes.TAREFA_MOVER, escopoProjeto = "#resolvedor.deTarefa(#id)")
  public ResponseEntity<TarefaDetalheResponse> mover(
      @PathVariable UUID id, @RequestBody @Valid MoverTarefaRequest request) {
    tarefaService.mover(id, request, usuarioAtualProvider.obrigatorio());
    return ResponseEntity.ok(detalhe(id));
  }

  /**
   * Nao declara {@code @ExigePermissao}: a permissao depende de quem esta sendo atribuido —
   * autoatribuicao exige {@code tarefa:mover}, atribuir terceiros exige {@code tarefa:atribuir}
   * (RN-012). A decisao fica no service, que tem o dado necessario.
   */
  @PatchMapping("/tarefas/{id}/responsavel")
  public ResponseEntity<TarefaDetalheResponse> atribuir(
      @PathVariable UUID id, @RequestBody @Valid AtribuirResponsavelRequest request) {
    tarefaService.atribuir(id, request.responsavelId(), usuarioAtualProvider.obrigatorio());
    return ResponseEntity.ok(detalhe(id));
  }

  @PostMapping("/tarefas/{id}/impedimento")
  @ExigePermissao(valor = Permissoes.TAREFA_IMPEDIR, escopoProjeto = "#resolvedor.deTarefa(#id)")
  public ResponseEntity<TarefaDetalheResponse> marcarImpedimento(
      @PathVariable UUID id, @RequestBody @Valid MarcarImpedimentoRequest request) {
    tarefaService.marcarImpedimento(id, request.motivo(), usuarioAtualProvider.obrigatorio());
    return ResponseEntity.ok(detalhe(id));
  }

  @DeleteMapping("/tarefas/{id}/impedimento")
  @ExigePermissao(valor = Permissoes.TAREFA_IMPEDIR, escopoProjeto = "#resolvedor.deTarefa(#id)")
  public ResponseEntity<TarefaDetalheResponse> desmarcarImpedimento(@PathVariable UUID id) {
    tarefaService.desmarcarImpedimento(id, usuarioAtualProvider.obrigatorio());
    return ResponseEntity.ok(detalhe(id));
  }

  @PostMapping("/tarefas/{id}/observadores/me")
  @ExigePermissao(
      valor = Permissoes.PROJETO_VISUALIZAR,
      escopoProjeto = "#resolvedor.deTarefa(#id)",
      escrita = false)
  public ResponseEntity<Void> observar(@PathVariable UUID id) {
    tarefaService.observar(id, usuarioAtualProvider.obrigatorio());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/tarefas/{id}/observadores/me")
  @ExigePermissao(
      valor = Permissoes.PROJETO_VISUALIZAR,
      escopoProjeto = "#resolvedor.deTarefa(#id)",
      escrita = false)
  public ResponseEntity<Void> desobservar(@PathVariable UUID id) {
    tarefaService.desobservar(id, usuarioAtualProvider.obrigatorio());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/tarefas/{id}/historico")
  @ExigePermissao(
      valor = Permissoes.PROJETO_VISUALIZAR,
      escopoProjeto = "#resolvedor.deTarefa(#id)",
      escrita = false)
  public ResponseEntity<List<AuditoriaResponse>> historico(
      @PathVariable UUID id, Pageable pageable) {
    return ResponseEntity.ok(
        tarefaService.detalhar(id, usuarioAtualProvider.obrigatorio(), pageable).historico());
  }

  private TarefaDetalheResponse detalhe(UUID id) {
    return tarefaService.detalhar(
        id, usuarioAtualProvider.obrigatorio(), Pageable.ofSize(50));
  }
}
