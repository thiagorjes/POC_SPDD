package br.com.idsd.kanban.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * O tratador global: toda resposta de erro sai daqui no mesmo formato.
 *
 * <p>Existe um formato so — {@code application/problem+json} com {@code traceId}
 * — e ele nao e obrigacao de cada rota. Rota que monta o proprio corpo diverge em
 * silencio das demais, e a divergencia so aparece no cliente.
 *
 * <p><b>Ele nao engole o conflito de estado.</b> {@link ProblemaDetalhado.Falha}
 * carrega membros adicionais que este arquivo nao conhece, e e por ai que o bloco
 * {@code estadoAtual} do {@code 409} entra quando a task que o define chegar —
 * serializar aqui um corpo fixo obrigaria a reabrir este arquivo para acrescentar
 * campo de outro dominio, e o corpo do conflito e justamente o que diz a quem
 * perdeu a corrida o que aconteceu.
 *
 * <p><b>Rota nao mapeada e negada.</b> Ela sai em {@code 404} com o mesmo
 * {@code detail} de qualquer recurso inexistente, sem pilha, sem pagina de erro
 * do contêiner e sem dizer que caminho existe — distinguir "rota que nao existe"
 * de "recurso que voce nao alcanca" devolveria ao cliente um mapa das rotas.
 *
 * <p>O que nao foi previsto sai em {@code 500} com {@code detail} generico, e a
 * excecao vai inteira para o log com o {@code traceId}. Mensagem de excecao no
 * corpo e o vazamento mais comum que existe — ela costuma trazer SQL, nome de
 * classe e, no pior caso, o dado que a recusa protegia.
 *
 * <p>O {@link ResponseBodyAdvice} fecha o unico buraco que sobraria: corpo de
 * erro montado por controlador nao passa por tratador nenhum. Enriquecer no
 * caminho de saida faz o {@code traceId} valer para toda resposta
 * {@link ProblemDetail} do sistema, inclusive as que ainda serao escritas.
 */
