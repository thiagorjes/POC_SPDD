package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;
import static br.com.idsd.kanban.suporte.Sujeitos.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.idsd.kanban.suporte.ContagemDeConsultas;
import br.com.idsd.kanban.suporte.TesteDeIntegracao;
import jakarta.persistence.EntityManagerFactory;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * O custo da relacao de projetos nao cresce com a massa.
 *
 * <p>Verificacao <b>alem dos cenarios</b>: cenario algum descreve o custo de uma
 * leitura, e o criterio de aceite que proibia N+1 vinha sendo marcado por
 * inspecao do JPQL. Inspecao nao sobrevive a refatoracao — montar cada item
 * navegando a associacao produz o mesmo corpo de resposta, deixa todo cenario
 * verde e emite uma consulta por projeto.
 *
 * <p>A forma da assercao e deliberada e esta explicada em
 * {@link ContagemDeConsultas}: afirma-se <b>invariancia a massa</b>, e nunca um
 * numero absoluto de consultas. Numero absoluto quebra por mudanca inocua, e
 * teste que quebra por nada acaba desabilitado.
 *
 * <p>A semeadura fica <b>fora</b> do trecho medido. Ela usa JDBC direto e nao
 * passaria pelas estatisticas do Hibernate, mas depender disso seria fazer a
 * medicao valer por um detalhe do arnes de massa.
 *
 * <p>Origem: ACH-09 da revisao de TASK-01.5.
 */
class AusenciaDeNMaisUmIT extends TesteDeIntegracao {

    @Autowired
    private EntityManagerFactory fabrica;

    private int contador = 0;

    @Test
    @DisplayName("GET /v1/projetos custa o mesmo com 2 projetos e com 20")
    void relacaoNaoCresceComOVolume() throws Exception {
        var contagem = new ContagemDeConsultas(fabrica);

        semear(2, "dev");
        long comPoucos = contagem.durante(() -> relacaoEsperando(2));

        semear(18, "dev");
        long comMuitos = contagem.durante(() -> relacaoEsperando(20));

        assertThat(comMuitos)
                .as("2 projetos custaram %d consultas e 20 custaram %d — "
                                + "a contagem cresce com o volume, que e N+1",
                        comPoucos, comMuitos)
                .isEqualTo(comPoucos);
    }

    @Test
    @DisplayName("GET /v1/projetos custa o mesmo com 1 papel por projeto e com quatro")
    void relacaoNaoCresceComOsPapeisAcumulados() throws Exception {
        var contagem = new ContagemDeConsultas(fabrica);

        // Massa igual dos dois lados — dois projetos em ambos —, variando so o
        // numero de papeis. Os papeis abrem em linhas do mesmo join, entao
        // acumula-los nao pode custar consulta. E o ramo que uma troca de
        // projecao por navegacao de associacao quebraria primeiro, porque a
        // colecao de papeis e a parte lazy.
        semear(2, "dev");
        long comUmPapel = contagem.durante(() -> relacaoEsperando(2));

        esvaziarBanco();
        semear(2, "dev", "product_owner", "gestor", "project_admin");
        long comQuatroPapeis = contagem.durante(() -> relacaoEsperando(2));

        assertThat(comQuatroPapeis)
                .as("um papel por projeto custou %d consultas e quatro custaram %d — "
                                + "os papeis estao sendo buscados um a um",
                        comUmPapel, comQuatroPapeis)
                .isEqualTo(comUmPapel);
    }

    private void semear(int quantos, String... papeis) {
        for (int i = 0; i < quantos; i++) {
            var projeto = cenario.projeto("Projeto %03d".formatted(contador++));
            cenario.participante(projeto, SUB_ANA, "Ana", "ana@empresa.example", papeis);
        }
    }

    private void relacaoEsperando(int itens) throws Exception {
        mockMvc.perform(get("/v1/projetos").with(ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", Matchers.hasSize(itens)));
    }
}
