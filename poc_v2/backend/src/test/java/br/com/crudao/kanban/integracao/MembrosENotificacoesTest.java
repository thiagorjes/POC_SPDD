package br.com.crudao.kanban.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.crudao.kanban.common.BusinessException;
import br.com.crudao.kanban.common.PermissaoNegadaException;
import br.com.crudao.kanban.common.RecursoNaoEncontradoException;
import br.com.crudao.kanban.notificacao.Notificacao;
import br.com.crudao.kanban.notificacao.NotificacaoService;
import br.com.crudao.kanban.rbac.CodigoPapel;
import br.com.crudao.kanban.rbac.CodigoPermissao;
import br.com.crudao.kanban.rbac.PermissaoService;
import br.com.crudao.kanban.rbac.Usuario;
import br.com.crudao.kanban.rbac.UsuarioProjetoPapelService;
import br.com.crudao.kanban.rbac.dto.MembroProjetoResponse;
import br.com.crudao.kanban.tarefa.Tarefa;
import br.com.crudao.kanban.tarefa.TarefaService;
import br.com.crudao.kanban.tarefa.TipoTarefa;
import br.com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import br.com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/** Bloco 19.2/19.5: papeis acumulaveis por projeto (RF-015) e caixa de notificacoes (RF-005). */
class MembrosENotificacoesTest extends IntegracaoBase {

  @Autowired private UsuarioProjetoPapelService usuarioProjetoPapelService;
  @Autowired private PermissaoService permissaoService;
  @Autowired private NotificacaoService notificacaoService;
  @Autowired private TarefaService tarefaService;

  private Usuario admin;
  private Usuario dev;
  private CenarioDeTeste.Cenario cenario;

  @BeforeEach
  void montar() {
    dev = cenarioDeTeste.autenticar("dev@kanban.test");
    admin = cenarioDeTeste.autenticarComoAdmin();
    cenario = cenarioDeTeste.criarProjetoCompleto("Projeto Membros", admin);
  }

  private UUID projetoId() {
    return cenario.projeto().getId();
  }

  @Nested
  @DisplayName("papeis por projeto")
  class Papeis {

    @Test
    @DisplayName("associar concede as permissoes do papel e e idempotente")
    void associarConcedePermissoes() {
      usuarioProjetoPapelService.associar(projetoId(), dev.getId(), CodigoPapel.DEV, admin);
      usuarioProjetoPapelService.associar(projetoId(), dev.getId(), CodigoPapel.DEV, admin);

      assertThat(permissaoService.papeis(dev.getId(), projetoId()))
          .containsExactly(CodigoPapel.DEV);
      assertThat(permissaoService.permissoesEfetivas(dev.getId(), projetoId()))
          .contains(CodigoPermissao.TAREFA_MOVER, CodigoPermissao.TAREFA_GERENCIAR)
          .doesNotContain(CodigoPermissao.PROJETO_ADMINISTRAR);
      assertThat(permissaoService.membro(dev.getId(), projetoId())).isTrue();
    }

    @Test
    @DisplayName("papeis acumulam: a permissao efetiva e a uniao dos papeis (BDR-001)")
    void papeisAcumulam() {
      usuarioProjetoPapelService.associar(projetoId(), dev.getId(), CodigoPapel.DEV, admin);
      usuarioProjetoPapelService.associar(projetoId(), dev.getId(), CodigoPapel.GESTOR, admin);

      assertThat(permissaoService.papeis(dev.getId(), projetoId()))
          .containsExactlyInAnyOrder(CodigoPapel.DEV, CodigoPapel.GESTOR);
      assertThat(permissaoService.permissoesEfetivas(dev.getId(), projetoId()))
          .contains(CodigoPermissao.TAREFA_MOVER, CodigoPermissao.DASHBOARD_VISUALIZAR);
    }

    @Test
    @DisplayName("o papel admin e global e protegido: nao entra no escopo de projeto (RN-006)")
    void papelGlobalNaoEntraNoProjeto() {
      UUID projetoId = projetoId();
      UUID devId = dev.getId();

      assertThatThrownBy(
              () -> usuarioProjetoPapelService.associar(projetoId, devId, CodigoPapel.ADMIN, admin))
          .isInstanceOf(BusinessException.class)
          .hasMessage("O papel informado nao pode ser atribuido no escopo de um projeto.");
    }

    @Test
    @DisplayName("papel fora do catalogo resulta em recurso nao encontrado")
    void papelForaDoCatalogo() {
      UUID projetoId = projetoId();
      UUID devId = dev.getId();

      assertThatThrownBy(
              () -> usuarioProjetoPapelService.associar(projetoId, devId, "arquiteto", admin))
          .isInstanceOf(RecursoNaoEncontradoException.class)
          .hasMessage("Papel nao encontrado.");
    }

    @Test
    @DisplayName("usuario inexistente nao pode ser associado")
    void usuarioInexistente() {
      UUID projetoId = projetoId();
      UUID inexistente = UUID.randomUUID();

      assertThatThrownBy(
              () ->
                  usuarioProjetoPapelService.associar(
                      projetoId, inexistente, CodigoPapel.DEV, admin))
          .isInstanceOf(RecursoNaoEncontradoException.class)
          .hasMessage("Usuario nao encontrado.");
    }

