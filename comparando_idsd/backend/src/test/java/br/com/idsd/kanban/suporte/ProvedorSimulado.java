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

    /** Realm no ar: descoberta e JWKS respondem. */
    public void disponivel() {
        servidor.resetAll();
        servidor.stubFor(WireMock.get(WireMock.urlPathMatching("/realms/idsd/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("identidade/descoberta-200.json")));
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
}
