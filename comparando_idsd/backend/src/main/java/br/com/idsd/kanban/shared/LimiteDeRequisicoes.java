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
 * Limite de requisicoes por minuto, <b>por sujeito autenticado</b> (RNF-010).
 *
 * <p>Pesa mais aqui do que num sistema comum, e por uma razao de desenho: cada
 * escrita aceita dispara difusao de evento para todas as instancias e todas as
 * sessoes (ADR-004), entao o custo de uma requisicao abusiva e amplificado, e os
 * envelopes de RNF-001 e RNF-002 nao tem outra protecao.
 *
 * <p><b>Leitura e escrita contam separado.</b> Sao envelopes diferentes porque
 * sao custos diferentes: leitura consulta a projecao e para ali, escrita grava
 * evento e acorda todo mundo. Um contador unico deixaria a escrita se esconder
 * atras da folga da leitura, que e onde o custo esta.
 *
 * <p><b>Uma dimensao so, e ela e o sujeito.</b> Houve uma segunda, por endereco
 * de origem, e ela foi removida (ACH-01 e ACH-05 da revisao de TASK-01.6). A
 * justificativa que ela tinha — "atras do proxy reverso, que e a unica entrada do
 * sistema" — era falsa contra o {@code docker/compose.yaml}, que publica o
 * backend direto: o unico proxy do repositorio existe no arnes de broadcast de
 * {@code compose.test.yaml}. Sem proxy que o escreva, {@code X-Forwarded-For} e
 * escolhido pelo cliente, e disso saiam tres defeitos ao mesmo tempo — a dimensao
 * nao continha nada (trocar o cabecalho zerava a contagem), permitia recusar
 * servico a terceiros (queimar o envelope de uma origem alheia) e fazia o mapa
 * crescer sem teto real. Mais decisivo que os tres: a condicao de medicao de
 * RNF-010 exige provar <b>que o consumo de um sujeito nao afeta a resposta de
 * outro</b>, e envelope compartilhado por origem afirma o contrario.
 *
 * <p>O sujeito e a dimensao que resiste: sai do {@code sub} de um token ja
 * verificado e nao pode ser forjado. Com ela sozinha, a cardinalidade do mapa e
 * limitada pelo numero de sujeitos que se autenticam no minuto — nao mais por
 * valor que o cliente inventa.
 *
 * <p>Requisicao sem token nao chega aqui contada: o filtro roda depois da
 * autenticacao e quem nao se autenticou ja e recusado com {@code 401}. Conter
 * rajada anonima e trabalho da borda, e nao ha o que atribuir a ninguem antes de
 * haver sujeito.
 *
 * <p>A janela e fixa de um minuto, e o {@code Retry-After} diz quantos segundos
 * faltam para ela virar. Recusar sem dizer quando repetir converte a protecao em
 * laco de tentativa, que custa mais do que o abuso que ela contem.
 *
 * <p><b>A contagem e por instancia</b>, e o desenho preve tres (RNF-002): o
 * envelope efetivo e multiplicado pelo numero de replicas. Esta declarado na
 * TechSpec Secao 8, e nao e defeito escondido — contador compartilhado exigiria
 * armazenamento que ADR-002 recusa nesta fase.
 */
public class LimiteDeRequisicoes extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LimiteDeRequisicoes.class);

    /** Metodos que apenas leem. O resto conta como escrita. */
    private static final Set<String> LEITURA = Set.of("GET", "HEAD", "OPTIONS");

    private static final long JANELA_SEGUNDOS = 60L;

    /**
     * A partir de quantas chaves vivas o mapa e varrido.
     *
     * <p>Uma chave e um par (sujeito, tipo) do minuto corrente, e todo sujeito
     * saiu de um token verificado — nao ha como um cliente criar chaves a vontade.
     * A varredura existe mesmo assim porque memoria que so cresce transforma a
     * protecao contra abuso na propria forma de derrubar a instancia, e sujeito
     * que entrou uma vez e nunca mais voltou nao deve ficar no mapa para sempre.
     */
    private static final int LIMIAR_DE_PODA = 20_000;

    private final Envelope envelope;
    private final ObjectMapper conversor;
    private final Clock relogio;
    private final Map<String, Contagem> contagens = new ConcurrentHashMap<>();

    public LimiteDeRequisicoes(Envelope envelope, ObjectMapper conversor, Clock relogio) {
        this.envelope = envelope;
        this.conversor = conversor;
        this.relogio = relogio;
    }

    /** Os dois tetos de RNF-010, por minuto e por sujeito. */
    public record Envelope(int leiturasPorSujeito, int escritasPorSujeito) {
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
        int teto = leitura ? envelope.leiturasPorSujeito() : envelope.escritasPorSujeito();

        if (excedeu(sujeito + "|" + leitura, minuto, teto)) {
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
        if (contagens.size() <= LIMIAR_DE_PODA) {
            return;
        }
        contagens.values().removeIf(contagem -> contagem.minuto() < minuto);
    }

    /**
     * O {@code sub} do token, ou {@code null} se ninguem se autenticou.
     *
     * <p>Sai do token verificado e nunca de cabecalho: chave de contagem que o
     * cliente escolhe e contorno de um comando — foi exatamente o que derrubou a
     * dimensao de origem.
     */
    private String sujeitoAutenticado() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao instanceof JwtAuthenticationToken token
                ? token.getToken().getSubject()
                : null;
    }

    private void recusar(HttpServletRequest requisicao, HttpServletResponse resposta, long minuto)
            throws IOException {
        long faltam = (minuto + 1) * JANELA_SEGUNDOS - relogio.instant().getEpochSecond();
        String esperar = String.valueOf(Math.max(faltam, 1L));

        LOG.warn("Limite de requisicoes excedido em {} {}; recusado com 429",
                requisicao.getMethod(), requisicao.getRequestURI());

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
