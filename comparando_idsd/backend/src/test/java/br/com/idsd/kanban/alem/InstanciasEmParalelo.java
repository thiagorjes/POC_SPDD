package br.com.idsd.kanban.alem;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Arnes de topologia: sobe N contextos completos da aplicacao contra o mesmo
 * banco, em portas distintas.
 *
 * <p>Existe porque {@code MockMvc} nao serve aqui. Ele nao tem porta, nao tem
 * WebSocket e, sobretudo, nao tem segunda instancia — verificar broadcast com
 * ele mediria a entrega dentro do proprio processo, que e o unico caminho que
 * nunca falha. O defeito que ADR-004 existe para impedir so aparece quando o
 * escritor e o ouvinte estao em processos diferentes.
 *
 * <p>Isto e suporte de teste, e nao codigo de producao: nada aqui e importado
 * pela aplicacao, e a unica coisa que o arnes conhece do sistema e o que o
 * contrato congelado ja expoe — a rota de criacao de tarefa e o destino STOMP.
 */
final class InstanciasEmParalelo {

    private InstanciasEmParalelo() {}

    static List<Instancia> subir(int quantidade, PostgreSQLContainer<?> postgres) {
        var instancias = new ArrayList<Instancia>();
        for (int i = 0; i < quantidade; i++) {
            instancias.add(new Instancia(postgres));
        }
        return List.copyOf(instancias);
    }

    /** O envelope de broadcast, como o contrato o declara. */
    record Envelope(UUID projetoId, UUID tarefaId, String tipo, long seq, String ocorridoEm) {}

    static final class Instancia {

        private final ConfigurableApplicationContext contexto;
        private final int porta;
        private final Map<String, StompSession.Subscription> inscricoes = new ConcurrentHashMap<>();
        private final Map<String, Assinatura> pendentes = new ConcurrentHashMap<>();

        private record Assinatura(UUID projeto, Consumer<Envelope> destino) {}

        Instancia(PostgreSQLContainer<?> postgres) {
            var aplicacao = new SpringApplication(br.com.idsd.kanban.Aplicacao.class);
            aplicacao.setDefaultProperties(Map.of(
                    "server.port", "0",
                    "spring.datasource.url", postgres.getJdbcUrl(),
                    "spring.datasource.username", postgres.getUsername(),
                    "spring.datasource.password", postgres.getPassword(),
                    // O schema ja foi migrado pela extensao da suite; migrar
                    // por instancia poria as tres em disputa pelo lock, que e
                    // exatamente o que ADR-011 tirou do boot.
                    "spring.flyway.enabled", "false",
                    "spring.jpa.hibernate.ddl-auto", "validate",
                    // Estas instancias validam assinatura de verdade: elas
                    // falam por HTTP e WebSocket reais, e nao pelo resolvedor
                    // substituido que os demais testes usam.
                    "spring.security.oauth2.resourceserver.jwt.issuer-uri",
                    TokenDeTeste.emissor(),
                    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                    TokenDeTeste.jwkSetUri(),
                    "spring.profiles.active", "test"));
            this.contexto = aplicacao.run();
            this.porta = ((WebServerApplicationContext) contexto).getWebServer().getPort();
        }

        UUID criarTarefa(UUID projeto, String titulo) {
            var resposta = RestClient.create()
                    .post()
                    .uri("http://localhost:" + porta + "/v1/projetos/{id}/tarefas", projeto)
                    .headers(this::autenticar)
                    .body(Map.of("titulo", titulo))
                    .retrieve()
                    .body(Map.class);
            return UUID.fromString(String.valueOf(resposta.get("id")));
        }

        void inscrever(UUID projeto, String sessao, Consumer<Envelope> destino) {
            pendentes.put(sessao, new Assinatura(projeto, destino));
            abrir(sessao);
        }

        /** Simula a queda da escuta desta instancia, sem derrubar o contexto. */
        void interromperEscuta() {
            inscricoes.values().forEach(StompSession.Subscription::unsubscribe);
            inscricoes.clear();
        }

        void retomarEscuta() {
            pendentes.keySet().forEach(this::abrir);
        }

        void derrubar() {
            interromperEscuta();
            contexto.close();
        }

        private void abrir(String sessao) {
            var assinatura = pendentes.get(sessao);
            var cliente = new WebSocketStompClient(new StandardWebSocketClient());
            cliente.setMessageConverter(new MappingJackson2MessageConverter());
            try {
                CompletableFuture<StompSession> futuro = cliente.connectAsync(
                        "ws://localhost:" + porta + "/ws",
                        cabecalhosAutenticados(),
                        new StompSessionHandlerAdapter() {});
                StompSession stomp = futuro.get(Duration.ofSeconds(10).toSeconds(),
                        java.util.concurrent.TimeUnit.SECONDS);
                var inscricao = stomp.subscribe(
                        "/topic/projetos/" + assinatura.projeto(),
                        new StompFrameHandler() {
                            @Override
                            public java.lang.reflect.Type getPayloadType(StompHeaders cabecalhos) {
                                return Envelope.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders cabecalhos, Object corpo) {
                                assinatura.destino().accept((Envelope) corpo);
                            }
                        });
                inscricoes.put(sessao, inscricao);
            } catch (Exception erro) {
                throw new IllegalStateException("nao foi possivel abrir a sessao " + sessao, erro);
            }
        }

        private org.springframework.web.socket.WebSocketHttpHeaders cabecalhosAutenticados() {
            var cabecalhos = new org.springframework.web.socket.WebSocketHttpHeaders();
            autenticar(cabecalhos);
            return cabecalhos;
        }

        private void autenticar(HttpHeaders cabecalhos) {
            cabecalhos.setBearerAuth(TokenDeTeste.paraAna());
        }
    }
}
