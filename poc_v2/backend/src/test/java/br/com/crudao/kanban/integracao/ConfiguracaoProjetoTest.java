package br.com.crudao.kanban.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.ProjetoFinalizadoException;
import br.com.crudao.kanban.common.RecursoEmUsoException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.projeto.ChaveToggle;
import br.com.crudao.kanban.projeto.Projeto;
import br.com.crudao.kanban.projeto.ProjetoService;
import br.com.crudao.kanban.projeto.StatusProjeto;
import br.com.crudao.kanban.projeto.dto.AtualizarProjetoRequest;
import br.com.crudao.kanban.projeto.dto.CriarProjetoRequest;
import br.com.crudao.kanban.raia.Raia;
import br.com.crudao.kanban.raia.RaiaService;
import br.com.crudao.kanban.rbac.CodigoPapel;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.tarefa.TarefaService;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Bloco 19.2/19.5: ciclo de vida do projeto (RF-008), toggles (RF-016) e raias (RF-011). */
class ConfiguracaoProjetoTest extends IntegracaoBase {

  @Autowired private ProjetoService projetoService;
  @Autowired private RaiaService raiaService;
  @Autowired private TarefaService tarefaService;

  private Usuario admin;
  private CenarioDeTeste.Cenario cenario;

  @BeforeEach
  void montar() {
    admin = cenarioDeTeste.autenticarComoAdmin();
    cenario = cenarioDeTeste.criarProjetoCompleto("Projeto Base", admin);
  }

  private UUID criarTarefa(UUID raiaId) {
    return tarefaService
        .criar(
            cenario.projeto().getId(),
            new CriarTarefaRequest("Card", null, TipoTarefa.TAREFA, null, raiaId, null),
            admin)
        .getId();
  }

  @Nested
  @DisplayName("projeto")
  class Projetos {

    @Test
    @DisplayName("criar prepara toggles default, sequencia, raia padrao e o autor como admin")
    void criarPreparaOEstadoInicial() {
      assertThat(cenario.projeto().getStatus()).isEqualTo(StatusProjeto.ATIVO);
      assertThat(cenario.raia().isPadrao()).isTrue();
      assertThat(projetoService.toggles(cenario.projeto().getId()))
          .containsEntry(ChaveToggle.DEV_PODE_EXCLUIR_TAREFA, true)
          .containsEntry(ChaveToggle.DEV_PODE_FINALIZAR_TAREFA, false);
      assertThat(projetoService.permissoesEfetivas(cenario.projeto().getId(), admin).adminGlobal())
          .isTrue();
    }

    @Test
    @DisplayName("nome duplicado e recusado independentemente de caixa")
    void nomeDuplicado() {
      CriarProjetoRequest duplicado = new CriarProjetoRequest("projeto base", null);

      assertThatThrownBy(() -> projetoService.criar(duplicado, admin))
          .isInstanceOf(BusinessException.class)
          .hasMessage("Ja existe um projeto com este nome.");
    }

    @Test
    @DisplayName("apenas o administrador global cria projeto (ADR-007)")
    void apenasAdminGlobalCria() {
      Usuario comum = cenarioDeTeste.autenticar("comum@kanban.test");
      CriarProjetoRequest request = new CriarProjetoRequest("Projeto do Comum", null);

      assertThatThrownBy(() -> projetoService.criar(request, comum))
          .isInstanceOf(PermissaoNegadaException.class)
          .hasMessage("Apenas o administrador global pode criar projetos.");
    }

    @Test
    @DisplayName("renomear mantendo o proprio nome nao colide consigo mesmo")
    void renomearMantendoONome() {
      Projeto atualizado =
          projetoService.atualizar(
              cenario.projeto().getId(), new AtualizarProjetoRequest("Projeto Base", "Descricao"));

      assertThat(atualizado.getNome()).isEqualTo("Projeto Base");
      assertThat(atualizado.getDescricao()).isEqualTo("Descricao");
    }

    @Test
    @DisplayName("renomear para o nome de outro projeto e recusado")
    void renomearParaNomeExistente() {
      projetoService.criar(new CriarProjetoRequest("Outro Projeto", null), admin);
      UUID id = cenario.projeto().getId();
      AtualizarProjetoRequest request = new AtualizarProjetoRequest("Outro Projeto", null);

      assertThatThrownBy(() -> projetoService.atualizar(id, request))
          .isInstanceOf(BusinessException.class)
          .hasMessage("Ja existe um projeto com este nome.");
    }

