package br.com.crudao.kanban.integracao;

import br.com.crudao.kanban.projeto.Projeto;
import br.com.crudao.kanban.projeto.ProjetoService;
import br.com.crudao.kanban.projeto.dto.CriarProjetoRequest;
import br.com.crudao.kanban.raia.Raia;
import br.com.crudao.kanban.raia.RaiaService;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioProjetoPapelService;
import br.com.crudao.kanban.security.UsuarioAtualProvider;
import br.com.crudao.kanban.workflow.Etapa;
import br.com.crudao.kanban.workflow.EtapaService;
import br.com.crudao.kanban.workflow.TransicaoService;
import br.com.crudao.kanban.workflow.Workflow;
import br.com.crudao.kanban.workflow.WorkflowService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Montagem de cenario compartilhada pelos testes de integracao e pelos passos BDD (blocos 19.2 e
 * 19.3). Constroi o estado pelos proprios services, para que o teste exercite as mesmas regras que
 * a aplicacao executa em producao, e nao um atalho de repositorio.
 */
@Component
@RequiredArgsConstructor
public class CenarioDeTeste {

  /** Mesmo e-mail configurado em {@code application-test.yml}: promove a admin global (ADR-007). */
  public static final String EMAIL_ADMIN = "admin@kanban.test";

  private static final List<String> TABELAS_DE_DADOS =
      List.of(
          "notificacao",
          "auditoria_tarefa",
          "periodo_impedimento",
          "periodo_etapa",
          "tarefa_observador",
          "tarefa",
          "transicao",
          "etapa",
          "workflow",
          "raia",
          "sequencia_projeto",
          "projeto_toggle",
          "usuario_projeto_papel",
          "projeto",
          "usuario");

  private final JdbcTemplate jdbcTemplate;
  private final UsuarioAtualProvider usuarioAtualProvider;
  private final ProjetoService projetoService;
  private final WorkflowService workflowService;
  private final EtapaService etapaService;
  private final TransicaoService transicaoService;
  private final RaiaService raiaService;
  private final UsuarioProjetoPapelService usuarioProjetoPapelService;

  /** Limpa apenas os dados; o catalogo de papeis e permissoes vem das migrations e permanece. */
  public void limpar() {
    SecurityContextHolder.clearContext();
    jdbcTemplate.execute(
        "TRUNCATE TABLE " + String.join(", ", TABELAS_DE_DADOS) + " RESTART IDENTITY CASCADE");
  }

  /** Autentica com um token sintetico e provisiona o usuario just-in-time (ADR-003). */
  public Usuario autenticar(String email) {
    Jwt jwt =
        Jwt.withTokenValue("token-de-teste")
            .header("alg", "none")
            .subject("sub-" + email)
            .claim("email", email)
            .claim("name", email)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    return usuarioAtualProvider.atual();
  }

  public Usuario autenticarComoAdmin() {
    return autenticar(EMAIL_ADMIN);
  }

  public void associarPapel(UUID projetoId, Usuario usuario, String codigoPapel, Usuario autor) {
    usuarioProjetoPapelService.associar(projetoId, usuario.getId(), codigoPapel, autor);
  }

  /**
   * Projeto ativo com workflow ativo de tres etapas encadeadas (A Fazer -> Fazendo -> Concluido) e
   * a raia padrao criada junto com o projeto.
   */
  public Cenario criarProjetoCompleto(String nome, Usuario admin) {
    Projeto projeto = projetoService.criar(new CriarProjetoRequest(nome, null), admin);
    Workflow workflow = workflowService.criar(projeto.getId(), "Principal");
    Etapa aFazer = etapaService.criar(workflow.getId(), "A Fazer", false);
    Etapa fazendo = etapaService.criar(workflow.getId(), "Fazendo", false);
    Etapa concluido = etapaService.criar(workflow.getId(), "Concluido", true);
    transicaoService.criar(workflow.getId(), aFazer.getId(), fazendo.getId());
    // A saida da etapa final nao e uma transicao do grafo: "desfinalizar" volta para a
    // predecessora direta e e governado por RN-004/RN-011.
    transicaoService.criar(workflow.getId(), fazendo.getId(), concluido.getId());
    Raia raia = raiaService.listar(projeto.getId()).getFirst();
    return new Cenario(projeto, workflow, aFazer, fazendo, concluido, raia);
  }

  /** Estado montado, exposto para os testes sem consultas auxiliares. */
  public record Cenario(
      Projeto projeto,
      Workflow workflow,
      Etapa aFazer,
      Etapa fazendo,
      Etapa concluido,
      Raia raia) {}
}
