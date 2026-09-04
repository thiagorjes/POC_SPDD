package br.com.crudao.kanban.raia;

import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.rbac.CodigoPermissao;
import br.com.crudao.kanban.security.ExigePermissao;
import br.com.crudao.kanban.security.OrigemEscopo;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Raias sao agrupamento visual puro: nao possuem transicoes, regras nem permissoes proprias (design
 * brief secao 5).
 */
@Service
@RequiredArgsConstructor
public class RaiaService {

  private final RaiaRepository raiaRepository;
  private final TarefaRepository tarefaRepository;
  private final EventoBoardPublisher eventoBoardPublisher;

  /** Cria ao final da ordem corrente. A primeira raia do projeto nasce padrao (RN-CB-005). */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.RAIA_GERENCIAR,
      escopoProjeto = "#projetoId",
      origem = OrigemEscopo.PROJETO)
  public Raia criar(UUID projetoId, String nome) {
    boolean primeira = raiaRepository.countByProjetoId(projetoId) == 0;
    int ordem = raiaRepository.buscarMaiorOrdem(projetoId) + 1;
    Raia raia = raiaRepository.save(new Raia(projetoId, nome, ordem, primeira));
    publicarReconfiguracao(projetoId);
    return raia;
  }

  /** Renomeia a raia. */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.RAIA_GERENCIAR,
      escopoProjeto = "#raiaId",
      origem = OrigemEscopo.RAIA)
  public Raia atualizar(UUID raiaId, String nome) {
    Raia raia = buscarEntidade(raiaId);
    raia.setNome(nome);
    publicarReconfiguracao(raia.getProjetoId());
    return raia;
  }

  /** Move a flag de raia padrao, garantindo exatamente uma por projeto. */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.RAIA_GERENCIAR,
      escopoProjeto = "#raiaId",
      origem = OrigemEscopo.RAIA)
  public Raia definirPadrao(UUID raiaId) {
    Raia nova = buscarEntidade(raiaId);
    if (nova.isPadrao()) {
      return nova;
    }
    Optional<Raia> atual = raiaRepository.findByProjetoIdAndPadraoTrue(nova.getProjetoId());
    atual.ifPresent(raia -> raia.setPadrao(false));
    raiaRepository.flush();
    nova.setPadrao(true);
    publicarReconfiguracao(nova.getProjetoId());
    return nova;
  }

  /**
   * RN-005: bloqueia com tarefa ativa na raia. A raia padrao so e removivel quando e a unica do
   * projeto e nao ha nenhuma tarefa vinculada.
   */
  @Transactional
  @ExigePermissao(
      valor = CodigoPermissao.RAIA_GERENCIAR,
      escopoProjeto = "#raiaId",
      origem = OrigemEscopo.RAIA)
  public void excluir(UUID raiaId) {
    Raia raia = buscarEntidade(raiaId);
    if (tarefaRepository.contarAtivasPorRaia(raiaId) > 0) {
      throw new RecursoEmUsoException("A raia possui tarefas ativas e nao pode ser excluida.");
    }
    if (raia.isPadrao()) {
      boolean unica = raiaRepository.countByProjetoId(raia.getProjetoId()) == 1;
      if (!unica || tarefaRepository.countByRaiaId(raiaId) > 0) {
        throw new RecursoEmUsoException(
            "A raia padrao so pode ser excluida se for a unica do projeto e nao possuir tarefas.");
      }
    }
    raiaRepository.delete(raia);
    publicarReconfiguracao(raia.getProjetoId());
  }

  /** Raias ordenadas do projeto. */
  @Transactional(readOnly = true)
  @ExigePermissao(
      valor = CodigoPermissao.PROJETO_VISUALIZAR,
      escopoProjeto = "#projetoId",
      origem = OrigemEscopo.PROJETO,
      escrita = false)
  public List<Raia> listar(UUID projetoId) {
    return raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId);
  }

  /** Raia padrao do projeto, usada como default de criacao de tarefa (RN-CB-005). */
  @Transactional(readOnly = true)
  public Optional<Raia> padraoDoProjeto(UUID projetoId) {
    return raiaRepository.findByProjetoIdAndPadraoTrue(projetoId);
  }

  Raia buscarEntidade(UUID raiaId) {
    return raiaRepository
        .findById(raiaId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Raia nao encontrada."));
  }

  private void publicarReconfiguracao(UUID projetoId) {
    eventoBoardPublisher.publicar(
        EventoBoard.deProjeto(projetoId, TipoEventoBoard.BOARD_RECONFIGURADO), Set.of());
  }
}