    @Test
    @DisplayName("finalizar congela a escrita; reabrir e idempotente e a restaura")
    void finalizarEReabrirRestauramAEscrita() {
      UUID id = cenario.projeto().getId();

      assertThat(projetoService.finalizar(id).getStatus()).isEqualTo(StatusProjeto.FINALIZADO);
      // Finalizar de novo e uma escrita: o guard de projeto ativo (RN-015) barra antes do servico.
      assertThatThrownBy(() -> projetoService.finalizar(id))
          .isInstanceOf(ProjetoFinalizadoException.class)
          .hasMessage("O projeto esta finalizado e nao aceita alteracoes.");
      assertThat(projetoService.reabrir(id).getStatus()).isEqualTo(StatusProjeto.ATIVO);
      assertThat(projetoService.reabrir(id).getFinalizadoEm()).isNull();
      assertThatCode(() -> raiaService.criar(id, "Nova")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("projeto com tarefa ativa nao pode ser excluido (RN-005)")
    void naoExcluiComTarefaAtiva() {
      criarTarefa(null);
      UUID id = cenario.projeto().getId();

      assertThatThrownBy(() -> projetoService.excluir(id))
          .isInstanceOf(RecursoEmUsoException.class)
          .hasMessage("O projeto possui tarefas ativas e nao pode ser excluido.");
    }

    @Test
    @DisplayName("projeto sem tarefas e excluido e some da listagem")
    void excluiProjetoVazio() {
      Projeto vazio = projetoService.criar(new CriarProjetoRequest("Projeto Vazio", null), admin);

      projetoService.excluir(vazio.getId());

      assertThat(projetoService.listarDoUsuario(admin))
          .extracting(Projeto::getId)
          .doesNotContain(vazio.getId());
    }

    @Test
    @DisplayName("usuario sem papel nenhum enxerga uma lista vazia")
    void listaVaziaSemPapel() {
      Usuario forasteiro = cenarioDeTeste.autenticar("forasteiro@kanban.test");

      assertThat(projetoService.listarDoUsuario(forasteiro)).isEmpty();
    }

    @Test
    @DisplayName("membro enxerga apenas os projetos em que possui papel")
    void listaDoMembro() {
      Usuario dev = cenarioDeTeste.autenticar("dev@kanban.test");
      cenarioDeTeste.autenticarComoAdmin();
      projetoService.criar(new CriarProjetoRequest("Projeto Alheio", null), admin);
      cenarioDeTeste.associarPapel(cenario.projeto().getId(), dev, CodigoPapel.DEV, admin);

      assertThat(projetoService.listarDoUsuario(dev))
          .extracting(Projeto::getId)
          .containsExactly(cenario.projeto().getId());
    }

    @Test
    @DisplayName("nao membro nao obtem as permissoes efetivas do projeto")
    void naoMembroNaoObtemPermissoes() {
      Usuario forasteiro = cenarioDeTeste.autenticar("outro@kanban.test");
      UUID id = cenario.projeto().getId();

      assertThatThrownBy(() -> projetoService.permissoesEfetivas(id, forasteiro))
          .isInstanceOf(PermissaoNegadaException.class)
          .hasMessage("Voce nao possui acesso a este projeto.");
    }

    @Test
    @DisplayName("projeto inexistente resulta em recurso nao encontrado")
    void projetoInexistente() {
      UUID inexistente = UUID.randomUUID();

      assertThatThrownBy(() -> projetoService.buscar(inexistente))
          .isInstanceOf(RecursoNaoEncontradoException.class);
    }
  }

  @Nested
  @DisplayName("toggles")
  class Toggles {

    @Test
    @DisplayName("atualizar grava e devolve o mapa completo do catalogo")
    void atualizarToggles() {
      Map<ChaveToggle, Boolean> resultado =
          projetoService.atualizarToggles(
              cenario.projeto().getId(),
              Map.of(ChaveToggle.DEV_PODE_FINALIZAR_TAREFA.name(), true));

      assertThat(resultado)
          .hasSize(ChaveToggle.values().length)
          .containsEntry(ChaveToggle.DEV_PODE_FINALIZAR_TAREFA, true);
    }

    @Test
    @DisplayName("chave fora do catalogo e rejeitada em vez de ignorada (BDR-001)")
    void chaveDesconhecida() {
      UUID id = cenario.projeto().getId();
      Map<String, Boolean> invalido = Map.of("DEV_PODE_TUDO", true);

      assertThatThrownBy(() -> projetoService.atualizarToggles(id, invalido))
          .isInstanceOf(BusinessException.class)
          .hasMessage("O toggle informado nao pertence ao catalogo de configuracoes do projeto.");
    }
  }

  @Nested
  @DisplayName("raia")
  class Raias {

    @Test
    @DisplayName("a segunda raia nasce nao padrao e ao final da ordem")
    void segundaRaiaNaoEhPadrao() {
      Raia suporte = raiaService.criar(cenario.projeto().getId(), "Suporte");

      assertThat(suporte.isPadrao()).isFalse();
      assertThat(suporte.getOrdem()).isEqualTo(cenario.raia().getOrdem() + 1);
      assertThat(raiaService.listar(cenario.projeto().getId())).hasSize(2);
    }

    @Test
    @DisplayName("renomear altera apenas o nome")
    void renomear() {
      Raia renomeada = raiaService.atualizar(cenario.raia().getId(), "Time A");

      assertThat(renomeada.getNome()).isEqualTo("Time A");
      assertThat(renomeada.isPadrao()).isTrue();
    }

    @Test
    @DisplayName("definir padrao move a flag e mantem exatamente uma raia padrao")
    void definirPadrao() {
      Raia suporte = raiaService.criar(cenario.projeto().getId(), "Suporte");

      raiaService.definirPadrao(suporte.getId());

      assertThat(raiaService.listar(cenario.projeto().getId()))
          .filteredOn(Raia::isPadrao)
          .extracting(Raia::getId)
          .containsExactly(suporte.getId());
      assertThat(raiaService.padraoDoProjeto(cenario.projeto().getId()))
          .get()
          .extracting(Raia::getId)
          .isEqualTo(suporte.getId());
    }

    @Test
    @DisplayName("definir padrao na raia que ja e padrao e idempotente")
    void definirPadraoIdempotente() {
      Raia mesma = raiaService.definirPadrao(cenario.raia().getId());

      assertThat(mesma.isPadrao()).isTrue();
    }

    @Test
    @DisplayName("raia com tarefa ativa nao pode ser excluida (RN-005)")
    void naoExcluiRaiaComTarefa() {
      Raia suporte = raiaService.criar(cenario.projeto().getId(), "Suporte");
      criarTarefa(suporte.getId());

      assertThatThrownBy(() -> raiaService.excluir(suporte.getId()))
          .isInstanceOf(RecursoEmUsoException.class)
          .hasMessage("A raia possui tarefas ativas e nao pode ser excluida.");
    }

    @Test
    @DisplayName("a raia padrao nao sai enquanto houver outra raia no projeto")
    void naoExcluiRaiaPadraoComOutras() {
      raiaService.criar(cenario.projeto().getId(), "Suporte");
      UUID padraoId = cenario.raia().getId();

      assertThatThrownBy(() -> raiaService.excluir(padraoId))
          .isInstanceOf(RecursoEmUsoException.class)
          .hasMessage(
              "A raia padrao so pode ser excluida se for a unica do projeto e nao possuir"
                  + " tarefas.");
    }

    @Test
    @DisplayName("raia nao padrao e sem tarefas e removida")
    void excluiRaiaSimples() {
      Raia suporte = raiaService.criar(cenario.projeto().getId(), "Suporte");

      raiaService.excluir(suporte.getId());

      assertThat(raiaService.listar(cenario.projeto().getId())).hasSize(1);
    }

    @Test
    @DisplayName("raia inexistente resulta em recurso nao encontrado")
    void raiaInexistente() {
      UUID inexistente = UUID.randomUUID();

      assertThatThrownBy(() -> raiaService.atualizar(inexistente, "X"))
          .isInstanceOf(RecursoNaoEncontradoException.class);
    }
  }
}
