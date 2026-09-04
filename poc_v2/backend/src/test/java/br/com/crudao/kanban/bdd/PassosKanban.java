package br.com.crudao.kanban.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

import br.com.crudao.kanban.auditoria.CampoAuditado;
import br.com.crudao.kanban.board.BoardQueryService;
import br.com.crudao.kanban.board.dto.BoardSnapshotResponse;
import br.com.crudao.kanban.dashboard.DashboardService;
import br.com.crudao.kanban.dashboard.dto.DashboardResponse;
import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.TipoEventoBoard;
import br.com.crudao.kanban.integracao.CenarioDeTeste;
import br.com.crudao.kanban.leadtime.PeriodoImpedimentoRepository;
import br.com.crudao.kanban.notificacao.Notificacao;
import br.com.crudao.kanban.notificacao.NotificacaoService;
import br.com.crudao.kanban.projeto.ProjetoService;
import br.com.crudao.kanban.projeto.dto.AtualizarProjetoRequest;
import br.com.crudao.kanban.raia.Raia;
import br.com.crudao.kanban.raia.RaiaService;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioProjetoPapelService;
import br.com.crudao.kanban.security.UsuarioAtualProvider;
import br.com.crudao.kanban.tarefa.PrioridadeTarefa;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaService;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import br.com.crudao.kanban.tarefa.dto.AtualizarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.AuditoriaResponse;
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.TarefaDetalheResponse;
import br.com.crudao.kanban.workflow.EtapaService;
import br.com.crudao.kanban.workflow.WorkflowService;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;

/** Glue dos cenarios de aceite RF-001 a RF-019 (bloco 19.3). */
@RequiredArgsConstructor
public class PassosKanban {

  private final CenarioDeTeste cenarioDeTeste;
  private final UsuarioAtualProvider usuarioAtualProvider;
  private final ProjetoService projetoService;
  private final WorkflowService workflowService;
  private final EtapaService etapaService;
  private final RaiaService raiaService;
  private final TarefaService tarefaService;
  private final BoardQueryService boardQueryService;
  private final DashboardService dashboardService;
  private final NotificacaoService notificacaoService;
  private final UsuarioProjetoPapelService usuarioProjetoPapelService;
  private final PeriodoImpedimentoRepository periodoImpedimentoRepository;
  private final EventoBoardPublisher eventoBoardPublisher;

  private CenarioDeTeste.Cenario cenario;
  private Usuario admin;
  private Usuario usuarioAtual;
  private UUID tarefaId;
  private Exception excecao;
  private BoardSnapshotResponse snapshot;
  private TarefaDetalheResponse detalhe;
  private DashboardResponse dashboard;

  @Before
  public void preparar() {
    cenarioDeTeste.limpar();
    clearInvocations(eventoBoardPublisher);
    excecao = null;
    snapshot = null;
    detalhe = null;
    dashboard = null;
    tarefaId = null;
  }

  // ----------------------------------------------------------------- contexto

  @Dado("que existe um projeto ativo com workflow de tres etapas")
  public void projetoAtivo() {
    admin = cenarioDeTeste.autenticarComoAdmin();
    usuarioAtual = admin;
    cenario = cenarioDeTeste.criarProjetoCompleto("Projeto BDD", admin);
  }

  @Dado("que existe uma tarefa no board")
  public void tarefaNoBoard() {
    tarefaId = criarTarefa("Card de aceite", null);
  }

  @Dado("que existe uma tarefa na raia {string}")
  public void tarefaNaRaia(String nomeRaia) {
    tarefaId = criarTarefa("Card de aceite", raiaPorNome(nomeRaia).getId());
  }

  @Dado("que a tarefa foi movida para {string}")
  public void tarefaFoiMovidaPara(String nomeEtapa) {
    mover(nomeEtapa);
    if (excecao != null) {
      throw new IllegalStateException("Pre-condicao falhou", excecao);
    }
  }

  @Dado("que o toggle {string} esta desabilitado")
  public void toggleDesabilitado(String chave) {
    projetoService.atualizarToggles(cenario.projeto().getId(), Map.of(chave, false));
  }

  @Dado("que o usuario {string} possui o papel {string} no projeto")
  public void usuarioComPapel(String email, String papel) {
    associarPapel(email, papel);
  }

  @Dado("que existe a etapa {string} sem transicao de saida")
  public void etapaSemSaida(String nome) {
    etapaService.criar(cenario.workflow().getId(), nome, false);
  }

