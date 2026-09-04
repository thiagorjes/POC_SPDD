package br.com.crudao.kanban.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.ProjetoFinalizadoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.projeto.Projeto;
import br.com.crudao.kanban.projeto.ProjetoRepository;
import br.com.crudao.kanban.projeto.ProjetoToggleService;
import br.com.crudao.kanban.projeto.StatusProjeto;
import br.com.crudao.kanban.rbac.CodigoPapel;
import br.com.crudao.kanban.rbac.CodigoPermissao;
import br.com.crudao.kanban.rbac.PermissaoService;
import br.com.crudao.kanban.rbac.Usuario;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Autorizacao efetiva (bloco 19.1): papeis acumulados, bypass de admin global e ausencia de bypass
 * em projeto finalizado (RN-015).
 */
@ExtendWith(MockitoExtension.class)
class PermissaoGuardTest {

  private static final UUID PROJETO = UUID.randomUUID();

  @Mock private PermissaoService permissaoService;
  @Mock private ProjetoRepository projetoRepository;
  @Mock private ProjetoToggleService projetoToggleService;
  @Mock private UsuarioAtualProvider usuarioAtualProvider;

  @InjectMocks private PermissaoGuard permissaoGuard;

  private Usuario comum;
  private Usuario admin;

  @BeforeEach
  void preparar() {
    comum = Usuario.builder().id(UUID.randomUUID()).nome("Dev").email("d@x").build();
    admin =
        Usuario.builder()
            .id(UUID.randomUUID())
            .nome("Admin")
            .email("admin@x")
            .adminGlobal(true)
            .build();
  }

  private void usuarioAtual(Usuario usuario) {
    when(usuarioAtualProvider.atual()).thenReturn(usuario);
  }

  private Projeto projeto(StatusProjeto status) {
    Projeto projeto = new Projeto();
    projeto.setId(PROJETO);
    projeto.setNome("Projeto");
    projeto.setStatus(status);
    return projeto;
  }

