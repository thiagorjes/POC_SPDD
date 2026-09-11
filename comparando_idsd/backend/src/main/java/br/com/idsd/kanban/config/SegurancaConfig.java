package br.com.idsd.kanban.config;

import br.com.idsd.kanban.shared.FiltroDeCorrelacao;
import br.com.idsd.kanban.shared.LimiteDeRequisicoes;
import br.com.idsd.kanban.shared.ProblemaDetalhado;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
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

    /**
     * O envelope de RNF-010, por sujeito autenticado.
     *
     * <p>Configuravel porque o teste precisa apertar o numero para provar o
     * mecanismo, e nao porque o envelope seja negociavel: o valor de producao e o
     * que esta escrito aqui como padrao.
     *
     * <p>Nao ha mais dimensao por origem — ver o javadoc de
     * {@link LimiteDeRequisicoes}.
     */
    @Value("${idsd.limite.leituras-por-sujeito:120}")
    private int leiturasPorSujeito;

    @Value("${idsd.limite.escritas-por-sujeito:30}")
    private int escritasPorSujeito;

    /**
     * O identificador de correlacao, registrado <b>fora</b> da cadeia de seguranca
     * e antes dela.
     *
     * <p>Precisa envolver a cadeia inteira, porque as recusas que mais precisam de
     * correlacao — {@code 401}, {@code 503} e {@code 429} — sao produzidas dentro
     * dela e nunca chegam ao despachante. Registrar por
     * {@link FilterRegistrationBean} em vez de expor um bean de {@link Filter} e
     * deliberado: o Boot registra sozinho todo bean de filtro, e o registro
     * automatico nao permite fixar a precedencia.
     *
     * <p><b>Vale tambem no despacho de erro</b> (ACH-10 da revisao de TASK-01.6).
     * O padrao de {@link FilterRegistrationBean} e so {@code REQUEST}, e o
     * contêiner reprocessa o despacho de erro numa passagem propria: sem
     * {@code ERROR} declarado, o MDC ja teria sido limpo quando o tratador de
     * ultimo recurso monta o {@code 500} — justamente a resposta cuja correlacao
     * com o stacktrace e a razao de este filtro existir. {@code ASYNC} entra pelo
     * mesmo motivo, antes que a primeira rota assincrona apareca e perca a
     * correlacao sem que ninguem note.
     */
    @Bean
    FilterRegistrationBean<FiltroDeCorrelacao> registroDaCorrelacao() {
        var registro = new FilterRegistrationBean<>(new FiltroDeCorrelacao());
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registro.setDispatcherTypes(
                DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.ASYNC);
        return registro;
    }

    /**
     * O relogio que o limite consulta.
     *
     * <p>E bean para que a verificacao da janela possa substitui-lo. Sem isso, um
     * teste de limite depende de a janela nao virar no meio dele — o resultado
     * passa a variar com o instante em que a suite roda, que e a forma mais cara
     * de teste intermitente.
     */
    @Bean
    Clock relogioDoLimite() {
        return Clock.systemUTC();
    }

    @Bean
    SecurityFilterChain filtros(
            HttpSecurity http,
            MappingJackson2HttpMessageConverter conversor,
            Clock relogio) throws Exception {
        var limite = new LimiteDeRequisicoes(
                new LimiteDeRequisicoes.Envelope(leiturasPorSujeito, escritasPorSujeito),
                conversor.getObjectMapper(),
                relogio);
        return http
                // Sem estado no servidor: nao ha cookie de sessao a falsificar, e
                // por isso nao ha CSRF a proteger. Desativar sem ser stateless
                // seria outra coisa.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        // Os tres caminhos exatos, e nao `/actuator/health/**`
                        // (ACH-04 da revisao de TASK-01.6). O curinga abre todo
                        // grupo de health que alguem vier a declarar, inclusive um
                        // criado para depurar producao com detalhe de dependencia —
                        // e a abertura seria retroativa e silenciosa, porque nascer
                        // publico nao quebra nada que se perceba. Grupo novo que
                        // precise ser publico entra aqui por decisao.
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness").permitAll()
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
                // Depois do filtro do bearer token, e nao antes: a contagem e por
                // sujeito autenticado, e antes dele nao ha sujeito nenhum — o
                // limite contaria todo mundo junto, na mesma chave vazia.
                .addFilterAfter(limite, BearerTokenAuthenticationFilter.class)
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
            var problema = ProblemaDetalhado.de(
                    HttpStatus.UNAUTHORIZED,
                    "nao-autenticado",
                    "Nao autenticado",
                    "Token ausente, invalido ou expirado.",
                    requisicao.getRequestURI());
            resposta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            escrever(resposta, problema, conversor);
        };
    }

    private AccessDeniedHandler acessoNegado(MappingJackson2HttpMessageConverter conversor) {
        return (requisicao, resposta, excecao) -> {
            var problema = ProblemaDetalhado.de(
                    HttpStatus.FORBIDDEN,
                    "sem-permissao",
                    "Sem permissao",
                    "Voce esta autenticado, mas nao tem permissao para esta operacao.",
                    requisicao.getRequestURI());
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
                var http = (HttpServletResponse) resposta;
                var problema = ProblemaDetalhado.de(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "provedor-indisponivel",
                        "Provedor de identidade indisponivel",
                        "O provedor de identidade nao respondeu e o token nao pode ser validado."
                                + " Tente novamente em instantes.",
                        ((jakarta.servlet.http.HttpServletRequest) requisicao).getRequestURI());
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
