package br.com.crudao.kanban.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.rbac.dto.MembroProjetoResponse;
import br.com.crudao.kanban.rbac.dto.PapelResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Papeis cumulativos escopados ao projeto (RF-015) e protecao do papel admin (RN-006). */
@ExtendWith(MockitoExtension.class)
class UsuarioProjetoPapelServiceTest {

  private static final UUID PROJETO = UUID.randomUUID();

  @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private PapelRepository papelRepository;
  @InjectMocks private UsuarioProjetoPapelService service;

  private Papel papel(String codigo, boolean protegido) {
    Papel papel = new Papel();
    papel.setId(UUID.randomUUID());
    papel.setCodigo(codigo);
    papel.setNome(codigo);
    papel.setProtegido(protegido);
    return papel;
  }

  private Usuario usuario(String nome, boolean adminGlobal) {
    return Usuario.builder()
        .id(UUID.randomUUID())
        .nome(nome)
        .email(nome.toLowerCase() + "@exemplo.test")
        .adminGlobal(adminGlobal)
        .build();
  }

  @Test
  @DisplayName("listarMembros agrupa os papeis cumulativos de cada usuario")
  void listarMembrosAgrupaPapeis() {
    Papel dev = papel(Papeis.DEV, false);
    Papel gestor = papel(Papeis.GESTOR, false);
    Usuario ana = usuario("Ana", false);
    when(usuarioProjetoPapelRepository.findByIdProjetoId(PROJETO))
        .thenReturn(
            List.of(
                new UsuarioProjetoPapel(ana.getId(), PROJETO, dev.getId(), ana.getId()),
                new UsuarioProjetoPapel(ana.getId(), PROJETO, gestor.getId(), ana.getId())));
    when(papelRepository.findAll()).thenReturn(List.of(dev, gestor));
    when(usuarioRepository.findAllById(Set.of(ana.getId()))).thenReturn(List.of(ana));

    List<MembroProjetoResponse> membros = service.listarMembros(PROJETO);

    assertThat(membros).hasSize(1);
    assertThat(membros.get(0).usuarioId()).isEqualTo(ana.getId());
    assertThat(membros.get(0).papeis()).containsExactlyInAnyOrder(Papeis.DEV, Papeis.GESTOR);
  }

  @Test
  @DisplayName("vinculo cujo usuario nao existe mais e ignorado na listagem")
  void listarMembrosIgnoraUsuarioAusente() {
    Papel dev = papel(Papeis.DEV, false);
    UUID fantasma = UUID.randomUUID();
    when(usuarioProjetoPapelRepository.findByIdProjetoId(PROJETO))
        .thenReturn(List.of(new UsuarioProjetoPapel(fantasma, PROJETO, dev.getId(), fantasma)));
    when(papelRepository.findAll()).thenReturn(List.of(dev));
    when(usuarioRepository.findAllById(Set.of(fantasma))).thenReturn(List.of());

    assertThat(service.listarMembros(PROJETO)).isEmpty();
  }

  @Test
  @DisplayName("listarPapeis expoe o catalogo fechado com as permissoes de cada papel")
  void listarPapeis() {
    Papel dev = papel(Papeis.DEV, false);
    Permissao permissao = new Permissao();
    permissao.setCodigo(Permissoes.TAREFA_MOVER);
    dev.setPermissoes(Set.of(permissao));
    when(papelRepository.findAll()).thenReturn(List.of(dev));

    List<PapelResponse> papeis = service.listarPapeis();

    assertThat(papeis).hasSize(1);
    assertThat(papeis.get(0).codigo()).isEqualTo(Papeis.DEV);
    assertThat(papeis.get(0).protegido()).isFalse();
    assertThat(papeis.get(0).permissoes()).containsExactly(Permissoes.TAREFA_MOVER);
  }

  @Test
  @DisplayName("associar registra o vinculo e quem atribuiu")
  void associar() {
    Papel dev = papel(Papeis.DEV, false);
    Usuario autor = usuario("Chefe", false);
    UUID alvo = UUID.randomUUID();
    when(papelRepository.findByCodigo(Papeis.DEV)).thenReturn(Optional.of(dev));
    when(usuarioRepository.existsById(alvo)).thenReturn(true);

    service.associar(PROJETO, alvo, Papeis.DEV, autor);

    ArgumentCaptor<UsuarioProjetoPapel> captor =
        ArgumentCaptor.forClass(UsuarioProjetoPapel.class);
    verify(usuarioProjetoPapelRepository).save(captor.capture());
    assertThat(captor.getValue().getId().getUsuarioId()).isEqualTo(alvo);
    assertThat(captor.getValue().getId().getProjetoId()).isEqualTo(PROJETO);
    assertThat(captor.getValue().getId().getPapelId()).isEqualTo(dev.getId());
  }

