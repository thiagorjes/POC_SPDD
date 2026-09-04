# Integrações Externas (WebClient) — Backend Java

`WebClient` em ecossistema **imperativo (bloqueante)** com `.block()`. Substitui `RestTemplate`.
Autenticação da chamada (token transcodificado do RH-SSO): [`_shared/api-security.md`](../../_shared/api-security.md).

## Estrutura de pastas

`src/main/java/br/com/banestes/<app>/integrations/<nome-da-api>/`:

- `<NomeApi>Client.java` — chamada
- `<NomeApi>Config.java` — bean do `WebClient` (base URL, timeouts)
- `<NomeApi>Settings.java` — `@ConfigurationProperties`
- `models/` — DTOs de request/response **exclusivos** desta API

## Configuração do WebClient (timeouts obrigatórios)

```java
@Configuration
public class ClientesApiConfig {
    @Bean
    public WebClient webClientClientes(WebClient.Builder builder, ClientesApiSettings s) {
        return builder
            .baseUrl(s.baseUrl())
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().responseTimeout(s.responseTimeout())))
            .build();
    }
}
```

Nunca deixar timeout default — uma API externa lenta pendura todas as threads do Spring MVC.

## Client com retry

```java
@Component
public class ClientesApiClient {
    private final WebClient webClient;

    public ClientesApiClient(WebClient webClientClientes) {
        this.webClient = webClientClientes;
    }

    public ClienteResponse buscarCliente(String id) {
        return webClient.get()
            .uri("/clientes/{id}", id)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, r -> Mono.error(new ClienteIntegracaoException(...)))
            .bodyToMono(ClienteResponse.class)
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
            .block();
    }
}
```

## Boas práticas

- **Isolamento de models:** nunca usar entity JPA nem DTO da própria API na integração.
- **`.onStatus()`** antes de `.bodyToMono()` para traduzir 4xx/5xx em exceção de negócio.
- **Retry** só para falha transitória e operação idempotente; com limite e intervalo.
- **Timeout de integração** ⇒ mapear para `504`; indisponibilidade ⇒ `503` (ver `_shared/api-standards.md`).
- **Log** de requisição só em dev e sem dado sensível (ver [`_shared/logging-and-levels.md`](../../_shared/logging-and-levels.md)).
