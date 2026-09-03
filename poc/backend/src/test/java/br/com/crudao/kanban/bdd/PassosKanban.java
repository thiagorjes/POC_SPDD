package br.com.crudao.kanban.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Vocabulario de passos dos criterios de aceite. Os passos falam em HTTP porque e assim que o
 * criterio e observavel pelo cliente: status, {@code errorCode} e corpo fazem parte do contrato.
 */
public class PassosKanban {

  /** Referencias resolvidas em URL e corpo, ex.: {@code {etapa:Fazendo}}. */
  private static final Pattern REFERENCIA = Pattern.compile("\\{(\\w+)(?::([^}]+))?}");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private DataSource dataSource;
  @Autowired private MundoBdd mundo;

  @Value("${kanban.eventos.canal}")
  private String canal;

  private Connection ouvinte;

  @Before
  public void iniciarCenario() {
    mundo.reiniciar();
  }

  @After
  public void encerrarCenario() throws Exception {
    if (ouvinte != null) {
      ouvinte.close();
      ouvinte = null;
    }
  }

  // ---------------------------------------------------------------- contexto

  @Dado("um projeto ativo com o workflow padrao de tres etapas")
  public void umProjetoAtivo() {
    mundo.criarProjetoComWorkflowPadrao();
  }

  @Dado("o usuario {string} com o papel {string} no projeto")
  public void usuarioComPapel(String apelido, String papel) {
    mundo.associarPapel(apelido, papel);
  }

  @Dado("o usuario {string} sem papel no projeto")
  public void usuarioSemPapel(String apelido) {
    mundo.criarUsuario(apelido, false);
  }

  @Dado("o toggle {string} habilitado no projeto")
  public void toggleHabilitado(String chave) {
    mundo.definirToggle(chave, true);
  }

  @Dado("o toggle {string} desabilitado no projeto")
  public void toggleDesabilitado(String chave) {
    mundo.definirToggle(chave, false);
  }

  @Dado("que o projeto esta finalizado")
  public void projetoFinalizado() {
    mundo.finalizarProjeto();
  }

  @Dado("uma etapa {string} sem transicao de saida na ordem {int}")
  public void etapaSemSaida(String nome, int ordem) {
    mundo.getEtapas().put(nome, mundo.criarEtapa(nome, ordem, false));
  }

  @Dado("que {string} criou a tarefa {string}")
  public void criouTarefa(String apelido, String titulo) throws Exception {
    executar(
        apelido,
        "POST",
        "/api/projetos/{projeto}/tarefas",
        """
        {"titulo": "%s", "tipo": "FEATURE"}
        """
            .formatted(titulo));
    assertThat(statusAtual()).as("criacao de tarefa de preparacao do cenario").isEqualTo(201);
    JsonNode corpo = objectMapper.readTree(mundo.getCorpoResposta());
    mundo.definirTarefa(UUID.fromString(corpo.get("id").asText()));
  }

  @Dado("que {string} moveu a tarefa para {string}")
  public void moveuTarefaPara(String apelido, String etapa) throws Exception {
    moverPara(apelido, etapa);
    assertThat(statusAtual()).as("movimentacao de preparacao do cenario").isEqualTo(200);
  }

  @Dado("que o board esta escutando eventos")
  public void boardEscutando() throws Exception {
    ouvinte = dataSource.getConnection();
    try (Statement statement = ouvinte.createStatement()) {
      statement.execute("LISTEN " + canal);
    }
  }

  // ------------------------------------------------------------------ acoes

  @Quando("{string} envia {word} para {string}")
  public void enviaSemCorpo(String apelido, String metodo, String url) throws Exception {
    executar(apelido, metodo, url, null);
  }

  @Quando("{string} envia {word} para {string} com o corpo:")
  public void enviaComCorpo(String apelido, String metodo, String url, String corpo)
      throws Exception {
    executar(apelido, metodo, url, corpo);
  }

  @Quando("uma requisicao anonima e enviada para {string}")
  public void requisicaoAnonima(String url) throws Exception {
    ResultActions acoes = mockMvc.perform(MockMvcRequestBuilders.get(resolver(url)));
    mundo.registrarResposta(acoes, acoes.andReturn().getResponse().getContentAsString());
  }

