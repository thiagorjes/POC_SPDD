package br.com.idsd.kanban.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite de requisicoes por minuto, por sujeito e por origem (RNF-010).
 *
 * <p>Pesa mais aqui do que num sistema comum, e por uma razao de desenho: cada
 * escrita aceita dispara difusao de evento para todas as instancias e todas as
 * sessoes (ADR-004), entao o custo de uma requisicao abusiva e amplificado, e os
 * envelopes de RNF-001 e RNF-002 nao tem outra protecao — nao ha gateway na
 * Secao 6 de onde herdar throttling.
 *
 * <p><b>Leitura e escrita contam separado.</b> Sao envelopes diferentes porque
 * sao custos diferentes: leitura consulta a projecao e para ali, escrita grava
 * evento e acorda todo mundo. Um contador unico deixaria a escrita se esconder
 * atras da folga da leitura, que e onde o custo esta.
 *
 * <p><b>Duas dimensoes, com forcas diferentes.</b> O sujeito e a dimensao firme:
 * sai do {@code sub} de um token ja verificado e nao pode ser forjado. A origem e
 * uma rede grossa e deliberadamente folgada — atras de proxy reverso, que e a
 * unica entrada do sistema na topologia do compose, um endereco costuma ser a
 * saida compartilhada de um time inteiro, e igualar os dois envelopes faria o
 * escritorio inteiro caber no orcamento de uma pessoa. Ela existe para conter
 * rajada de um host, nao para policiar egresso compartilhado.
 *
 * <p>Requisicao sem token nao chega aqui contada: o filtro roda depois da
 * autenticacao e quem nao se autenticou ja e recusado com {@code 401}. Conter
 * rajada anonima e trabalho da borda, e nao ha o que atribuir a ninguem antes de
 * haver sujeito.
 *
 * <p>A janela e fixa de um minuto, e o {@code Retry-After} diz quantos segundos
 * faltam para ela virar. Recusar sem dizer quando repetir converte a protecao em
 * laco de tentativa, que custa mais do que o abuso que ela contem.
 */
