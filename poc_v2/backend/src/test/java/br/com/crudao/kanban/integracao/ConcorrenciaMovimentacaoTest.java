package br.com.crudao.kanban.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.crudao.kanban.common.ConflitoConcorrenciaException;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaService;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bloco 19.2: dois usuarios arrastando o mesmo card. O segundo carrega a versao que leu antes do
 * commit do primeiro e precisa ser recusado, nunca sobrescrever silenciosamente.
 */
class ConcorrenciaMovimentacaoTest extends IntegracaoBase {

  @Autowired private TarefaService tarefaService;

  private CenarioDeTeste.Cenario cenario;
  private Usuario admin;
  private Tarefa tarefa;

  @BeforeEach
  void montar() {
    admin = cenarioDeTeste.autenticarComoAdmin();
    cenario = cenarioDeTeste.criarProjetoCompleto("Projeto Concorrencia", admin);
    tarefa =
        tarefaService.criar(
            cenario.projeto().getId(),
            new CriarTarefaRequest("Card disputado", null, TipoTarefa.TAREFA, null, null, null),
            admin);
  }

  @Test
  @DisplayName("a segunda movimentacao com a versao antiga e recusada por conflito")
  void segundaMovimentacaoComVersaoAntigaFalha() {
    long versaoLidaPelosDois = tarefa.getVersao();

    tarefaService.mover(
        tarefa.getId(),
        new MoverTarefaRequest(cenario.fazendo().getId(), null, versaoLidaPelosDois),
        admin);

    assertThatThrownBy(
            () ->
                tarefaService.mover(
                    tarefa.getId(),
                    new MoverTarefaRequest(cenario.concluido().getId(), null, versaoLidaPelosDois),
                    admin))
        .isInstanceOf(ConflitoConcorrenciaException.class)
        .hasMessage(
            "A tarefa foi alterada por outro usuario. Recarregue o board e tente novamente.");

    assertThat(tarefaService.buscar(tarefa.getId()).getEtapaId())
        .isEqualTo(cenario.fazendo().getId());
  }

  @Test
  @DisplayName("com a versao recarregada a segunda movimentacao passa")
  void versaoRecarregadaPassa() {
    tarefaService.mover(
        tarefa.getId(),
        new MoverTarefaRequest(cenario.fazendo().getId(), null, tarefa.getVersao()),
        admin);

    long versaoAtual = tarefaService.buscar(tarefa.getId()).getVersao();

    assertThatCode(
            () ->
                tarefaService.mover(
                    tarefa.getId(),
                    new MoverTarefaRequest(cenario.concluido().getId(), null, versaoAtual),
                    admin))
        .doesNotThrowAnyException();
  }
}
