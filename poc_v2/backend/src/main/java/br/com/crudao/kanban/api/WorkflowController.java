package br.com.crudao.kanban.api;

import br.com.crudao.kanban.workflow.EtapaService;
import br.com.crudao.kanban.workflow.TransicaoService;
import br.com.crudao.kanban.workflow.Workflow;
import br.com.crudao.kanban.workflow.WorkflowMapper;
import br.com.crudao.kanban.workflow.WorkflowService;
import br.com.crudao.kanban.workflow.dto.AtualizarEtapaRequest;
import br.com.crudao.kanban.workflow.dto.CriarEtapaRequest;
import br.com.crudao.kanban.workflow.dto.CriarTransicaoRequest;
import br.com.crudao.kanban.workflow.dto.CriarWorkflowRequest;
import br.com.crudao.kanban.workflow.dto.EtapaResponse;
import br.com.crudao.kanban.workflow.dto.ReordenarEtapasRequest;
import br.com.crudao.kanban.workflow.dto.TransicaoResponse;
import br.com.crudao.kanban.workflow.dto.WorkflowResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
 * Configuracao do workflow: workflows do projeto, etapas e grafo de transicoes (RF-002, RF-009,
 * RF-010). Reordenar colunas nunca altera o grafo (Safeguards, secao 1).
 */
@RestController
@Validated
@RequiredArgsConstructor
public class WorkflowController {

  private final WorkflowService workflowService;
  private final EtapaService etapaService;
  private final TransicaoService transicaoService;
  private final WorkflowMapper workflowMapper;

  @GetMapping("/api/projetos/{projetoId}/workflows")
  public ResponseEntity<List<WorkflowResponse>> listar(@PathVariable UUID projetoId) {
    return ResponseEntity.ok(
        workflowMapper.paraWorkflowResponses(workflowService.listar(projetoId)));
  }

  @PostMapping("/api/projetos/{projetoId}/workflows")
  public ResponseEntity<WorkflowResponse> criar(
      @PathVariable UUID projetoId, @RequestBody @Valid CriarWorkflowRequest request) {
    Workflow workflow = workflowService.criar(projetoId, request.nome());
    return ResponseEntity.created(URI.create("/api/workflows/" + workflow.getId()))
        .body(workflowMapper.paraResponse(workflow));
  }

  @PutMapping("/api/projetos/{projetoId}/workflows/{workflowId}/ativar")
  public ResponseEntity<WorkflowResponse> ativar(
      @PathVariable UUID projetoId, @PathVariable UUID workflowId) {
    return ResponseEntity.ok(workflowMapper.paraResponse(workflowService.ativar(workflowId)));
  }

  @DeleteMapping("/api/projetos/{projetoId}/workflows/{workflowId}")
  public ResponseEntity<Void> excluir(@PathVariable UUID projetoId, @PathVariable UUID workflowId) {
    workflowService.excluir(workflowId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/api/workflows/{workflowId}/etapas")
  public ResponseEntity<List<EtapaResponse>> listarEtapas(@PathVariable UUID workflowId) {
    return ResponseEntity.ok(workflowMapper.paraEtapaResponses(etapaService.listar(workflowId)));
  }

  @PostMapping("/api/workflows/{workflowId}/etapas")
  public ResponseEntity<EtapaResponse> criarEtapa(
      @PathVariable UUID workflowId, @RequestBody @Valid CriarEtapaRequest request) {
    EtapaResponse etapa =
        workflowMapper.paraResponse(
            etapaService.criar(workflowId, request.nome(), request.etapaFinal()));
    return ResponseEntity.created(
            URI.create("/api/workflows/" + workflowId + "/etapas/" + etapa.id()))
        .body(etapa);
  }

  @PutMapping("/api/workflows/{workflowId}/etapas/{etapaId}")
  public ResponseEntity<EtapaResponse> atualizarEtapa(
      @PathVariable UUID workflowId,
      @PathVariable UUID etapaId,
      @RequestBody @Valid AtualizarEtapaRequest request) {
    return ResponseEntity.ok(
        workflowMapper.paraResponse(etapaService.atualizar(etapaId, request.nome())));
  }

  @PatchMapping("/api/workflows/{workflowId}/etapas/ordem")
  public ResponseEntity<List<EtapaResponse>> reordenarEtapas(
      @PathVariable UUID workflowId, @RequestBody @Valid ReordenarEtapasRequest request) {
    etapaService.reordenar(workflowId, request.ordemIds());
    return ResponseEntity.ok(workflowMapper.paraEtapaResponses(etapaService.listar(workflowId)));
  }

  @DeleteMapping("/api/workflows/{workflowId}/etapas/{etapaId}")
  public ResponseEntity<Void> excluirEtapa(
      @PathVariable UUID workflowId, @PathVariable UUID etapaId) {
    etapaService.excluir(etapaId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/api/workflows/{workflowId}/transicoes")
  public ResponseEntity<List<TransicaoResponse>> listarTransicoes(@PathVariable UUID workflowId) {
    return ResponseEntity.ok(
        workflowMapper.paraTransicaoResponses(transicaoService.listar(workflowId)));
  }

  @PostMapping("/api/workflows/{workflowId}/transicoes")
  public ResponseEntity<TransicaoResponse> criarTransicao(
      @PathVariable UUID workflowId, @RequestBody @Valid CriarTransicaoRequest request) {
    TransicaoResponse transicao =
        workflowMapper.paraResponse(
            transicaoService.criar(workflowId, request.etapaOrigemId(), request.etapaDestinoId()));
    return ResponseEntity.created(
            URI.create("/api/workflows/" + workflowId + "/transicoes/" + transicao.id()))
        .body(transicao);
  }

  @DeleteMapping("/api/workflows/{workflowId}/transicoes/{transicaoId}")
  public ResponseEntity<Void> excluirTransicao(
      @PathVariable UUID workflowId, @PathVariable UUID transicaoId) {
    transicaoService.excluir(transicaoId);
    return ResponseEntity.noContent().build();
  }
}
