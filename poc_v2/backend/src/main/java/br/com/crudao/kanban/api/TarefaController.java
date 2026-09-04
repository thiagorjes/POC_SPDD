package br.com.crudao.kanban.api;

import br.com.crudao.kanban.security.UsuarioAtualProvider;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaMapper;
import br.com.crudao.kanban.tarefa.TarefaService;
import br.com.crudao.kanban.tarefa.dto.AtribuirResponsavelRequest;
import br.com.crudao.kanban.tarefa.dto.AtualizarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.AuditoriaResponse;
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.MarcarImpedimentoRequest;
import br.com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.TarefaDetalheResponse;
import br.com.crudao.kanban.tarefa.dto.TarefaResumoResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operacoes sobre o card (RF-002/003/004/005/006/012/017/018/019). Avancar, retroceder e
 * desfinalizar usam o mesmo {@code PATCH /mover} (DDR-002).
 */
@RestController
@Validated
@RequiredArgsConstructor
public class TarefaController {

  private static final int TAMANHO_PAGINA_HISTORICO_DETALHE = 20;

  private final TarefaService tarefaService;
  private final TarefaMapper tarefaMapper;
  private final UsuarioAtualProvider usuarioAtualProvider;

  @PostMapping("/api/projetos/{projetoId}/tarefas")
  public ResponseEntity<TarefaResumoResponse> criar(
      @PathVariable UUID projetoId, @RequestBody @Valid CriarTarefaRequest request) {
    Tarefa tarefa = tarefaService.criar(projetoId, request, usuarioAtualProvider.atual());
    return ResponseEntity.created(URI.create("/api/tarefas/" + tarefa.getId()))
        .body(tarefaMapper.paraResumo(tarefa, List.of()));
  }

  @GetMapping("/api/tarefas/{id}")
  public ResponseEntity<TarefaDetalheResponse> detalhe(@PathVariable UUID id) {
    return ResponseEntity.ok(
        tarefaService.detalhe(id, PageRequest.of(0, TAMANHO_PAGINA_HISTORICO_DETALHE)));
  }

  @PutMapping("/api/tarefas/{id}")
  public ResponseEntity<TarefaResumoResponse> atualizar(
      @PathVariable UUID id, @RequestBody @Valid AtualizarTarefaRequest request) {
    Tarefa tarefa = tarefaService.atualizar(id, request, usuarioAtualProvider.atual());
    return ResponseEntity.ok(tarefaMapper.paraResumo(tarefa, List.of()));
  }

  @DeleteMapping("/api/tarefas/{id}")
  public ResponseEntity<Void> excluir(@PathVariable UUID id) {
    tarefaService.excluir(id, usuarioAtualProvider.atual());
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/api/tarefas/{id}/mover")
  public ResponseEntity<TarefaResumoResponse> mover(
      @PathVariable UUID id, @RequestBody @Valid MoverTarefaRequest request) {
    Tarefa tarefa = tarefaService.mover(id, request, usuarioAtualProvider.atual());
    return ResponseEntity.ok(tarefaMapper.paraResumo(tarefa, List.of()));
  }

  @PatchMapping("/api/tarefas/{id}/responsavel")
  public ResponseEntity<TarefaResumoResponse> atribuir(
      @PathVariable UUID id, @RequestBody @Valid AtribuirResponsavelRequest request) {
    Tarefa tarefa =
        tarefaService.atribuir(id, request.responsavelId(), usuarioAtualProvider.atual());
    return ResponseEntity.ok(tarefaMapper.paraResumo(tarefa, List.of()));
  }

  @PostMapping("/api/tarefas/{id}/impedimento")
  public ResponseEntity<TarefaResumoResponse> marcarImpedimento(
      @PathVariable UUID id, @RequestBody @Valid MarcarImpedimentoRequest request) {
    Tarefa tarefa =
        tarefaService.marcarImpedimento(id, request.motivo(), usuarioAtualProvider.atual());
    return ResponseEntity.ok(tarefaMapper.paraResumo(tarefa, List.of()));
  }

  @DeleteMapping("/api/tarefas/{id}/impedimento")
  public ResponseEntity<TarefaResumoResponse> desmarcarImpedimento(@PathVariable UUID id) {
    Tarefa tarefa = tarefaService.desmarcarImpedimento(id, usuarioAtualProvider.atual());
    return ResponseEntity.ok(tarefaMapper.paraResumo(tarefa, List.of()));
  }

  @PostMapping("/api/tarefas/{id}/observadores/me")
  public ResponseEntity<Void> observar(@PathVariable UUID id) {
    tarefaService.observar(id, usuarioAtualProvider.atual());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/api/tarefas/{id}/observadores/me")
  public ResponseEntity<Void> desobservar(@PathVariable UUID id) {
    tarefaService.desobservar(id, usuarioAtualProvider.atual());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/api/tarefas/{id}/historico")
  public ResponseEntity<Page<AuditoriaResponse>> historico(
      @PathVariable UUID id, @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(tarefaService.historico(id, pageable));
  }
}
