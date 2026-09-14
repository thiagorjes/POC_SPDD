package br.com.idsd.kanban.internal.acesso;

import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static br.com.idsd.kanban.suporte.Sujeitos.bruno;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.Sujeitos;
import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RF-001 e RF-002 — entrada no sistema e projetos visiveis.
 *
 * <p>SCN-001.1 e SCN-001.2 sao tipados {@code e2e} e vivem em Playwright: o que
 * eles afirmam e sobre a chegada a uma tela, e MockMvc nao verifica isso. Aqui
 * ficam a indisponibilidade do provedor e os tres cenarios de RF-002.
 */
class SessaoEProjetosIT extends TesteDeIntegracao {

    @Test
    @DisplayName("SCN-001.3 — provedor indisponivel recusa a entrada e nao oferece alternativa")
    void provedorIndisponivelRecusaSemOferecerAlternativa() throws Exception {
        // O JWKS do realm esta inalcancavel: o token nao pode ser validado.
        // `503` distingue indisponibilidade de credencial ruim, e `401` aqui
        // mandaria a pessoa tentar outra credencial que nao resolveria nada.
        provedorDeIdentidade().indisponivel();

        // Token de verdade no cabecalho, e nao o pos-processador `jwt()`: ele
        // injeta a autenticacao ja pronta e nunca chega ao decodificador, de
        // modo que este teste responderia `200` com o provedor fora do ar —
        // verde afirmando o contrario do que o cenario exige.
        mockMvc.perform(get("/v1/sessao")
                        .header("Authorization", "Bearer " + Sujeitos.bearerBemFormado()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty())
                // ADR-006: nao ha autenticacao local de emergencia. Qualquer
                // campo que aponte caminho alternativo reprova o cenario.
                .andExpect(jsonPath("$.autenticacaoAlternativa").doesNotExist())
                .andExpect(jsonPath("$.loginLocal").doesNotExist())
                .andExpect(jsonPath("$.fallback").doesNotExist());
    }

    @Test
    @DisplayName("SCN-002.1 — vejo os tres projetos e a permissao que tenho em cada um")
    void listaProjetosComAPermissaoDeCadaUm() throws Exception {
        var alfa = cenario.projeto("Alfa");
        var beta = cenario.projeto("Beta");
        var gama = cenario.projeto("Gama");
        cenario.participante(alfa, SUB_ANA, "Ana", "ana@empresa.example", "dev");
        cenario.participante(beta, SUB_ANA, "Ana", "ana@empresa.example", "product_owner");
        cenario.participante(gama, SUB_ANA, "Ana", "ana@empresa.example", "gestor");

        mockMvc.perform(get("/v1/projetos").with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Alfa')].papeis[0]").value("dev"))
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Beta')].papeis[0]").value("product_owner"))
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Gama')].papeis[0]").value("gestor"))
                // `permissoes` e derivado no servidor: o cliente usa para nao
                // apresentar acao que nao pode executar (RNF-004).
                //
                // O `[*]` no fim nao e enfeite. Caminho com filtro devolve uma
                // colecao cujo elemento e o proprio array de permissoes, e
                // `hasItem` compararia contra o array e nunca contra seus itens:
                // a forma positiva reprovava com a permissao presente, e a
                // negativa passava para qualquer resposta. O `[*]` achata a
                // colecao, e e sobre o item que a assercao passa a falar.
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Alfa')].permissoes[*]",
                        Matchers.hasItem("ESCREVER_TAREFA")))
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Beta')].permissoes[*]",
                        Matchers.hasItem("REABRIR")))
                // `gestor` e somente-leitura (RN-015). A ausencia e o ponto — e
                // por isso o caminho e afirmado nao vazio antes: assercao
                // negativa sobre colecao vazia passa sem verificar nada, que era
                // exatamente o defeito daqui.
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Gama')].permissoes[*]",
                        Matchers.hasItem("LER")))
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Gama')].permissoes[*]",
                        Matchers.not(Matchers.hasItem("ESCREVER_TAREFA"))));
    }

    @Test
    @DisplayName("SCN-002.2 — sem participacao a lista vem vazia, e nao com os projetos alheios")
    void semParticipacaoAListaVemVazia() throws Exception {
        var alheio = cenario.projeto("Projeto dos outros");
        cenario.participante(alheio, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        cenario.usuario(SUB_ANA, "Ana", "ana@empresa.example");

        mockMvc.perform(get("/v1/projetos").with(ana()))
                .andExpect(status().isOk())
                // Lista vazia e `200` com `conteudo: []`, nunca `404`: nao
                // participar de projeto algum e um estado legitimo do sistema.
                .andExpect(jsonPath("$.conteudo", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("SCN-002.3 — projeto sem participacao devolve 404 e nao revela dado algum")
    void projetoSemParticipacaoNaoERevelado() throws Exception {
        var alheio = cenario.projeto("Confidencial");
        cenario.participante(alheio, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        cenario.usuario(SUB_ANA, "Ana", "ana@empresa.example");

        mockMvc.perform(get("/v1/projetos/{id}", alheio).with(ana()))
                // `404` e nao `403`: o cenario exige que nenhum dado do projeto
                // seja revelado, e `403` ja revelaria que ele existe.
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.nome").doesNotExist())
                .andExpect(jsonPath("$.descricao").doesNotExist())
                .andExpect(jsonPath("$.detail", Matchers.not(Matchers.containsString("Confidencial"))));
    }

    @Test
    @DisplayName("SCN-002.3 — o board do projeto alheio tambem devolve 404")
    void boardDeProjetoSemParticipacaoTambemE404() throws Exception {
        var alheio = cenario.projeto("Confidencial");
        cenario.participante(alheio, SUB_BRUNO, "Bruno", "bruno@empresa.example", "project_admin");
        cenario.fluxoPadrao(alheio, bruno());
        cenario.usuario(SUB_ANA, "Ana", "ana@empresa.example");

        // A recusa nao pode valer so na rota de detalhe: qualquer porta de
        // entrada do projeto revela o mesmo dado.
        mockMvc.perform(get("/v1/projetos/{id}/board", alheio).with(ana()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.etapas").doesNotExist());
    }

    @Test
    @DisplayName("SCN-002.4 — o projeto sem etapa alguma vem marcado, e o configurado nao")
    void projetoSemFluxoVemMarcadoNaRelacao() throws Exception {
        var configurado = cenario.projeto("Com fluxo");
        var semFluxo = cenario.projeto("Sem fluxo");
        cenario.participante(
                configurado, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.participante(semFluxo, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");
        cenario.fluxoPadrao(configurado, ana());

        // Os dois na mesma resposta, de proposito. A marca so tem valor se
        // distinguir um projeto do outro dentro da mesma relacao: um campo que
        // viesse constante — sempre falso, ou sempre verdadeiro — passaria em
        // qualquer verificacao que olhasse um projeto de cada vez, e foi assim que
        // `fluxoConfigurado` ficou fixo em `List.of()` na criacao ate TASK-02.2.
        mockMvc.perform(get("/v1/projetos").with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Sem fluxo')].fluxoConfigurado")
                        .value(false))
                .andExpect(jsonPath("$.conteudo[?(@.nome=='Com fluxo')].fluxoConfigurado")
                        .value(true));
    }

    @Test
    @DisplayName("RF-001 — a sessao autoprovisiona o usuario pelo sub, sem cadastro local")
    void sessaoAutoprovisionaPeloSub() throws Exception {
        mockMvc.perform(get("/v1/sessao").with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@empresa.example"))
                .andExpect(jsonPath("$.adminGlobal").value(false));
    }

    @Test
    @DisplayName("RF-001 — sem token a sessao devolve 401")
    void semTokenA401() throws Exception {
        mockMvc.perform(get("/v1/sessao"))
                .andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.result.HeaderResultMatchers header() {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.header();
    }

    /** Controle do provedor de identidade simulado. Ver {@code ProvedorSimulado}. */
    private br.com.idsd.kanban.suporte.ProvedorSimulado provedorDeIdentidade() {
        return br.com.idsd.kanban.suporte.ProvedorSimulado.instancia();
    }
}
