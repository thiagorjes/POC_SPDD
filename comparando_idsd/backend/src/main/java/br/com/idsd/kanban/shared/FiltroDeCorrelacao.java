package br.com.idsd.kanban.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Da a cada requisicao um identificador de correlacao, e o mantem no MDC.
 *
 * <p>E o que faz o {@code traceId} do corpo de erro e a linha de log falarem da
 * mesma requisicao. Sem isso o corpo de erro traz um identificador que nao existe
 * em lugar nenhum, e a pessoa que abre o chamado cita um numero que ninguem
 * consegue procurar.
 *
 * <p>Roda <b>antes de toda a cadeia de seguranca</b>, e nao depois: as recusas
 * mais frequentes — {@code 401} sem token, {@code 503} com o provedor fora do ar,
 * {@code 429} no limite de requisicoes — sao produzidas dentro da cadeia, e um
 * filtro posterior nao as alcancaria. Justamente os erros que mais precisam de
 * correlacao sairiam sem ela.
 *
 * <p>O identificador vindo do cliente e <b>aceito e conferido</b>, nunca aceito
 * cru. Ele vai para a linha de log e para o corpo da resposta, entao valor
 * arbitrario ali e injecao em log: quebra de linha forja registro, e comprimento
 * livre entope o arquivo. Fora do formato, gera-se um novo em silencio — recusar
 * a requisicao seria transformar cabecalho decorativo em pre-condicao de entrada.
 */
public class FiltroDeCorrelacao extends OncePerRequestFilter {

    /** Formato aceito do cabecalho: o suficiente para UUID e para id de tracing. */
    private static final Pattern ACEITAVEL = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao,
            HttpServletResponse resposta,
            FilterChain cadeia) throws ServletException, IOException {
        String correlacao = resolver(requisicao, resposta);
        MDC.put(ProblemaDetalhado.CHAVE_TRACE, correlacao);
        resposta.setHeader(ProblemaDetalhado.CABECALHO_CORRELACAO, correlacao);
        try {
            cadeia.doFilter(requisicao, resposta);
        } finally {
            // O contêiner reusa a thread. Deixar a chave para tras faz a proxima
            // requisicao herdar a correlacao da anterior, que e pior do que nao
            // ter correlacao nenhuma: o log fica confiantemente errado.
            MDC.remove(ProblemaDetalhado.CHAVE_TRACE);
        }
    }

    /**
     * <b>Tambem no despacho de erro e no assincrono</b> (ACH-10 da revisao de
     * TASK-01.6). Os dois sao passagens proprias do contêiner, e
     * {@link OncePerRequestFilter} as dispensa por padrao: o MDC ja teria sido
     * limpo no {@code finally} da passagem original quando o tratador de ultimo
     * recurso monta o {@code 500}, e a linha de log do stacktrace — a que mais
     * precisa de correlacao — sairia sem ela.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * O identificador desta requisicao, o mesmo em toda passagem do contêiner.
     *
     * <p>A resposta e consultada antes do cliente porque ela e o unico estado que
     * sobrevive entre as passagens: no despacho de erro o cabecalho ja foi escrito
     * pela passagem original, e ignora-lo cunharia um segundo identificador para a
     * mesma requisicao — dois valores com cara de validos, nenhum casando com o
     * outro, que e exatamente o defeito que a correlacao existe para nao ter.
     */
    private String resolver(HttpServletRequest requisicao, HttpServletResponse resposta) {
        String jaEmitido = resposta.getHeader(ProblemaDetalhado.CABECALHO_CORRELACAO);
        if (aceitavel(jaEmitido)) {
            return jaEmitido;
        }
        String doCliente = requisicao.getHeader(ProblemaDetalhado.CABECALHO_CORRELACAO);
        return aceitavel(doCliente) ? doCliente : UUID.randomUUID().toString();
    }

    private boolean aceitavel(String valor) {
        return valor != null && ACEITAVEL.matcher(valor).matches();
    }
}
