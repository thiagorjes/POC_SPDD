package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.SUB_BRUNO;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A regra unica de {@code 403} contra {@code 404} (TechSpec v1.8).
 *
 * <p>Verificacao <b>alem dos cenarios</b>: nenhum cenario congelado cobre o
 * participante sem papel, e a regra que ele exercita e a que o contrato fixa
 * para todo o produto — participacao e o eixo da existencia, papel e o eixo da
 * capacidade. Os cenarios cobrem as duas pontas (quem participa e le, quem nao
 * participa e recebe {@code 404}) e deixam de fora justamente o meio, que e o
 * ramo mais facil de inverter numa refatoracao: trocar o {@code 403} por
 * {@code 404}, ou remove-lo deixando o {@code 200}, nao deixava teste algum
 * vermelho.
 *
 * <p>Origem: ACH-07 da revisao de TASK-01.5.
 */
class ExistenciaECapacidadeIT extends TesteDeIntegracao {

    @Test
    @DisplayName("participante sem papel algum: aparece na relacao e recebe 403 no detalhe")
    void participanteSemPapelRecebe403() throws Exception {
        var projeto = cenario.projeto("Alfa");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example");

        // A relacao lista por participacao, e nao por permissao: o projeto
        // aparece com o conjunto de permissoes vazio. Esconde-lo aqui e
        // responder 404 adiante seriam a mesma decisao, e ela contradiria o
        // 200 que a pessoa acabou de receber.
        mockMvc.perform(get("/v1/projetos").with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Alfa"))
                .andExpect(jsonPath("$.conteudo[0].permissoes", Matchers.empty()))
                // O corolario da regra: `nome` vem porque e o que torna o 403
                // acionavel — sem ele a recusa fala de um identificador opaco.
                // `descricao` nao vem, porque e dado protegido por LER e esta
                // pessoa acabou de ser recusada por nao te-lo.
                .andExpect(jsonPath("$.conteudo[0].descricao").doesNotExist());

        mockMvc.perform(get("/v1/projetos/{id}", projeto).with(ana()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("participante so com `user` legado: mesmo desfecho — participar nao concede nada")
    void participanteApenasComUserRecebe403() throws Exception {
        var projeto = cenario.projeto("Beta");
        cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", "user");

        mockMvc.perform(get("/v1/projetos/{id}", projeto).with(ana()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("os dois vazios nao se confundem: quem nao participa recebe 404")
    void naoParticipanteRecebe404() throws Exception {
        var alheio = cenario.projeto("Gama");
        cenario.participante(alheio, SUB_BRUNO, "Bruno", "bruno@empresa.example", "dev");
        cenario.usuario(SUB_ANA, "Ana", "ana@empresa.example");

        // Este e o caso que faz o teste acima valer alguma coisa. As duas
        // pessoas tem conjunto de permissoes vazio, e derivar a distincao do
        // conjunto vazio e exatamente o erro: uma delas veria a existencia de
        // projeto de terceiro.
        mockMvc.perform(get("/v1/projetos/{id}", alheio).with(ana()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("projeto inexistente e projeto fora de alcance respondem igual")
    void projetoInexistenteTambemE404() throws Exception {
        cenario.usuario(SUB_ANA, "Ana", "ana@empresa.example");

        mockMvc.perform(get("/v1/projetos/{id}", java.util.UUID.randomUUID()).with(ana()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.nome").doesNotExist());
    }
}
