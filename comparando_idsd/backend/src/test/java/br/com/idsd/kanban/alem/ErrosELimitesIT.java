package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
    "idsd.limite.escritas-por-sujeito=2"
})
@ExtendWith(OutputCaptureExtension.class)
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
    @DisplayName("sem cabecalho do cliente, cabecalho e corpo trazem o mesmo traceId")
    void traceIdEGeradoQuandoOClienteNaoManda() throws Exception {
        var resposta = mockMvc.perform(get("/v1/isto-nao-existe").with(ana()))
                .andExpect(status().isNotFound())
                .andReturn();

        String doCabecalho = resposta.getResponse().getHeader("X-Correlation-Id");
        assertNotNull(doCabecalho);
        // A igualdade e o que se verifica, e nao a mera existencia (ACH-08 da
        // revisao de TASK-01.6): dois identificadores validos e diferentes na
        // mesma resposta passariam por "nao nulo" e quebrariam exatamente o que a
        // correlacao existe para dar. O caminho sem cabecalho do cliente e o unico
        // em que os dois sao cunhados independentemente, e por isso e o unico em
        // que a divergencia pode nascer.
        assertEquals(doCabecalho,
                jsonPathTexto(resposta.getResponse().getContentAsString(), "traceId"));
    }

    @Test
    @DisplayName("o corpo montado por controlador tambem sai enriquecido com traceId")
    void corpoDeControladorERecebeTraceId() throws Exception {
        // `GET /v1/projetos/{id}` monta o proprio ProblemDetail e nao passa pelo
        // tratador de excecao — e o unico caminho que exercita o ResponseBodyAdvice
        // (ACH-06). Sem ele, rota que monta o proprio corpo produziria erro que
        // ninguem casa com o log, e o esquecimento nao quebraria nada visivel.
        var resposta = mockMvc.perform(
                        get("/v1/projetos/00000000-0000-0000-0000-000000000000").with(ana()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andReturn();

        assertEquals(resposta.getResponse().getHeader("X-Correlation-Id"),
                jsonPathTexto(resposta.getResponse().getContentAsString(), "traceId"));
    }

    @Test
    @DisplayName("metodo nao suportado sai no mesmo formato, com traceId")
    void metodoNaoSuportadoSaiEmProblemJson() throws Exception {
        // O 405 vem do tratamento padrao do Spring MVC, que monta o ProblemDetail
        // sozinho — outro caminho de saida, e o criterio 1 fala de formato unico
        // para todo erro e nao para os erros que este arquivo constroi (ACH-07).
        //
        // O verbo era POST ate TASK-01.8, quando `POST /v1/projetos` passou a
        // existir e este teste comecou a medir o corpo ausente (400) em vez do
        // metodo. DELETE na colecao nao esta previsto em rota nenhuma do produto.
        mockMvc.perform(delete("/v1/projetos").with(ana()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("o traceId da resposta aparece na linha de log da mesma requisicao")
    void traceIdDaRespostaCasaComOLog(CapturedOutput saida) throws Exception {
        String correlacao = "correlacao-de-log-1";

        mockMvc.perform(get("/v1/isto-nao-existe")
                        .header("X-Correlation-Id", correlacao)
                        .with(ana()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").value(correlacao));

        // Criterio de aceite 6, que ate a revisao estava marcado cumprido sem
        // verificacao alguma. O identificador chega ao log pelo padrao `%X{traceId}`
        // e nao por concatenacao em cada chamada: o que se afirma aqui e que a
        // linha emitida durante a requisicao o carrega, e nao que alguem lembrou de
        // escreve-lo naquela mensagem.
        assertTrue(saida.getOut().contains(correlacao),
                "a linha de log da requisicao deveria carregar o traceId da resposta");
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
                // O valor, e nao a mera existencia (ACH-09). `Retry-After: 0` ou um
                // numero que nao corresponde a janela satisfaz "existe" e converte a
                // protecao em laco de tentativa, que e o modo de falha que o
                // cabecalho existe para evitar. O relogio esta parado no segundo 0
                // do minuto, entao faltam os 60 segundos inteiros.
                .andExpect(header().string("Retry-After", "60"))
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
    @DisplayName("o consumo de um sujeito nao afeta a resposta de outro")
    void sujeitosNaoSeContaminam() throws Exception {
        // Esta e a condicao de medicao de RNF-010, escrita no PRD, e ela era
        // afirmada ao contrario ate a revisao de TASK-01.6: havia uma dimensao por
        // origem, e o teste anterior declarava como correto que bruno recebesse 429
        // sem ter estourado o proprio envelope (ACH-05). Removida a origem, o
        // requisito volta a estar integro — e passa a ser este teste que o prova.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/v1/projetos").with(ana()))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/v1/projetos").with(ana()))
                .andExpect(status().isTooManyRequests());

        // Bruno vem da mesma origem e nao consumiu nada. O envelope dele esta
        // inteiro, inclusive a leitura que empataria com o teto de ana.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/v1/projetos").with(bruno()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("cabecalho de origem forjado nao altera a contagem")
    void origemForjadaNaoContaNada() throws Exception {
        // A dimensao de origem confiava em `X-Forwarded-For`, e nao ha proxy
        // reverso que o escreva na topologia do compose — trocar o valor a cada
        // requisicao zerava a contagem (ACH-01). Sem a dimensao, o cabecalho e
        // inerte: o teto do sujeito vale igual, mande ele o que mandar.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/v1/projetos")
                            .header("X-Forwarded-For", "10.0.0." + i)
                            .with(ana()))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/v1/projetos")
                        .header("X-Forwarded-For", "10.0.0.99")
                        .with(ana()))
                .andExpect(status().isTooManyRequests());
    }

    /** Le um campo de texto da raiz do corpo, sem depender de biblioteca extra. */
    private static String jsonPathTexto(String corpo, String campo) {
        var achado = java.util.regex.Pattern
                .compile("\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(corpo);
        return achado.find() ? achado.group(1) : null;
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
