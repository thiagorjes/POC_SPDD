package br.com.crudao.kanban.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.crudao.kanban.common.ConfiguracaoWorkflowInvalidaException;
import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.tarefa.TarefaService;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import br.com.crudao.kanban.workflow.Etapa;
import br.com.crudao.kanban.workflow.EtapaService;
import br.com.crudao.kanban.workflow.Transicao;
import br.com.crudao.kanban.workflow.TransicaoService;
import br.com.crudao.kanban.workflow.Workflow;
import br.com.crudao.kanban.workflow.WorkflowService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bloco 19.2/19.5: invariantes de configuracao do board (RN-003, RN-004, RN-005) exercitadas contra
 * o banco real, onde as restricoes de unicidade e as revalidacoes pos-mutacao de fato acontecem.
 */
class ConfiguracaoWorkflowTest extends IntegracaoBase {

  @Autowired private WorkflowService workflowService;
  @Autowired private EtapaService etapaService;
  @Autowired private TransicaoService transicaoService;
  @Autowired private TarefaService tarefaService;

  private Usuario admin;
  private CenarioDeTeste.Cenario cenario;

  @BeforeEach
  void montar() {
    admin = cenarioDeTeste.autenticarComoAdmin();
    cenario = cenarioDeTeste.criarProjetoCompleto("Projeto Workflow", admin);
  }

  private UUID criarTarefa() {
    return tarefaService
        .criar(
            cenario.projeto().getId(),
            new CriarTarefaRequest("Card", null, TipoTarefa.TAREFA, null, null, null),
            admin)
        .getId();
  }

  @Nested
  @DisplayName("workflow")
  class Workflows {

    @Test
    @DisplayName("o primeiro workflow do projeto nasce ativo e o segundo nao")
    void primeiroNasceAtivo() {
      Workflow segundo = workflowService.criar(cenario.projeto().getId(), "Alternativo");

      assertThat(cenario.workflow().isAtivo()).isTrue();
      assertThat(segundo.isAtivo()).isFalse();
      assertThat(workflowService.listar(cenario.projeto().getId())).hasSize(2);
      assertThat(workflowService.ativoDoProjeto(cenario.projeto().getId()))
          .get()
          .extracting(Workflow::getId)
          .isEqualTo(cenario.workflow().getId());
    }

    @Test
    @DisplayName("ativar troca o workflow vigente e desativa o anterior")
    void ativarTrocaOVigente() {
      Workflow segundo = workflowService.criar(cenario.projeto().getId(), "Alternativo");

      workflowService.ativar(segundo.getId());

      assertThat(workflowService.ativoDoProjeto(cenario.projeto().getId()))
          .get()
          .extracting(Workflow::getId)
          .isEqualTo(segundo.getId());
    }

