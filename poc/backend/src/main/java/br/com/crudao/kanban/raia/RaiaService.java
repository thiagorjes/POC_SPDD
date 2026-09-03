package br.com.crudao.kanban.raia;

import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.raia.dto.CriarRaiaRequest;
import br.com.crudao.kanban.tarefa.TarefaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Raias do projeto (RF-011). Agrupamento visual puro: nenhuma regra de transicao, permissao ou
 * validacao de movimentacao e associada a raia.
 */
@Service
@RequiredArgsConstructor
public class RaiaService {

  private final RaiaRepository raiaRepository;
  private final TarefaRepository tarefaRepository;
  private final EventoBoardPublisher eventoBoardPublisher;

  /** A primeira raia do projeto nasce como padrao. */
  @Transactional
  public Raia criar(UUID projetoId, CriarRaiaRequest request) {
    boolean primeira = raiaRepository.countByProjetoId(projetoId) == 0;
    Raia raia =
        raiaRepository.save(
            Raia.builder()
                .projetoId(projetoId)
                .nome(request.nome())
                .ordem(raiaRepository.findMaiorOrdem(projetoId) + 1)
                .padrao(primeira)
                .build());
    publicarReconfiguracao(projetoId);
    return raia;
  }

  @Transactional
  public Raia renomear(UUID raiaId, String nome) {
    Raia raia = buscar(raiaId);
    raia.setNome(nome);
    publicarReconfiguracao(raia.getProjetoId());
    return raia;
  }

  /** Move a flag garantindo exatamente uma raia padrao por projeto. */
  @Transactional
  public Raia definirPadrao(UUID raiaId) {
    Raia nova = buscar(raiaId);
    if (nova.isPadrao()) {
      return nova;
    }
    Optional<Raia> atual = raiaRepository.findByProjetoIdAndPadraoTrue(nova.getProjetoId());
    if (atual.isPresent()) {
      atual.get().setPadrao(false);
      raiaRepository.saveAndFlush(atual.get());
    }
    nova.setPadrao(true);
    publicarReconfiguracao(nova.getProjetoId());
    return nova;
  }

  /**
   * RN-005 para raia. A raia padrao so e removivel se for a unica do projeto e nao houver nenhuma
   * tarefa — caso contrario o projeto ficaria sem destino default para novas tarefas.
   */
  @Transactional
  public void excluir(UUID raiaId) {
    Raia raia = buscar(raiaId);
    long ativas = tarefaRepository.contarAtivasNaRaia(raiaId);
    if (ativas > 0) {
      throw new RecursoEmUsoException(
          "A raia possui %d tarefa(s) ativa(s) e nao pode ser excluida.".formatted(ativas));
    }
    if (tarefaRepository.countByRaiaId(raiaId) > 0) {
      throw new RecursoEmUsoException(
          "A raia possui tarefas historicas vinculadas e nao pode ser excluida.");
    }
    if (raia.isPadrao() && raiaRepository.countByProjetoId(raia.getProjetoId()) > 1) {
      throw new RecursoEmUsoException(
          "A raia padrao nao pode ser excluida. Defina outra raia como padrao antes.");
    }
    raiaRepository.delete(raia);
    publicarReconfiguracao(raia.getProjetoId());
  }

  @Transactional(readOnly = true)
  public List<Raia> listar(UUID projetoId) {
    return raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId);
  }

  @Transactional(readOnly = true)
  public Raia buscar(UUID raiaId) {
    return raiaRepository
        .findById(raiaId)
        .orElseThrow(() -> RecursoNaoEncontradoException.de("Raia", raiaId));
  }

  @Transactional(readOnly = true)
  public Optional<Raia> padraoDoProjeto(UUID projetoId) {
    return raiaRepository.findByProjetoIdAndPadraoTrue(projetoId);
  }

  private void publicarReconfiguracao(UUID projetoId) {
    eventoBoardPublisher.publicar(
        EventoBoard.de(projetoId, TipoEventoBoard.BOARD_RECONFIGURADO, null));
  }
}
