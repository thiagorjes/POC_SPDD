package br.com.idsd.kanban.suporte;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Provedor de identidade simulado.
 *
 * <p>Existe por um cenario so — SCN-001.3, a indisponibilidade — e por uma razao
 * que a colecao de backend ja registra: o autoconfigure do OAuth2 resolve o
 * issuer OIDC <b>eagerly na subida do contexto</b>. Sem alguem respondendo na
 * URL configurada, nenhum teste que sobe contexto completo inicializa, e a
 * suite inteira falharia por motivo que nada tem a ver com o contrato.
 *
 * <p>Simular o provedor e legitimo e esta declarado no plano de verificacao:
 * ele e dependencia externa, e o que esta sob verificacao aqui e o
 * comportamento do sistema <b>diante</b> dele, nao o provedor.
 */
public final class ProvedorSimulado {

    private static ProvedorSimulado instancia;

    private final WireMockServer servidor;

    private ProvedorSimulado() {
        this.servidor = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        this.servidor.start();
        disponivel();
    }

    public static synchronized ProvedorSimulado instancia() {
        if (instancia == null) {
            instancia = new ProvedorSimulado();
        }
        return instancia;
    }

    public String issuerUri() {
        return servidor.baseUrl() + "/realms/idsd";
    }

    /**
     * Onde o servidor de recurso busca a chave de assinatura.
     *
     * <p>E esta URL, e nao o issuer, que o perfil de teste configura: apontar o
     * issuer faria o autoconfigure resolver a descoberta OIDC <b>eagerly na
     * subida do contexto</b>, e todo teste que sobe contexto passaria a depender
     * de uma chamada de rede dar certo no instante exato do arranque.
     */
    public String jwkSetUri() {
        return issuerUri() + "/protocol/openid-connect/certs";
    }

    /**
     * Realm no ar: descoberta e JWKS respondem.
     *
     * <p>O corpo da descoberta e montado aqui, e nao lido de um arquivo de
     * fixture, porque ele contem URLs absolutas e a porta do servidor e sorteada
     * a cada execucao — um arquivo estatico estaria errado por construcao. Era o
     * que a versao anterior tentava fazer, apontando para um
     * {@code identidade/descoberta-200.json} que nunca existiu em disco.
     *
     * <p>O JWKS sai <b>sem chave nenhuma</b>, e isso e deliberado: nenhum teste
     * desta suite valida assinatura de verdade — a identidade e injetada pelo
     * pos-processador {@code jwt()}, que nao passa pelo decodificador. O unico
     * teste que exercita o decodificador e o da indisponibilidade, e o que ele
     * precisa e justamente que a busca da chave <b>falhe</b>.
     */
    public void disponivel() {
        servidor.resetAll();
        servidor.stubFor(WireMock.get(WireMock.urlPathEqualTo(
                        "/realms/idsd/.well-known/openid-configuration"))
                .willReturn(json(200, """
                        {
                          "issuer": "%1$s",
                          "jwks_uri": "%1$s/protocol/openid-connect/certs",
                          "authorization_endpoint": "%1$s/protocol/openid-connect/auth",
                          "token_endpoint": "%1$s/protocol/openid-connect/token",
                          "response_types_supported": ["code"],
                          "subject_types_supported": ["public"],
                          "id_token_signing_alg_values_supported": ["RS256"]
                        }
                        """.formatted(issuerUri()))));
        servidor.stubFor(WireMock.get(WireMock.urlPathEqualTo(
                        "/realms/idsd/protocol/openid-connect/certs"))
                .willReturn(json(200, "{\"keys\": []}")));
    }

    /**
     * Realm fora do ar. O JWKS nao pode ser buscado e, portanto, nenhum token
     * pode ser validado — que e a condicao de SCN-001.3.
     */
    public void indisponivel() {
        servidor.resetAll();
        servidor.stubFor(WireMock.get(WireMock.urlPathMatching("/realms/idsd/.*"))
                .willReturn(WireMock.aResponse().withStatus(503)));
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(
            int status, String corpo) {
        return WireMock.aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(corpo);
    }
}
