# Testes — Backend Java

## 1. Estratégia

| Camada | Ferramenta | Foco |
|---|---|---|
| `controller` | `MockMvc` | Contrato: request válido → response, validação campo a campo |
| `service` | `Mockito` | Regra de negócio isolada |
| `repository` | H2 (`@DataJpaTest`) | Queries e mapeamento de entidade |
| `integrations` | WireMock + JSON estático | Simulação de API externa |

## 2. Integrações externas com WireMock

Respostas nunca hardcoded no Java. Arquivos JSON em `src/test/resources/mappings/<nome-da-api>/`
(ex.: `mappings/correios/get-address-200.json`, `get-address-404.json`).

```java
@SpringBootTest
@AutoConfigureWireMock(port = 0, stubs = "classpath:/mappings/<nome-da-api>")
class ExternalIntegrationTest { /* stubs carregados automaticamente */ }
```

Uma pasta por API; arquivos de sucesso e de erro.

## 3. Controller com MockMvc

Obrigatório validar **cada campo** do JSON de resposta, não só o status.

```java
@Test
void deveRetornarSucessoAoBuscarCliente() throws Exception {
    mockMvc.perform(get("/v1/clientes/{id}", 1L).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.nome").value("João Silva"))
        .andExpect(jsonPath("$.email").value("joao@email.com"))
        .andExpect(jsonPath("$.status").value("ATIVO"));
}
```

Endpoints protegidos: usar `spring-security-test` (`jwt()` post-processor) para simular o token
do RH-SSO com os scopes/roles necessários.

## 4. Repository com H2

```java
@DataJpaTest
@ActiveProfiles("test")
class ClienteRepositoryTest {
    @Autowired ClienteRepository repository;

    @Test
    void devePersistirEntidade() {
        var salvo = repository.save(new ClienteEntity("Teste", "teste@email.com"));
        assertNotNull(salvo.getId());
        assertEquals("Teste", salvo.getNome());
    }
}
```

## 5. Contexto Spring com OAuth2 client

O autoconfigure do `spring-boot-starter-oauth2-client` resolve o *issuer* OIDC
(`ClientRegistrationRepository`) **eagerly na subida do `ApplicationContext`**.
Todo teste que sobe contexto completo — `@SpringBootTest`, ou `@WebMvcTest` que
importe a `SecurityConfig` real — exige o provedor de identidade acessível na
URL configurada; sem ele o contexto nem inicializa.

- Teste que só usa Mockito, sem contexto Spring, não tem esse requisito.
- Declare no README do sistema o comando que sobe as dependências antes de
  `mvn test` (ex.: `docker compose up -d keycloak postgres`, aguardando
  `healthy`).
- Alternativa preferida em `@WebMvcTest`: substituir a configuração real por uma
  de teste e simular o token com `spring-security-test` (§3), em vez de exigir
  provedor no ar.

**Verificador:** `mvn test` a partir de um clone limpo, seguindo apenas o README.

## 6. Protocolo para a IA

1. **Asserts exaustivos** em controller/integrations — todos os campos via `jsonPath`.
2. **Simplicidade** — teste é `Envia Request → Checa Response`; sem lógica complexa no teste.
3. **Separação de mocks** — uma pasta `mappings/<api>/` por integração, com sucesso e erro.
4. **MockMvc** sempre para endpoint de `controller`.
5. Se o JSON for grande, não ignore campos — liste todos os `jsonPath` para travar o contrato.

## Cobertura

- JaCoCo; meta geral **> 80%** (recomendado, ver [`definition-of-done.md`](definition-of-done.md)).
- Cobrir fluxo principal + casos de erro do domínio.