  @Dado("que existe a raia {string}")
  public void existeRaia(String nome) {
    raiaService.criar(cenario.projeto().getId(), nome);
  }

  @Dado("que estou autenticado como {string}")
  public void queEstouAutenticadoComo(String email) {
    estouAutenticadoComo(email);
  }

  @Quando("estou autenticado como {string}")
  public void estouAutenticadoComo(String email) {
    usuarioAtual = cenarioDeTeste.autenticar(email);
  }

  // ------------------------------------------------------------------- acoes

  @Quando("abro o board do projeto")
  public void abroOBoard() {
    executar(() -> snapshot = boardQueryService.snapshot(cenario.projeto().getId()));
  }

  @Quando("movo a tarefa para {string}")
  public void mover(String nomeEtapa) {
    executar(
        () -> {
          long versao = tarefaService.buscar(tarefaId).getVersao();
          tarefaService.mover(
              tarefaId,
              new MoverTarefaRequest(etapaPorNome(nomeEtapa), null, versao),
              usuarioAtual);
        });
  }

  @Quando("edito a descricao da tarefa")
  public void editoADescricao() {
    Tarefa tarefa = tarefaService.buscar(tarefaId);
    executar(
        () ->
            tarefaService.atualizar(
                tarefaId,
                new AtualizarTarefaRequest(
                    tarefa.getTitulo(),
                    "Escopo renegociado",
                    tarefa.getTipo(),
                    tarefa.getPrioridade(),
                    tarefa.getRaiaId()),
                usuarioAtual));
  }

  @Quando("edito o titulo da tarefa para {string}")
  public void editoOTitulo(String titulo) {
    Tarefa tarefa = tarefaService.buscar(tarefaId);
    executar(
        () ->
            tarefaService.atualizar(
                tarefaId,
                new AtualizarTarefaRequest(
                    titulo,
                    tarefa.getDescricao(),
                    tarefa.getTipo(),
                    tarefa.getPrioridade(),
                    tarefa.getRaiaId()),
                usuarioAtual));
  }

  @Quando("marco a tarefa como impedida")
  public void marcoImpedida() {
    executar(() -> tarefaService.marcarImpedimento(tarefaId, "aguardando cliente", usuarioAtual));
  }

  @Quando("abro o detalhe da tarefa")
  public void abroODetalhe() {
    executar(() -> detalhe = tarefaService.detalhe(tarefaId, PageRequest.of(0, 50)));
  }

  @Quando("abro o dashboard do projeto")
  public void abroODashboard() {
    Instant agora = Instant.now();
    executar(
        () ->
            dashboard =
                dashboardService.obter(
                    cenario.projeto().getId(),
                    agora.minus(1, ChronoUnit.DAYS),
                    agora.plus(1, ChronoUnit.DAYS)));
  }

  @Quando("finalizo o projeto")
  public void finalizoOProjeto() {
    executar(() -> projetoService.finalizar(cenario.projeto().getId()));
  }

  @Quando("reabro o projeto")
  public void reabroOProjeto() {
    executar(() -> projetoService.reabrir(cenario.projeto().getId()));
  }

  @Quando("excluo o workflow ativo")
  public void excluoOWorkflow() {
    executar(() -> workflowService.excluir(cenario.workflow().getId()));
  }

  @Quando("valido a configuracao do workflow")
  public void validoOWorkflow() {
    executar(() -> etapaService.validarWorkflow(cenario.workflow().getId()));
  }

  @Quando("renomeio o projeto para {string}")
  public void renomeioOProjeto(String nome) {
    executar(
        () ->
            projetoService.atualizar(
                cenario.projeto().getId(), new AtualizarProjetoRequest(nome, null)));
  }

  @Quando("encerro a sessao e consulto o usuario autenticado")
  public void encerroASessao() {
    SecurityContextHolder.clearContext();
    executar(usuarioAtualProvider::atual);
  }

  @Quando("associo {string} ao projeto com o papel {string}")
  public void associo(String email, String papel) {
    associarPapel(email, papel);
  }

  @Quando("excluo a tarefa")
  public void excluoATarefa() {
    executar(() -> tarefaService.excluir(tarefaId, usuarioAtual));
  }

  @Quando("crio um card com titulo {string} sem responsavel e sem raia")
  public void crioCardSemResponsavelNemRaia(String titulo) {
    executar(() -> tarefaId = criarTarefa(titulo, null));
  }

