package br.com.crudao.kanban.leadtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.ClockProvider;
import br.com.crudao.kanban.leadtime.dto.LeadTimeDetalhe;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.workflow.Etapa;
import br.com.crudao.kanban.workflow.EtapaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reancoragem de impedimento na troca de etapa e contabilizacao de intervalo aberto. */
@ExtendWith(MockitoExtension.class)
class LeadTimeServiceTest {

  private static final Instant AGORA = Instant.parse("2026-09-03T15:00:00Z");
  private static final UUID WORKFLOW = UUID.randomUUID();

  @Mock private PeriodoEtapaRepository periodoEtapaRepository;
  @Mock private PeriodoImpedimentoRepository periodoImpedimentoRepository;
  @Mock private EtapaRepository etapaRepository;
  @Mock private ClockProvider clockProvider;

  @InjectMocks private LeadTimeService servico;

  private Tarefa tarefa;
  private UUID etapaA;
  private UUID etapaB;

  @BeforeEach
  void preparar() {
    etapaA = UUID.randomUUID();
    etapaB = UUID.randomUUID();
    tarefa =
        Tarefa.builder()
            .id(UUID.randomUUID())
            .projetoId(UUID.randomUUID())
            .workflowId(WORKFLOW)
            .etapaId(etapaA)
            .build();
  }

  @Test
  @DisplayName("abrir segundo periodo de etapa viola a invariante de intervalo unico aberto")
  void segundoPeriodoAbertoFalha() {
    when(periodoEtapaRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(PeriodoEtapa.builder().tarefaId(tarefa.getId()).build()));

    assertThatThrownBy(() -> servico.abrirPeriodoEtapa(tarefa, etapaB))
        .isInstanceOf(IllegalStateException.class);
    verify(periodoEtapaRepository, never()).save(any());
  }

  @Test
  @DisplayName("encerrar periodo de etapa carimba com o now() do banco")
  void encerraComRelogioDoBanco() {
    PeriodoEtapa aberto = PeriodoEtapa.builder().tarefaId(tarefa.getId()).etapaId(etapaA).build();
    when(periodoEtapaRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(aberto));
    when(clockProvider.agora()).thenReturn(AGORA);

    servico.encerrarPeriodoEtapa(tarefa);

    assertThat(aberto.getEncerradoEm()).isEqualTo(AGORA);
  }

  @Test
  @DisplayName("abrir impedimento em tarefa ja impedida nao cria segundo periodo")
  void abrirImpedimentoIdempotente() {
    when(periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(PeriodoImpedimento.builder().motivo("Antigo").build()));

    assertThat(servico.abrirImpedimento(tarefa, "Novo")).isEmpty();
    verify(periodoImpedimentoRepository, never()).save(any());
  }

  @Test
  @DisplayName("reancorar fecha na etapa antiga, reabre na nova e preserva o motivo")
  void reancoraPreservandoMotivo() {
    PeriodoImpedimento anterior =
        PeriodoImpedimento.builder()
            .tarefaId(tarefa.getId())
            .projetoId(tarefa.getProjetoId())
            .etapaId(etapaA)
            .motivo("Aguardando cliente")
            .build();
    when(periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(anterior));
    when(clockProvider.agora()).thenReturn(AGORA);

    servico.reancorarImpedimento(tarefa, etapaB);

    assertThat(anterior.getEncerradoEm()).isEqualTo(AGORA);
    ArgumentCaptor<PeriodoImpedimento> captor =
        ArgumentCaptor.forClass(PeriodoImpedimento.class);
    verify(periodoImpedimentoRepository).save(captor.capture());
    assertThat(captor.getValue().getEtapaId()).isEqualTo(etapaB);
    assertThat(captor.getValue().getMotivo()).isEqualTo("Aguardando cliente");
    assertThat(captor.getValue().getEncerradoEm()).isNull();
  }

  @Test
  @DisplayName("sem impedimento aberto a reancoragem e no-op")
  void reancorarSemImpedimento() {
    when(periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()))
        .thenReturn(Optional.empty());

    servico.reancorarImpedimento(tarefa, etapaB);

    verify(periodoImpedimentoRepository, never()).save(any());
    verify(periodoImpedimentoRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("intervalo aberto e contabilizado ate o now() do banco")
  void intervaloAbertoContabilizado() {
    PeriodoEtapa fechado =
        PeriodoEtapa.builder()
            .etapaId(etapaA)
            .iniciadoEm(AGORA.minus(Duration.ofHours(5)))
            .encerradoEm(AGORA.minus(Duration.ofHours(3)))
            .build();
    PeriodoEtapa aberto =
        PeriodoEtapa.builder().etapaId(etapaB).iniciadoEm(AGORA.minus(Duration.ofHours(3))).build();
    PeriodoImpedimento impedimento =
        PeriodoImpedimento.builder()
            .etapaId(etapaB)
            .motivo("Bloqueado")
            .iniciadoEm(AGORA.minus(Duration.ofHours(1)))
            .build();

    when(clockProvider.agora()).thenReturn(AGORA);
    when(periodoEtapaRepository.findByTarefaIdOrderByIniciadoEmAsc(tarefa.getId()))
        .thenReturn(List.of(fechado, aberto));
    when(periodoImpedimentoRepository.findByTarefaIdOrderByIniciadoEmAsc(tarefa.getId()))
        .thenReturn(List.of(impedimento));
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW))
        .thenReturn(
            List.of(
                Etapa.builder().id(etapaA).workflowId(WORKFLOW).nome("A fazer").ordem(1).build(),
                Etapa.builder().id(etapaB).workflowId(WORKFLOW).nome("Fazendo").ordem(2).build()));

    LeadTimeDetalhe detalhe = servico.calcularDetalhe(tarefa.getId(), WORKFLOW);

    assertThat(detalhe.porEtapa()).hasSize(2);
    assertThat(detalhe.porEtapa().get(0).permanenciaSegundos())
        .isEqualTo(Duration.ofHours(2).toSeconds());
    assertThat(detalhe.porEtapa().get(1).permanenciaSegundos())
        .isEqualTo(Duration.ofHours(3).toSeconds());
    assertThat(detalhe.porEtapa().get(1).impedimentoSegundos())
        .isEqualTo(Duration.ofHours(1).toSeconds());
    assertThat(detalhe.impedimentoTotalSegundos()).isEqualTo(Duration.ofHours(1).toSeconds());
  }

  @Test
  @DisplayName("etapa sem periodo registrado reporta zero, nao some do detalhe")
  void etapaSemPeriodo() {
    when(clockProvider.agora()).thenReturn(AGORA);
    when(periodoEtapaRepository.findByTarefaIdOrderByIniciadoEmAsc(tarefa.getId()))
        .thenReturn(List.of());
    when(periodoImpedimentoRepository.findByTarefaIdOrderByIniciadoEmAsc(tarefa.getId()))
        .thenReturn(List.of());
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW))
        .thenReturn(
            List.of(Etapa.builder().id(etapaA).workflowId(WORKFLOW).nome("A fazer").ordem(1).build()));

    LeadTimeDetalhe detalhe = servico.calcularDetalhe(tarefa.getId(), WORKFLOW);

    assertThat(detalhe.porEtapa()).singleElement().satisfies(etapa -> {
      assertThat(etapa.permanenciaSegundos()).isZero();
      assertThat(etapa.impedimentoSegundos()).isZero();
    });
  }
}
