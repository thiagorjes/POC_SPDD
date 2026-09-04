package br.com.crudao.kanban.projeto;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.ClockProvider;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.SequenciaProjeto;
import br.com.crudao.kanban.evento.SequenciaProjetoRepository;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.projeto.dto.AtualizarProjetoRequest;
import br.com.crudao.kanban.projeto.dto.CriarProjetoRequest;
import br.com.crudao.kanban.projeto.dto.PermissoesEfetivasResponse;
import br.com.crudao.kanban.raia.Raia;
import br.com.crudao.kanban.raia.RaiaRepository;
import br.com.crudao.kanban.rbac.CodigoPapel;
import br.com.crudao.kanban.rbac.CodigoPermissao;
import br.com.crudao.kanban.rbac.Papel;
import br.com.crudao.kanban.rbac.PapelRepository;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioProjetoPapel;
import br.com.crudao.kanban.rbac.UsuarioProjetoPapelRepository;
import br.com.crudao.kanban.security.ExigePermissao;
import br.com.crudao.kanban.security.OrigemEscopo;
import br.com.crudao.kanban.security.PermissaoGuard;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ciclo de vida do projeto (RF-008, RN-005, RN-015). */
@Service
@RequiredArgsConstructor
public class ProjetoService {

  /** Nome da raia criada automaticamente com o projeto (A-7). */
  private static final String NOME_RAIA_PADRAO = "Geral";

  private final ProjetoRepository projetoRepository;
  private final ProjetoToggleService projetoToggleService;
  private final RaiaRepository raiaRepository;
  private final SequenciaProjetoRepository sequenciaProjetoRepository;
  private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  private final PapelRepository papelRepository;
  private final TarefaRepository tarefaRepository;
  private final EventoBoardPublisher eventoBoardPublisher;
  private final PermissaoGuard permissaoGuard;
  private final ClockProvider clockProvider;

  /**
   * Cria o projeto com toggles default, sequencia de eventos, raia padrao e associa o autor como
   * {@code project_admin}. Nao ha projeto para escopar a autorizacao, portanto a checagem e feita
   * diretamente aqui (ADR-007).
   */
  @Transactional
  public Projeto criar(CriarProjetoRequest request, Usuario autor) {
    if (!autor.isAdminGlobal()) {
      throw new PermissaoNegadaException("Apenas o administrador global pode criar projetos.");
    }
    if (projetoRepository.existsByNomeIgnoreCase(request.nome())) {
      throw new BusinessException(
          "NOME_PROJETO_DUPLICADO", "Ja existe um projeto com este nome.", HttpStatus.CONFLICT);
    }

    Projeto projeto = new Projeto();
    projeto.setNome(request.nome());
    projeto.setDescricao(request.descricao());
    projeto.setStatus(StatusProjeto.ATIVO);
    Projeto salvo = projetoRepository.save(projeto);

    sequenciaProjetoRepository.save(new SequenciaProjeto(salvo.getId()));
    projetoToggleService.criarPadroes(salvo.getId());
    raiaRepository.save(new Raia(salvo.getId(), NOME_RAIA_PADRAO, 0, true));
    associarPapel(autor.getId(), salvo.getId(), CodigoPapel.PROJECT_ADMIN, autor.getId());

    return salvo;
  }