  // ---------------------------------------------------------------- assercoes

  @Entao("a acao e recusada com a mensagem {string}")
  public void acaoRecusada(String mensagem) {
    assertThat(excecao).as("nenhuma excecao foi lancada").isNotNull();
    assertThat(excecao.getMessage()).isEqualTo(mensagem);
    excecao = null;
  }

  @Entao("a acao e concluida com sucesso")
  public void acaoConcluida() {
    assertThat(excecao).as("a acao falhou inesperadamente").isNull();
  }

  @Entao("o board exibe as colunas na ordem {string}")
  public void boardExibeColunas(String nomes) {
    acaoConcluida();
    assertThat(snapshot.etapas().stream().map(etapa -> etapa.nome()))
        .containsExactlyElementsOf(separar(nomes));
  }

  @Entao("a tarefa aparece na coluna {string}")
  public void tarefaNaColuna(String nomeEtapa) {
    assertThat(snapshot.tarefas())
        .singleElement()
        .satisfies(
            resumo -> {
              assertThat(resumo.id()).isEqualTo(tarefaId);
              assertThat(resumo.etapaId()).isEqualTo(etapaPorNome(nomeEtapa));
            });
  }

  @Entao("o board expoe as raias {string} e a tarefa esta agrupada na raia {string}")
  public void boardAgrupaPorRaia(String nomes, String nomeRaia) {
    acaoConcluida();
    assertThat(snapshot.raias().stream().map(raia -> raia.nome()))
        .containsExactlyElementsOf(separar(nomes));
    assertThat(snapshot.tarefas())
        .singleElement()
        .satisfies(resumo -> assertThat(resumo.raiaId()).isEqualTo(raiaPorNome(nomeRaia).getId()));
  }

  @Entao("o titulo da tarefa e {string}")
  public void tituloDaTarefa(String titulo) {
    assertThat(tarefaService.buscar(tarefaId).getTitulo()).isEqualTo(titulo);
  }

  @Entao("a tarefa esta impedida")
  public void tarefaImpedida() {
    acaoConcluida();
    assertThat(tarefaService.buscar(tarefaId).isImpedida()).isTrue();
  }

  @Entao("existe um periodo de impedimento aberto para a tarefa")
  public void periodoDeImpedimentoAberto() {
    assertThat(periodoImpedimentoRepository.findByTarefaIdAndEncerradoEmIsNull(tarefaId))
        .isPresent();
  }

  @Entao("o usuario {string} possui uma notificacao nao lida")
  public void notificacaoNaoLida(String email) {
    acaoConcluida();
    Usuario destinatario = comoAdminExecutando(() -> cenarioDeTeste.autenticar(email));
    List<Notificacao> notificacoes =
        notificacaoService.listar(destinatario, true, PageRequest.of(0, 20)).getContent();
    assertThat(notificacoes).isNotEmpty();
    assertThat(notificacoes).allSatisfy(n -> assertThat(n.getLidaEm()).isNull());
  }

  @Entao("o detalhe exibe o lead-time de cada etapa e o total de impedimento")
  public void detalheExibeLeadTime() {
    acaoConcluida();
    assertThat(detalhe.leadTimePorEtapa()).hasSize(3);
    assertThat(detalhe.leadTimePorEtapa())
        .allSatisfy(etapa -> assertThat(etapa.duracaoSegundos()).isGreaterThanOrEqualTo(0));
    assertThat(detalhe.impedimentoTotalSegundos()).isZero();
  }

  @Entao("o dashboard exibe o lead-time medio de cada etapa")
  public void dashboardExibeMedias() {
    acaoConcluida();
    assertThat(dashboard.etapas()).hasSize(3);
    assertThat(dashboard.etapas().stream().map(etapa -> etapa.nome()))
        .containsExactly("A Fazer", "Fazendo", "Concluido");
    assertThat(dashboard.etapas().getFirst().amostras()).isPositive();
  }

  @Entao("a tarefa esta na etapa {string}")
  public void tarefaNaEtapa(String nomeEtapa) {
    assertThat(tarefaService.buscar(tarefaId).getEtapaId()).isEqualTo(etapaPorNome(nomeEtapa));
  }

  @Entao("o usuario autenticado e {string} e foi provisionado sem senha local")
  public void usuarioProvisionado(String email) {
    assertThat(usuarioAtual.getEmail()).isEqualTo(email);
    assertThat(usuarioAtual.getKeycloakSub()).isNotBlank();
    assertThat(usuarioAtual.isAdminGlobal()).isFalse();
  }

