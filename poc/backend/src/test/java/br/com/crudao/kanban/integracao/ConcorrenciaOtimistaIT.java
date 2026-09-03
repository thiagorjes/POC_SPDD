package br.com.crudao.kanban.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dois usuarios arrastando o mesmo card: o segundo tem de falhar de forma explicita, nunca
 * sobrescrever silenciosamente a movimentacao do primeiro.
 */
class ConcorrenciaOtimistaIT extends AbstractIntegracaoTest {

  @Autowired private TarefaRepository tarefaRepository;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private CenarioFixture fixture;

  @Test
  @DisplayName("@Version faz a segunda escrita concorrente falhar com bloqueio otimista")
  void segundaEscritaFalha() throws Exception {
    CenarioFixture.Cenario cenario = fixture.criar();
    UUID tarefaId = fixture.criarTarefa(cenario);

    CountDownLatch ambosLeram = new CountDownLatch(2);
    AtomicInteger sucessos = new AtomicInteger();
    AtomicInteger conflitos = new AtomicInteger();

    Runnable mover =
        () -> {
          try {
            transactionTemplate.executeWithoutResult(
                status -> {
                  Tarefa tarefa = tarefaRepository.findById(tarefaId).orElseThrow();
                  ambosLeram.countDown();
                  try {
                    // Garante que ambas as transacoes leram a mesma versao antes de escrever.
                    ambosLeram.await(5, TimeUnit.SECONDS);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                  }
                  tarefa.setEtapaId(cenario.fazendoId());
                  tarefaRepository.saveAndFlush(tarefa);
                });
            sucessos.incrementAndGet();
          } catch (ObjectOptimisticLockingFailureException e) {
            conflitos.incrementAndGet();
          }
        };

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      executor.submit(mover);
      executor.submit(mover);
      executor.shutdown();
      assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(sucessos.get()).isEqualTo(1);
    assertThat(conflitos.get()).isEqualTo(1);

    Tarefa persistida = tarefaRepository.findById(tarefaId).orElseThrow();
    assertThat(persistida.getEtapaId()).isEqualTo(cenario.fazendoId());
    assertThat(persistida.getVersao()).isEqualTo(1L);
  }

  @Test
  @DisplayName("escrita com versao esperada desatualizada nao sobrescreve o estado atual")
  void versaoDesatualizadaNaoSobrescreve() {
    CenarioFixture.Cenario cenario = fixture.criar();
    UUID tarefaId = fixture.criarTarefa(cenario);

    transactionTemplate.executeWithoutResult(
        status -> {
          Tarefa tarefa = tarefaRepository.findById(tarefaId).orElseThrow();
          tarefa.setTitulo("Primeiro");
          tarefaRepository.saveAndFlush(tarefa);
        });

    Tarefa atual = tarefaRepository.findById(tarefaId).orElseThrow();
    assertThat(atual.getVersao()).isEqualTo(1L);
    assertThat(atual.getTitulo()).isEqualTo("Primeiro");
  }
}
