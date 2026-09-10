package br.com.idsd.kanban.internal.acesso;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /v1/sessao} — quem sou eu (RF-001).
 *
 * <p>Nao ha entrada alem do token. Nome e e-mail sao lidos das claims e nunca do
 * corpo da requisicao: aceita-los do cliente permitiria a qualquer pessoa
 * reescrever a propria identidade no sistema com um token valido.
 */
@RestController
@RequestMapping("/v1/sessao")
public class SessaoController {

    private final SessaoService sessoes;

    public SessaoController(SessaoService sessoes) {
        this.sessoes = sessoes;
    }

    @GetMapping
    public SessaoResposta sessao(JwtAuthenticationToken autenticacao) {
        Jwt token = autenticacao.getToken();
        return sessoes.entrar(
                token.getSubject(),
                token.getClaimAsString("name"),
                token.getClaimAsString("email"));
    }
}
