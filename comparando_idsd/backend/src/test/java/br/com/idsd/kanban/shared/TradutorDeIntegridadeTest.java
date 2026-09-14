package br.com.idsd.kanban.shared;

import java.sql.SQLException;
import org.assertj.core.api.Assertions;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * O tradutor <b>nominal</b> de violacao de integridade.
 *
 * <p>ACH-12 da reexecucao de TASK-02.2: o {@code 409} de
 * {@code etapa_projeto_ordem_unico} nasceu para fechar um bloqueante da revisao
 * anterior e nao tinha verificacao nenhuma. Ele e dificil de alcancar por HTTP de
 * proposito — toda recusa de ordem acontece na borda, antes da escrita —, e por
 * isso o tradutor e exercitado aqui, na unidade, e nao numa integracao que
 * precisaria de um defeito para chegar la.
 *
 * <p>As duas metades importam igualmente. A traducao sem a recusa faria toda
 * violacao imprevista sair como {@code 409}, que convida o cliente a retentar o
 * que nunca vai funcionar e apaga a diferenca entre conflito e defeito.
 */
class TradutorDeIntegridadeTest {

    private final TratadorDeErro tratador = new TratadorDeErro();

    @Test
    @DisplayName("violacao de etapa_projeto_ordem_unico sai 409 com slug proprio")
    void violacaoNominalSai409() throws Exception {
        var resposta = tratador.integridadeViolada(
                violacaoDe("etapa_projeto_ordem_unico"), requisicao());

        Assertions.assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(resposta.getBody()).isNotNull();
        Assertions.assertThat(resposta.getBody().getType().toString())
                .endsWith("conflito-de-ordem-de-etapa");
        // A mensagem do driver traz nome de indice, de coluna e o valor recusado:
        // ela fica no log, e nao no corpo.
        Assertions.assertThat(resposta.getBody().getDetail())
                .doesNotContain("etapa_projeto_ordem_unico");
    }

    @Test
    @DisplayName("restricao fora do catalogo sai 500, e nao 409")
    void violacaoImprevistaSai500() throws Exception {
        var resposta = tratador.integridadeViolada(violacaoDe("alguma_outra_unica"), requisicao());

        Assertions.assertThat(resposta.getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("violacao sem nome de restricao sai 500")
    void violacaoSemNomeSai500() throws Exception {
        var resposta = tratador.integridadeViolada(
                new DataIntegrityViolationException("sem causa nomeada"), requisicao());

        Assertions.assertThat(resposta.getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static DataIntegrityViolationException violacaoDe(String restricao) {
        return new DataIntegrityViolationException(
                "violacao",
                new ConstraintViolationException(
                        "violacao", new SQLException("detalhe do driver"), restricao));
    }

    private static MockHttpServletRequest requisicao() {
        var requisicao = new MockHttpServletRequest("PUT", "/v1/projetos/qualquer/etapas");
        return requisicao;
    }
}