  /** Atualiza nome e descricao do projeto (RF-008). */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.PROJETO_ADMINISTRAR,
      escopoProjeto = "#id",
      origem = OrigemEscopo.PROJETO)
  public Projeto atualizar(UUID id, AtualizarProjetoRequest request) {
    Projeto projeto = buscarEntidade(id);
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
   * Finaliza o projeto (RN-015). Idempotente. Nao encerra periodos abertos: finalizar bloqueia
   * escrita, nao interrompe a contagem de lead-time.
   */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.PROJETO_ADMINISTRAR,
      escopoProjeto = "#id",
      origem = OrigemEscopo.PROJETO)
  public Projeto finalizar(UUID id) {
    Projeto projeto = buscarEntidade(id);
    if (projeto.getStatus() == StatusProjeto.FINALIZADO) {
      return projeto;
    }
    projeto.setStatus(StatusProjeto.FINALIZADO);
    projeto.setFinalizadoEm(clockProvider.agora());
    publicarStatus(projeto);
    return projeto;
  }

  /**
   * Reabre o projeto. Nao usa o guard declarativo porque {@code escrita=true} exigiria projeto
   * ativo, o que tornaria a reabertura impossivel (RN-015 sem bypass).
   */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.PROJETO_ADMINISTRAR,
      escopoProjeto = "#id",
      origem = OrigemEscopo.PROJETO,
      escrita = false)
  public Projeto reabrir(UUID id) {
    Projeto projeto = buscarEntidade(id);
    if (projeto.getStatus() == StatusProjeto.ATIVO) {
      return projeto;
    }
    projeto.setStatus(StatusProjeto.ATIVO);
    projeto.setFinalizadoEm(null);
    publicarStatus(projeto);
    return projeto;
  }

  /** RN-005: bloqueia a exclusao enquanto houver qualquer tarefa ativa no projeto. */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.PROJETO_ADMINISTRAR,
      escopoProjeto = "#id",
      origem = OrigemEscopo.PROJETO,
      escrita = false)
  public void excluir(UUID id) {
    Projeto projeto = buscarEntidade(id);
    if (tarefaRepository.contarAtivasPorProjeto(id) > 0) {
      throw new RecursoEmUsoException("O projeto possui tarefas ativas e nao pode ser excluido.");
    }
    projetoRepository.delete(projeto);
  }

  /** Projetos em que o usuario possui algum papel; admin global enxerga todos (RF-008). */
  @Transactional(readOnly = true)
  public List<Projeto> listarDoUsuario(Usuario usuario) {
    if (usuario.isAdminGlobal()) {
      return projetoRepository.findAll();
    }
    List<UUID> ids = usuarioProjetoPapelRepository.buscarProjetosDoUsuario(usuario.getId());
    return ids.isEmpty() ? List.of() : projetoRepository.findByIdInOrderByNomeAsc(ids);
  }

  /** Leitura de projeto; permitida tambem quando finalizado (UC-002). */
  @Transactional(readOnly = true)
  @ExigePermissao(
      valor = CodigoPermissao.PROJETO_VISUALIZAR,
      escopoProjeto = "#id",
      origem = OrigemEscopo.PROJETO,
      escrita = false)
  public Projeto buscar(UUID id) {
    return buscarEntidade(id);
  }

  /**
   * Permissoes efetivas e toggles do usuario atual no projeto, para a UI condicional (RNF-003). A
   * UI e conveniencia: a autoridade de autorizacao permanece no backend.
   */
  @Transactional(readOnly = true)
  public PermissoesEfetivasResponse permissoesEfetivas(UUID projetoId, Usuario usuario) {
    Projeto projeto = buscarEntidade(projetoId);
    if (!permissaoGuard.membro(projetoId)) {
      throw new PermissaoNegadaException("Voce nao possui acesso a este projeto.");
    }
    Map<String, Boolean> toggles = new LinkedHashMap<>();
    projetoToggleService
        .obter(projetoId)
        .forEach((chave, valor) -> toggles.put(chave.name(), valor));
    return new PermissoesEfetivasResponse(
        projetoId,
        usuario.isAdminGlobal(),
        projeto.ativo(),
        permissaoGuard.permissoesEfetivas(projetoId),
        toggles);
  }

  /** Toggles vigentes do projeto (RF-016). */
  @Transactional(readOnly = true)
  @ExigePermissao(
      valor = CodigoPermissao.PROJETO_ADMINISTRAR,
      escopoProjeto = "#projetoId",
      origem = OrigemEscopo.PROJETO,
      escrita = false)
  public Map<ChaveToggle, Boolean> toggles(UUID projetoId) {
    return projetoToggleService.obter(projetoId);
  }

  /**
   * Atualiza os toggles do projeto (RF-016). O catalogo e fechado: chave desconhecida e rejeitada
   * em vez de silenciosamente ignorada (BDR-001).
   */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.PROJETO_ADMINISTRAR,
      escopoProjeto = "#projetoId",
      origem = OrigemEscopo.PROJETO)
  public Map<ChaveToggle, Boolean> atualizarToggles(UUID projetoId, Map<String, Boolean> valores) {
    for (Map.Entry<String, Boolean> entrada : valores.entrySet()) {
      ChaveToggle chave;
      try {
        chave = ChaveToggle.valueOf(entrada.getKey());
      } catch (IllegalArgumentException excecao) {
        throw new BusinessException(
            "TOGGLE_DESCONHECIDO",
            "O toggle informado nao pertence ao catalogo de configuracoes do projeto.");
      }
      projetoToggleService.definir(projetoId, chave, Boolean.TRUE.equals(entrada.getValue()));
    }
    eventoBoardPublisher.publicar(
        EventoBoard.deProjeto(projetoId, TipoEventoBoard.BOARD_RECONFIGURADO), Set.of());
    return projetoToggleService.obter(projetoId);
  }

  private void associarPapel(UUID usuarioId, UUID projetoId, String codigoPapel, UUID autorId) {
    Papel papel =
        papelRepository
            .findByCodigo(codigoPapel)
            .orElseThrow(() -> new IllegalStateException("Catalogo de papeis nao inicializado."));
    usuarioProjetoPapelRepository.save(
        new UsuarioProjetoPapel(usuarioId, projetoId, papel.getId(), autorId));
  }

  private void publicarStatus(Projeto projeto) {
    eventoBoardPublisher.publicar(
        EventoBoard.deProjeto(projeto.getId(), TipoEventoBoard.PROJETO_STATUS_ALTERADO), Set.of());
  }

  private Projeto buscarEntidade(UUID id) {
    return projetoRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto nao encontrado."));
  }
}
