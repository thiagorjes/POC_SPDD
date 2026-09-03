package br.com.crudao.kanban.bdd;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Estado do cenario corrente. Cada cenario monta seu proprio projeto, de modo que a ordem de
 * execucao nao importa e nao ha limpeza global de tabelas entre cenarios.
 */
@Getter
@TestComponent
@RequiredArgsConstructor
public class MundoBdd {

  private final JdbcTemplate jdbc;

  private final Map<String, UUID> etapas = new HashMap<>();
  private final Map<String, UUID> usuarios = new HashMap<>();

  private UUID projetoId;
  private UUID workflowId;
  private UUID raiaId;
  private UUID tarefaId;
  private ResultActions resposta;
  private String corpoResposta;

  public void reiniciar() {
    etapas.clear();
    usuarios.clear();
    projetoId = null;
    workflowId = null;
    raiaId = null;
    tarefaId = null;
    resposta = null;
    corpoResposta = null;
  }

  public void registrarResposta(ResultActions resposta, String corpo) {
    this.resposta = resposta;
    this.corpoResposta = corpo;
  }

  public void definirTarefa(UUID tarefaId) {
    this.tarefaId = tarefaId;
  }

  /** Projeto ativo com workflow "A fazer" -> "Fazendo" -> "Concluido" e raia padrao. */
  public void criarProjetoComWorkflowPadrao() {
    String sufixo = UUID.randomUUID().toString().substring(0, 8);

    projetoId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projeto (id, nome, status) VALUES (?, ?, 'ATIVO')",
        projetoId,
        "Projeto " + sufixo);
    jdbc.update("INSERT INTO sequencia_projeto (projeto_id, ultimo_seq) VALUES (?, 0)", projetoId);

    workflowId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO workflow (id, projeto_id, nome, ativo) VALUES (?, ?, 'Padrao', true)",
        workflowId,
        projetoId);

    etapas.put("A fazer", criarEtapa("A fazer", 1, false));
    etapas.put("Fazendo", criarEtapa("Fazendo", 2, false));
    etapas.put("Concluido", criarEtapa("Concluido", 3, true));
    criarTransicao(etapas.get("A fazer"), etapas.get("Fazendo"));
    criarTransicao(etapas.get("Fazendo"), etapas.get("Concluido"));

    raiaId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO raia (id, projeto_id, nome, ordem, padrao) VALUES (?, ?, 'Padrao', 1, true)",
        raiaId,
        projetoId);
  }

  public UUID criarEtapa(String nome, int ordem, boolean etapaFinal) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO etapa (id, workflow_id, nome, ordem, etapa_final) VALUES (?, ?, ?, ?, ?)",
        id,
        workflowId,
        nome,
        ordem,
        etapaFinal);
    return id;
  }

  public void criarTransicao(UUID origem, UUID destino) {
    jdbc.update(
        "INSERT INTO transicao (id, workflow_id, etapa_origem_id, etapa_destino_id)"
            + " VALUES (?, ?, ?, ?)",
        UUID.randomUUID(),
        workflowId,
        origem,
        destino);
  }

  /**
   * O usuario e criado ja provisionado; o {@code sub} determinista permite que o MockMvc autentique
   * o mesmo apelido em qualquer passo do cenario.
   */
  public UUID criarUsuario(String apelido, boolean adminGlobal) {
    return usuarios.computeIfAbsent(
        apelido,
        chave -> {
          UUID id = UUID.randomUUID();
          jdbc.update(
              "INSERT INTO usuario (id, keycloak_sub, email, nome, admin_global)"
                  + " VALUES (?, ?, ?, ?, ?)",
              id,
              sub(chave),
              chave + "-" + id + "@exemplo.test",
              chave,
              adminGlobal);
          return id;
        });
  }

  public String sub(String apelido) {
    return "sub-" + apelido + "-" + projetoId;
  }

  public void associarPapel(String apelido, String codigoPapel) {
    UUID usuarioId = criarUsuario(apelido, false);
    jdbc.update(
        "INSERT INTO usuario_projeto_papel (usuario_id, projeto_id, papel_id)"
            + " SELECT ?, ?, p.id FROM papel p WHERE p.codigo = ?"
            + " ON CONFLICT DO NOTHING",
        usuarioId,
        projetoId,
        codigoPapel);
  }

  public void definirToggle(String chave, boolean habilitado) {
    jdbc.update(
        "INSERT INTO projeto_toggle (projeto_id, chave, habilitado) VALUES (?, ?, ?)"
            + " ON CONFLICT (projeto_id, chave) DO UPDATE SET habilitado = EXCLUDED.habilitado",
        projetoId,
        chave,
        habilitado);
  }

  public void finalizarProjeto() {
    jdbc.update("UPDATE projeto SET status = 'FINALIZADO' WHERE id = ?", projetoId);
  }

  /** Versao corrente da tarefa do cenario, para montar o {@code versaoEsperada} obrigatorio. */
  public long versaoDaTarefa() {
    Long versao =
        jdbc.queryForObject("SELECT versao FROM tarefa WHERE id = ?", Long.class, tarefaId);
    return versao == null ? 0L : versao;
  }

  /** Id da transicao pelo par de nomes de etapa, ex.: {@code "Fazendo>Concluido"}. */
  public UUID transicao(String origemDestino) {
    String[] partes = origemDestino.split(">");
    return jdbc.queryForObject(
        "SELECT id FROM transicao WHERE workflow_id = ? AND etapa_origem_id = ?"
            + " AND etapa_destino_id = ?",
        UUID.class,
        workflowId,
        etapa(partes[0]),
        etapa(partes[1]));
  }

  public UUID etapa(String nome) {
    UUID id = etapas.get(nome);
    if (id == null) {
      throw new IllegalArgumentException("Etapa desconhecida no cenario: " + nome);
    }
    return id;
  }

  public UUID usuario(String apelido) {
    UUID id = usuarios.get(apelido);
    if (id == null) {
      throw new IllegalArgumentException("Usuario desconhecido no cenario: " + apelido);
    }
    return id;
  }
}
