package br.com.crudao.kanban.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Permissoes efetivas sao a uniao dos papeis acumulados no projeto, sem bypass (BDR-001). */
@ExtendWith(MockitoExtension.class)
class PermissaoServiceTest {

  private static final UUID USUARIO = UUID.randomUUID();
  private static final UUID PROJETO = UUID.randomUUID();

  @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  @Mock private PapelRepository papelRepository;
  @InjectMocks private PermissaoService service;

  @Test
  @DisplayName("permissoes de papeis acumulados sao unidas e deduplicadas")
  void permissoesEfetivasUnemPapeis() {
    when(usuarioProjetoPapelRepository.findCodigosPermissao(USUARIO, PROJETO))
        .thenReturn(
            List.of(
                Permissoes.PROJETO_VISUALIZAR,
                Permissoes.TAREFA_MOVER,
                Permissoes.PROJETO_VISUALIZAR));

    assertThat(service.permissoesEfetivas(USUARIO, PROJETO))
        .containsExactlyInAnyOrder(Permissoes.PROJETO_VISUALIZAR, Permissoes.TAREFA_MOVER);
  }

  @Test
  @DisplayName("sem papel no projeto o usuario nao tem nenhuma permissao")
  void semPapelNenhumaPermissao() {
    when(usuarioProjetoPapelRepository.findCodigosPermissao(USUARIO, PROJETO))
        .thenReturn(List.of());

    assertThat(service.permissoesEfetivas(USUARIO, PROJETO)).isEmpty();
  }

  @Test
  @DisplayName("papeisNoProjeto devolve os codigos acumulados")
  void papeisNoProjeto() {
    when(usuarioProjetoPapelRepository.findCodigosPapel(USUARIO, PROJETO))
        .thenReturn(List.of(Papeis.DEV, Papeis.GESTOR));

    assertThat(service.papeisNoProjeto(USUARIO, PROJETO))
        .containsExactlyInAnyOrder(Papeis.DEV, Papeis.GESTOR);
  }

  @Test
  @DisplayName("membro reflete a existencia de qualquer vinculo no projeto")
  void membro() {
    when(usuarioProjetoPapelRepository.existsByIdUsuarioIdAndIdProjetoId(USUARIO, PROJETO))
        .thenReturn(true);

    assertThat(service.membro(USUARIO, PROJETO)).isTrue();
  }

  @Test
  @DisplayName("porCodigo devolve o papel do catalogo")
  void porCodigoEncontrado() {
    Papel papel = new Papel();
    papel.setCodigo(Papeis.DEV);
    papel.setNome("Desenvolvedor");
    when(papelRepository.findByCodigo(Papeis.DEV)).thenReturn(Optional.of(papel));

    assertThat(service.porCodigo(Papeis.DEV)).isSameAs(papel);
  }

  @Test
  @DisplayName("codigo fora do catalogo fechado nao resolve para papel algum")
  void porCodigoInexistente() {
    when(papelRepository.findByCodigo("inventado")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.porCodigo("inventado"))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Papel nao encontrado: inventado");
  }
}
