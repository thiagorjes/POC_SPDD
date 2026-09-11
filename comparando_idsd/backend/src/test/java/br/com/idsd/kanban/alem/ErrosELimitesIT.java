package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/**
 * Formato do erro, negacao por padrao e limite de requisicoes (TASK-01.6).
 *
 * <p><b>Fora da contagem de cenarios.</b> Nenhum cenario congelado exercita
 * {@code 429}, {@code Retry-After} ou {@code traceId} — a varredura da suite
 * devolve apenas a assercao de {@code Retry-After} do {@code 503} de SCN-001.3.
 * Os criterios de aceite 1 a 4 e 6 da task nao tinham, portanto, verificacao
 * alguma; estes testes existem para que passem a ter, e nao para acrescentar
 * cenario. Nada de {@code .feature}, step definition ou assercao congelada foi
 * tocado. Mesmo lugar e mesma razao de {@code ExistenciaECapacidadeIT} e
 * {@code AusenciaDeNMaisUmIT}.
 *
 * <p>Os envelopes sao apertados por propriedade em vez de exercitados nos 120/30
 * de RNF-010: o mecanismo verificado e o mesmo, e disparar 121 requisicoes por
 * teste custaria minutos de suite para provar exatamente a mesma coisa. O numero
 * de producao esta no padrao de {@code SegurancaConfig} e nao aqui.
 *
 * <p>O relogio e controlado. Com o relogio do sistema, a janela pode virar no
 * meio de um teste e ele passa a depender do instante em que a suite roda.
 */
@TestPropertySource(properties = {
    "idsd.limite.leituras-por-sujeito=3",
    "idsd.limite.escritas-por-sujeito=2",
    "idsd.limite.leituras-por-origem=5",
    "idsd.limite.escritas-por-origem=4"
})
class ErrosELimitesIT extends TesteDeIntegracao {

    /** Substitui o relogio da aplicacao; ver {@link RelogioParado}. */
    @TestConfiguration
    static class RelogioDeTeste {
        @Bean
        @Primary
        Clock relogioParado() {
            return RELOGIO;
        }
    }

    private static final RelogioParado RELOGIO =
            new RelogioParado(Instant.parse("2026-09-11T10:00:00Z"));

    /**
     * Avanca a janela antes de cada teste.
     *
     * <p>Os contadores vivem no contexto da aplicacao e o contexto e reusado entre
     * testes — sem virar a janela, o consumo de um teste seria herdado pelo
     * seguinte e o resultado passaria a depender da ordem das classes.
     */
    @BeforeEach
    void virarAJanela() {
        RELOGIO.avancar(60);
    }

    @Test
    @DisplayName("rota nao mapeada e negada, sem pilha e sem 200")
    void rotaNaoMapeadaENegada() throws Exception {
        mockMvc.perform(get("/v1/isto-nao-existe").with(ana()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                // Pilha, nome de classe e mensagem de excecao nao saem no corpo:
                // e por ali que desenho interno vaza sem que ninguem decida.
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.detail",
                        Matchers.not(Matchers.containsString("Exception"))));
    }

    @Test
    @DisplayName("recusa sem token sai no mesmo formato, com traceId")
    void recusaSemTokenSaiEmProblemJson() throws Exception {
        mockMvc.perform(get("/v1/projetos"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(Matchers.startsWith("https://errors.idsd/")))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("o identificador de correlacao do cliente volta no cabecalho e no corpo")
    void correlacaoDoClienteAtravessaARequisicao() throws Exception {
        String correlacao = "correlacao-de-teste-1";

        var resposta = mockMvc.perform(get("/v1/isto-nao-existe")
                        .header("X-Correlation-Id", correlacao)
                        .with(ana()))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", correlacao))
                .andExpect(jsonPath("$.traceId").value(correlacao))
                .andReturn();

        // O mesmo identificador no cabecalho e no corpo e o que torna o par
        // acionavel: e ele que a pessoa cita no chamado e que se procura no log.
        assertEquals(correlacao,
                resposta.getResponse().getHeader("X-Correlation-Id"));
    }

    @Test
    @DisplayName("sem cabecalho do cliente, a resposta ainda traz um traceId proprio")
    void traceIdEGeradoQuandoOClienteNaoManda() throws Exception {
        var resposta = mockMvc.perform(get("/v1/isto-nao-existe").with(ana()))
                .andExpect(status().isNotFound())
                .andReturn();

        assertNotNull(resposta.getResponse().getHeader("X-Correlation-Id"));
    }

    @Test
    @DisplayName("a leitura seguinte ao teto no mesmo minuto devolve 429 com Retry-After")
    void leituraAlemDoTetoRecebe429() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/v1/projetos").with(ana()))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/v1/projetos").with(ana()))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        // Virada a janela, o mesmo sujeito volta a ser atendido: o limite contem
        // rajada, nao bane quem a fez.
        RELOGIO.avancar(60);
        mockMvc.perform(get("/v1/projetos").with(ana()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a escrita seguinte ao teto no mesmo minuto devolve 429 com Retry-After")
    void escritaAlemDoTetoRecebe429() throws Exception {
        // O caminho nao precisa existir: a contagem acontece antes do despacho, e
        // e isso que faz o limite proteger tambem a rota que ainda vai nascer.
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/v1/isto-nao-existe").with(ana()))
                    .andExpect(status().isNotFound());
        }

        mockMvc.perform(post("/v1/isto-nao-existe").with(ana()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("leitura e escrita contam separado")
    void leituraNaoConsomeOEnvelopeDaEscrita() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/v1/projetos").with(ana()))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/v1/projetos").with(ana()))
                .andExpect(status().isTooManyRequests());

        // O envelope de escrita esta intacto: sao custos diferentes, e um contador
        // unico deixaria a escrita se esconder atras da folga da leitura.
        mockMvc.perform(post("/v1/isto-nao-existe").with(ana()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("o limite tambem vale por origem, alem do sujeito")
    void origemTemEnvelopeProprio() throws Exception {
        // Ana consome 3 leituras — exatamente o teto dela, nenhuma recusa.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/v1/projetos").with(ana()))
                    .andExpect(status().isOk());
        }
        // Bruno vem da mesma origem e esta dentro do proprio teto nas tres, mas a
        // origem so comporta 5. A sexta e recusada sem que sujeito algum tenha
        // estourado o envelope dele.
        mockMvc.perform(get("/v1/projetos").with(bruno()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/projetos").with(bruno()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/projetos").with(bruno()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    /** Relogio que so anda quando o teste manda. */
    static final class RelogioParado extends Clock {

        private final AtomicReference<Instant> agora;

        RelogioParado(Instant inicio) {
            this.agora = new AtomicReference<>(inicio);
        }

        void avancar(long segundos) {
            agora.updateAndGet(instante -> instante.plusSeconds(segundos));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora.get();
        }
    }
}
