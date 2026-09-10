package br.com.idsd.kanban.alem;

import static br.com.idsd.kanban.suporte.Sujeitos.SUB_ANA;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;

/**
 * Emissor de token para os testes de topologia.
 *
 * <p>Os demais testes de integracao autenticam por {@code jwt()} do
 * spring-security-test, que substitui o resolvedor e nunca chega a validar
 * assinatura. Isso serve para verificar contrato, mas nao serve aqui: estas
 * instancias sobem completas e falam por HTTP e WebSocket reais, entao o token
 * precisa ser um token de verdade, assinado e verificavel contra um JWKS.
 *
 * <p>O par de chaves e efemero e nasce com o processo de teste. O servidor de
 * chaves abaixo e minimo de proposito — ele existe para satisfazer a validacao
 * do resource server, e nao para simular o provedor de identidade, que ja tem
 * seu proprio duble.
 */
final class TokenDeTeste {

    private static final KeyPair PAR = gerarPar();
    private static final HttpServer SERVIDOR = publicarChaves();
    private static final String ID_DA_CHAVE = "teste";

    private TokenDeTeste() {}

    static String jwkSetUri() {
        return "http://localhost:" + SERVIDOR.getAddress().getPort() + "/jwks";
    }

    static String emissor() {
        return "http://localhost:" + SERVIDOR.getAddress().getPort() + "/realms/idsd";
    }

    static String paraAna() {
        return para(SUB_ANA, "Ana", "ana@empresa.example");
    }

    static String para(String sub, String nome, String email) {
        long agora = Instant.now().getEpochSecond();
        String cabecalho = """
                {"alg":"RS256","typ":"JWT","kid":"%s"}""".formatted(ID_DA_CHAVE);
        String corpo = """
                {"iss":"%s","sub":"%s","name":"%s","email":"%s","email_verified":true,\
                "iat":%d,"exp":%d}"""
                .formatted(emissor(), sub, nome, email, agora, agora + 3600);

        String naoAssinado = base64(cabecalho) + "." + base64(corpo);
        return naoAssinado + "." + assinar(naoAssinado);
    }

    // ------------------------------------------------------------ utilitarios

    private static String assinar(String conteudo) {
        try {
            var assinatura = Signature.getInstance("SHA256withRSA");
            assinatura.initSign((RSAPrivateKey) PAR.getPrivate());
            assinatura.update(conteudo.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura.sign());
        } catch (Exception erro) {
            throw new IllegalStateException("falha ao assinar o token de teste", erro);
        }
    }

    private static String base64(String texto) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static KeyPair gerarPar() {
        try {
            var gerador = KeyPairGenerator.getInstance("RSA");
            gerador.initialize(2048);
            return gerador.generateKeyPair();
        } catch (Exception erro) {
            throw new IllegalStateException("falha ao gerar o par de chaves de teste", erro);
        }
    }

    private static HttpServer publicarChaves() {
        try {
            var servidor = HttpServer.create(new InetSocketAddress(0), 0);
            var publica = (RSAPublicKey) PAR.getPublic();
            String jwks = """
                    {"keys":[{"kty":"RSA","use":"sig","alg":"RS256","kid":"%s","n":"%s","e":"%s"}]}"""
                    .formatted(
                            ID_DA_CHAVE,
                            semSinal(publica.getModulus()),
                            semSinal(publica.getPublicExponent()));
            servidor.createContext("/jwks", troca -> {
                byte[] corpo = jwks.getBytes(StandardCharsets.UTF_8);
                troca.getResponseHeaders().add("Content-Type", "application/json");
                troca.sendResponseHeaders(200, corpo.length);
                try (var saida = troca.getResponseBody()) {
                    saida.write(corpo);
                }
            });
            servidor.start();
            return servidor;
        } catch (Exception erro) {
            throw new IllegalStateException("falha ao publicar as chaves de teste", erro);
        }
    }

    private static String semSinal(java.math.BigInteger valor) {
        byte[] bytes = valor.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] cortado = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, cortado, 0, cortado.length);
            bytes = cortado;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