    @Test
    @DisplayName("desassociar remove o papel e e idempotente")
    void desassociar() {
      usuarioProjetoPapelService.associar(projetoId(), dev.getId(), CodigoPapel.DEV, admin);

      usuarioProjetoPapelService.desassociar(projetoId(), dev.getId(), CodigoPapel.DEV);
      usuarioProjetoPapelService.desassociar(projetoId(), dev.getId(), CodigoPapel.DEV);

      assertThat(permissaoService.papeis(dev.getId(), projetoId())).isEmpty();
      assertThat(permissaoService.membro(dev.getId(), projetoId())).isFalse();
    }

    @Test
    @DisplayName("listar membros devolve cada usuario com os papeis acumulados")
    void listarMembros() {
      usuarioProjetoPapelService.associar(projetoId(), dev.getId(), CodigoPapel.DEV, admin);
      usuarioProjetoPapelService.associar(projetoId(), dev.getId(), CodigoPapel.GESTOR, admin);

      List<MembroProjetoResponse> membros = usuarioProjetoPapelService.listarMembros(projetoId());

      assertThat(membros).hasSize(2);
      assertThat(membros)
          .filteredOn(membro -> membro.usuarioId().equals(dev.getId()))
          .singleElement()
          .satisfies(
              membro -> {
                assertThat(membro.email()).isEqualTo("dev@kanban.test");
                assertThat(membro.papeis())
                    .containsExactlyInAnyOrder(CodigoPapel.DEV, CodigoPapel.GESTOR);
              });
    }

    @Test
    @DisplayName("quem nao possui usuario:associar nao lista nem associa membros")
    void semPermissaoDeAssociar() {
      usuarioProjetoPapelService.associar(projetoId(), dev.getId(), CodigoPapel.DEV, admin);
      cenarioDeTeste.autenticar("dev@kanban.test");
      UUID projetoId = projetoId();

      assertThatThrownBy(() -> usuarioProjetoPapelService.listarMembros(projetoId))
          .isInstanceOf(PermissaoNegadaException.class)
          .hasMessage("Voce nao possui a permissao necessaria para esta acao neste projeto.");
    }
  }

  @Nested
  @DisplayName("notificacoes")
  class Notificacoes {

    private Tarefa tarefa;

    @BeforeEach
    void criarTarefaObservada() {
      usuarioProjetoPapelService.associar(
          projetoId(), dev.getId(), CodigoPapel.PRODUCT_OWNER, admin);
      tarefa =
          tarefaService.criar(
              projetoId(),
              new CriarTarefaRequest("Card observado", null, TipoTarefa.TAREFA, null, null, null),
              admin);
      cenarioDeTeste.autenticar("dev@kanban.test");
      tarefaService.mover(
          tarefa.getId(),
          new MoverTarefaRequest(cenario.fazendo().getId(), null, tarefa.getVersao()),
          dev);
    }

    @Test
    @DisplayName("o observador recebe a notificacao e o autor da acao nao")
    void observadorRecebeAutorNao() {
      assertThat(notificacaoService.listar(admin, true, PageRequest.of(0, 10)).getContent())
          .isNotEmpty();
      assertThat(notificacaoService.listar(dev, false, PageRequest.of(0, 10)).getContent())
          .isEmpty();
    }

    @Test
    @DisplayName("marcar como lida e idempotente e some da caixa de nao lidas")
    void marcarLida() {
      Notificacao notificacao =
          notificacaoService.listar(admin, true, PageRequest.of(0, 10)).getContent().getFirst();

      notificacaoService.marcarLida(notificacao.getId(), admin);
      notificacaoService.marcarLida(notificacao.getId(), admin);

      assertThat(notificacaoService.listar(admin, true, PageRequest.of(0, 10)).getContent())
          .isEmpty();
      assertThat(notificacaoService.listar(admin, false, PageRequest.of(0, 10)).getContent())
          .hasSize(1);
    }

    @Test
    @DisplayName("a notificacao de outro usuario nao pode ser marcada como lida")
    void notificacaoDeOutroUsuario() {
      UUID id =
          notificacaoService
              .listar(admin, true, PageRequest.of(0, 10))
              .getContent()
              .getFirst()
              .getId();

      assertThatThrownBy(() -> notificacaoService.marcarLida(id, dev))
          .isInstanceOf(PermissaoNegadaException.class)
          .hasMessage("A notificacao pertence a outro usuario.");
    }

    @Test
    @DisplayName("notificacao inexistente resulta em recurso nao encontrado")
    void notificacaoInexistente() {
      UUID inexistente = UUID.randomUUID();

      assertThatThrownBy(() -> notificacaoService.marcarLida(inexistente, admin))
          .isInstanceOf(RecursoNaoEncontradoException.class)
          .hasMessage("Notificacao nao encontrada.");
    }

    @Test
    @DisplayName("excluir a tarefa limpa as notificacoes que a referenciam")
    void exclusaoLimpaNotificacoes() {
      cenarioDeTeste.autenticarComoAdmin();

      assertThatCode(() -> tarefaService.excluir(tarefa.getId(), admin)).doesNotThrowAnyException();

      assertThat(notificacaoService.listar(admin, false, PageRequest.of(0, 10)).getContent())
          .isEmpty();
    }
  }
}
