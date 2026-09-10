package br.com.idsd.kanban.suporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Monta a massa de teste.
 *
 * <p>Duas fontes, e a divisao entre elas e deliberada:
 *
 * <ul>
 *   <li><b>Pela API</b> — tudo que o produto sabe fazer. Etapas, raias, tarefas,
 *       movimentos, tomadas e impedimentos nascem por HTTP, contra o contrato
 *       congelado. Montar por dentro tornaria a suite cega a mudanca de
 *       contrato, que e justamente o que ela existe para travar.
 *   <li><b>Por SQL</b> — apenas <b>projeto</b> e <b>participacao inicial</b>.
 *       Desde a emenda de 2026-09-10 o produto expoe {@code POST /v1/projetos}
 *       (RF-022), e nao ha mais lacuna de especificacao aqui. A semeadura por
 *       SQL permanece por uma razao de forma de fixture, nao de ausencia de
 *       rota: a rota sempre cria uma primeira {@code project_admin}, e boa
 *       parte dos cenarios precisa de um projeto cujo conjunto de
 *       participantes seja <b>exatamente</b> o que o teste declara — inclusive
 *       projeto em que o sujeito e so {@code gestor}, e projeto sem
 *       participacao nenhuma, que SCN-002.2 exige. Semear pela rota tornaria
 *       esses estados inalcancaveis.
 * </ul>
 *
 * <p>A propria rota e verificada por {@code CriacaoDeProjetoIT}, contra o
 * contrato e sem passar por aqui.
 */
public class Cenario {

    private final MockMvc mockMvc;

