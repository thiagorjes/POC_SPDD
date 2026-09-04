# Stack — Backend Java

> **Stack backend padrão e obrigatória do workspace: Java 21 + Spring Boot 3.x.**
> Todo novo serviço backend nasce com esta stack. Qualquer outra (C#, Kotlin, Node…) é
> exceção e exige ADR aprovado justificando o desvio + sua própria pasta `backend/<stack>/`
> gerada pelo template.
>
> Versões "gerenciado pelo Boot" seguem o BOM do Spring Boot.

## Linguagem

| Linguagem | Versão | Uso |
|---|---|---|
| Java | 21 (LTS) | Toda a aplicação. `--release 21`. Sem preview features em produção. |

## Framework principal

| Biblioteca | Versão | Finalidade |
|---|---|---|
| Spring Boot | 3.5.x (última minor estável da linha 3.x) | MVC, DI, auto-config, actuator |
| Spring Web MVC | gerenciado pelo Boot | API REST imperativa |
| Spring Security | gerenciado pelo Boot | Autenticação e autorização |
| Spring Security OAuth2 Resource Server | gerenciado pelo Boot | Validação de JWT do RH-SSO (ver `_shared/api-security.md`) |
| Spring Data JPA | gerenciado pelo Boot | Persistência |
| Spring WebFlux (`WebClient`) | gerenciado pelo Boot | Cliente HTTP para integrações (uso bloqueante com `.block()`) |
| Spring Cloud | 2024.0.x (trilho compatível com o Boot) | BOM cloud; WireMock para testes |
| Lombok | gerenciado pelo Boot | `@Slf4j`, `@Builder`, redução de boilerplate |
| SpringDoc OpenAPI | 2.6.x | Swagger UI / OpenAPI 3 |
| Flyway + Flyway Oracle | gerenciado pelo Boot | Migrations (dialeto Oracle) |
| Apache POI | 5.x | Leitura/geração de `.xlsx` |
| Commons-net | 3.x | FTP |

## Bibliotecas proprietárias

| Lib | Versão | Finalidade |
|---|---|---|
| `banestes-token-rhsso` | 2.1.0 | Obtenção e transcode de tokens RH-SSO |

> Resolução **somente** via Nexus corporativo. Ver `_shared/vulnerable-and-proprietary-libs.md`.

## Banco de dados

| Sistema | Driver | Uso | Observação |
|---|---|---|---|
| Oracle | `ojdbc11` | Produção / HML | Migrations Flyway com dialeto Oracle |
| H2 | gerenciado pelo Boot | Testes | `@DataJpaTest` + `@ActiveProfiles("test")` |

## Build e qualidade

| Ferramenta | Versão | Finalidade |
|---|---|---|
| Maven (wrapper) | — | Build, testes, empacotamento |
| JaCoCo | 0.8.12+ | Cobertura |
| SonarQube Maven Plugin | 3.11.x | Análise estática (perfil `Banestes_way`) |
| OWASP Dependency-Check ou scanner corporativo | — | SCA de CVEs (ver `_shared/vulnerable-and-proprietary-libs.md`) |

## Testes

| Ferramenta | Uso |
|---|---|
| JUnit 5 | Framework de testes (`spring-boot-starter-test`) |
| Mockito | Mocks de service |
| MockMvc | Testes de controller sem servidor |
| Spring Security Test | Contexto de segurança em teste |
| WireMock (Spring Cloud Contract) | Mock de APIs externas |
| H2 | Repositório em memória |

## Infraestrutura

- **Plataforma:** OpenShift Container Platform (OCP4 — `ioc.bcloud.sfb`)
- **Artefatos:** Nexus `nexus3-cicd-tools.cloud.sfb` (releases e snapshots)
- **CI/CD:** Pipeline OCP4 com SonarQube e SCA integrados
- **SonarQube:** `sonarqube.apps.ioc.bcloud.sfb` — Quality Profile `Banestes_way`
