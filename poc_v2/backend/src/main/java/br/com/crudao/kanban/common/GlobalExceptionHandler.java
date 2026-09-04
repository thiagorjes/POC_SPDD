package br.com.crudao.kanban.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traducao unica de excecao para {@link ErrorResponse}. Nenhuma resposta expoe stack trace, SQL,
 * nome de constraint/tabela ou identificador interno de infraestrutura.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String MENSAGEM_ERRO_INTERNO =
      "Ocorreu um erro inesperado ao processar a requisicao.";
  private static final String MENSAGEM_INTEGRIDADE =
      "A operacao viola uma regra de integridade dos dados.";

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(
      BusinessException ex, HttpServletRequest request) {
    log.warn("Regra de negocio violada [{}]: {}", ex.getErrorCode(), ex.getMessage());
    return ResponseEntity.status(ex.getHttpStatus())
        .body(
            ErrorResponse.of(
                ex.getErrorCode(), ex.getMessage(), request.getRequestURI(), ex.getCampos()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<CampoErro> campos =
        ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::paraCampoErro)
            .toList();
    log.warn(
        "Validacao de entrada rejeitada em {} ({} campo(s))",
        request.getRequestURI(),
        campos.size());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ErrorResponse.of(
                "VALIDACAO_ENTRADA",
                "Ha campos invalidos na requisicao.",
                request.getRequestURI(),
                campos));
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handleOptimisticLock(
      ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
    log.warn("Conflito de concorrencia em {}", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ErrorResponse.of(
                "CONFLITO_CONCORRENCIA",
                "O registro foi alterado por outro usuario. Recarregue e tente novamente.",
                request.getRequestURI()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Violacao de integridade em {}", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ErrorResponse.of(
                "VIOLACAO_INTEGRIDADE", MENSAGEM_INTEGRIDADE, request.getRequestURI()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    log.warn("Acesso negado em {}", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ErrorResponse.of(
                "PERMISSAO_NEGADA",
                "Voce nao possui permissao para executar esta acao.",
                request.getRequestURI()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    String correlationId = UUID.randomUUID().toString();
    log.error("Erro interno [correlationId={}] em {}", correlationId, request.getRequestURI(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("ERRO_INTERNO", MENSAGEM_ERRO_INTERNO, request.getRequestURI()));
  }

  private static CampoErro paraCampoErro(FieldError erro) {
    String mensagem =
        erro.getDefaultMessage() == null ? "Valor invalido." : erro.getDefaultMessage();
    return new CampoErro(erro.getField(), mensagem);
  }
}