    public Cenario(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // ---------------------------------------------------------------- semeadura

    /**
     * Le o id de um usuario ja autoprovisionado. Usado por quem precisa do id
     * sem semear nada — a criacao de projeto, por exemplo, exige que a pessoa
     * nomeada ja tenha entrado ao menos uma vez.
     */
    public UUID idDoUsuarioPorSub(String sub) {
        return idDoUsuario(sub);
    }

    /** Cria o projeto direto no banco. Ver a nota de forma de fixture no cabecalho. */
    public UUID projeto(String nome) {
        UUID id = UUID.randomUUID();
        executar(
                "INSERT INTO projeto (id, nome, descricao, criado_em, seq_atual)"
                        + " VALUES (?, ?, ?, ?, 0)",
                stmt -> {
                    stmt.setObject(1, id);
                    stmt.setString(2, nome);
                    stmt.setString(3, "Projeto de verificacao");
                    stmt.setObject(4, java.sql.Timestamp.from(Instant.now()));
                });
        return id;
    }

    /**
     * Vincula um sujeito ao projeto com os papeis indicados, autoprovisionando o
     * usuario a partir do {@code sub} — o mesmo vinculo que {@code GET /v1/sessao}
     * faz na primeira entrada.
     */
    public UUID participante(UUID projetoId, String sub, String nome, String email, String... papeis) {
        UUID usuarioId = usuario(sub, nome, email);
        UUID participacaoId = UUID.randomUUID();
        executar(
                "INSERT INTO participacao (id, usuario_id, projeto_id, criada_em)"
                        + " VALUES (?, ?, ?, ?)",
                stmt -> {
                    stmt.setObject(1, participacaoId);
                    stmt.setObject(2, usuarioId);
                    stmt.setObject(3, projetoId);
                    stmt.setObject(4, java.sql.Timestamp.from(Instant.now()));
                });
        for (String papel : papeis) {
            executar(
                    "INSERT INTO participacao_papel (participacao_id, papel) VALUES (?, ?)",
                    stmt -> {
                        stmt.setObject(1, participacaoId);
                        stmt.setString(2, papel);
                    });
        }
        return usuarioId;
    }

    public UUID usuario(String sub, String nome, String email) {
        UUID id = UUID.randomUUID();
        executar(
                "INSERT INTO usuario (id, subject_id, nome, email, admin_global, criado_em)"
                        + " VALUES (?, ?, ?, ?, false, ?)"
                        + " ON CONFLICT (subject_id) DO UPDATE SET nome = EXCLUDED.nome",
                stmt -> {
                    stmt.setObject(1, id);
                    stmt.setString(2, sub);
                    stmt.setString(3, nome);
                    stmt.setString(4, email);
                    stmt.setObject(5, java.sql.Timestamp.from(Instant.now()));
                });
        return idDoUsuario(sub);
    }

    /**
     * Recua o inicio de um intervalo em curso, para exercitar as contagens sem
     * que o teste espere o tempo passar. Toca apenas o instante de inicio: o
     * intervalo continua sendo o que a implementacao abriu.
     */
    public void recuarInicioDoIntervalo(UUID tarefaId, String tipo, java.time.Duration recuo) {
        executar(
                "UPDATE intervalo_tarefa SET inicio = inicio - ?::interval"
                        + " WHERE tarefa_id = ? AND tipo = ? AND fim IS NULL",
                stmt -> {
                    stmt.setString(1, recuo.toMinutes() + " minutes");
                    stmt.setObject(2, tarefaId);
                    stmt.setString(3, tipo);
                });
    }

    // -------------------------------------------------------------- pela API

    /** Fluxo minimo utilizavel: Backlog, Desenvolvimento, Review, Concluido. */
    public List<Etapa> fluxoPadrao(UUID projetoId, RequestPostProcessor quem) {
        String corpo = """
                { "etapas": [
                  { "nome": "Backlog",         "ordem": 1, "terminal": false },
                  { "nome": "Desenvolvimento", "ordem": 2, "terminal": false },
                  { "nome": "Review",          "ordem": 3, "terminal": false },
                  { "nome": "Concluido",       "ordem": 4, "terminal": true  }
                ] }
                """;
        String resposta = executarHttp(
                put("/v1/projetos/{projetoId}/etapas", projetoId)
                        .with(quem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo));
        List<String> ids = JsonPath.read(resposta, "$..id");
        List<String> nomes = JsonPath.read(resposta, "$..nome");
        return java.util.stream.IntStream.range(0, ids.size())
                .mapToObj(i -> new Etapa(UUID.fromString(ids.get(i)), nomes.get(i), i + 1))
                .toList();
    }

    public UUID criarTarefa(UUID projetoId, String titulo, RequestPostProcessor quem) {
        String resposta = executarHttp(
                post("/v1/projetos/{projetoId}/tarefas", projetoId)
                        .with(quem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"" + titulo + "\"}"));
        return UUID.fromString(JsonPath.read(resposta, "$.id"));
    }

    /** Le o cartao e devolve o bloco de origem exigido por toda escrita. */
    public String origemDe(UUID tarefaId, RequestPostProcessor quem) {
        String cartao = executarHttp(get("/v1/tarefas/{tarefaId}", tarefaId).with(quem));
        String etapaId = JsonPath.read(cartao, "$.etapaId");
        String condicao = JsonPath.read(cartao, "$.condicao");
        Number versao = JsonPath.read(cartao, "$.versao");
        return """
                { "etapaId": "%s", "condicao": "%s", "versao": %s }
                """.formatted(etapaId, condicao, versao);
    }

    public String comOrigem(UUID tarefaId, RequestPostProcessor quem, String camposExtras) {
        String origem = origemDe(tarefaId, quem);
        return camposExtras == null || camposExtras.isBlank()
                ? "{ \"origem\": " + origem + " }"
                : "{ \"origem\": " + origem + ", " + camposExtras + " }";
    }

    public void mover(UUID tarefaId, UUID etapaDestinoId, RequestPostProcessor quem) {
        executarHttp(
                post("/v1/tarefas/{tarefaId}/movimentos", tarefaId)
                        .with(quem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comOrigem(tarefaId, quem,
                                "\"etapaDestinoId\": \"" + etapaDestinoId + "\"")));
    }

    public void assumir(UUID tarefaId, RequestPostProcessor quem) {
        executarHttp(
                post("/v1/tarefas/{tarefaId}/tomada", tarefaId)
                        .with(quem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comOrigem(tarefaId, quem, null)));
    }

    public UUID abrirImpedimento(UUID tarefaId, String motivo, RequestPostProcessor quem) {
        String resposta = executarHttp(
                post("/v1/tarefas/{tarefaId}/impedimentos", tarefaId)
                        .with(quem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comOrigem(tarefaId, quem, "\"motivo\": \"" + motivo + "\"")));
        return UUID.fromString(JsonPath.read(resposta, "$.id"));
    }

    // -------------------------------------------------------------- utilitarios

    public record Etapa(UUID id, String nome, int ordem) {
    }

    private String executarHttp(org.springframework.test.web.servlet.RequestBuilder pedido) {
        try {
            return mockMvc.perform(pedido).andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw new IllegalStateException("falha ao montar o cenario", e);
        }
    }

    private UUID idDoUsuario(String sub) {
        try (Connection conexao = conectar();
                PreparedStatement stmt =
                        conexao.prepareStatement("SELECT id FROM usuario WHERE subject_id = ?")) {
            stmt.setString(1, sub);
            var resultado = stmt.executeQuery();
            resultado.next();
            return resultado.getObject(1, UUID.class);
        } catch (Exception e) {
            throw new IllegalStateException("usuario nao encontrado: " + sub, e);
        }
    }

    private void executar(String sql, Preparador preparador) {
        try (Connection conexao = conectar();
                PreparedStatement stmt = conexao.prepareStatement(sql)) {
            preparador.preparar(stmt);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("falha ao semear: " + sql, e);
        }
    }

    private Connection conectar() throws Exception {
        return DriverManager.getConnection(
                TesteDeIntegracao.POSTGRES.getJdbcUrl(),
                TesteDeIntegracao.POSTGRES.getUsername(),
                TesteDeIntegracao.POSTGRES.getPassword());
    }

    @FunctionalInterface
    private interface Preparador {
        void preparar(PreparedStatement stmt) throws Exception;
    }
}
