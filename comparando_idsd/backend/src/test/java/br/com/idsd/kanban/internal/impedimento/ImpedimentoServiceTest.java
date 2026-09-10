package br.com.idsd.kanban.internal.impedimento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.idsd.kanban.internal.tarefa.Impedimento;
import br.com.idsd.kanban.internal.tarefa.RegistradorDeEvento;
import br.com.idsd.kanban.internal.tempo.AplicadorDeIntervalos;
import br.com.idsd.kanban.shared.RegraDeNegocioViolada;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SCN-009.2 — Sinalizacao sem motivo, e SCN-010.3 — Resolucao repetida.
 *
 * <p>Os dois cenarios sao tipados {@code unitario} no PRD e sao regra pura: nao
 * dependem de banco nem de contexto. O par vive no mesmo arquivo porque
 * verificam as duas pontas do mesmo objeto — a que recusa abrir sem razao e a
 * que recusa contar duas vezes o mesmo desfecho.
 */
@ExtendWith(MockitoExtension.class)
class ImpedimentoServiceTest {

    @Mock
    private RegistradorDeEvento registradorDeEvento;

    @Mock
    private AplicadorDeIntervalos aplicadorDeIntervalos;

    @Mock
    private DestaqueDeImpedimento destaque;

    private final UUID tarefaId = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();

    private ImpedimentoService servico() {
        return new ImpedimentoService(registradorDeEvento, aplicadorDeIntervalos, destaque);
    }

    // ------------------------------------------------------------ SCN-009.2

    @Test
    @DisplayName("SCN-009.2 — sinalizacao sem motivo e recusada")
    void recusaSinalizacaoSemMotivo() {
        assertThatThrownBy(() -> servico().abrir(tarefaId, null, ana))
                .isInstanceOf(RegraDeNegocioViolada.class);
    }

    @ParameterizedTest(name = "SCN-009.2 — motivo [{0}] e recusado")
    @ValueSource(strings = {"", " ", "    ", "\t"})
    @DisplayName("SCN-009.2 — motivo em branco conta como ausente")
    void recusaMotivoEmBranco(String motivo) {
        assertThatThrownBy(() -> servico().abrir(tarefaId, motivo, ana))
                .isInstanceOf(RegraDeNegocioViolada.class);
    }

    @Test
    @DisplayName("SCN-009.2 — nenhum impedimento e aberto na recusa")
    void recusaNaoAbreImpedimento() {
        assertThatThrownBy(() -> servico().abrir(tarefaId, "  ", ana))
                .isInstanceOf(RegraDeNegocioViolada.class);

        // Nem intervalo, nem evento, nem destaque. A marca so existe quando ha
        // impedimento aberto, e abrir um sem motivo produziria marca que
        // ninguem sabe explicar.
        verify(aplicadorDeIntervalos, never()).aplicar(any(), any());
        verify(registradorDeEvento, never()).registrar(any());
        verify(destaque, never()).destacar(any());
    }

    // ------------------------------------------------------------ SCN-010.3

    private Impedimento jaResolvido() {
        var impedimento = new Impedimento();
        impedimento.setId(UUID.randomUUID());
        impedimento.setTarefaId(tarefaId);
        impedimento.setMotivo("dependencia externa");
        impedimento.setAbertoPor(ana);
        impedimento.setDesfecho("resolvido com a equipe de plataforma");
        impedimento.setResolvidoPor(ana);
        impedimento.setResolvidoEm(Instant.parse("2026-03-01T13:00:00Z"));
        return impedimento;
    }

    @Test
    @DisplayName("SCN-010.3 — resolver de novo nao altera nada")
    void resolucaoRepetidaNaoAlteraNada() {
        var impedimento = jaResolvido();
        var desfechoOriginal = impedimento.getDesfecho();
        var resolvidoEmOriginal = impedimento.getResolvidoEm();

        var resultado = servico().resolver(impedimento, "outro desfecho qualquer", ana);

        assertThat(resultado.getDesfecho()).isEqualTo(desfechoOriginal);
        assertThat(resultado.getResolvidoEm()).isEqualTo(resolvidoEmOriginal);
    }

    @Test
    @DisplayName("SCN-010.3 — o tempo registrado de impedimento permanece o mesmo")
    void resolucaoRepetidaNaoTocaOIntervalo() {
        servico().resolver(jaResolvido(), "outro desfecho", ana);

        // Fechar de novo um intervalo ja fechado moveria o `fim` e reescreveria
        // tempo ja contado, que RNF-008 protege.
        verify(aplicadorDeIntervalos, never()).aplicar(any(), any());
    }

    @Test
    @DisplayName("SCN-010.3 — resolucao repetida nao grava novo evento")
    void resolucaoRepetidaNaoGravaEvento() {
        servico().resolver(jaResolvido(), "outro desfecho", ana);

        verify(registradorDeEvento, never()).registrar(any());
    }
}
