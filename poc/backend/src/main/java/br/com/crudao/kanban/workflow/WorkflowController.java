package br.com.crudao.kanban.workflow;

import br.com.crudao.kanban.rbac.Permissoes;
import br.com.crudao.kanban.security.ExigePermissao;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuracao de workflow, etapas e transicoes (RF-002, RF-009, RF-010). */
@RestController
@RequestMapping("/api")
@Validated
@RequiredArgsConstructor
public class WorkflowController {

  private final WorkflowService workflowService;
  private final EtapaService etapaService;
  private final TransicaoService transicaoService;
  private final WorkflowMapper workflowMapper;

  @GetMapping("/projetos/{projetoId}/workflows")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR,
      escopoProjeto = "#resolvedor.deProjeto(#projetoId)",
      escrita = false)
  public ResponseEntity<List<WorkflowResponse>> listar(@PathVariable UUID projetoId) {
    return ResponseEntity.ok(workflowMapper.paraWorkflowResponse(workflowService.listar(projetoId)));
  }

  @PostMapping("/projetos/{projetoId}/workflows")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR, escopoProjeto = "#resolvedor.deProjeto(#projetoId)")
  public ResponseEntity<WorkflowResponse> criar(
      @PathVariable UUID projetoId, @RequestBody @Valid CriarWorkflowRequest request) {
    Workflow workflow = workflowService.criar(projetoId, request);
    return ResponseEntity.created(URI.create("/api/workflows/" + workflow.getId()))
        .body(workflowMapper.paraResponse(workflow));
  }

  /** Ativar um workflow bloqueia se o workflow vigente ainda tiver tarefas ativas (RN-005). */
  @PutMapping("/projetos/{projetoId}/workflows/{workflowId}/ativar")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR, escopoProjeto = "#resolvedor.deWorkflow(#workflowId)")
  public ResponseEntity<WorkflowResponse> ativar(
      @PathVariable UUID projetoId, @PathVariable UUID workflowId) {
    return ResponseEntity.ok(workflowMapper.paraResponse(workflowService.ativar(workflowId)));
  }

  @DeleteMapping("/projetos/{projetoId}/workflows/{workflowId}")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR, escopoProjeto = "#resolvedor.deWorkflow(#workflowId)")
  public ResponseEntity<Void> excluirWorkflow(
      @PathVariable UUID projetoId, @PathVariable UUID workflowId) {
    workflowService.excluir(workflowId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/workflows/{workflowId}/etapas")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR,
      escopoProjeto = "#resolvedor.deWorkflow(#workflowId)",
      escrita = false)
  public ResponseEntity<List<EtapaResponse>> listarEtapas(@PathVariable UUID workflowId) {
    return ResponseEntity.ok(workflowMapper.paraEtapaResponse(etapaService.listar(workflowId)));
  }

  @PostMapping("/workflows/{workflowId}/etapas")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR, escopoProjeto = "#resolvedor.deWorkflow(#workflowId)")
  public ResponseEntity<EtapaResponse> criarEtapa(
      @PathVariable UUID workflowId, @RequestBody @Valid CriarEtapaRequest request) {
    Etapa etapa = etapaService.criar(workflowId, request);
    return ResponseEntity.created(URI.create("/api/workflows/" + workflowId + "/etapas/" + etapa.getId()))
        .body(workflowMapper.paraResponse(etapa));
  }

  @PutMapping("/workflows/{workflowId}/etapas/{etapaId}")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR, escopoProjeto = "#resolvedor.deEtapa(#etapaId)")
  public ResponseEntity<EtapaResponse> renomearEtapa(
      @PathVariable UUID workflowId,
      @PathVariable UUID etapaId,
      @RequestBody @Valid CriarEtapaRequest request) {
    return ResponseEntity.ok(
        workflowMapper.paraResponse(etapaService.renomear(etapaId, request.nome())));
  }

  /** Reordenar e apresentacao pura: nao altera o grafo de transicoes (RF-010). */
  @PatchMapping("/workflows/{workflowId}/etapas/ordem")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR, escopoProjeto = "#resolvedor.deWorkflow(#workflowId)")
  public ResponseEntity<List<EtapaResponse>> reordenarEtapas(
      @PathVariable UUID workflowId, @RequestBody @Valid ReordenarEtapasRequest request) {
    etapaService.reordenar(workflowId, request.ordemIds());
    return ResponseEntity.ok(workflowMapper.paraEtapaResponse(etapaService.listar(workflowId)));
  }

  @DeleteMapping("/workflows/{workflowId}/etapas/{etapaId}")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR, escopoProjeto = "#resolvedor.deEtapa(#etapaId)")
  public ResponseEntity<Void> excluirEtapa(
      @PathVariable UUID workflowId, @PathVariable UUID etapaId) {
    etapaService.excluir(etapaId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/workflows/{workflowId}/transicoes")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR,
      escopoProjeto = "#resolvedor.deWorkflow(#workflowId)",
      escrita = false)
  public ResponseEntity<List<TransicaoResponse>> listarTransicoes(@PathVariable UUID workflowId) {
    return ResponseEntity.ok(
        workflowMapper.paraTransicaoResponse(transicaoService.listar(workflowId)));
  }

  @PostMapping("/workflows/{workflowId}/transicoes")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR, escopoProjeto = "#resolvedor.deWorkflow(#workflowId)")
  public ResponseEntity<TransicaoResponse> criarTransicao(
      @PathVariable UUID workflowId, @RequestBody @Valid CriarTransicaoRequest request) {
    Transicao transicao =
        transicaoService.criar(workflowId, request.etapaOrigemId(), request.etapaDestinoId());
    return ResponseEntity.created(
            URI.create("/api/workflows/" + workflowId + "/transicoes/" + transicao.getId()))
        .body(workflowMapper.paraResponse(transicao));
  }

  @DeleteMapping("/workflows/{workflowId}/transicoes/{transicaoId}")
  @ExigePermissao(
      valor = Permissoes.WORKFLOW_GERENCIAR, escopoProjeto = "#resolvedor.deWorkflow(#workflowId)")
  public ResponseEntity<Void> excluirTransicao(
      @PathVariable UUID workflowId, @PathVariable UUID transicaoId) {
    transicaoService.excluir(transicaoId);
    return ResponseEntity.noContent().build();
  }
}