    @Test
    @DisplayName("ativar o workflow ja ativo e idempotente")
    void ativarOMesmoEhIdempotente() {
      assertThatCode(() -> workflowService.ativar(cenario.workflow().getId()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nao troca o workflow vigente enquanto ele tiver tarefas ativas")
    void naoTrocaComTarefaAtiva() {
      criarTarefa();
      Workflow segundo = workflowService.criar(cenario.projeto().getId(), "Alternativo");

      assertThatThrownBy(() -> workflowService.ativar(segundo.getId()))
          .isInstanceOf(RecursoEmUsoException.class)
          .hasMessage("O workflow atual possui tarefas ativas e nao pode ser substituido.");
    }

    @Test
    @DisplayName("workflow sem tarefas pode ser excluido")
    void excluiWorkflowSemTarefa() {
      Workflow segundo = workflowService.criar(cenario.projeto().getId(), "Alternativo");

      workflowService.excluir(segundo.getId());

      assertThat(workflowService.listar(cenario.projeto().getId())).hasSize(1);
    }

    @Test
    @DisplayName("workflow inexistente resulta em recurso nao encontrado")
    void workflowInexistente() {
      UUID inexistente = UUID.randomUUID();
      assertThatThrownBy(() -> workflowService.excluir(inexistente))
          .isInstanceOf(RecursoNaoEncontradoException.class)
          .hasMessage("Workflow nao encontrado(a).");
    }
  }

  @Nested
  @DisplayName("etapa")
  class Etapas {

    @Test
    @DisplayName("a segunda etapa final e recusada: o workflow admite exatamente uma")
    void apenasUmaEtapaFinal() {
      assertThatThrownBy(() -> etapaService.criar(cenario.workflow().getId(), "Outra Final", true))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("O workflow ja possui uma etapa final. Cada workflow admite exatamente uma.");
    }

    @Test
    @DisplayName("renomear nao altera ordem nem grafo")
    void renomear() {
      Etapa renomeada = etapaService.atualizar(cenario.fazendo().getId(), "Em Progresso");

      assertThat(renomeada.getNome()).isEqualTo("Em Progresso");
      assertThat(renomeada.getOrdem()).isEqualTo(cenario.fazendo().getOrdem());
      assertThat(transicaoService.destinosPermitidos(cenario.aFazer().getId()))
          .extracting(Etapa::getId)
          .containsExactly(cenario.fazendo().getId());
    }

    @Test
    @DisplayName("reordenar reescreve a ordem sem colidir com a unicidade (workflow, ordem)")
    void reordenar() {
      etapaService.reordenar(
          cenario.workflow().getId(),
          List.of(cenario.fazendo().getId(), cenario.aFazer().getId(), cenario.concluido().getId()));

      assertThat(etapaService.listar(cenario.workflow().getId()))
          .extracting(Etapa::getNome)
          .containsExactly("Fazendo", "A Fazer", "Concluido");
    }

    @Test
    @DisplayName("reordenar com conjunto diferente das etapas do workflow e recusado")
    void reordenarConjuntoInvalido() {
      List<UUID> incompleta = List.of(cenario.fazendo().getId(), cenario.aFazer().getId());

      assertThatThrownBy(() -> etapaService.reordenar(cenario.workflow().getId(), incompleta))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("A ordem informada deve conter exatamente as etapas do workflow.");
    }

    @Test
    @DisplayName("etapa com tarefa ativa nao pode ser excluida (RN-005)")
    void naoExcluiEtapaComTarefa() {
      criarTarefa();

      assertThatThrownBy(() -> etapaService.excluir(cenario.aFazer().getId()))
          .isInstanceOf(RecursoEmUsoException.class)
          .hasMessage("A etapa possui tarefas ativas e nao pode ser excluida.");
    }

    @Test
    @DisplayName("excluir etapa intermediaria e recusado quando desconecta o grafo (RN-003)")
    void excluirEtapaIntermediariaDesconectaGrafo() {
      assertThatThrownBy(() -> etapaService.excluir(cenario.fazendo().getId()))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("A etapa \"A Fazer\" nao possui nenhuma transicao de saida.");
    }

    @Test
    @DisplayName("etapa recem-criada sem saida invalida o workflow, e a exclusao restaura")
    void etapaSemSaidaEDepoisRemovida() {
      Etapa revisao = etapaService.criar(cenario.workflow().getId(), "Revisao", false);

      assertThatThrownBy(() -> etapaService.validarWorkflow(cenario.workflow().getId()))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("A etapa \"Revisao\" nao possui nenhuma transicao de saida.");

      etapaService.excluir(revisao.getId());

      assertThatCode(() -> etapaService.validarWorkflow(cenario.workflow().getId()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("workflow sem etapa alguma e considerado valido: nada a violar ainda")
    void workflowVazioEhValido() {
      Workflow vazio = workflowService.criar(cenario.projeto().getId(), "Vazio");

      assertThatCode(() -> etapaService.validarWorkflow(vazio.getId())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("workflow sem etapa final e recusado")
    void workflowSemEtapaFinal() {
      Workflow outro = workflowService.criar(cenario.projeto().getId(), "Sem Final");
      etapaService.criar(outro.getId(), "Unica", false);

      assertThatThrownBy(() -> etapaService.validarWorkflow(outro.getId()))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("O workflow deve possuir exatamente uma etapa final.");
    }

    @Test
    @DisplayName("etapa inalcancavel a partir da primeira e recusada")
    void etapaInalcancavel() {
      Workflow outro = workflowService.criar(cenario.projeto().getId(), "Ilha");
      Etapa inicio = etapaService.criar(outro.getId(), "Inicio", false);
      Etapa fim = etapaService.criar(outro.getId(), "Fim", true);
      Etapa ilha = etapaService.criar(outro.getId(), "Ilha", false);
      transicaoService.criar(outro.getId(), inicio.getId(), fim.getId());
      transicaoService.criar(outro.getId(), ilha.getId(), fim.getId());

      assertThatThrownBy(() -> etapaService.validarWorkflow(outro.getId()))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("A etapa \"Ilha\" e inalcancavel a partir da primeira etapa.");
    }

    @Test
    @DisplayName("etapa inexistente resulta em recurso nao encontrado")
    void etapaInexistente() {
      UUID inexistente = UUID.randomUUID();
      assertThatThrownBy(() -> etapaService.atualizar(inexistente, "X"))
          .isInstanceOf(RecursoNaoEncontradoException.class)
          .hasMessage("Etapa nao encontrado(a).");
    }
  }

  @Nested
  @DisplayName("transicao")
  class Transicoes {

    @Test
    @DisplayName("self-loop e recusado")
    void selfLoop() {
      UUID workflowId = cenario.workflow().getId();
      UUID aFazer = cenario.aFazer().getId();

      assertThatThrownBy(() -> transicaoService.criar(workflowId, aFazer, aFazer))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("A etapa de origem e a de destino nao podem ser a mesma.");
    }

    @Test
    @DisplayName("duplicata e recusada")
    void duplicata() {
      UUID workflowId = cenario.workflow().getId();
      UUID aFazer = cenario.aFazer().getId();
      UUID fazendo = cenario.fazendo().getId();

      assertThatThrownBy(() -> transicaoService.criar(workflowId, aFazer, fazendo))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("Esta transicao ja esta configurada.");
    }

    @Test
    @DisplayName("etapa de outro workflow e recusada")
    void etapaDeOutroWorkflow() {
      Workflow outro = workflowService.criar(cenario.projeto().getId(), "Outro");
      Etapa forasteira = etapaService.criar(outro.getId(), "Forasteira", false);
      UUID workflowId = cenario.workflow().getId();
      UUID aFazer = cenario.aFazer().getId();

      assertThatThrownBy(() -> transicaoService.criar(workflowId, aFazer, forasteira.getId()))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("As etapas informadas nao pertencem a este workflow.");
    }

    @Test
    @DisplayName("listar devolve as arestas configuradas e destinosPermitidos filtra pelo grafo")
    void listarEDestinos() {
      assertThat(transicaoService.listar(cenario.workflow().getId()))
          .extracting(Transicao::getEtapaDestinoId)
          .containsExactlyInAnyOrder(cenario.fazendo().getId(), cenario.concluido().getId());
      assertThat(transicaoService.destinosPermitidos(cenario.concluido().getId())).isEmpty();
    }

    @Test
    @DisplayName("excluir aresta que desconecta o grafo e recusado por RN-003")
    void excluirDesconectando() {
      Transicao saidaInicial =
          transicaoService.listar(cenario.workflow().getId()).stream()
              .filter(t -> t.getEtapaOrigemId().equals(cenario.aFazer().getId()))
              .findFirst()
              .orElseThrow();

      assertThatThrownBy(() -> transicaoService.excluir(saidaInicial.getId()))
          .isInstanceOf(ConfiguracaoWorkflowInvalidaException.class)
          .hasMessage("A etapa \"A Fazer\" nao possui nenhuma transicao de saida.");
    }

    @Test
    @DisplayName("excluir aresta redundante mantem o grafo valido")
    void excluirRedundante() {
      Etapa atalho = etapaService.criar(cenario.workflow().getId(), "Atalho", false);
      transicaoService.criar(cenario.workflow().getId(), cenario.aFazer().getId(), atalho.getId());
      Transicao saidaDoAtalho =
          transicaoService.criar(
              cenario.workflow().getId(), atalho.getId(), cenario.concluido().getId());
      Transicao paraOAtalho =
          transicaoService.listar(cenario.workflow().getId()).stream()
              .filter(t -> t.getEtapaDestinoId().equals(atalho.getId()))
              .findFirst()
              .orElseThrow();

      transicaoService.excluir(saidaDoAtalho.getId());
      etapaService.excluir(atalho.getId());

      assertThat(transicaoService.listar(cenario.workflow().getId()))
          .extracting(Transicao::getId)
          .doesNotContain(paraOAtalho.getId());
    }

    @Test
    @DisplayName("transicao inexistente resulta em recurso nao encontrado")
    void transicaoInexistente() {
      UUID inexistente = UUID.randomUUID();
      assertThatThrownBy(() -> transicaoService.excluir(inexistente))
          .isInstanceOf(RecursoNaoEncontradoException.class);
    }
  }
}
