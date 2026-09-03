package br.com.crudao.kanban.leadtime;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.ClockProvider;
import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.leadtime.dto.DashboardResponse;
import br.com.crudao.kanban.leadtime.dto.LeadTimeMedioEtapaResponse;
import br.com.crudao.kanban.rbac.Permissoes;
import br.com.crudao.kanban.security.PermissaoGuard;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import br.com.crudao.kanban.tarefa.TarefaRepositoryCustom.AgregadoEtapa;
import br.com.crudao.kanban.workflow.Etapa;
import br.com.crudao.kanban.workflow.EtapaRepository;
import br.com.crudao.kanban.workflow.Workflow;
import br.com.crudao.kanban.workflow.WorkflowRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Metricas de lead-time por etapa (RF-007).
 *
 * <p>Leitura permanece liberada em projeto finalizado (UC-002): {@code exigirProjetoAtivo} so vale
 * para escrita.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

  private static final Duration JANELA_PADRAO = Duration.ofDays(30);
  private static final Duration JANELA_MAXIMA = Duration.ofDays(365);

  private final TarefaRepository tarefaRepository;
  private final WorkflowRepository workflowRepository;
  private final EtapaRepository etapaRepository;
  private final PermissaoGuard permissaoGuard;
  private final ClockProvider clockProvider;

  @Transactional(readOnly = true)
  public DashboardResponse obter(UUID projetoId, Instant inicio, Instant fim) {
    permissaoGuard.exigir(Permissoes.DASHBOARD_VISUALIZAR, projetoId);

    Instant fimEfetivo = fim != null ? fim : clockProvider.agora();
    Instant inicioEfetivo = inicio != null ? inicio : fimEfetivo.minus(JANELA_PADRAO);
    validarJanela(inicioEfetivo, fimEfetivo);

    Workflow workflow =
        workflowRepository
            .findByProjetoIdAndAtivoTrue(projetoId)
            .orElseThrow(
                () ->
                    new ConfiguracaoWorkflowInvalidaException(
                        "O projeto nao possui workflow ativo configurado."));

    Map<UUID, AgregadoEtapa> agregados = new HashMap<>();
    for (AgregadoEtapa agregado :
        tarefaRepository.agregarLeadTimePorEtapa(projetoId, inicioEfetivo, fimEfetivo)) {
      agregados.put(agregado.etapaId(), agregado);
    }

    List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId());
    List<LeadTimeMedioEtapaResponse> linhas = new ArrayList<>(etapas.size());
    long somaImpedimento = 0L;
    int etapasComAmostra = 0;

    for (Etapa etapa : etapas) {
      AgregadoEtapa agregado = agregados.get(etapa.getId());
      long amostras = agregado != null ? agregado.amostras() : 0L;
      long permanencia = agregado != null ? agregado.mediaPermanenciaSegundos() : 0L;
      long impedimento = agregado != null ? agregado.mediaImpedimentoSegundos() : 0L;

      if (amostras > 0) {
        somaImpedimento += impedimento;
        etapasComAmostra++;
      }
      linhas.add(
          new LeadTimeMedioEtapaResponse(
              etapa.getId(), etapa.getNome(), etapa.getOrdem(), amostras, permanencia, impedimento));
    }

    long impedimentoMedioTotal = etapasComAmostra == 0 ? 0L : somaImpedimento / etapasComAmostra;
    return new DashboardResponse(
        projetoId, inicioEfetivo, fimEfetivo, linhas, impedimentoMedioTotal);
  }

  private void validarJanela(Instant inicio, Instant fim) {
    if (!fim.isAfter(inicio)) {
      throw new BusinessException(
          "VALIDACAO_ENTRADA",
          "A data final do periodo deve ser posterior a data inicial.",
          HttpStatus.BAD_REQUEST);
    }
    if (Duration.between(inicio, fim).compareTo(JANELA_MAXIMA) > 0) {
      throw new BusinessException(
          "VALIDACAO_ENTRADA",
          "O periodo consultado nao pode exceder 365 dias.",
          HttpStatus.BAD_REQUEST);
    }
  }
}
