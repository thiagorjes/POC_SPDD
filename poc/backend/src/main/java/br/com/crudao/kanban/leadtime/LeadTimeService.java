package br.com.crudao.kanban.leadtime;

import br.com.crudao.kanban.common.ClockProvider;
import br.com.crudao.kanban.leadtime.dto.LeadTimeDetalhe;
import br.com.crudao.kanban.leadtime.dto.LeadTimeEtapaResponse;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.workflow.Etapa;
import br.com.crudao.kanban.workflow.EtapaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestao dos intervalos temporais que sustentam RF-006/RF-007 e RN-002.
 *
 * <p>Invariante: uma tarefa tem no maximo um {@link PeriodoEtapa} aberto e no maximo um {@link
 * PeriodoImpedimento} aberto — garantido tambem por indice unico parcial no banco.
 *
 * <p>Nenhum {@code Instant} e gerado na JVM: todo carimbo vem do {@code now()} transacional do
 * PostgreSQL via {@link ClockProvider} (R-6).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadTimeService {

  private final PeriodoEtapaRepository periodoEtapaRepository;
  private final PeriodoImpedimentoRepository periodoImpedimentoRepository;
  private final EtapaRepository etapaRepository;
  private final ClockProvider clockProvider;

  /** Abre o periodo de permanencia na etapa. Falha se ja houver periodo aberto (invariante). */
  @Transactional
  public PeriodoEtapa abrirPeriodoEtapa(Tarefa tarefa, UUID etapaId) {
    periodoEtapaRepository
        .findByTarefaIdAndEncerradoEmIsNull(tarefa.getId())
        .ifPresent(
            aberto -> {
              throw new IllegalStateException(
                  "Ja existe periodo de etapa aberto para a tarefa " + tarefa.getId());
            });
    return periodoEtapaRepository.save(
        PeriodoEtapa.builder()
            .tarefaId(tarefa.getId())
            .projetoId(tarefa.getProjetoId())
            .etapaId(etapaId)
            .build());
  }

  /** Encerra o periodo de etapa aberto, se houver. Idempotente. */
  @Transactional
  public void encerrarPeriodoEtapa(Tarefa tarefa) {
    periodoEtapaRepository
        .findByTarefaIdAndEncerradoEmIsNull(tarefa.getId())
        .ifPresent(periodo -> periodo.setEncerradoEm(clockProvider.agora()));
  }

  /** Abre o periodo de impedimento carimbado com a etapa vigente. Idempotente (RF-004). */
  @Transactional
  public Optional<PeriodoImpedimento> abrirImpedimento(Tarefa tarefa, String motivo) {
    if (periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()).isPresent()) {
      return Optional.empty();
    }
    return Optional.of(
        periodoImpedimentoRepository.save(
            PeriodoImpedimento.builder()
                .tarefaId(tarefa.getId())
                .projetoId(tarefa.getProjetoId())
                .etapaId(tarefa.getEtapaId())
                .motivo(motivo)
                .build()));
  }

  /** Encerra o impedimento aberto, se houver. Idempotente. */
  @Transactional
  public boolean encerrarImpedimento(Tarefa tarefa) {
    Optional<PeriodoImpedimento> aberto =
        periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId());
    aberto.ifPresent(periodo -> periodo.setEncerradoEm(clockProvider.agora()));
    return aberto.isPresent();
  }

  /**
   * Fecha o impedimento na etapa antiga e reabre na nova, preservando o motivo. Sem isso, RN-002
   * nao conseguiria atribuir o tempo impedido a etapa correta.
   */
  @Transactional
  public void reancorarImpedimento(Tarefa tarefa, UUID novaEtapaId) {
    Optional<PeriodoImpedimento> aberto =
        periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId());
    if (aberto.isEmpty()) {
      return;
    }
    PeriodoImpedimento anterior = aberto.get();
    anterior.setEncerradoEm(clockProvider.agora());
    periodoImpedimentoRepository.saveAndFlush(anterior);
    periodoImpedimentoRepository.save(
        PeriodoImpedimento.builder()
            .tarefaId(tarefa.getId())
            .projetoId(tarefa.getProjetoId())
            .etapaId(novaEtapaId)
            .motivo(anterior.getMotivo())
            .build());
  }

  /** Fecha todos os intervalos abertos — usado antes da exclusao, para nao contaminar medias. */
  @Transactional
  public void encerrarTudo(Tarefa tarefa) {
    encerrarPeriodoEtapa(tarefa);
    encerrarImpedimento(tarefa);
  }

  /** Soma por etapa; intervalo aberto computado ate {@code now()} do banco (RF-006). */
  @Transactional(readOnly = true)
  public LeadTimeDetalhe calcularDetalhe(UUID tarefaId, UUID workflowId) {
    Instant agora = clockProvider.agora();

    Map<UUID, Duration> permanencia = new LinkedHashMap<>();
    for (PeriodoEtapa periodo : periodoEtapaRepository.findByTarefaIdOrderByIniciadoEmAsc(tarefaId)) {
      permanencia.merge(periodo.getEtapaId(), periodo.duracao(agora), Duration::plus);
    }

    Map<UUID, Duration> impedimento = new LinkedHashMap<>();
    Duration impedimentoTotal = Duration.ZERO;
    for (PeriodoImpedimento periodo :
        periodoImpedimentoRepository.findByTarefaIdOrderByIniciadoEmAsc(tarefaId)) {
      Duration duracao = periodo.duracao(agora);
      impedimento.merge(periodo.getEtapaId(), duracao, Duration::plus);
      impedimentoTotal = impedimentoTotal.plus(duracao);
    }

    List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflowId);
    List<LeadTimeEtapaResponse> porEtapa = new ArrayList<>(etapas.size());
    for (Etapa etapa : etapas) {
      porEtapa.add(
          new LeadTimeEtapaResponse(
              etapa.getId(),
              etapa.getNome(),
              permanencia.getOrDefault(etapa.getId(), Duration.ZERO).toSeconds(),
              impedimento.getOrDefault(etapa.getId(), Duration.ZERO).toSeconds()));
    }
    return new LeadTimeDetalhe(porEtapa, impedimentoTotal.toSeconds());
  }
}
