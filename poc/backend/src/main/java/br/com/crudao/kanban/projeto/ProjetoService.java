package br.com.crudao.kanban.projeto;

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
import br.com.crudao.kanban.rbac.PapelRepository;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioProjetoPapel;
import br.com.crudao.kanban.rbac.UsuarioProjetoPapelRepository;
import br.com.crudao.kanban.security.PermissaoGuard;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ciclo de vida do projeto (RF-008) e sua configuracao inicial. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjetoService {

  /** Nome da raia criada automaticamente com o projeto — resolve o "default" de RN-CB-005 (A-7). */
  private static final String RAIA_PADRAO = "Geral";

  private final ProjetoRepository projetoRepository;
  private final ProjetoToggleService projetoToggleService;
  private final RaiaRepository raiaRepository;
  private final SequenciaProjetoRepository sequenciaProjetoRepository;
  private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  private final PapelRepository papelRepository;
  private final TarefaRepository tarefaRepository;
  private final EventoBoardPublisher eventoBoardPublisher;
  private final PermissaoGuard permissaoGuard;

  /**
   * Cria o projeto ja configurado: sequencia de eventos, os 4 toggles default, a raia padrao e o
   * autor como {@code project_admin}.
   *
   * <p>A permissao e checada aqui, e nao pelo aspecto: nao existe projeto ao qual escopar a
   * verificacao, entao apenas admin global cria projeto (ADR-007).
   */
  @Transactional
  public Projeto criar(CriarProjetoRequest request, Usuario autor) {
    if (!autor.isAdminGlobal()) {
      throw new PermissaoNegadaException("Apenas o administrador global pode criar projetos.");
    }
    if (projetoRepository.existsByNomeIgnoreCase(request.nome())) {
      throw new BusinessException(
          "NOME_PROJETO_DUPLICADO",
          "Ja existe um projeto com este nome.",
          HttpStatus.CONFLICT);
    }

    Projeto projeto =
        projetoRepository.save(
            Projeto.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .status(StatusProjeto.ATIVO)
                .build());

    sequenciaProjetoRepository.criarSeAusente(projeto.getId());
    projetoToggleService.criarDefaults(projeto.getId());
    raiaRepository.save(
        Raia.builder().projetoId(projeto.getId()).nome(RAIA_PADRAO).ordem(0).padrao(true).build());
    associar(projeto.getId(), autor.getId(), Papeis.PROJECT_ADMIN, autor.getId());

    log.info("Projeto {} criado", projeto.getId());
    return projeto;
  }

  @Transactional
  public Projeto atualizar(UUID id, AtualizarProjetoRequest request) {
    Projeto projeto = buscar(id);
    if (!projeto.getNome().equalsIgnoreCase(request.nome())
        && projetoRepository.existsByNomeIgnoreCase(request.nome())) {
      throw new BusinessException(
          "NOME_PROJETO_DUPLICADO", "Ja existe um projeto com este nome.", HttpStatus.CONFLICT);
    }
    projeto.setNome(request.nome());
    projeto.setDescricao(request.descricao());
    return projeto;
  }

  /**
   * Finaliza o projeto (RN-015). Idempotente. <b>Nao</b> encerra periodos abertos: finalizar
   * bloqueia escrita, nao interrompe a contagem de lead-time.
   */
  @Transactional
  public Projeto finalizar(UUID id) {
    Projeto projeto = buscar(id);
    if (projeto.getStatus() == StatusProjeto.FINALIZADO) {
      return projeto;
    }
    projeto.setStatus(StatusProjeto.FINALIZADO);
    projeto.setFinalizadoEm(Instant.now());
    eventoBoardPublisher.publicar(
        EventoBoard.de(projeto.getId(), TipoEventoBoard.PROJETO_STATUS_ALTERADO, null));
    log.info("Projeto {} finalizado", id);
    return projeto;
  }

  @Transactional
  public Projeto reabrir(UUID id) {
    Projeto projeto = buscar(id);
    if (projeto.getStatus() == StatusProjeto.ATIVO) {
      return projeto;
    }
    projeto.setStatus(StatusProjeto.ATIVO);
    projeto.setFinalizadoEm(null);
    eventoBoardPublisher.publicar(
        EventoBoard.de(projeto.getId(), TipoEventoBoard.PROJETO_STATUS_ALTERADO, null));
    log.info("Projeto {} reaberto", id);
    return projeto;
  }

  /** Bloqueia a exclusao enquanto houver qualquer tarefa ativa vinculada (RN-005). */
  @Transactional
  public void excluir(UUID id) {
    Projeto projeto = buscar(id);
    long ativas = tarefaRepository.contarAtivasNoProjeto(id);
    if (ativas > 0) {
      throw new RecursoEmUsoException(
          "O projeto possui %d tarefa(s) ativa(s) e nao pode ser excluido.".formatted(ativas));
    }
    projetoRepository.delete(projeto);
    log.info("Projeto {} excluido", id);
  }

  @Transactional(readOnly = true)
  public List<Projeto> listarDoUsuario(Usuario usuario) {
    if (usuario.isAdminGlobal()) {
      return projetoRepository.findAllByOrderByNomeAsc();
    }
    List<UUID> ids = usuarioProjetoPapelRepository.findProjetoIdsDoUsuario(usuario.getId());
    return ids.isEmpty() ? List.of() : projetoRepository.findByIdInOrderByNomeAsc(ids);
  }

  @Transactional(readOnly = true)
  public Projeto buscar(UUID id) {
    return projetoRepository
        .findById(id)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Projeto", id));
  }

  /** Permissoes efetivas do usuario atual naquele projeto — alimenta a UI condicional (RNF-003). */
  @Transactional(readOnly = true)
  public PermissoesEfetivasResponse permissoesEfetivas(UUID projetoId) {
    Projeto projeto = buscar(projetoId);
    Usuario usuario = permissaoGuard.usuarioAtual();
    return new PermissoesEfetivasResponse(
        projetoId,
        usuario.isAdminGlobal(),
        projeto.isAtivo(),
        permissaoGuard.permissoesEfetivas(projetoId),
        permissaoGuard.papeisNoProjeto(projetoId),
        projetoToggleService.obterComoMapa(projetoId));
  }

  @Transactional
  public void associar(UUID projetoId, UUID usuarioId, String codigoPapel, UUID atribuidoPorId) {
    UUID papelId =
        papelRepository
            .findByCodigo(codigoPapel)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Papel nao encontrado: " + codigoPapel))
            .getId();
    usuarioProjetoPapelRepository.save(
        new UsuarioProjetoPapel(usuarioId, projetoId, papelId, atribuidoPorId));
  }
}
