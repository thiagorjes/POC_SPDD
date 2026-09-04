# Spring Boot 3 + Java 21 — Boas Práticas

> Substitui o antigo `GUIDELINE_SPRING.MD` (citado no README mas inexistente).
> Segurança de exposição (resource server, transcode RH-SSO): [`_shared/api-security.md`](../../_shared/api-security.md).

## Configuração

- `application.yml` por profile (`application-<profile>.yml`); profile ativo via
  `SPRING_PROFILES_ACTIVE`. Sem lógica de negócio dependendo de profile.
- Toda config parametrizável em `@ConfigurationProperties(prefix = "...")` (record) com
  `@Validated` e Bean Validation nos campos. Sem `@Value("${...}")` espalhado pelo código.
- Registrar via `@EnableConfigurationProperties(ApiSettings.class)`.
- Segredos (senha de banco, client secret) **não** vão no `application.yml` versionado —
  vêm de variável de ambiente injetada por secret do OpenShift / cofre.
- `spring.jpa.hibernate.ddl-auto=validate` (nunca `update`/`create` fora de teste).

## Web / REST

- `@RestController` fino; `@RequestMapping("/v1/<recurso>")` no nível da classe.
- `@Valid` em todo `@RequestBody`; `@Validated` na classe para validar `@PathVariable`/`@RequestParam`.
- Status explícito: `@ResponseStatus` ou `ResponseEntity` conforme [`_shared/api-standards.md`](../../_shared/api-standards.md).
- `@RestControllerAdvice` único traduz exceções → `ProblemDetail`. Sem `try/catch` de
  tradução no controller.
- Sem `ResponseEntity<Object>` genérico; tipar a resposta.

## Persistência

- Detalhes em [`database.md`](database.md). Resumo: entity só para escrita; leitura via
  projeção `record`; `@Transactional(readOnly = true)` em consulta; transação curta, sem I/O
  de rede dentro dela.

## Segurança

- `SecurityFilterChain` como bean; `authorizeHttpRequests` com **deny-by-default**
  (`.anyRequest().denyAll()` ao final).
- `oauth2ResourceServer(oauth2 -> oauth2.jwt(...))` com `JwtDecoder` validando `issuer` e
  `audience`; converter de authorities a partir de `scope`/roles do realm.
- CSRF desabilitado só para API stateless com JWT; `SessionCreationPolicy.STATELESS`.
- Chamada a downstream usa token transcodificado (lib `banestes-token-rhsso`), nunca o token
  recebido.

## Observabilidade

- `spring-boot-starter-actuator`: expor `health`, `info`, `prometheus`; **não** expor `env`,
  `beans`, `heapdump` em produção.
- `/actuator/health` com liveness/readiness para o OpenShift.
- Log estruturado + `correlationId` propagado (filtro/MDC) — ver [`_shared/logging-and-levels.md`](../../_shared/logging-and-levels.md).

## Resiliência

- Timeout obrigatório em todo `WebClient` (ver [`integrations.md`](integrations.md)).
- Retry só para falha transitória e idempotente; com limite e backoff.
- Sem `@Async`/thread manual sem necessidade; se usar, `TaskExecutor` configurado e limitado.

## Java 21

- `record` para DTO e projeção; `sealed` para hierarquias fechadas de domínio.
- `switch` com pattern matching em vez de cadeia de `if instanceof`.
- Sem preview features em produção.
- Virtual threads (`spring.threads.virtual.enabled=true`) permitido, mas avaliar impacto em
  código bloqueante com `synchronized` e pools de conexão antes de habilitar.

## Startup / build

- Fat jar via `spring-boot-maven-plugin`; imagem por Dockerfile/buildpack padrão da esteira.
- Falhar rápido: config inválida no startup derruba a aplicação (não degradar silenciosamente).

## Checklist

- [ ] Config em `@ConfigurationProperties` validado; segredos fora do yaml versionado
- [ ] `ddl-auto=validate`; migrations via Flyway
- [ ] `SecurityFilterChain` deny-by-default; resource server validando `iss` + `aud`; stateless
- [ ] Downstream com token transcodificado
- [ ] Actuator só com endpoints seguros expostos; health liveness/readiness
- [ ] Timeout em todo WebClient; retry só transitório/idempotente
- [ ] `@RestControllerAdvice` único → `ProblemDetail`
