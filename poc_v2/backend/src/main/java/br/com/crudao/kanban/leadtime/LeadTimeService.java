package br.com.crudao.kanban.leadtime;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.ClockProvider;
import br.com.crudao.kanban.leadtime.dto.DetalheLeadTime;
import br.com.crudao.kanban.leadtime.dto.LeadTimeEtapaResponse;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.workflow.Etapa;
import br.com.crudao.kanban.workflow.EtapaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intervalos temporais que sustentam o lead-time (RF-006, RN-002). Nenhum instante e gerado na JVM:
 * todo carimbo vem do {@code now()} transacional do PostgreSQL (R-6).
 */
@Service
@RequiredArgsConstructor
public class LeadTimeService {

  private final PeriodoEtapaRepository periodoEtapaRepository;
  private final PeriodoImpedimentoRepository periodoImpedimentoRepository;
  private final EtapaRepository etapaRepository;
  private final ClockProvider clockProvider;

  /** Abre o periodo de permanencia na etapa. Falha se ja houver um aberto (invariante). */
  @Transactional
  public PeriodoEtapa abrirPeriodoEtapa(Tarefa tarefa, UUID etapaId) {
    if (periodoEtapaRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()).isPresent()) {
      throw new BusinessException(
          "PERIODO_ETAPA_JA_ABERTO",
          "A tarefa ja possui um periodo de etapa em aberto.",
          HttpStatus.CONFLICT);
    }
    return periodoEtapaRepository.save(
        new PeriodoEtapa(tarefa.getId(), tarefa.getProjetoId(), etapaId, clockProvider.agora()));
  }

  /** Encerra o periodo de etapa aberto, se existir. */
  @Transactional
  public void encerrarPeriodoEtapa(Tarefa tarefa) {
    periodoEtapaRepository
        .findByTarefaIdAndEncerradoEmIsNull(tarefa.getId())
        .ifPresent(periodo -> periodo.setEncerradoEm(clockProvider.agora()));
    periodoEtapaRepository.flush();
  }

  /** Abre o periodo de impedimento carimbado com a etapa vigente. Idempotente (RF-004). */
  @Transactional
  public Optional<PeriodoImpedimento> abrirImpedimento(Tarefa tarefa, String motivo) {
    if (periodoImpedimentoRepository
        .findByTarefaIdAndEncerradoEmIsNull(tarefa.getId())
        .isPresent()) {
      return Optional.empty();
    }
    return Optional.of(
        periodoImpedimentoRepository.save(
            new PeriodoImpedimento(
                tarefa.getId(),
                tarefa.getProjetoId(),
                tarefa.getEtapaId(),
                motivo,
                clockProvider.agora())));
  }

  /** Encerra o periodo de impedimento aberto, se existir. Idempotente. */
  @Transactional
  public boolean encerrarImpedimento(Tarefa tarefa) {
    Optional<PeriodoImpedimento> aberto =
        periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId());
    aberto.ifPresent(periodo -> periodo.setEncerradoEm(clockProvider.agora()));
    periodoImpedimentoRepository.flush();
    return aberto.isPresent();
  }

  /**
   * Fecha o impedimento na etapa antiga e reabre na nova, de forma atomica. Sem isso o tempo
   * impedido nao poderia ser atribuido corretamente por etapa (RN-002).
   */
  @Transactional
  public void reancorarImpedimento(Tarefa tarefa, UUID novaEtapaId) {
    Optional<PeriodoImpedimento> aberto =
        periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId());
    if (aberto.isEmpty()) {
      return;
    }
    Instant agora = clockProvider.agora();
    PeriodoImpedimento anterior = aberto.get();
    anterior.setEncerradoEm(agora);
    periodoImpedimentoRepository.flush();
    periodoImpedimentoRepository.save(
        new PeriodoImpedimento(
            tarefa.getId(), tarefa.getProjetoId(), novaEtapaId, anterior.getMotivo(), agora));
  }

  /** Encerra todos os intervalos abertos da tarefa (usado antes da exclusao, RF-019). */
  @Transactional
  public void encerrarTodosOsPeriodos(Tarefa tarefa) {
    encerrarPeriodoEtapa(tarefa);
    encerrarImpedimento(tarefa);
  }

  /**
   * Detalhe por etapa e total impedido. Intervalo aberto e computado ate o {@code now()} do banco.
   */
  @Transactional(readOnly = true)
  public DetalheLeadTime calcularDetalhe(UUID tarefaId, UUID workflowId) {
    Instant agora = clockProvider.agora();
    Map<UUID, Long> permanencia = new HashMap<>();
    for (PeriodoEtapa periodo : periodoEtapaRepository.findByTarefaId(tarefaId)) {
      permanencia.merge(periodo.getEtapaId(), periodo.duracao(agora).toSeconds(), Long::sum);
    }
    Map<UUID, Long> impedimento = new HashMap<>();
    long impedimentoTotal = 0L;
    for (PeriodoImpedimento periodo : periodoImpedimentoRepository.findByTarefaId(tarefaId)) {
      long segundos = periodo.duracao(agora).toSeconds();
      impedimento.merge(periodo.getEtapaId(), segundos, Long::sum);
      impedimentoTotal += segundos;
    }
    List<LeadTimeEtapaResponse> etapas = new ArrayList<>();
    for (Etapa etapa : etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflowId)) {
      etapas.add(
          new LeadTimeEtapaResponse(
              etapa.getId(),
              etapa.getNome(),
              etapa.getOrdem(),
              permanencia.getOrDefault(etapa.getId(), 0L),
              impedimento.getOrDefault(etapa.getId(), 0L)));
    }
    return new DetalheLeadTime(etapas, impedimentoTotal);
  }
}
