package br.com.crudao.kanban.leadtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.ClockProvider;
import br.com.crudao.kanban.leadtime.dto.DetalheLeadTime;
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

/**
 * Intervalos de lead-time (bloco 19.1): reancoragem do impedimento na troca de etapa e computo do
 * intervalo aberto ate o instante do banco.
 */
@ExtendWith(MockitoExtension.class)
class LeadTimeServiceTest {

  private static final Instant AGORA = Instant.parse("2026-09-04T12:00:00Z");
  private static final UUID PROJETO = UUID.randomUUID();
  private static final UUID WORKFLOW = UUID.randomUUID();

  @Mock private PeriodoEtapaRepository periodoEtapaRepository;
  @Mock private PeriodoImpedimentoRepository periodoImpedimentoRepository;
  @Mock private EtapaRepository etapaRepository;
  @Mock private ClockProvider clockProvider;

  @InjectMocks private LeadTimeService leadTimeService;

  private Tarefa tarefa;
  private Etapa aFazer;
  private Etapa fazendo;

  @BeforeEach
  void preparar() {
    aFazer = new Etapa(WORKFLOW, "A Fazer", 0, false);
    aFazer.setId(UUID.randomUUID());
    fazendo = new Etapa(WORKFLOW, "Fazendo", 1, false);
    fazendo.setId(UUID.randomUUID());
    tarefa =
        Tarefa.builder()
            .id(UUID.randomUUID())
            .projetoId(PROJETO)
            .workflowId(WORKFLOW)
            .etapaId(aFazer.getId())
            .build();
  }

  @Test
  @DisplayName("abrir periodo de etapa com outro em aberto viola a invariante")
  void naoAbreSegundoPeriodoDeEtapa() {
    when(periodoEtapaRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(new PeriodoEtapa()));

    assertThatThrownBy(() -> leadTimeService.abrirPeriodoEtapa(tarefa, aFazer.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("A tarefa ja possui um periodo de etapa em aberto.");
    verify(periodoEtapaRepository, never()).save(any());
  }

  @Test
  @DisplayName("abrir impedimento duas vezes e idempotente")
  void abrirImpedimentoEhIdempotente() {
    when(periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(new PeriodoImpedimento()));

    assertThat(leadTimeService.abrirImpedimento(tarefa, "motivo")).isEmpty();
    verify(periodoImpedimentoRepository, never()).save(any());
  }

  @Test
  @DisplayName("encerrar impedimento inexistente devolve false sem efeito colateral")
  void encerrarImpedimentoInexistente() {
    when(periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()))
        .thenReturn(Optional.empty());

    assertThat(leadTimeService.encerrarImpedimento(tarefa)).isFalse();
  }

  @Test
  @DisplayName("reancorar fecha o impedimento na etapa antiga e reabre na nova no mesmo instante")
  void reancoraImpedimento() {
    PeriodoImpedimento aberto =
        new PeriodoImpedimento(
            tarefa.getId(),
            PROJETO,
            aFazer.getId(),
            "aguardando cliente",
            AGORA.minus(Duration.ofHours(2)));
    when(periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()))
        .thenReturn(Optional.of(aberto));
    when(clockProvider.agora()).thenReturn(AGORA);

    leadTimeService.reancorarImpedimento(tarefa, fazendo.getId());

    assertThat(aberto.getEncerradoEm()).isEqualTo(AGORA);
    ArgumentCaptor<PeriodoImpedimento> captor = ArgumentCaptor.forClass(PeriodoImpedimento.class);
    verify(periodoImpedimentoRepository).save(captor.capture());
    PeriodoImpedimento novo = captor.getValue();
    assertThat(novo.getEtapaId()).isEqualTo(fazendo.getId());
    assertThat(novo.getIniciadoEm()).isEqualTo(AGORA);
    assertThat(novo.getMotivo()).isEqualTo("aguardando cliente");
    assertThat(novo.getEncerradoEm()).isNull();
  }

  @Test
  @DisplayName("reancorar sem impedimento aberto nao cria periodo")
  void reancorarSemImpedimentoNaoFazNada() {
    when(periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefa.getId()))
        .thenReturn(Optional.empty());

    leadTimeService.reancorarImpedimento(tarefa, fazendo.getId());

    verify(periodoImpedimentoRepository, never()).save(any());
  }

  @Test
  @DisplayName("intervalo aberto e computado ate o now() do banco e somado por etapa")
  void computaIntervaloAberto() {
    PeriodoEtapa encerrado =
        new PeriodoEtapa(tarefa.getId(), PROJETO, aFazer.getId(), AGORA.minus(Duration.ofHours(5)));
    encerrado.setEncerradoEm(AGORA.minus(Duration.ofHours(4)));
    PeriodoEtapa emAberto =
        new PeriodoEtapa(
            tarefa.getId(), PROJETO, fazendo.getId(), AGORA.minus(Duration.ofHours(3)));
    PeriodoImpedimento impedimento =
        new PeriodoImpedimento(
            tarefa.getId(), PROJETO, fazendo.getId(), null, AGORA.minus(Duration.ofHours(1)));

    when(clockProvider.agora()).thenReturn(AGORA);
    when(periodoEtapaRepository.findByTarefaId(tarefa.getId()))
        .thenReturn(List.of(encerrado, emAberto));
    when(periodoImpedimentoRepository.findByTarefaId(tarefa.getId()))
        .thenReturn(List.of(impedimento));
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW))
        .thenReturn(List.of(aFazer, fazendo));

    DetalheLeadTime detalhe = leadTimeService.calcularDetalhe(tarefa.getId(), WORKFLOW);

    assertThat(detalhe.etapas()).hasSize(2);
    assertThat(detalhe.etapas().get(0).duracaoSegundos())
        .isEqualTo(Duration.ofHours(1).toSeconds());
    assertThat(detalhe.etapas().get(1).duracaoSegundos())
        .isEqualTo(Duration.ofHours(3).toSeconds());
    assertThat(detalhe.etapas().get(1).impedimentoSegundos())
        .isEqualTo(Duration.ofHours(1).toSeconds());
    assertThat(detalhe.impedimentoTotalSegundos()).isEqualTo(Duration.ofHours(1).toSeconds());
  }

  @Test
  @DisplayName("etapa sem periodo aparece com duracao zero")
  void etapaSemPeriodoZera() {
    when(clockProvider.agora()).thenReturn(AGORA);
    when(periodoEtapaRepository.findByTarefaId(tarefa.getId())).thenReturn(List.of());
    when(periodoImpedimentoRepository.findByTarefaId(tarefa.getId())).thenReturn(List.of());
    when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(WORKFLOW)).thenReturn(List.of(aFazer));

    DetalheLeadTime detalhe = leadTimeService.calcularDetalhe(tarefa.getId(), WORKFLOW);

    assertThat(detalhe.etapas().get(0).duracaoSegundos()).isZero();
    assertThat(detalhe.impedimentoTotalSegundos()).isZero();
  }
}
