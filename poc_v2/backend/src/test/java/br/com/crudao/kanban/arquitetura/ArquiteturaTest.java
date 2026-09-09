package br.com.crudao.kanban.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import br.com.crudao.kanban.KanbanApplication;
import br.com.crudao.kanban.security.ExigePermissao;
import br.com.crudao.kanban.security.PermissaoGuard;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras estruturais que nao sobrevivem so em revisao humana (bloco 19.4). A autorizacao vive na
 * Service Layer (A-15): e la que a regra e verificada, nao no Controller.
 */
class ArquiteturaTest {

  /**
   * Servicos alcancados diretamente por Controller — a fronteira de autorizacao. Servicos
   * colaboradores internos (lead time, auditoria, evento) sao chamados por eles e nao repetem a
   * checagem.
   */
  private static final Set<String> SERVICOS_DE_FRONTEIRA =
      Set.of(
          "ProjetoService",
          "TarefaService",
          "RaiaService",
          "WorkflowService",
          "EtapaService",
          "TransicaoService",
          "BoardQueryService",
          "DashboardService",
          "UsuarioProjetoPapelService");

  /**
   * Excecao unica e justificada: {@code ProjetoService.criar} acontece antes de existir projeto,
   * logo nao ha escopo para {@code @ExigePermissao}; a checagem de admin global e feita no proprio
   * metodo (ADR-007). {@code NotificacaoService} nao entra na lista de fronteira porque e
   * autorizado por posse (o destinatario e o usuario autenticado), sem projeto no escopo.
   */
  private static final Set<String> ESCRITAS_SEM_ESCOPO_DE_PROJETO = Set.of("ProjetoService.criar");

  private static JavaClasses classes;

  @BeforeAll
  static void importar() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(KanbanApplication.class);
  }

  @Test
  @DisplayName("metodo de escrita de service de fronteira exige permissao (A-15/A-16)")
  void escritaDeServicoDeFronteiraEAutorizada() {
    methods()
        .that(
            new DescribedPredicate<JavaMethod>("sao escrita em service de fronteira") {
              @Override
              public boolean test(JavaMethod metodo) {
                String assinatura = metodo.getOwner().getSimpleName() + "." + metodo.getName();
                return SERVICOS_DE_FRONTEIRA.contains(metodo.getOwner().getSimpleName())
                    && !ESCRITAS_SEM_ESCOPO_DE_PROJETO.contains(assinatura)
                    && metodo.getModifiers().contains(JavaModifier.PUBLIC)
                    && escrita(metodo);
              }
            })
        .should(exigirAutorizacao())
        .check(classes);
  }

  @Test
  @DisplayName("service nao depende do adaptador concreto de evento, apenas da porta")
  void serviceNaoDependeDoAdaptadorDeEvento() {
    noClasses()
        .that()
        .haveSimpleNameEndingWith("Service")
        .should()
        .dependOnClassesThat()
        .haveSimpleNameStartingWith("ListenNotify")
        .because(
            "a publicacao de evento e consumida pela porta EventoBoardPublisher; o adaptador"
                + " LISTEN/NOTIFY e detalhe de infraestrutura (ADR-004)")
        .allowEmptyShould(true)
        .check(classes);

    noClasses()
        .that()
        .haveSimpleNameEndingWith("Service")
        .should()
        .dependOnClassesThat()
        .haveSimpleName("BoardEventLoop")
        .allowEmptyShould(true)
        .check(classes);
  }

  @Test
  @DisplayName("campo boolean nao usa duas maiusculas apos o prefixo (Norms 6)")
  void booleanSemDuasMaiusculasAposPrefixo() {
    fields()
        .that()
        .haveRawType(boolean.class)
        .should(
            new ArchCondition<JavaField>("ter nome introspectavel por JavaBeans") {
              @Override
              public void check(JavaField campo, ConditionEvents eventos) {
                String nome = campo.getName();
                boolean quebrado =
                    nome.length() >= 2
                        && Character.isLowerCase(nome.charAt(0))
                        && Character.isUpperCase(nome.charAt(1));
                if (quebrado) {
                  eventos.add(
                      SimpleConditionEvent.violated(
                          campo,
                          "o campo boolean "
                              + campo.getFullName()
                              + " tem duas maiusculas apos o prefixo: a introspecao JavaBeans"
                              + " quebra Jackson/MapStruct silenciosamente"));
                }
              }
            })
        .check(classes);
  }

  @Test
  @DisplayName("controller nao abre transacao: a fronteira transacional e a service (Norms 9)")
  void controllerNaoAbreTransacao() {
    noClasses()
        .that()
        .haveSimpleNameEndingWith("Controller")
        .should()
        .beAnnotatedWith(Transactional.class)
        .check(classes);
  }

  @Test
  @DisplayName("repository nunca e usado diretamente por controller")
  void controllerNaoUsaRepository() {
    noClasses()
        .that()
        .haveSimpleNameEndingWith("Controller")
        .should()
        .dependOnClassesThat()
        .haveSimpleNameEndingWith("Repository")
        .check(classes);
  }

  private static boolean escrita(JavaMethod metodo) {
    return metodo
        .tryGetAnnotationOfType(Transactional.class)
        .map(anotacao -> !anotacao.readOnly())
        .orElse(false);
  }

  private static ArchCondition<JavaMethod> exigirAutorizacao() {
    return new ArchCondition<>("estar anotado com @ExigePermissao ou chamar PermissaoGuard") {
      @Override
      public void check(JavaMethod metodo, ConditionEvents eventos) {
        boolean anotado = metodo.isAnnotatedWith(ExigePermissao.class);
        boolean chamaGuard =
            metodo.getCallsFromSelf().stream()
                .map(chamada -> chamada.getTargetOwner())
                .anyMatch(alvo -> alvo.isEquivalentTo(PermissaoGuard.class));
        if (!anotado && !chamaGuard) {
          eventos.add(
              SimpleConditionEvent.violated(
                  metodo,
                  "metodo de escrita sem autorizacao: "
                      + metodo.getFullName()
                      + " nao possui @ExigePermissao nem chama PermissaoGuard (A-15/A-16)"));
        }
      }
    };
  }
}