  @Test
  @DisplayName("permissao concedida por qualquer papel do usuario passa")
  void permiteQuandoAlgumPapelConcede() {
    usuarioAtual(comum);
    when(permissaoService.permissoesEfetivas(comum.getId(), PROJETO))
        .thenReturn(Set.of(CodigoPermissao.PROJETO_VISUALIZAR, CodigoPermissao.TAREFA_MOVER));

    assertThatCode(() -> permissaoGuard.exigir(CodigoPermissao.TAREFA_MOVER, PROJETO))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("permissao ausente na uniao dos papeis e negada")
  void negaQuandoNenhumPapelConcede() {
    usuarioAtual(comum);
    when(permissaoService.permissoesEfetivas(comum.getId(), PROJETO))
        .thenReturn(Set.of(CodigoPermissao.PROJETO_VISUALIZAR));

    assertThatThrownBy(() -> permissaoGuard.exigir(CodigoPermissao.TAREFA_FINALIZAR, PROJETO))
        .isInstanceOf(PermissaoNegadaException.class)
        .hasMessage("Voce nao possui a permissao necessaria para esta acao neste projeto.");
  }

  @Test
  @DisplayName("admin global passa sem consultar as permissoes efetivas (ADR-007)")
  void adminGlobalTemBypass() {
    usuarioAtual(admin);

    assertThatCode(() -> permissaoGuard.exigir(CodigoPermissao.PROJETO_ADMINISTRAR, PROJETO))
        .doesNotThrowAnyException();
    verify(permissaoService, never()).permissoesEfetivas(any(), any());
  }

  @Test
  @DisplayName("admin global recebe o catalogo completo de permissoes efetivas")
  void adminGlobalRecebeCatalogoCompleto() {
    usuarioAtual(admin);

    assertThat(permissaoGuard.permissoesEfetivas(PROJETO))
        .containsExactlyInAnyOrderElementsOf(CatalogoPermissoes.TODAS);
  }

  @Test
  @DisplayName("projeto finalizado bloqueia escrita inclusive para admin global (RN-015)")
  void projetoFinalizadoNaoTemBypass() {
    when(projetoRepository.findById(PROJETO))
        .thenReturn(Optional.of(projeto(StatusProjeto.FINALIZADO)));

    assertThatThrownBy(() -> permissaoGuard.exigirProjetoAtivo(PROJETO))
        .isInstanceOf(ProjetoFinalizadoException.class)
        .hasMessage("O projeto esta finalizado e nao aceita alteracoes.");
    verify(usuarioAtualProvider, never()).atual();
  }

  @Test
  @DisplayName("projeto ativo passa na checagem de escrita")
  void projetoAtivoPassa() {
    when(projetoRepository.findById(PROJETO)).thenReturn(Optional.of(projeto(StatusProjeto.ATIVO)));

    assertThatCode(() -> permissaoGuard.exigirProjetoAtivo(PROJETO)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("projeto inexistente resulta em recurso nao encontrado")
  void projetoInexistente() {
    when(projetoRepository.findById(PROJETO)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> permissaoGuard.exigirProjetoAtivo(PROJETO))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  @DisplayName("toggle desabilitado nega quem depende exclusivamente do papel condicionante")
  void toggleNegaPapelExclusivo() {
    usuarioAtual(comum);
    when(permissaoService.papeis(comum.getId(), PROJETO)).thenReturn(Set.of(CodigoPapel.DEV));
    when(projetoToggleService.habilitado(PROJETO, ChaveToggle.DEV_PODE_EXCLUIR_TAREFA))
        .thenReturn(false);

    assertThatThrownBy(
            () ->
                permissaoGuard.exigirToggle(
                    ChaveToggle.DEV_PODE_EXCLUIR_TAREFA, PROJETO, CodigoPapel.DEV))
        .isInstanceOf(PermissaoNegadaException.class)
        .hasMessage("Esta acao esta desabilitada para o seu papel na configuracao do projeto.");
  }

  @Test
  @DisplayName("papel acumulado escapa do toggle: a restricao so vale para o papel exclusivo")
  void toggleNaoAtingePapelAcumulado() {
    usuarioAtual(comum);
    when(permissaoService.papeis(comum.getId(), PROJETO))
        .thenReturn(Set.of(CodigoPapel.DEV, CodigoPapel.PRODUCT_OWNER));

    assertThatCode(
            () ->
                permissaoGuard.exigirToggle(
                    ChaveToggle.DEV_PODE_EXCLUIR_TAREFA, PROJETO, CodigoPapel.DEV))
        .doesNotThrowAnyException();
    verify(projetoToggleService, never()).habilitado(any(), any());
  }

  @Test
  @DisplayName("toggle habilitado libera o papel condicionante")
  void toggleHabilitadoLibera() {
    usuarioAtual(comum);
    when(permissaoService.papeis(comum.getId(), PROJETO)).thenReturn(Set.of(CodigoPapel.DEV));
    when(projetoToggleService.habilitado(PROJETO, ChaveToggle.DEV_PODE_FINALIZAR_TAREFA))
        .thenReturn(true);

    assertThatCode(
            () ->
                permissaoGuard.exigirToggle(
                    ChaveToggle.DEV_PODE_FINALIZAR_TAREFA, PROJETO, CodigoPapel.DEV))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("admin global ignora o toggle condicionante")
  void adminGlobalIgnoraToggle() {
    usuarioAtual(admin);

    assertThatCode(
            () ->
                permissaoGuard.exigirToggle(
                    ChaveToggle.DEV_PODE_EXCLUIR_TAREFA, PROJETO, CodigoPapel.DEV))
        .doesNotThrowAnyException();
    verify(projetoToggleService, never()).habilitado(any(), any());
  }

  @Test
  @DisplayName("possui nao lanca e responde false para permissao ausente")
  void possuiNaoLanca() {
    usuarioAtual(comum);
    when(permissaoService.permissoesEfetivas(comum.getId(), PROJETO)).thenReturn(Set.of());

    assertThat(permissaoGuard.possui(CodigoPermissao.TAREFA_MOVER, PROJETO)).isFalse();
  }
}