public class LimiteDeRequisicoes extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LimiteDeRequisicoes.class);

    /** Metodos que apenas leem. O resto conta como escrita. */
    private static final Set<String> LEITURA = Set.of("GET", "HEAD", "OPTIONS");

    private static final long JANELA_SEGUNDOS = 60L;

    /**
     * Teto de chaves vivas. Uma chave e um par (sujeito ou origem, tipo) do
     * minuto corrente; o mapa e podado quando cresce, porque memoria que so cresce
     * transforma a protecao contra abuso na propria forma de derrubar a instancia.
     */
    private static final int TETO_DE_CHAVES = 20_000;

    private final Envelope envelope;
    private final ObjectMapper conversor;
    private final Clock relogio;
    private final Map<String, Contagem> contagens = new ConcurrentHashMap<>();

    public LimiteDeRequisicoes(Envelope envelope, ObjectMapper conversor, Clock relogio) {
        this.envelope = envelope;
        this.conversor = conversor;
        this.relogio = relogio;
    }

    /**
     * Os quatro tetos, por minuto. Os dois primeiros sao RNF-010; os dois ultimos
     * sao a rede grossa da origem, ajustaveis por configuracao.
     */
    public record Envelope(
            int leiturasPorSujeito,
            int escritasPorSujeito,
            int leiturasPorOrigem,
            int escritasPorOrigem) {
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao,
            HttpServletResponse resposta,
            FilterChain cadeia) throws ServletException, IOException {
        String sujeito = sujeitoAutenticado();
        if (sujeito == null) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }

        boolean leitura = LEITURA.contains(requisicao.getMethod());
        long minuto = Math.floorDiv(relogio.instant().getEpochSecond(), JANELA_SEGUNDOS);

        int tetoDoSujeito = leitura ? envelope.leiturasPorSujeito() : envelope.escritasPorSujeito();
        int tetoDaOrigem = leitura ? envelope.leiturasPorOrigem() : envelope.escritasPorOrigem();

        boolean excedeu =
                excedeu("s|" + sujeito + "|" + leitura, minuto, tetoDoSujeito)
                        // Sempre as duas: interromper na primeira deixaria a
                        // segunda dimensao sem contagem no minuto em que a
                        // primeira estourou, e ela voltaria zerada em seguida.
                        | excedeu("o|" + origem(requisicao) + "|" + leitura, minuto, tetoDaOrigem);

        if (excedeu) {
            recusar(requisicao, resposta, minuto);
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    /** Registra mais uma requisicao na chave e diz se ela passou do teto. */
    private boolean excedeu(String chave, long minuto, int teto) {
        podarSeNecessario(minuto);
        Contagem contagem = contagens.compute(chave, (ignorado, atual) ->
                atual == null || atual.minuto() != minuto
                        ? new Contagem(minuto, 1)
                        : new Contagem(minuto, atual.quantas() + 1));
        return contagem.quantas() > teto;
    }

    private void podarSeNecessario(long minuto) {
        if (contagens.size() <= TETO_DE_CHAVES) {
            return;
        }
        contagens.values().removeIf(contagem -> contagem.minuto() < minuto);
    }

    /**
     * O {@code sub} do token, ou {@code null} se ninguem se autenticou.
     *
     * <p>Sai do token verificado e nunca de cabecalho: chave de contagem que o
     * cliente escolhe e contorno de um comando.
     */
    private String sujeitoAutenticado() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao instanceof JwtAuthenticationToken token
                ? token.getToken().getSubject()
                : null;
    }

    /**
     * O endereco de origem, olhando o primeiro salto de {@code X-Forwarded-For}.
     *
     * <p>O cabecalho e forjavel, e por isso a origem e a dimensao fraca das duas —
     * ela e uma rede grossa, e o que nao pode ser forjado e o sujeito. Ignora-lo
     * seria pior: atras do proxy reverso, que e a unica entrada do sistema, toda
     * requisicao teria o mesmo endereco e a dimensao nao distinguiria nada.
     */
    private String origem(HttpServletRequest requisicao) {
        String encaminhado = requisicao.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            String primeiro = encaminhado.split(",")[0].trim();
            if (!primeiro.isEmpty()) {
                // Limitado no comprimento: e entrada do cliente e vira chave de
                // mapa, entao valor livre ali e consumo de memoria por requisicao.
                return primeiro.length() > 64 ? primeiro.substring(0, 64) : primeiro;
            }
        }
        String remoto = requisicao.getRemoteAddr();
        return remoto == null ? "desconhecida" : remoto;
    }

    private void recusar(HttpServletRequest requisicao, HttpServletResponse resposta, long minuto)
            throws IOException {
        long faltam = (minuto + 1) * JANELA_SEGUNDOS - relogio.instant().getEpochSecond();
        String esperar = String.valueOf(Math.max(faltam, 1L));

        LOG.warn("Limite de requisicoes excedido em {} {}; recusado com 429, traceId={}",
                requisicao.getMethod(), requisicao.getRequestURI(), ProblemaDetalhado.traceId());

        ProblemDetail corpo = ProblemaDetalhado.de(
                HttpStatus.TOO_MANY_REQUESTS,
                "limite-de-requisicoes",
                "Requisicoes demais",
                "Voce fez requisicoes demais em pouco tempo. Aguarde "
                        + esperar + " segundos e tente novamente.",
                requisicao.getRequestURI());
        resposta.setHeader(HttpHeaders.RETRY_AFTER, esperar);
        ProblemaDetalhado.escrever(resposta, corpo, conversor);
    }

    /** Quantas requisicoes uma chave fez no minuto indicado. */
    private record Contagem(long minuto, int quantas) {
    }
}
