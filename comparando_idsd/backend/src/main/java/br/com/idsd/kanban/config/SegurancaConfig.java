package br.com.idsd.kanban.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Quem entra, e o que acontece com quem nao entra.
 *
 * <p>Servidor de recurso puro: nao ha sessao de servidor, nao ha formulario de
 * login e nao ha autenticacao local de emergencia (ADR-006). O token e a unica
 * credencial, e rota nao mapeada nega por padrao — a regra final e
 * {@code anyRequest().authenticated()}, e nao uma lista de rotas protegidas, para
 * que a rota criada amanha nasca fechada em vez de nascer aberta.
 *
 * <p>As unicas rotas abertas sao os probes: o healthcheck do compose os consulta
 * antes de existir qualquer token, e sem isso o conteiner nunca fica saudavel.
 * {@code show-details: never} garante que eles nao contam nada alem do estado.
 */
@Configuration
public class SegurancaConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SegurancaConfig.class);

    /**
     * Quanto tempo o cliente deve esperar antes de tentar de novo, quando o
     * provedor de identidade esta fora. Nao e promessa de recuperacao: e o que
     * impede que a recusa vire laco de tentativa a cada instante.
     */
    private static final String RETRY_AFTER_SEGUNDOS = "30";

    @Bean
    SecurityFilterChain filtros(
            HttpSecurity http,
            MappingJackson2HttpMessageConverter conversor) throws Exception {
        return http
                // Sem estado no servidor: nao ha cookie de sessao a falsificar, e
                // por isso nao ha CSRF a proteger. Desativar sem ser stateless
                // seria outra coisa.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint(conversor)))
                // Antes do filtro do bearer token porque precisa envolve-lo: a
                // falha de alcance ao provedor nao chega ao entry point, ela
                // escapa da cadeia inteira. Ver o javadoc do metodo.
                .addFilterBefore(
                        traducaoDeIndisponibilidade(conversor),
                        BearerTokenAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint(conversor))
                        .accessDeniedHandler(acessoNegado(conversor)))
                .build();
    }

    /**
     * A recusa de quem nao se autenticou, em {@code application/problem+json}.
     *
     * <p>Distingue <b>credencial ruim</b> de <b>provedor fora do ar</b>, e essa
     * distincao e o cerne de SCN-001.3: responder {@code 401} quando o JWKS esta
     * inalcancavel manda a pessoa tentar outra credencial, o que nao resolveria
     * nada e sugeriria que o problema e dela. {@code 503} com {@code Retry-After}
     * diz o que de fato aconteceu.
     *
     * <p>Nenhum dos dois oferece caminho alternativo. Nao existe autenticacao
     * local de emergencia (ADR-006), e um campo apontando para uma seria a porta
     * que o cenario existe para verificar que nao ha.
     */
    private AuthenticationEntryPoint entryPoint(MappingJackson2HttpMessageConverter conversor) {
        return (requisicao, resposta, excecao) -> {
            var problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED,
                    "Token ausente, invalido ou expirado.");
            problema.setTitle("Nao autenticado");
            resposta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            escrever(resposta, problema, conversor);
        };
    }

    private AccessDeniedHandler acessoNegado(MappingJackson2HttpMessageConverter conversor) {
        return (requisicao, resposta, excecao) -> {
            var problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Voce esta autenticado, mas nao tem permissao para esta operacao.");
            problema.setTitle("Sem permissao");
            escrever(resposta, problema, conversor);
        };
    }

    /**
     * Traduz "nao consegui falar com o provedor" em {@code 503}.
     *
     * <p>Precisa ser filtro, e nao entry point, por um detalhe que so a execucao
     * revela: quando o JWKS esta inalcancavel o Spring levanta
     * {@link AuthenticationServiceException}, que <b>escapa da cadeia de
     * seguranca</b> em vez de chegar ao entry point — o container despacha para o
     * tratamento de erro e a segunda passagem chega ao entry point ja anonima, com
     * um {@code InsufficientAuthenticationException} que nao distingue nada. Sem
     * este filtro, indisponibilidade do provedor responde {@code 401}, que e
     * exatamente o que SCN-001.3 recusa: mandaria a pessoa tentar outra
     * credencial para um problema que nao e dela.
     *
     * <p>{@link AuthenticationServiceException} e o tipo que o proprio Spring
     * reserva a "nao deu para processar por problema de sistema", em oposicao a
     * "a credencial nao presta" — token malformado ou expirado levanta
     * {@code InvalidBearerTokenException} e continua saindo em {@code 401}, como
     * deve. A captura e deliberadamente estreita: alarga-la transformaria
     * credencial ruim em {@code 503}, o erro oposto e o mais silencioso dos dois.
     *
     * <p>A resposta nao oferece caminho alternativo, porque nao ha: ADR-006
     * recusa autenticacao local de emergencia, e um campo apontando para uma seria
     * a porta que o cenario existe para verificar que nao existe.
     */
    private Filter traducaoDeIndisponibilidade(MappingJackson2HttpMessageConverter conversor) {
        return (requisicao, resposta, cadeia) -> {
            try {
                cadeia.doFilter(requisicao, resposta);
            } catch (AuthenticationServiceException indisponivel) {
                LOG.warn("Provedor de identidade inalcancavel; a entrada foi recusada com 503",
                        indisponivel);
                var problema = ProblemDetail.forStatusAndDetail(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "O provedor de identidade nao respondeu e o token nao pode ser validado."
                                + " Tente novamente em instantes.");
                problema.setTitle("Provedor de identidade indisponivel");
                var http = (HttpServletResponse) resposta;
                http.setHeader(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SEGUNDOS);
                escrever(http, problema, conversor);
            }
        };
    }

    private void escrever(
            HttpServletResponse resposta,
            ProblemDetail problema,
            MappingJackson2HttpMessageConverter conversor) throws IOException {
        resposta.setStatus(problema.getStatus());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        conversor.getObjectMapper().writeValue(resposta.getOutputStream(), problema);
    }
}
