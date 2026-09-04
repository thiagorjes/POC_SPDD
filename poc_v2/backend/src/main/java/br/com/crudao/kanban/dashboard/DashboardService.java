package br.com.crudao.kanban.dashboard;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.ClockProvider;
import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.dashboard.DashboardRepository.AgregadoEtapa;
import br.com.crudao.kanban.dashboard.dto.DashboardResponse;
import br.com.crudao.kanban.dashboard.dto.LeadTimeMedioEtapaResponse;
import br.com.crudao.kanban.rbac.CodigoPermissao;
import br.com.crudao.kanban.security.PermissaoGuard;
import br.com.crudao.kanban.workflow.WorkflowRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicadores de lead time do projeto (RF-007). Leitura permitida mesmo com o projeto finalizado
 * (UC-002): RN-015 restringe escrita, nao consulta.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

  private static final Duration JANELA_PADRAO = Duration.ofDays(30);
  private static final Duration JANELA_MAXIMA = Duration.ofDays(365);

  private final DashboardRepository dashboardRepository;
  private final WorkflowRepository workflowRepository;
  private final PermissaoGuard permissaoGuard;
  private final ClockProvider clockProvider;

  /**
   * Agrega permanencia media e impedimento medio por etapa do workflow ativo no periodo. Sem
   * periodo informado usa os ultimos 30 dias; a janela nao pode exceder 365 dias.
   */
  @Transactional(readOnly = true)
  public DashboardResponse obter(UUID projetoId, Instant inicio, Instant fim) {
    permissaoGuard.exigir(CodigoPermissao.DASHBOARD_VISUALIZAR, projetoId);

    Instant agora = clockProvider.agora();
    Instant fimEfetivo = fim == null ? agora : fim;
    Instant inicioEfetivo = inicio == null ? fimEfetivo.minus(JANELA_PADRAO) : inicio;
    validarJanela(inicioEfetivo, fimEfetivo);

    UUID workflowId =
        workflowRepository
            .findByProjetoIdAndAtivoTrue(projetoId)
            .orElseThrow(
                () ->
                    new ConfiguracaoWorkflowInvalidaException(
                        "O projeto nao possui um workflow ativo configurado."))
            .getId();

    List<AgregadoEtapa> agregados =
        dashboardRepository.agregarPorEtapa(projetoId, workflowId, inicioEfetivo, fimEfetivo);
    List<LeadTimeMedioEtapaResponse> etapas =
        agregados.stream()
            .map(
                agregado ->
                    new LeadTimeMedioEtapaResponse(
                        agregado.getEtapaId(),
                        agregado.getNome(),
                        agregado.getOrdem(),
                        agregado.getMediaSegundos(),
                        agregado.getAmostras(),
                        agregado.getImpedimentoMedioSegundos()))
            .toList();

    long etapasComImpedimento =
        etapas.stream().filter(etapa -> etapa.impedimentoMedioSegundos() > 0).count();
    long impedimentoMedioTotal =
        etapasComImpedimento == 0
            ? 0L
            : etapas.stream().mapToLong(LeadTimeMedioEtapaResponse::impedimentoMedioSegundos).sum()
                / etapasComImpedimento;

    return new DashboardResponse(
        inicioEfetivo,
        fimEfetivo,
        etapas,
        impedimentoMedioTotal,
        dashboardRepository.contarTarefasNoPeriodo(projetoId, inicioEfetivo, fimEfetivo));
  }

  private void validarJanela(Instant inicio, Instant fim) {
    if (!fim.isAfter(inicio)) {
      throw new BusinessException(
          "PERIODO_INVALIDO", "A data final deve ser posterior a data inicial.");
    }
    if (Duration.between(inicio, fim).compareTo(JANELA_MAXIMA) > 0) {
      throw new BusinessException(
          "PERIODO_INVALIDO", "O periodo consultado nao pode exceder 365 dias.");
    }
  }
}
