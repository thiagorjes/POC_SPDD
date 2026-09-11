package br.com.idsd.kanban.shared;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
        LOG.info("Requisicao recusada em {} {} com {}",
                requisicao.getMethod(), requisicao.getRequestURI(),
                falha.status().value());
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
        LOG.info("Requisicao invalida em {} {}",
                requisicao.getMethod(), requisicao.getRequestURI(), invalida);
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
     *
     * <p><b>Recusa de acesso nao passa por aqui</b>, e a excecao e declarada e nao
     * acidental (ACH-03 da revisao de TASK-01.6). {@link AccessDeniedException} e
     * {@link AuthenticationException} sao traduzidas pelo
     * {@code ExceptionTranslationFilter} da cadeia de seguranca, que produz
     * {@code 403} e {@code 401}; capturadas aqui, elas nunca chegariam la e a
     * negacao sairia como {@code 500}. Relancar e o que mantem os dois codigos
     * corretos no dia em que houver seguranca de metodo — que hoje nao ha, e e por
     * isso que este cuidado seria facil de esquecer ate deixar de ser barato.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> naoPrevisto(Exception erro, HttpServletRequest requisicao)
            throws Exception {
        if (erro instanceof AccessDeniedException || erro instanceof AuthenticationException) {
            throw erro;
        }
        LOG.error("Falha nao prevista em {} {}",
                requisicao.getMethod(), requisicao.getRequestURI(), erro);
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

    /**
     * Corpo que nao pode ser lido, ou campo cujo valor o contrato nao aceita.
     *
     * <p><b>A linha entre {@code 400} e {@code 422} e onde o corpo para de ser
     * ilegivel e passa a ser inaceitavel</b> (ACH-04 da revisao de TASK-01.8). JSON
     * quebrado ou ausente nao chega a dizer nada: e {@code 400}, e e o que
     * SCN-004.2 congela. Ja um campo bem posicionado com valor que nao converte —
     * um {@code primeiroAdministradorId} que nao e UUID — <b>disse</b> alguma
     * coisa, e o que ha e recusa de conteudo: sai em {@code 422}, com o campo
     * nomeado, o mesmo codigo que o contrato promete para esse campo ausente ou sem
     * {@code usuario} correspondente. Sem essa distincao o cliente recebia dois
     * codigos para a mesma classe de erro, decidida por um detalhe do desserializador.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ilegivel,
            HttpHeaders cabecalhos,
            HttpStatusCode status,
            WebRequest requisicao) {
        String campo = campoIncompativel(ilegivel);
        if (campo != null) {
            LOG.info("Valor incompativel em {} no campo {}", caminho(requisicao), campo);
            ProblemDetail problema = ProblemaDetalhado.de(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "entrada-invalida",
                    "Entrada invalida",
                    "Os dados enviados nao atendem ao contrato. Confira os campos indicados.",
                    caminho(requisicao));
            problema.setProperty("errors", List.of(Map.of(
                    "campo", campo,
                    "mensagem", "O valor informado nao e valido para este campo.")));
            return corpo(problema);
        }
        LOG.info("Corpo ilegivel em {}", caminho(requisicao));
        return corpo(ProblemaDetalhado.de(
                HttpStatus.BAD_REQUEST,
                "requisicao-invalida",
                "Requisicao invalida",
                "Os dados enviados nao puderam ser lidos. Confira o conteudo da requisicao.",
                caminho(requisicao)));
    }

    /**
     * Validacao de fronteira: {@code 422}, com os campos nomeados.
     *
     * <p>{@code 422} e nao {@code 400} porque e o codigo que os contratos congelam
     * para entrada recusada — nome em branco, papel fora do catalogo, motivo vazio.
     * O padrao do Spring MVC e {@code 400}, e deixa-lo faria a rota que valida por
     * anotacao responder diferente da rota que valida no servico, para o mesmo erro.
     *
     * <p>Os campos vao em {@code errors} porque a tela precisa saber <b>onde</b> por
     * a mensagem (RNF-003); {@code detail} sozinho obriga o cliente a adivinhar.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException invalido,
            HttpHeaders cabecalhos,
            HttpStatusCode status,
            WebRequest requisicao) {
        LOG.info("Entrada invalida em {}", caminho(requisicao));
        ProblemDetail problema = ProblemaDetalhado.de(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "entrada-invalida",
                "Entrada invalida",
                "Os dados enviados nao atendem ao contrato. Confira os campos indicados.",
                caminho(requisicao));
        List<Map<String, String>> campos = new ArrayList<>();
        for (FieldError erro : invalido.getBindingResult().getFieldErrors()) {
            campos.add(Map.of(
                    "campo", erro.getField(),
                    "mensagem", erro.getDefaultMessage() == null
                            ? "Valor invalido." : erro.getDefaultMessage()));
        }
        problema.setProperty("errors", campos);
        return corpo(problema);
    }

    /**
     * O campo cujo valor nao converteu, se foi isso que aconteceu.
     *
     * <p>Devolve {@code null} quando o corpo nem chegou a ser JSON valido — a
     * causa, ai, nao tem caminho de campo nenhum a apontar.
     */
    private String campoIncompativel(HttpMessageNotReadableException ilegivel) {
        if (!(ilegivel.getCause() instanceof MismatchedInputException incompativel)
                || incompativel.getPath().isEmpty()) {
            return null;
        }
        String caminhoDoCampo = incompativel.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(nome -> nome != null)
                .collect(Collectors.joining("."));
        return caminhoDoCampo.isEmpty() ? null : caminhoDoCampo;
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
        LOG.info("Requisicao recusada com {} pelo tratamento padrao", status.value());
        HttpHeaders comTipo = new HttpHeaders();
        comTipo.putAll(cabecalhos);
        comTipo.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return super.handleExceptionInternal(excecao, enriquecido, comTipo, status, requisicao);
    }

    private ResponseEntity<Object> naoEncontrado(WebRequest requisicao) {
        LOG.info("Caminho nao mapeado: {}", caminho(requisicao));
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