  @Quando("{string} move a tarefa para {string}")
  public void moveTarefa(String apelido, String etapa) throws Exception {
    moverPara(apelido, etapa);
  }

  // -------------------------------------------------------------- assercoes

  @Entao("o status da resposta e {int}")
  public void statusDaResposta(int esperado) {
    assertThat(statusAtual()).as("corpo: %s", mundo.getCorpoResposta()).isEqualTo(esperado);
  }

  @Entao("o errorCode e {string}")
  public void errorCodeE(String esperado) throws Exception {
    assertThat(json().get("errorCode").asText()).isEqualTo(esperado);
  }

  @Entao("a mensagem de erro e {string}")
  public void mensagemDeErroE(String esperada) throws Exception {
    assertThat(json().get("message").asText()).isEqualTo(esperada);
  }

  @Entao("a mensagem de erro nao expoe detalhe de infraestrutura")
  public void mensagemSemDetalheDeInfraestrutura() throws Exception {
    String mensagem = json().get("message").asText();
    assertThat(mensagem)
        .as("mensagem de erro visivel ao cliente")
        .doesNotContainIgnoringCase("select ")
        .doesNotContainIgnoringCase("constraint")
        .doesNotContainIgnoringCase("exception")
        .doesNotContainIgnoringCase("org.postgresql")
        .doesNotContainIgnoringCase("br.com.crudao");
  }

  @Entao("o campo {string} da resposta e {string}")
  public void campoDaRespostaE(String caminho, String esperado) throws Exception {
    assertThat(no(caminho).asText()).isEqualTo(resolver(esperado));
  }

  @Entao("o campo {string} da resposta e verdadeiro")
  public void campoVerdadeiro(String caminho) throws Exception {
    assertThat(no(caminho).asBoolean()).as(caminho).isTrue();
  }

  @Entao("o campo {string} da resposta e falso")
  public void campoFalso(String caminho) throws Exception {
    assertThat(no(caminho).asBoolean()).as(caminho).isFalse();
  }

  @Entao("o campo {string} da resposta e nulo")
  public void campoNulo(String caminho) throws Exception {
    JsonNode atual = json();
    for (String parte : caminho.split("\\.")) {
      atual = parte.matches("\\d+") ? atual.get(Integer.parseInt(parte)) : atual.get(parte);
      if (atual == null) {
        return;
      }
    }
    assertThat(atual.isNull()).as("campo %s deveria ser nulo, veio %s", caminho, atual).isTrue();
  }

  @Entao("a resposta e uma lista nao vazia")
  public void respostaListaNaoVazia() throws Exception {
    assertThat(json()).as("corpo: %s", mundo.getCorpoResposta()).isNotEmpty();
  }

  @Entao("a resposta e uma lista com {int} itens")
  public void respostaListaComItens(int esperado) throws Exception {
    assertThat(json()).as("corpo: %s", mundo.getCorpoResposta()).hasSize(esperado);
  }

  @Entao("a lista {string} da resposta tem {int} itens")
  public void listaTemItens(String caminho, int esperado) throws Exception {
    assertThat(no(caminho)).hasSize(esperado);
  }

  @Entao("a lista {string} da resposta nao esta vazia")
  public void listaNaoVazia(String caminho) throws Exception {
    assertThat(no(caminho)).isNotEmpty();
  }

  @Entao("a lista {string} da resposta contem {string}")
  public void listaContem(String caminho, String esperado) throws Exception {
    String alvo = resolver(esperado);
    assertThat(no(caminho)).anyMatch(item -> alvo.equals(item.asText()));
  }

  @Entao("a lista {string} da resposta nao contem {string}")
  public void listaNaoContem(String caminho, String esperado) throws Exception {
    String alvo = resolver(esperado);
    assertThat(no(caminho)).noneMatch(item -> alvo.equals(item.asText()));
  }

  @Entao("um evento {string} chega ao board em ate {int} segundos")
  public void eventoChegaEmAte(String tipo, int segundos) throws Exception {
    long limite = System.currentTimeMillis() + segundos * 1000L;
    while (System.currentTimeMillis() < limite) {
      PGNotification[] recebidas = ouvinte.unwrap(PGConnection.class).getNotifications(200);
      if (recebidas == null) {
        continue;
      }
      for (PGNotification notificacao : recebidas) {
        JsonNode evento = objectMapper.readTree(notificacao.getParameter());
        if (mundo.getProjetoId().toString().equals(evento.get("projetoId").asText())
            && tipo.equals(evento.get("tipo").asText())) {
          return;
        }
      }
    }
    throw new AssertionError("Evento " + tipo + " nao chegou em " + segundos + "s");
  }

