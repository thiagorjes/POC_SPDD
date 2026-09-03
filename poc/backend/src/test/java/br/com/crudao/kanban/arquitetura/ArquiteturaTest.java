package br.com.crudao.kanban.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import br.com.crudao.kanban.security.ExigePermissao;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import java.util.regex.Pattern;

/** Invariantes estruturais que a revisao humana nao pega de forma confiavel. */
@AnalyzeClasses(
    packages = "br.com.crudao.kanban",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

  private static final Set<String> ANOTACOES_ESCRITA =
      Set.of(
          "org.springframework.web.bind.annotation.PostMapping",
          "org.springframework.web.bind.annotation.PutMapping",
          "org.springframework.web.bind.annotation.PatchMapping",
          "org.springframework.web.bind.annotation.DeleteMapping");

  /**
   * Handlers de escrita que legitimamente nao tem escopo de projeto estatico. Lista fechada: cada
   * entrada tem a justificativa documentada no Javadoc do proprio metodo.
   *
   * <ul>
   *   <li>{@code ProjetoController.criar} — nao existe projeto ao qual dar escopo; a checagem de
   *       {@code adminGlobal} vive na Service.
   *   <li>{@code TarefaController.atribuir} — a permissao exigida depende de quem esta sendo
   *       atribuido, decidido em runtime (RN-012).
   *   <li>{@code NotificacaoController.marcarLida} — o escopo e o proprio destinatario, nao um
   *       projeto; a Service rejeita id de outro usuario.
   * </ul>
   */
  private static final Set<String> EXCECOES_JUSTIFICADAS =
      Set.of(
          "ProjetoController.criar",
          "TarefaController.atribuir",
          "NotificacaoController.marcarLida");

  /** Nenhum handler de escrita sem {@link ExigePermissao}, salvo as excecoes acima. */
  @ArchTest
  static final ArchRule handlerDeEscritaExigePermissao =
      methods()
          .that(
              new DescribedPredicate<JavaMethod>("sao handlers de escrita") {
                @Override
                public boolean test(JavaMethod metodo) {
                  return metodo.getOwner().getSimpleName().endsWith("Controller")
                      && metodo.getAnnotations().stream()
                          .anyMatch(
                              anotacao ->
                                  ANOTACOES_ESCRITA.contains(
                                      anotacao.getRawType().getFullName()));
                }
              })
          .should(
              new ArchCondition<JavaMethod>("declarar @ExigePermissao ou justificar a excecao") {
                @Override
                public void check(JavaMethod metodo, ConditionEvents eventos) {
                  boolean anotado = metodo.isAnnotatedWith(ExigePermissao.class);
                  String assinatura =
                      metodo.getOwner().getSimpleName() + "." + metodo.getName();
                  if (!anotado && !EXCECOES_JUSTIFICADAS.contains(assinatura)) {
                    eventos.add(
                        SimpleConditionEvent.violated(
                            metodo,
                            "Handler de escrita sem @ExigePermissao: " + metodo.getFullName()));
                  }
                }
              })
          .allowEmptyShould(true);

  /** A Service nao conhece o transporte do evento: ela publica pela porta, nunca pelo adaptador. */
  @ArchTest
  static final ArchRule serviceNaoConheceAdaptadorDeEvento =
      noClasses()
          .that()
          .haveSimpleNameEndingWith("Service")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("br.com.crudao.kanban.evento.adapter..");

  /**
   * Boolean com duas maiusculas apos o prefixo quebra a introspeccao JavaBeans e faz Jackson e
   * MapStruct perderem o campo silenciosamente (achado real da TASK-01.1).
   */
  @ArchTest
  static final ArchRule booleanSemDuasMaiusculasAposPrefixo =
      fields()
          .that()
          .haveRawType(boolean.class)
          .or()
          .haveRawType(Boolean.class)
          .should(
              new ArchCondition<>("nao ter duas maiusculas seguidas apos o prefixo") {
                private final Pattern proibido = Pattern.compile("^[a-z][A-Z][A-Z]");

                @Override
                public void check(JavaField campo, ConditionEvents eventos) {
                  if (proibido.matcher(campo.getName()).find()) {
                    eventos.add(
                        SimpleConditionEvent.violated(
                            campo,
                            "Campo boolean com duas maiusculas apos o prefixo: "
                                + campo.getFullName()));
                  }
                }
              })
          .allowEmptyShould(true);

  /** Controller nao abre transacao: a fronteira transacional e exclusivamente da Service. */
  @ArchTest
  static final ArchRule controllerNaoAbreTransacao =
      noClasses()
          .that()
          .haveSimpleNameEndingWith("Controller")
          .should()
          .beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class);
}
