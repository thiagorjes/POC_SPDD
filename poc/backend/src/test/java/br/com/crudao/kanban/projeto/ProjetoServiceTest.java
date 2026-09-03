package br.com.crudao.kanban.projeto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.SequenciaProjetoRepository;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.projeto.dto.AtualizarProjetoRequest;
import br.com.crudao.kanban.projeto.dto.CriarProjetoRequest;
import br.com.crudao.kanban.projeto.dto.PermissoesEfetivasResponse;
import br.com.crudao.kanban.raia.Raia;
import br.com.crudao.kanban.raia.RaiaRepository;
import br.com.crudao.kanban.rbac.Papeis;
import br.com.crudao.kanban.rbac.Papel;
import br.com.crudao.kanban.rbac.PapelRepository;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioProjetoPapel;
import br.com.crudao.kanban.rbac.UsuarioProjetoPapelRepository;
import br.com.crudao.kanban.security.PermissaoGuard;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import java.util.List;
import java.util.Map;
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

/** Ciclo de vida do projeto (RF-008) e sua configuracao inicial. */
@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

  @Mock private ProjetoRepository projetoRepository;
  @Mock private ProjetoToggleService projetoToggleService;
  @Mock private RaiaRepository raiaRepository;
  @Mock private SequenciaProjetoRepository sequenciaProjetoRepository;
  @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  @Mock private PapelRepository papelRepository;
  @Mock private TarefaRepository tarefaRepository;
  @Mock private EventoBoardPublisher eventoBoardPublisher;
  @Mock private PermissaoGuard permissaoGuard;
  @InjectMocks private ProjetoService service;

  private final Usuario adminGlobal =
      Usuario.builder().id(UUID.randomUUID()).nome("Root").adminGlobal(true).build();

  private Projeto projeto(StatusProjeto status) {
    return Projeto.builder()
        .id(UUID.randomUUID())
        .nome("Kanban")
        .descricao("Projeto piloto")
        .status(status)
        .build();
  }

  @Test
  @DisplayName("criar provisiona sequencia, toggles, raia padrao e vincula o autor como admin")
  void criarConfiguraOProjeto() {
    Papel projectAdmin = new Papel();
    projectAdmin.setId(UUID.randomUUID());
    projectAdmin.setCodigo(Papeis.PROJECT_ADMIN);
    when(projetoRepository.existsByNomeIgnoreCase("Kanban")).thenReturn(false);
    when(projetoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(papelRepository.findByCodigo(Papeis.PROJECT_ADMIN)).thenReturn(Optional.of(projectAdmin));

    Projeto criado = service.criar(new CriarProjetoRequest("Kanban", "Piloto"), adminGlobal);

    assertThat(criado.getStatus()).isEqualTo(StatusProjeto.ATIVO);
    verify(sequenciaProjetoRepository).criarSeAusente(criado.getId());
    verify(projetoToggleService).criarDefaults(criado.getId());

    ArgumentCaptor<Raia> raia = ArgumentCaptor.forClass(Raia.class);
    verify(raiaRepository).save(raia.capture());
    assertThat(raia.getValue().getNome()).isEqualTo("Geral");
    assertThat(raia.getValue().isPadrao()).isTrue();

    ArgumentCaptor<UsuarioProjetoPapel> vinculo =
        ArgumentCaptor.forClass(UsuarioProjetoPapel.class);
    verify(usuarioProjetoPapelRepository).save(vinculo.capture());
    assertThat(vinculo.getValue().getId().getUsuarioId()).isEqualTo(adminGlobal.getId());
    assertThat(vinculo.getValue().getId().getPapelId()).isEqualTo(projectAdmin.getId());
  }

  @Test
  @DisplayName("apenas o admin global cria projeto: nao ha projeto ao qual escopar a permissao")
  void criarSemAdminGlobal() {
    Usuario comum = Usuario.builder().id(UUID.randomUUID()).adminGlobal(false).build();
    CriarProjetoRequest request = new CriarProjetoRequest("Kanban", null);

    assertThatThrownBy(() -> service.criar(request, comum))
        .isInstanceOf(PermissaoNegadaException.class)
        .hasMessage("Apenas o administrador global pode criar projetos.");
    verifyNoInteractions(projetoRepository);
  }

  @Test
  @DisplayName("nome de projeto duplicado e conflito")
  void criarComNomeDuplicado() {
    CriarProjetoRequest request = new CriarProjetoRequest("Kanban", null);
    when(projetoRepository.existsByNomeIgnoreCase("Kanban")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(request, adminGlobal))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Ja existe um projeto com este nome.");
    verify(projetoRepository, never()).save(any());
  }

  @Test
  @DisplayName("atualizar altera nome e descricao")
  void atualizar() {
    Projeto projeto = projeto(StatusProjeto.ATIVO);
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
    when(projetoRepository.existsByNomeIgnoreCase("Kanban v2")).thenReturn(false);

    Projeto atualizado =
        service.atualizar(projeto.getId(), new AtualizarProjetoRequest("Kanban v2", "Nova"));

    assertThat(atualizado.getNome()).isEqualTo("Kanban v2");
    assertThat(atualizado.getDescricao()).isEqualTo("Nova");
  }

  @Test
  @DisplayName("manter o proprio nome nao colide com a unicidade")
  void atualizarMantendoONome() {
    Projeto projeto = projeto(StatusProjeto.ATIVO);
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));

    service.atualizar(projeto.getId(), new AtualizarProjetoRequest("kanban", "Outra"));

    verify(projetoRepository, never()).existsByNomeIgnoreCase(any());
  }

  @Test
  @DisplayName("renomear para um nome ja usado por outro projeto e conflito")
  void atualizarParaNomeDeOutroProjeto() {
    Projeto projeto = projeto(StatusProjeto.ATIVO);
    AtualizarProjetoRequest request = new AtualizarProjetoRequest("Outro", null);
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
    when(projetoRepository.existsByNomeIgnoreCase("Outro")).thenReturn(true);

    assertThatThrownBy(() -> service.atualizar(projeto.getId(), request))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Ja existe um projeto com este nome.");
  }

  @Test
  @DisplayName("finalizar carimba a data e anuncia a mudanca de status ao board")
  void finalizar() {
    Projeto projeto = projeto(StatusProjeto.ATIVO);
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));

    service.finalizar(projeto.getId());

    assertThat(projeto.getStatus()).isEqualTo(StatusProjeto.FINALIZADO);
    assertThat(projeto.getFinalizadoEm()).isNotNull();
    ArgumentCaptor<EventoBoard> captor = ArgumentCaptor.forClass(EventoBoard.class);
    verify(eventoBoardPublisher).publicar(captor.capture());
    assertThat(captor.getValue().tipo()).isEqualTo(TipoEventoBoard.PROJETO_STATUS_ALTERADO);
  }

  @Test
  @DisplayName("finalizar e idempotente")
  void finalizarIdempotente() {
    Projeto projeto = projeto(StatusProjeto.FINALIZADO);
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));

    service.finalizar(projeto.getId());

    verifyNoInteractions(eventoBoardPublisher);
  }

  @Test
  @DisplayName("reabrir limpa a data de finalizacao e anuncia a mudanca de status")
  void reabrir() {
    Projeto projeto = projeto(StatusProjeto.FINALIZADO);
    projeto.setFinalizadoEm(java.time.Instant.now());
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));

    service.reabrir(projeto.getId());

    assertThat(projeto.getStatus()).isEqualTo(StatusProjeto.ATIVO);
    assertThat(projeto.getFinalizadoEm()).isNull();
    verify(eventoBoardPublisher).publicar(any(EventoBoard.class));
  }

  @Test
  @DisplayName("reabrir e idempotente")
  void reabrirIdempotente() {
    Projeto projeto = projeto(StatusProjeto.ATIVO);
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));

    service.reabrir(projeto.getId());

    verifyNoInteractions(eventoBoardPublisher);
  }

  @Test
  @DisplayName("projeto com tarefa ativa nao pode ser excluido")
  void excluirComTarefaAtiva() {
    Projeto projeto = projeto(StatusProjeto.ATIVO);
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
    when(tarefaRepository.contarAtivasNoProjeto(projeto.getId())).thenReturn(9L);

    assertThatThrownBy(() -> service.excluir(projeto.getId()))
        .isInstanceOf(RecursoEmUsoException.class)
        .hasMessage("O projeto possui 9 tarefa(s) ativa(s) e nao pode ser excluido.");
    verify(projetoRepository, never()).delete(any());
  }

  @Test
  @DisplayName("projeto sem tarefa ativa e excluido")
  void excluirProjetoLivre() {
    Projeto projeto = projeto(StatusProjeto.FINALIZADO);
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
    when(tarefaRepository.contarAtivasNoProjeto(projeto.getId())).thenReturn(0L);

    service.excluir(projeto.getId());

    verify(projetoRepository).delete(projeto);
  }

  @Test
  @DisplayName("o admin global enxerga todos os projetos")
  void listarComoAdminGlobal() {
    List<Projeto> todos = List.of(projeto(StatusProjeto.ATIVO));
    when(projetoRepository.findAllByOrderByNomeAsc()).thenReturn(todos);

    assertThat(service.listarDoUsuario(adminGlobal)).isEqualTo(todos);
  }

  @Test
  @DisplayName("o usuario comum enxerga apenas os projetos em que possui papel")
  void listarComoMembro() {
    Usuario comum = Usuario.builder().id(UUID.randomUUID()).adminGlobal(false).build();
    Projeto projeto = projeto(StatusProjeto.ATIVO);
    List<UUID> ids = List.of(projeto.getId());
    when(usuarioProjetoPapelRepository.findProjetoIdsDoUsuario(comum.getId())).thenReturn(ids);
    when(projetoRepository.findByIdInOrderByNomeAsc(ids)).thenReturn(List.of(projeto));

    assertThat(service.listarDoUsuario(comum)).containsExactly(projeto);
  }

  @Test
  @DisplayName("usuario sem nenhum papel nao enxerga projeto algum")
  void listarSemVinculo() {
    Usuario comum = Usuario.builder().id(UUID.randomUUID()).adminGlobal(false).build();
    when(usuarioProjetoPapelRepository.findProjetoIdsDoUsuario(comum.getId())).thenReturn(List.of());

    assertThat(service.listarDoUsuario(comum)).isEmpty();
    verify(projetoRepository, never()).findByIdInOrderByNomeAsc(any());
  }

  @Test
  @DisplayName("buscar projeto inexistente resulta em recurso nao encontrado")
  void buscarInexistente() {
    UUID id = UUID.randomUUID();
    when(projetoRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  @DisplayName("permissoesEfetivas alimenta a UI condicional com o estado real do backend")
  void permissoesEfetivas() {
    Projeto projeto = projeto(StatusProjeto.ATIVO);
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
    when(permissaoGuard.usuarioAtual()).thenReturn(adminGlobal);
    when(permissaoGuard.permissoesEfetivas(projeto.getId())).thenReturn(Set.of("tarefa:mover"));
    when(permissaoGuard.papeisNoProjeto(projeto.getId())).thenReturn(Set.of(Papeis.DEV));
    when(projetoToggleService.obterComoMapa(projeto.getId())).thenReturn(Map.of());

    PermissoesEfetivasResponse resposta = service.permissoesEfetivas(projeto.getId());

    assertThat(resposta.projetoId()).isEqualTo(projeto.getId());
    assertThat(resposta.adminGlobal()).isTrue();
    assertThat(resposta.projetoAtivo()).isTrue();
    assertThat(resposta.permissoes()).containsExactly("tarefa:mover");
    assertThat(resposta.papeis()).containsExactly(Papeis.DEV);
  }

  @Test
  @DisplayName("associar com papel fora do catalogo resulta em recurso nao encontrado")
  void associarPapelInexistente() {
    UUID projetoId = UUID.randomUUID();
    UUID usuarioId = UUID.randomUUID();
    when(papelRepository.findByCodigo("inventado")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.associar(projetoId, usuarioId, "inventado", usuarioId))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Papel nao encontrado: inventado");
  }
}
