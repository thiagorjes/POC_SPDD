package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;

import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * O teto de tamanho do corpo, de ACH-05 da reexecucao de TASK-02.2.
 *
 * <p>A verificacao existe porque o teto so vale se for aplicado <b>antes</b> da
 * desserializacao, e essa e justamente a propriedade que nenhum outro teste
 * alcanca: {@code @Size} na lista de etapas recusa o mesmo corpo com o mesmo
 * {@code 422} depois de Jackson ter materializado o array inteiro na heap. O que
 * separa o teto do filtro do teto do bean e o <b>codigo</b>, e por isso o
 * predicado e {@code 413} exato.
 */
class TetoDeCorpoIT extends TesteDeIntegracao {

    @Value("${idsd.limite.corpo-em-bytes:262144}")
    private long teto;

    @Test
    @DisplayName("corpo acima do teto sai 413 antes de ser desserializado")
    void corpoAcimaDoTetoSai413() throws Exception {
        UUID projeto = cenario.projeto("Alfa");
        cenario.participante(
                projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");

        // Corpo sintaticamente valido e grande: o nome de uma unica etapa passa do
        // teto. Se a recusa viesse da validacao por anotacao, sairia `422` — o
        // `413` so aparece se o filtro cortou antes.
        String enorme = "x".repeat((int) teto + 1024);
        String corpo = "{ \"etapas\": [{\"nome\": \"" + enorme
                + "\", \"ordem\": 1, \"terminal\": true}] }";

        int status = mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/projetos/{projetoId}/etapas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertThat(status).isEqualTo(413);
    }

    @Test
    @DisplayName("corpo abaixo do teto atravessa o filtro sem ser tocado")
    void corpoAbaixoDoTetoAtravessa() throws Exception {
        UUID projeto = cenario.projeto("Beta");
        cenario.participante(
                projeto, SUB_ANA, "Ana", "ana@empresa.example", "dev", "project_admin");

        // A outra metade, e nao formalidade: envoltorio que conta bytes errado
        // truncaria corpo legitimo, e o sintoma seria corpo ilegivel em requisicao
        // comum. Esta linha e a que reprova essa regressao.
        int status = mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/projetos/{projetoId}/etapas", projeto)
                        .with(ana())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"etapas\": [{\"nome\": \"Feito\", \"ordem\": 1, "
                                + "\"terminal\": true}] }"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertThat(status).isEqualTo(200);
    }
}
