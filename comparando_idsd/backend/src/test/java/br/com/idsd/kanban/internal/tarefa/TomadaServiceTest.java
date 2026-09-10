package br.com.idsd.kanban.internal.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.idsd.kanban.internal.tempo.AplicadorDeIntervalos;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SCN-007.2 — Tomada repetida por quem ja assumiu.
 *
 * <p>E a idempotencia de RN-031 no caso mais simples: origem declarada coincide
 * com o estado corrente e o efeito ja esta aplicado. A resposta e {@code 200}
 * com o estado atual, <b>sem novo evento</b> — e e a ausencia do evento que este
 * teste existe para travar. Um segundo {@code TAREFA_ASSUMIDA} reabriria a
 * contagem de espera e falsearia a serie que RF-016 agrega.
 */
@ExtendWith(MockitoExtension.class)
class TomadaServiceTest {

    @Mock
    private RegistradorDeEvento registradorDeEvento;

    @Mock
    private AplicadorDeIntervalos aplicadorDeIntervalos;

    @Mock
    private VerificadorDeOrigem verificadorDeOrigem;

    private final UUID tarefaId = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final Instant assumidaEm = Instant.parse("2026-03-01T10:00:00Z");

    private TomadaService servico() {
        return new TomadaService(registradorDeEvento, aplicadorDeIntervalos, verificadorDeOrigem);
    }

    private Tarefa tarefaJaAssumidaPor(UUID responsavel) {
        var tarefa = new Tarefa();
        tarefa.setId(tarefaId);
        tarefa.setCondicao(Condicao.EM_CURSO);
        tarefa.setResponsavelId(responsavel);
        tarefa.setAssumidaEm(assumidaEm);
        return tarefa;
    }

    @Test
    @DisplayName("SCN-007.2 — quem ja assumiu continua responsavel ao repetir a tomada")
    void repeticaoMantemResponsavel() {
        var tarefa = tarefaJaAssumidaPor(ana);

        var resultado = servico().assumir(tarefa, ana);

        assertThat(resultado.getResponsavelId()).isEqualTo(ana);
        assertThat(resultado.getCondicao()).isEqualTo(Condicao.EM_CURSO);
    }

    @Test
    @DisplayName("SCN-007.2 — o momento em que assumi nao e alterado")
    void repeticaoNaoAlteraOInstanteDaTomada() {
        var tarefa = tarefaJaAssumidaPor(ana);

        var resultado = servico().assumir(tarefa, ana);

        assertThat(resultado.getAssumidaEm()).isEqualTo(assumidaEm);
    }

    @Test
    @DisplayName("SCN-007.2 — repetir a tomada nao grava novo evento")
    void repeticaoNaoGravaEvento() {
        var tarefa = tarefaJaAssumidaPor(ana);

        servico().assumir(tarefa, ana);

        verify(registradorDeEvento, never()).registrar(any());
    }

    @Test
    @DisplayName("SCN-007.2 — repetir a tomada nao mexe em intervalo algum")
    void repeticaoNaoAbreNemFechaIntervalo() {
        var tarefa = tarefaJaAssumidaPor(ana);

        servico().assumir(tarefa, ana);

        // A espera de tomada ja foi encerrada na primeira tomada. Reabri-la ou
        // fecha-la de novo produziria intervalo de duracao zero na serie.
        verify(aplicadorDeIntervalos, never()).aplicar(any(), any());
    }

    @Test
    @DisplayName("SCN-007.1 — a primeira tomada, essa sim, encerra a espera e grava evento")
    void primeiraTomadaProduzEfeito() {
        var tarefa = new Tarefa();
        tarefa.setId(tarefaId);
        tarefa.setCondicao(Condicao.AGUARDANDO_TOMADA);
        tarefa.setResponsavelId(null);
        when(aplicadorDeIntervalos.aplicar(any(), any())).thenReturn(null);

        var resultado = servico().assumir(tarefa, ana);

        assertThat(resultado.getResponsavelId()).isEqualTo(ana);
        assertThat(resultado.getCondicao()).isEqualTo(Condicao.EM_CURSO);
        verify(registradorDeEvento).registrar(any());
    }
}