  // ---------------------------------------------------------------- apoio

  private void moverPara(String apelido, String etapa) throws Exception {
    JsonNode atual = buscarTarefa(apelido);
    executar(
        apelido,
        "PATCH",
        "/api/tarefas/{tarefa}/mover",
        """
        {"etapaDestinoId": "%s", "versaoEsperada": %d}
        """
            .formatted(mundo.etapa(etapa), atual.get("versao").asLong()));
  }

  private JsonNode buscarTarefa(String apelido) throws Exception {
    MockHttpServletResponse resposta =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/tarefas/" + mundo.getTarefaId())
                    .with(autenticar(apelido)))
            .andReturn()
            .getResponse();
    return objectMapper.readTree(resposta.getContentAsString());
  }

  private void executar(String apelido, String metodo, String url, String corpo) throws Exception {
    String alvo = resolver(url);
    MockHttpServletRequestBuilder requisicao =
        switch (metodo.toUpperCase()) {
          case "GET" -> MockMvcRequestBuilders.get(alvo);
          case "POST" -> MockMvcRequestBuilders.post(alvo);
          case "PUT" -> MockMvcRequestBuilders.put(alvo);
          case "PATCH" -> MockMvcRequestBuilders.patch(alvo);
          case "DELETE" -> MockMvcRequestBuilders.delete(alvo);
          default -> throw new IllegalArgumentException("Metodo HTTP nao suportado: " + metodo);
        };
    requisicao.with(autenticar(apelido));
    if (corpo != null) {
      requisicao.contentType(MediaType.APPLICATION_JSON).content(resolver(corpo));
    }
    ResultActions acoes = mockMvc.perform(requisicao);
    mundo.registrarResposta(acoes, acoes.andReturn().getResponse().getContentAsString());
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor autenticar(
      String apelido) {
    mundo.criarUsuario(apelido, false);
    return jwt()
        .jwt(
            builder ->
                builder
                    .subject(mundo.sub(apelido))
                    .claim("email", apelido + "@exemplo.test")
                    .claim("name", apelido));
  }

  /** Troca {@code {projeto}}, {@code {tarefa}}, {@code {etapa:Nome}} etc. pelos ids do cenario. */
  private String resolver(String texto) {
    Matcher matcher = REFERENCIA.matcher(texto);
    StringBuilder saida = new StringBuilder();
    while (matcher.find()) {
      String tipo = matcher.group(1);
      String argumento = matcher.group(2);
      String valor =
          switch (tipo) {
            case "projeto" -> String.valueOf(mundo.getProjetoId());
            case "workflow" -> String.valueOf(mundo.getWorkflowId());
            case "raia" -> String.valueOf(mundo.getRaiaId());
            case "tarefa" -> String.valueOf(mundo.getTarefaId());
            case "etapa" -> String.valueOf(mundo.etapa(argumento));
            case "usuario" -> String.valueOf(mundo.usuario(argumento));
            case "versao" -> String.valueOf(mundo.versaoDaTarefa());
            case "transicao" -> String.valueOf(mundo.transicao(argumento));
            default -> matcher.group();
          };
      matcher.appendReplacement(saida, Matcher.quoteReplacement(valor));
    }
    matcher.appendTail(saida);
    return saida.toString();
  }

  private int statusAtual() {
    return mundo.getResposta().andReturn().getResponse().getStatus();
  }

  private JsonNode json() throws Exception {
    return objectMapper.readTree(mundo.getCorpoResposta());
  }

  private JsonNode no(String caminho) throws Exception {
    JsonNode atual = json();
    for (String parte : caminho.split("\\.")) {
      if (parte.matches("\\d+")) {
        atual = atual.get(Integer.parseInt(parte));
      } else {
        atual = atual.get(parte);
      }
      assertThat(atual).as("caminho %s no corpo %s", caminho, mundo.getCorpoResposta()).isNotNull();
    }
    return atual;
  }
}