  @Entao("{string} visualiza o projeto na sua lista")
  public void visualizaOProjeto(String email) {
    assertThat(usuarioAtual.getEmail()).isEqualTo(email);
    assertThat(projetoService.listarDoUsuario(usuarioAtual))
        .extracting(projeto -> projeto.getId())
        .contains(cenario.projeto().getId());
  }

  @Entao("o historico registra a alteracao de {string} de {string} para {string} com autor e data")
  public void historicoRegistra(String campo, String anterior, String novo) {
    acaoConcluida();
    List<AuditoriaResponse> historico =
        tarefaService.detalhe(tarefaId, PageRequest.of(0, 50)).historico();
    assertThat(historico)
        .anySatisfy(
            entrada -> {
              assertThat(entrada.campo()).isEqualTo(CampoAuditado.valueOf(campo));
              assertThat(entrada.valorAnterior()).isEqualTo(anterior);
              assertThat(entrada.valorNovo()).isEqualTo(novo);
              assertThat(entrada.autorId()).isEqualTo(usuarioAtual.getId());
              assertThat(entrada.ocorridoEm()).isNotNull();
            });
  }

  @Entao("a tarefa nao possui responsavel")
  public void tarefaSemResponsavel() {
    acaoConcluida();
    assertThat(tarefaService.buscar(tarefaId).getResponsavelId()).isNull();
  }

  @Entao("a tarefa esta na raia padrao do projeto")
  public void tarefaNaRaiaPadrao() {
    assertThat(tarefaService.buscar(tarefaId).getRaiaId()).isEqualTo(cenario.raia().getId());
    assertThat(cenario.raia().isPadrao()).isTrue();
  }

  @Entao("o evento {string} e publicado")
  public void eventoPublicado(String tipo) {
    verify(eventoBoardPublisher)
        .publicar(
            argThat(
                (EventoBoard evento) -> evento.tipo() == TipoEventoBoard.valueOf(tipo)
                    && tarefaId.equals(evento.tarefaId())),
            any());
  }

  @Entao("a tarefa nao aparece mais no board")
  public void tarefaForaDoBoard() {
    assertThat(boardQueryService.snapshot(cenario.projeto().getId()).tarefas()).isEmpty();
  }

  // ------------------------------------------------------------------ apoio

  private UUID criarTarefa(String titulo, UUID raiaId) {
    return tarefaService
        .criar(
            cenario.projeto().getId(),
            new CriarTarefaRequest(
                titulo, "Escopo inicial", TipoTarefa.TAREFA, PrioridadeTarefa.MEDIA, raiaId, null),
            usuarioAtual)
        .getId();
  }

  /** Provisiona o usuario pelo token e o associa ao projeto; a sessao volta a ser a do admin. */
  private void associarPapel(String email, String papel) {
    Usuario convidado = cenarioDeTeste.autenticar(email);
    cenarioDeTeste.autenticarComoAdmin();
    usuarioAtual = admin;
    usuarioProjetoPapelService.associar(cenario.projeto().getId(), convidado.getId(), papel, admin);
  }

  private <T> T comoAdminExecutando(java.util.function.Supplier<T> acao) {
    Usuario anterior = usuarioAtual;
    T resultado = acao.get();
    if (anterior != null) {
      cenarioDeTeste.autenticar(anterior.getEmail());
      usuarioAtual = anterior;
    }
    return resultado;
  }

  private UUID etapaPorNome(String nome) {
    return switch (nome) {
      case "A Fazer" -> cenario.aFazer().getId();
      case "Fazendo" -> cenario.fazendo().getId();
      case "Concluido" -> cenario.concluido().getId();
      default -> throw new IllegalArgumentException("Etapa desconhecida no cenario: " + nome);
    };
  }

  private Raia raiaPorNome(String nome) {
    return raiaService.listar(cenario.projeto().getId()).stream()
        .filter(raia -> raia.getNome().equals(nome))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Raia desconhecida no cenario: " + nome));
  }

  private static List<String> separar(String nomes) {
    return Arrays.stream(nomes.split(",")).map(String::trim).toList();
  }

  private void executar(Runnable acao) {
    excecao = null;
    try {
      acao.run();
    } catch (Exception e) {
      excecao = e;
    }
  }
}
