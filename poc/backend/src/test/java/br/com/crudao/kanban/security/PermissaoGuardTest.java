package br.com.crudao.kanban.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.ProjetoFinalizadoException;
import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.projeto.Projeto;
import br.com.crudao.kanban.projeto.ProjetoRepository;
import br.com.crudao.kanban.projeto.ProjetoToggle;
import br.com.crudao.kanban.projeto.ProjetoToggleRepository;
import br.com.crudao.kanban.projeto.StatusProjeto;
import br.com.crudao.kanban.rbac.Papeis;
import br.com.crudao.kanban.rbac.PermissaoService;
import br.com.crudao.kanban.rbac.Permissoes;
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

/** Papeis acumulados, bypass de admin global e ausencia de bypass em projeto finalizado. */
@ExtendWith(MockitoExtension.class)
class PermissaoGuardTest {

  private static final UUID PROJETO = UUID.randomUUID();

  @Mock private PermissaoService permissaoService;
  @Mock private ProjetoRepository projetoRepository;
  @Mock private ProjetoToggleRepository projetoToggleRepository;
  @Mock private UsuarioAtualProvider usuarioAtualProvider;

  @InjectMocks private PermissaoGuard guard;

  private Usuario comum;
  private Usuario adminGlobal;

  @BeforeEach
  void preparar() {
    comum = Usuario.builder().id(UUID.randomUUID()).nome("Ana").adminGlobal(false).build();
    adminGlobal = Usuario.builder().id(UUID.randomUUID()).nome("Root").adminGlobal(true).build();
  }

  @Test
  @DisplayName("permissoes de papeis diferentes se acumulam no mesmo projeto")
  void papeisAcumulam() {
    when(usuarioAtualProvider.obrigatorio()).thenReturn(comum);
    when(permissaoService.permissoesEfetivas(comum.getId(), PROJETO))
        .thenReturn(Set.of(Permissoes.TAREFA_MOVER, Permissoes.DASHBOARD_VISUALIZAR));

    assertThatCode(() -> guard.exigir(Permissoes.TAREFA_MOVER, PROJETO)).doesNotThrowAnyException();
    assertThat(guard.possui(Permissoes.DASHBOARD_VISUALIZAR, PROJETO)).isTrue();
    assertThat(guard.possui(Permissoes.PROJETO_ADMINISTRAR, PROJETO)).isFalse();
  }

  @Test
  @DisplayName("permissao ausente e negada com mensagem generica, sem citar recurso interno")
  void permissaoAusente() {
    when(usuarioAtualProvider.obrigatorio()).thenReturn(comum);
    when(permissaoService.permissoesEfetivas(comum.getId(), PROJETO)).thenReturn(Set.of());

    assertThatThrownBy(() -> guard.exigir(Permissoes.PROJETO_ADMINISTRAR, PROJETO))
        .isInstanceOf(PermissaoNegadaException.class)
        .hasMessage("Voce nao tem permissao para executar esta acao neste projeto.");
  }

  @Test
  @DisplayName("admin global passa sem consultar papeis do projeto")
  void bypassAdminGlobal() {
    when(usuarioAtualProvider.obrigatorio()).thenReturn(adminGlobal);

    guard.exigir(Permissoes.PROJETO_ADMINISTRAR, PROJETO);

    verifyNoInteractions(permissaoService);
    assertThat(guard.permissoesEfetivas(PROJETO)).isEqualTo(Permissoes.TODAS);
  }

  @Test
  @DisplayName("projeto finalizado bloqueia escrita inclusive para admin global (RN-015)")
  void projetoFinalizadoSemBypass() {
    when(projetoRepository.findById(PROJETO))
        .thenReturn(
            Optional.of(
                Projeto.builder()
                    .id(PROJETO)
                    .nome("X")
                    .status(StatusProjeto.FINALIZADO)
                    .build()));

    assertThatThrownBy(() -> guard.exigirProjetoAtivo(PROJETO))
        .isInstanceOf(ProjetoFinalizadoException.class)
        .hasMessage("O projeto esta finalizado e nao aceita alteracoes. Reabra o projeto para editar.");
    // Nenhuma consulta a usuario: nao existe caminho de bypass para checar.
    verify(usuarioAtualProvider, never()).obrigatorio();
  }

  @Test
  @DisplayName("toggle desabilitado nega o papel condicionante")
  void toggleNegaPapelCondicionante() {
    when(usuarioAtualProvider.obrigatorio()).thenReturn(comum);
    when(permissaoService.papeisNoProjeto(comum.getId(), PROJETO)).thenReturn(Set.of(Papeis.DEV));
    when(projetoToggleRepository.findByIdProjetoIdAndIdChave(
            PROJETO, ChaveToggle.DEV_PODE_EXCLUIR_TAREFA))
        .thenReturn(Optional.of(toggle(false)));

    assertThatThrownBy(
            () -> guard.exigirToggle(ChaveToggle.DEV_PODE_EXCLUIR_TAREFA, PROJETO, Papeis.DEV))
        .isInstanceOf(PermissaoNegadaException.class)
        .hasMessage("Esta acao esta desabilitada para o seu papel na configuracao do projeto.");
  }

  @Test
  @DisplayName("papel administrativo dispensa o toggle mesmo desabilitado")
  void administrativoDispensaToggle() {
    when(usuarioAtualProvider.obrigatorio()).thenReturn(comum);
    when(permissaoService.papeisNoProjeto(comum.getId(), PROJETO))
        .thenReturn(Set.of(Papeis.DEV, Papeis.PROJECT_ADMIN));

    assertThatCode(
            () -> guard.exigirToggle(ChaveToggle.DEV_PODE_EXCLUIR_TAREFA, PROJETO, Papeis.DEV))
        .doesNotThrowAnyException();
    verify(projetoToggleRepository, never()).findByIdProjetoIdAndIdChave(any(), any());
  }

  @Test
  @DisplayName("toggle sem registro cai no padrao declarado no enum")
  void toggleUsaPadraoDoEnum() {
    when(projetoToggleRepository.findByIdProjetoIdAndIdChave(
            PROJETO, ChaveToggle.DEV_PODE_FINALIZAR_TAREFA))
        .thenReturn(Optional.empty());

    assertThat(guard.toggleHabilitado(PROJETO, ChaveToggle.DEV_PODE_FINALIZAR_TAREFA))
        .isEqualTo(ChaveToggle.DEV_PODE_FINALIZAR_TAREFA.padrao());
  }

  private ProjetoToggle toggle(boolean habilitado) {
    ProjetoToggle projetoToggle = new ProjetoToggle();
    projetoToggle.setHabilitado(habilitado);
    return projetoToggle;
  }
}