@RestControllerAdvice
public class TratadorDeErro extends ResponseEntityExceptionHandler
        implements ResponseBodyAdvice<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorDeErro.class);

    /** Falha de negocio declarada, com o corpo que quem a levantou montou. */
    @ExceptionHandler(ProblemaDetalhado.Falha.class)
    ResponseEntity<ProblemDetail> falhaDeNegocio(
            ProblemaDetalhado.Falha falha, HttpServletRequest requisicao) {
        LOG.info("Requisicao recusada em {} {} com {}; traceId={}",
                requisicao.getMethod(), requisicao.getRequestURI(),
                falha.status().value(), ProblemaDetalhado.traceId());
        return resposta(falha.comoCorpo(requisicao.getRequestURI()));
    }

    /**
     * Parametro que o contrato nao conhece, ou valor que ele nao aceita.
     *
     * <p>Fica em {@code 400} e nao em {@code 500}: e defeito da requisicao, e
     * responder erro interno faria o cliente retentar o que nunca vai funcionar.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> requisicaoInvalida(
            IllegalArgumentException invalida, HttpServletRequest requisicao) {
        LOG.info("Requisicao invalida em {} {}; traceId={}",
                requisicao.getMethod(), requisicao.getRequestURI(),
                ProblemaDetalhado.traceId(), invalida);
        return resposta(ProblemaDetalhado.de(
                HttpStatus.BAD_REQUEST,
                "requisicao-invalida",
                "Requisicao invalida",
                "Os dados enviados nao puderam ser lidos. Confira o conteudo da requisicao.",
                requisicao.getRequestURI()));
    }

    /**
     * O que nao foi previsto.
     *
     * <p>A excecao vai inteira para o log, e nada dela vai para o corpo. E a mesma
     * razao que faz o projeto de terceiro responder {@code 404} sem dizer o nome:
     * o texto de uma excecao e o lugar de onde o dado protegido escapa sem que
     * ninguem tenha decidido publica-lo.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> naoPrevisto(Exception erro, HttpServletRequest requisicao) {
        LOG.error("Falha nao prevista em {} {}; traceId={}",
                requisicao.getMethod(), requisicao.getRequestURI(),
                ProblemaDetalhado.traceId(), erro);
        return resposta(ProblemaDetalhado.de(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "erro-interno",
                "Erro interno",
                "Nao foi possivel concluir a operacao. Tente novamente em instantes.",
                requisicao.getRequestURI()));
    }

    /** Caminho que nao corresponde a rota nem a recurso estatico algum. */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException naoEncontrada,
            HttpHeaders cabecalhos,
            HttpStatusCode status,
            WebRequest requisicao) {
        return naoEncontrado(requisicao);
    }

    /** Caminho sem handler, quando o despachante e configurado para levantar. */
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException semHandler,
            HttpHeaders cabecalhos,
            HttpStatusCode status,
            WebRequest requisicao) {
        return naoEncontrado(requisicao);
    }

    /** Corpo ilegivel: JSON quebrado, tipo incompativel, campo com valor absurdo. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ilegivel,
            HttpHeaders cabecalhos,
            HttpStatusCode status,
            WebRequest requisicao) {
        LOG.info("Corpo ilegivel em {}; traceId={}",
                caminho(requisicao), ProblemaDetalhado.traceId());
        return corpo(ProblemaDetalhado.de(
                HttpStatus.BAD_REQUEST,
                "requisicao-invalida",
                "Requisicao invalida",
                "Os dados enviados nao puderam ser lidos. Confira o conteudo da requisicao.",
                caminho(requisicao)));
    }

    /**
     * As demais excecoes que o proprio Spring MVC ja traduz — metodo nao
     * suportado, tipo de midia, validacao. Elas ja chegam como
     * {@link ProblemDetail}; o que falta e o {@code traceId} e o tipo de conteudo,
     * e e so isso que se acrescenta.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception excecao,
            Object corpo,
            HttpHeaders cabecalhos,
            HttpStatusCode status,
            WebRequest requisicao) {
        Object enriquecido = corpo instanceof ProblemDetail problema
                ? ProblemaDetalhado.comTraceId(problema)
                : corpo;
        LOG.info("Requisicao recusada com {} pelo tratamento padrao; traceId={}",
                status.value(), ProblemaDetalhado.traceId());
        HttpHeaders comTipo = new HttpHeaders();
        comTipo.putAll(cabecalhos);
        comTipo.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return super.handleExceptionInternal(excecao, enriquecido, comTipo, status, requisicao);
    }

    private ResponseEntity<Object> naoEncontrado(WebRequest requisicao) {
        LOG.info("Caminho nao mapeado: {}; traceId={}",
                caminho(requisicao), ProblemaDetalhado.traceId());
        return corpo(ProblemaDetalhado.de(
                HttpStatus.NOT_FOUND,
                "recurso-nao-encontrado",
                "Recurso nao encontrado",
                "O recurso pedido nao existe ou nao esta disponivel para voce.",
                caminho(requisicao)));
    }

    private String caminho(WebRequest requisicao) {
        return requisicao instanceof org.springframework.web.context.request.ServletWebRequest servlet
                ? servlet.getRequest().getRequestURI()
                : null;
    }

    private ResponseEntity<Object> corpo(ProblemDetail problema) {
        return ResponseEntity.status(problema.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }

    private ResponseEntity<ProblemDetail> resposta(ProblemDetail problema) {
        return ResponseEntity.status(problema.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }

    // --- enriquecimento dos corpos montados fora daqui ---

    @Override
    public boolean supports(MethodParameter retorno, Class<? extends HttpMessageConverter<?>> conversor) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object corpo,
            MethodParameter retorno,
            MediaType tipo,
            Class<? extends HttpMessageConverter<?>> conversor,
            ServerHttpRequest requisicao,
            ServerHttpResponse resposta) {
        return corpo instanceof ProblemDetail problema
                ? ProblemaDetalhado.comTraceId(problema)
                : corpo;
    }
}
