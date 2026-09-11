package br.com.idsd.kanban.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/**
 * O corpo de erro do sistema, em {@code application/problem+json} (RFC 7807).
 *
 * <p>Existe um formato so, e ele vale para toda falha de todo contrato: as
 * chaves sao {@code type}, {@code title}, {@code status}, {@code detail},
 * {@code instance} e {@code traceId} (TechSpec Secao 4). Quem escreve erro no
 * sistema passa por aqui — o tratador global, os dois pontos de recusa da cadeia
 * de seguranca e o limite de requisicoes —, porque formato de erro montado em
 * cada lugar diverge em silencio e o cliente descobre a divergencia em producao.
 *
 * <p><b>O {@code detail} fala a linguagem do negocio.</b> Mensagem tecnica ali
 * vaza desenho interno, nao ajuda quem le e, no caso das recusas de alcance,
 * entrega na propria recusa o que ela existe para proteger (SCN-002.3): nenhum
 * dado do recurso negado entra no corpo.
 *
 * <p>O {@code traceId} nao e gerado aqui. Ele vem do {@link FiltroDeCorrelacao},
 * que o fixa no MDC no inicio da requisicao — e por isso o identificador do corpo
 * e o mesmo que aparece na linha de log, que e o que torna o par acionavel.
 */
public final class ProblemaDetalhado {

    /** Cabecalho de correlacao, aceito na entrada e devolvido na saida. */
    public static final String CABECALHO_CORRELACAO = "X-Correlation-Id";

    /** Chave do identificador de correlacao, no MDC e no corpo de erro. */
    public static final String CHAVE_TRACE = "traceId";

    private static final String PREFIXO_TIPO = "https://errors.idsd/";

    private ProblemaDetalhado() {
    }

    /**
     * Monta o corpo de erro completo, com {@code traceId} ja preenchido.
     *
     * @param slug identificador estavel do tipo de erro; vira {@code type}
     * @param instancia caminho do recurso pedido; vira {@code instance}
     */
    public static ProblemDetail de(
            HttpStatusCode status,
            String slug,
            String titulo,
            String detalhe,
            String instancia) {
        ProblemDetail corpo = ProblemDetail.forStatusAndDetail(status, detalhe);
        corpo.setType(URI.create(PREFIXO_TIPO + slug));
        corpo.setTitle(titulo);
        // O caminho vem do cliente e `URI.create` levanta em caminho malformado.
        // Levantar daqui seria levantar de dentro do tratador de ultimo recurso,
        // trocando o corpo padronizado pela pagina de erro do contêiner —
        // exatamente na requisicao que ja deu errado (ACH-12). `instance` e campo
        // informativo: omiti-lo custa menos do que perder o corpo inteiro.
        if (instancia != null && !instancia.isBlank()) {
            try {
                corpo.setInstance(URI.create(instancia));
            } catch (IllegalArgumentException malformado) {
                corpo.setProperty("instancePath", instancia);
            }
        }
        return comTraceId(corpo);
    }

    /**
     * Garante o {@code traceId} num corpo que ja existe.
     *
     * <p>Serve tambem aos corpos montados por controlador — e por isso o
     * enriquecimento e central e nao obrigacao de cada rota: rota que esquecer de
     * pedir o identificador produz erro que ninguem consegue casar com o log, e o
     * esquecimento nao quebra nada que se perceba.
     */
    public static ProblemDetail comTraceId(ProblemDetail corpo) {
        if (corpo.getProperties() == null
                || !corpo.getProperties().containsKey(CHAVE_TRACE)) {
            corpo.setProperty(CHAVE_TRACE, traceId());
        }
        return corpo;
    }

    /**
     * O identificador da requisicao em curso.
     *
     * <p><b>Quando nao ha, cunha um e o fixa no MDC</b> em vez de devolver um
     * valor novo a cada chamada (ACH-02 da revisao de TASK-01.6). A versao
     * anterior divergia em silencio no caminho que mais importa: no despacho de
     * erro do contêiner o MDC esta vazio, o tratador consulta o identificador uma
     * vez para a linha de log e outra para o corpo, e os dois saiam diferentes —
     * ambos com cara de validos, e nenhum casando com o outro. Fixar torna a
     * segunda chamada da mesma requisicao concordante com a primeira, que e a
     * unica propriedade que faz o par ser acionavel.
     */
    public static String traceId() {
        String doContexto = MDC.get(CHAVE_TRACE);
        if (doContexto != null && !doContexto.isBlank()) {
            return doContexto;
        }
        String cunhado = UUID.randomUUID().toString();
        MDC.put(CHAVE_TRACE, cunhado);
        return cunhado;
    }

    /**
     * Escreve o corpo direto na resposta.
     *
     * <p>Necessario para quem recusa <b>antes</b> do dispatcher — a cadeia de
     * seguranca e o limite de requisicoes —, onde nao ha tratador de excecao nem
     * conversao de retorno de metodo.
     */
    public static void escrever(
            HttpServletResponse resposta,
            ProblemDetail corpo,
            ObjectMapper conversor) throws IOException {
        resposta.setStatus(corpo.getStatus());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        conversor.writeValue(resposta.getOutputStream(), corpo);
    }

    /**
     * Falha de negocio que o tratador global sabe traduzir, com <b>ponto de
     * extensao</b> para o que o corpo padrao nao cobre.
     *
     * <p>O caso que obriga a extensao e o {@code 409}: o envelope de escrita
     * exige que a resposta carregue um bloco {@code estadoAtual} com etapa,
     * condicao, responsavel e versao correntes, porque quem perdeu a corrida
     * precisa saber <b>o que aconteceu</b> e nao apenas que houve erro (SCN-005.3,
     * SCN-007.3, SCN-020.3). Esse bloco e propriedade do dominio da tarefa e nasce
     * numa task posterior; serializa-lo aqui, num corpo fixo, obrigaria a reabrir
     * este arquivo para acrescentar campo de outro dominio. Por isso o corpo e
     * extensivel e nao ampliado: quem levanta a falha anexa o bloco com
     * {@link #com(String, Object)}.
     */
    public static class Falha extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 1L;

        private final transient HttpStatusCode status;
        private final String slug;
        private final String titulo;
        private final transient Map<String, Object> extensoes = new LinkedHashMap<>();

        public Falha(HttpStatusCode status, String slug, String titulo, String detalhe) {
            super(detalhe);
            this.status = status;
            this.slug = slug;
            this.titulo = titulo;
        }

        /** Anexa um membro adicional ao corpo de erro. */
        public Falha com(String nome, Object valor) {
            extensoes.put(nome, valor);
            return this;
        }

        public HttpStatusCode status() {
            return status;
        }

        /** O corpo correspondente, ja com as extensoes anexadas. */
        public ProblemDetail comoCorpo(String instancia) {
            ProblemDetail corpo = de(status, slug, titulo, getMessage(), instancia);
            extensoes.forEach(corpo::setProperty);
            return corpo;
        }
    }
}
