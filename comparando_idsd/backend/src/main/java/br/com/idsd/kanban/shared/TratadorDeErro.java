package br.com.idsd.kanban.shared;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
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
import org.springframework.transaction.TransactionTimedOutException;
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

    /**
     * Segundos sugeridos no {@code Retry-After} da espera esgotada.
     *
     * <p>Mais curto que o {@code lock_timeout} de proposito: quem recebeu esta
     * resposta esperou o teto inteiro e a disputa ja deve ter terminado.
     */
    private static final int SEGUNDOS_ATE_NOVA_TENTATIVA = 2;

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
     *
     * <p><b>A pilha nao vai para {@code INFO}</b> (ACH-16 da reexecucao de
     * TASK-02.2). Este caminho e alcancavel por qualquer cliente, e pilha em nivel
     * que a producao mantem ligado e um custo que o cliente escolhe: basta repetir
     * a requisicao malformada para encher o log. Em {@code INFO} fica a mensagem,
     * que e o que serve para contar ocorrencia; a pilha fica em {@code DEBUG}, que
     * e onde se liga quando ha um caso concreto a diagnosticar.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> requisicaoInvalida(
            IllegalArgumentException invalida, HttpServletRequest requisicao) {
        LOG.info("Requisicao invalida em {} {}: {}",
                requisicao.getMethod(), requisicao.getRequestURI(), invalida.getMessage());
        LOG.debug("Requisicao invalida em {} {}",
                requisicao.getMethod(), requisicao.getRequestURI(), invalida);
        return resposta(ProblemaDetalhado.de(
                HttpStatus.BAD_REQUEST,
                "requisicao-invalida",
                "Requisição inválida",
                "Os dados enviados não puderam ser lidos. Confira o conteúdo da requisição.",
                requisicao.getRequestURI()));
    }

    /**
     * Restricoes de banco que o produto pode violar <b>por entrada</b>, traduzidas
     * uma a uma.
     *
     * <p>Cada restricao dessas ja tem recusa de borda propria — a unicidade de ordem
     * de etapa, por exemplo, sai em {@code 422} antes de qualquer escrita. Este
     * tratador e a rede por baixo daquilo (ACH-02 da revisao de TASK-02.2):
     * restricao sem tradutor e um {@code 500} esperando acontecer, e {@code 500}
     * convida o cliente a retentar o que nunca vai funcionar.
     *
     * <p><b>O catalogo e nominal de proposito.</b> Traduzir toda
     * {@link DataIntegrityViolationException} em {@code 409} apagaria a diferenca
     * entre "o pedido conflita com o estado" e "o banco recusou algo que ninguem
     * previu" — e o segundo caso e defeito, tem de sair em {@code 5xx} e e o que
     * {@code TransacaoUnicaDeCriacaoIT} fixa. Restricao nova entra nesta lista junto
     * com a rota que pode viola-la.
     *
     * <p>A mensagem do driver fica no log e nao no corpo: ela traz nome de indice,
     * de coluna e o proprio valor recusado.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integridadeViolada(
            DataIntegrityViolationException conflito, HttpServletRequest requisicao)
            throws Exception {
        if (!"etapa_projeto_ordem_unico".equals(restricaoViolada(conflito))) {
            return naoPrevisto(conflito, requisicao);
        }
        LOG.warn("Restricao de integridade violada em {} {}",
                requisicao.getMethod(), requisicao.getRequestURI(), conflito);
        return resposta(ProblemaDetalhado.de(
                HttpStatus.CONFLICT,
                "conflito-de-ordem-de-etapa",
                "Conflito na posição das etapas",
                "A configuração conflita com o fluxo que o projeto tem agora. Recarregue "
                        + "a tela e tente de novo.",
                requisicao.getRequestURI()));
    }

    /**
     * A espera pelo bloqueio acabou antes de o bloqueio vir.
     *
     * <p>ACH-01 da reexecucao de TASK-02.2. Com o teto de espera configurado
     * (`lock_timeout` na sessao, `@Transactional(timeout)` na rota), a espera longa
     * deixa de pendurar a requisicao e passa a **terminar**. O que ela termina
     * precisa ter codigo proprio: sem este tratador, o teto que acabou de nascer
     * apareceria como {@code 500}, e trocar indisponibilidade por defeito aparente
     * nao e conserto.
     *
     * <p>{@code 503} e nao {@code 409}: nada no pedido esta errado e nada precisa
     * ser recarregado — outra configuracao do mesmo projeto esta em curso, e daqui
     * a instantes nao estara. {@code Retry-After} diz isso em numero, do mesmo
     * modo que o limite de requisicoes e a indisponibilidade do provedor de
     * identidade ja dizem.
     *
     * <p>Continua sendo {@code 5xx}, o que preserva a exigencia de
     * {@code TransacaoUnicaDeCriacaoIT} — a suite congelada que recusa ver falha
     * imprevista de banco disfarcada de conflito.
     */
    @ExceptionHandler({
        PessimisticLockingFailureException.class,
        QueryTimeoutException.class,
        TransactionTimedOutException.class
    })
    ResponseEntity<ProblemDetail> esperaEsgotada(
            Exception espera, HttpServletRequest requisicao) {
        LOG.warn("Espera por bloqueio esgotada em {} {}",
                requisicao.getMethod(), requisicao.getRequestURI(), espera);
        ProblemDetail problema = ProblemaDetalhado.de(
                HttpStatus.SERVICE_UNAVAILABLE,
                "espera-por-bloqueio-esgotada",
                "Configuração em curso",
                "Outra alteração deste projeto está sendo gravada agora. "
                        + "Aguarde alguns segundos e tente de novo. O fluxo atual "
                        + "do projeto não foi alterado.",
                requisicao.getRequestURI());
        return ResponseEntity.status(problema.getStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(SEGUNDOS_ATE_NOVA_TENTATIVA))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }

    /** O nome da restricao violada, quando o driver o informa. */
    private String restricaoViolada(DataIntegrityViolationException conflito) {
        for (Throwable causa = conflito; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao) {
                return violacao.getConstraintName();
            }
        }
        return null;
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
                "Não foi possível concluir a operação. Tente novamente em instantes.",
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
                    "Entrada inválida",
                    "Os dados enviados não atendem ao contrato. Confira os campos indicados.",
                    caminho(requisicao));
            problema.setProperty("errors", List.of(Map.of(
                    "campo", campo,
                    "mensagem", "O valor informado não é válido para este campo.")));
            return corpo(problema);
        }
        LOG.info("Corpo ilegivel em {}", caminho(requisicao));
        return corpo(ProblemaDetalhado.de(
                HttpStatus.BAD_REQUEST,
                "requisicao-invalida",
                "Requisição inválida",
                "Os dados enviados não puderam ser lidos. Confira o conteúdo da requisição.",
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
                "Entrada inválida",
                "Os dados enviados não atendem ao contrato. Confira os campos indicados.",
                caminho(requisicao));
        List<Map<String, String>> campos = new ArrayList<>();
        for (FieldError erro : invalido.getBindingResult().getFieldErrors()) {
            campos.add(Map.of(
                    "campo", erro.getField(),
                    "mensagem", erro.getDefaultMessage() == null
                            ? "Valor inválido." : erro.getDefaultMessage()));
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
                "Recurso não encontrado",
                "O recurso pedido não existe ou não está disponível para você.",
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
