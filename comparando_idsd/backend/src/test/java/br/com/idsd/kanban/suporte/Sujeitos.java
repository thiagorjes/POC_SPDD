package br.com.idsd.kanban.suporte;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Identidades para os testes de contrato.
 *
 * <p>O token carrega identidade, e nada de permissao: por ADR-003 o provedor
 * autentica e as permissoes sao modeladas na aplicacao, por projeto. Um teste
 * que concedesse papel via claim estaria verificando um desenho que nao e o
 * deste sistema — e passaria em verde sobre RBAC que nunca foi exercitado.
 *
 * <p>O identificador estavel e o {@code sub}. O {@code email} e mutavel e, em
 * realm com autocadastro, atribuivel por quem se registra; ADR-010 existe por
 * causa disso, e nenhuma autorizacao aqui pode depender dele.
 */
public final class Sujeitos {

    public static final String SUB_ANA = "11111111-1111-1111-1111-111111111111";
    public static final String SUB_BRUNO = "22222222-2222-2222-2222-222222222222";
    public static final String SUB_CARLA = "33333333-3333-3333-3333-333333333333";
    public static final String SUB_DENIS = "44444444-4444-4444-4444-444444444444";
    public static final String SUB_ADMIN_GLOBAL = "99999999-9999-9999-9999-999999999999";

    private Sujeitos() {
    }

    public static RequestPostProcessor comoSub(String sub, String nome, String email) {
        return jwt().jwt(builder -> builder
                .subject(sub)
                .claim("name", nome)
                .claim("email", email)
                .claim("email_verified", true));
    }

    public static RequestPostProcessor ana() {
        return comoSub(SUB_ANA, "Ana", "ana@empresa.example");
    }

    public static RequestPostProcessor bruno() {
        return comoSub(SUB_BRUNO, "Bruno", "bruno@empresa.example");
    }

    public static RequestPostProcessor carla() {
        return comoSub(SUB_CARLA, "Carla", "carla@empresa.example");
    }

    public static RequestPostProcessor denis() {
        return comoSub(SUB_DENIS, "Denis", "denis@empresa.example");
    }

    public static RequestPostProcessor adminGlobal() {
        return comoSub(SUB_ADMIN_GLOBAL, "Admin", "admin@empresa.example");
    }

    /**
     * Um token <b>sintaticamente valido</b> e com assinatura que nao confere.
     *
     * <p>Serve a um caso so: fazer o servidor de recurso chegar ate a busca da
     * chave de assinatura. Os demais testes usam o pos-processador {@code jwt()},
     * que injeta a autenticacao ja pronta e nunca toca o decodificador — util
     * para verificar contrato, inutil para verificar o que acontece quando o
     * provedor nao responde.
     *
     * <p>Precisa ser bem formado, e nao uma cadeia qualquer: token que nao passa
     * no parse e recusado <b>antes</b> de o JWKS ser procurado, e o teste
     * receberia {@code 401} por credencial malformada — verde pelo motivo errado,
     * afirmando ter verificado uma indisponibilidade que nunca chegou a ocorrer.
     */
    public static String bearerBemFormado() {
        String cabecalho = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"chave-de-teste\"}");
        String corpo = base64Url("{\"sub\":\"" + SUB_ANA + "\",\"name\":\"Ana\","
                + "\"email\":\"ana@empresa.example\",\"exp\":4102444800}");
        return cabecalho + "." + corpo + "." + base64Url("assinatura-que-nao-confere");
    }

    private static String base64Url(String texto) {
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
