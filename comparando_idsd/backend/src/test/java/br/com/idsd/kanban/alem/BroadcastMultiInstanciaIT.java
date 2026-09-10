package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * RNF-002 — o broadcast alcanca todas as sessoes, em qualquer instancia.
 *
 * <p>Este e o mecanismo menos verificado do sistema por construcao: ele so
 * falha quando ha mais de uma instancia, e nenhum cenario congelado descreve
 * topologia. Uma suite que subisse uma instancia so passaria em verde sobre
 * exatamente o desenho que ADR-004 existe para resolver — quem esta conectado
 * na instancia B nao ve o que foi escrito na instancia A, e o board fica
 * mentindo ate alguem recarregar.
 *
 * <p>Tres instancias e 300 sessoes sao o envelope declarado na TechSpec. Duas
 * instancias verificariam a propagacao mas nao a fila do publicador unico sob
 * concorrencia de escrita.
 */
@Tag("carga")
class BroadcastMultiInstanciaIT extends TesteDeIntegracao {

    private static final int INSTANCIAS = 3;
    private static final int SESSOES = 300;
    private static final Duration LIMITE = Duration.ofSeconds(2);

    @Test
    @DisplayName("o evento escrito numa instancia chega as sessoes das tres, dentro do limite")
    void eventoAlcancaAsSessoesDeTodasAsInstancias() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.fluxoPadrao(projeto, ana());

        var instancias = InstanciasEmParalelo.subir(INSTANCIAS, POSTGRES);
        try {
            var recebidos = new ConcurrentHashMap<String, List<Long>>();
            var chegada = new CountDownLatch(SESSOES);

            // As sessoes sao distribuidas entre as instancias: concentrar
            // todas numa so verificaria a entrega local, que nao e o risco.
            for (int i = 0; i < SESSOES; i++) {
                var instancia = instancias.get(i % INSTANCIAS);
                String sessao = "sessao-" + i;
                instancia.inscrever(projeto, sessao, envelope -> {
                    recebidos.computeIfAbsent(sessao, chave -> new java.util.ArrayList<>())
                            .add(envelope.seq());
                    chegada.countDown();
                });
            }

            // A escrita entra por uma instancia so. O caminho ate as outras
            // duas e `NOTIFY` no commit, e nao memoria compartilhada.
            UUID tarefa = instancias.get(0).criarTarefa(projeto, "Visivel para todos");

            Assertions.assertThat(chegada.await(LIMITE.toSeconds(), TimeUnit.SECONDS))
                    .as("p95 de RNF-002: o evento precisa alcancar todas as sessoes em ate %s",
                            LIMITE)
                    .isTrue();

            Assertions.assertThat(recebidos).hasSize(SESSOES);
            Assertions.assertThat(recebidos.values())
                    .as("uma entrega por sessao; duplicata significa que mais de um "
                            + "publicador esta ativo, contra SDR-004")
                    .allSatisfy(seqs -> Assertions.assertThat(seqs).hasSize(1));
            Assertions.assertThat(tarefa).isNotNull();
        } finally {
            instancias.forEach(InstanciasEmParalelo.Instancia::derrubar);
        }
    }

    @Test
    @DisplayName("a instancia que reassume a escuta varre o que perdeu, sem deixar lacuna")
    void aInstanciaQueReassumeVarreOQuePerdeu() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.fluxoPadrao(projeto, ana());

        var instancias = InstanciasEmParalelo.subir(2, POSTGRES);
        try {
            var recebidos = new java.util.concurrent.CopyOnWriteArrayList<Long>();
            instancias.get(1).inscrever(projeto, "observadora", e -> recebidos.add(e.seq()));

            // A janela que o `afterCommit` deixa aberta: entre o commit e o
            // `NOTIFY` a instancia pode cair, e o evento nunca e anunciado. A
            // varredura na retomada e a rede que fecha isso, e sem ela a
            // perda e permanente e silenciosa.
            instancias.get(1).interromperEscuta();
            instancias.get(0).criarTarefa(projeto, "Escrita durante a queda");
            instancias.get(1).retomarEscuta();

            Assertions.assertThat(esperarAte(() -> recebidos.size() == 1, LIMITE))
                    .as("a varredura de retomada precisa entregar o evento perdido")
                    .isTrue();
            Assertions.assertThat(recebidos).containsExactly(1L);
        } finally {
            instancias.forEach(InstanciasEmParalelo.Instancia::derrubar);
        }
    }

    private boolean esperarAte(java.util.function.BooleanSupplier condicao, Duration limite)
            throws InterruptedException {
        long fim = System.nanoTime() + limite.toNanos();
        while (System.nanoTime() < fim) {
            if (condicao.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condicao.getAsBoolean();
    }
}