  @Test
  @DisplayName("codigo fora do catalogo fechado e rejeitado antes de consultar o banco")
  void associarPapelForaDoCatalogo() {
    Usuario autor = usuario("Chefe", false);
    UUID alvo = UUID.randomUUID();

    assertThatThrownBy(() -> service.associar(PROJETO, alvo, "superusuario", autor))
        .isInstanceOf(BusinessException.class)
        .hasMessage("O papel informado nao pertence ao catalogo de papeis do sistema.");
    verify(papelRepository, never()).findByCodigo(any());
  }

  @Test
  @DisplayName("papel delegado nao concede o papel admin, que e protegido (RN-006)")
  void associarPapelProtegidoSemAdminGlobal() {
    Papel admin = papel(Papeis.ADMIN, true);
    Usuario autor = usuario("Chefe", false);
    UUID alvo = UUID.randomUUID();
    when(papelRepository.findByCodigo(Papeis.ADMIN)).thenReturn(Optional.of(admin));

    assertThatThrownBy(() -> service.associar(PROJETO, alvo, Papeis.ADMIN, autor))
        .isInstanceOf(PermissaoNegadaException.class)
        .hasMessage("O papel administrativo do sistema nao pode ser delegado.");
    verify(usuarioProjetoPapelRepository, never()).save(any());
  }

  @Test
  @DisplayName("o admin global pode conceder o papel protegido")
  void associarPapelProtegidoComAdminGlobal() {
    Papel admin = papel(Papeis.ADMIN, true);
    Usuario autor = usuario("Root", true);
    UUID alvo = UUID.randomUUID();
    when(papelRepository.findByCodigo(Papeis.ADMIN)).thenReturn(Optional.of(admin));
    when(usuarioRepository.existsById(alvo)).thenReturn(true);

    service.associar(PROJETO, alvo, Papeis.ADMIN, autor);

    verify(usuarioProjetoPapelRepository).save(any());
  }

  @Test
  @DisplayName("associar usuario inexistente resulta em recurso nao encontrado")
  void associarUsuarioInexistente() {
    Papel dev = papel(Papeis.DEV, false);
    Usuario autor = usuario("Chefe", false);
    UUID alvo = UUID.randomUUID();
    when(papelRepository.findByCodigo(Papeis.DEV)).thenReturn(Optional.of(dev));
    when(usuarioRepository.existsById(alvo)).thenReturn(false);

    assertThatThrownBy(() -> service.associar(PROJETO, alvo, Papeis.DEV, autor))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  @DisplayName("desassociar sem codigo remove todos os papeis do usuario no projeto")
  void desassociarTodos() {
    UUID alvo = UUID.randomUUID();

    service.desassociar(PROJETO, alvo, null, usuario("Chefe", false));

    verify(usuarioProjetoPapelRepository).deleteByIdUsuarioIdAndIdProjetoId(alvo, PROJETO);
  }

  @Test
  @DisplayName("desassociar com codigo remove apenas aquele papel")
  void desassociarUmPapel() {
    Papel dev = papel(Papeis.DEV, false);
    UUID alvo = UUID.randomUUID();
    when(papelRepository.findByCodigo(Papeis.DEV)).thenReturn(Optional.of(dev));

    service.desassociar(PROJETO, alvo, Papeis.DEV, usuario("Chefe", false));

    verify(usuarioProjetoPapelRepository)
        .deleteById(new UsuarioProjetoPapel.Id(alvo, PROJETO, dev.getId()));
  }

  @Test
  @DisplayName("papel delegado tambem nao revoga o papel protegido")
  void desassociarPapelProtegidoSemAdminGlobal() {
    Papel admin = papel(Papeis.ADMIN, true);
    Usuario autor = usuario("Chefe", false);
    UUID alvo = UUID.randomUUID();
    when(papelRepository.findByCodigo(Papeis.ADMIN)).thenReturn(Optional.of(admin));

    assertThatThrownBy(() -> service.desassociar(PROJETO, alvo, Papeis.ADMIN, autor))
        .isInstanceOf(PermissaoNegadaException.class)
        .hasMessage("O papel administrativo do sistema nao pode ser delegado.");
    verify(usuarioProjetoPapelRepository, never()).deleteById(any());
  }

  @Test
  @DisplayName("papel do catalogo ausente no banco resulta em recurso nao encontrado")
  void papelDoCatalogoAusenteNoBanco() {
    Usuario autor = usuario("Chefe", false);
    UUID alvo = UUID.randomUUID();
    when(papelRepository.findByCodigo(Papeis.DEV)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.associar(PROJETO, alvo, Papeis.DEV, autor))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Papel nao encontrado: dev");
  }
}
