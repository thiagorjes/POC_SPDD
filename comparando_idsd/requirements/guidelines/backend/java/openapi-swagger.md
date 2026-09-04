# OpenAPI / Swagger — Backend Java

SpringDoc OpenAPI. A documentação é o **contrato vivo** da aplicação.

## Política de atualização

- Todo novo endpoint nasce com documentação OpenAPI.
- Qualquer alteração de contrato (campo em record, novo status code, mudança de URL) é
  refletida **imediatamente** nas anotações.

## Tags

`@Tag` no nível da controller, agrupando por domínio:

```java
@Tag(name = "Clientes", description = "Operações de gerenciamento de clientes")
@RestController
@RequestMapping("/v1/clientes")
public class ClienteController { ... }
```

## @Operation

```java
@Operation(summary = "Busca um cliente pelo ID",
           description = "Retorna dados cadastrais consultando a base local e a API de integração.")
@GetMapping("/{id}")
public ClienteResponse buscar(@PathVariable String id) { ... }
```

## @Schema em records

```java
public record ClienteResponse(
    @Schema(description = "ID único do cliente", example = "UUID-123") String id,
    @Schema(description = "Nome completo", example = "João Silva") String nome
) {}
```

O SpringDoc lê Bean Validation (`@NotBlank`, `@Email`, `@Min`) — use para as restrições
aparecerem na doc.

## @ApiResponses

Documentar sucesso + erros específicos de negócio. Erro genérico (500) é global.
Com `ProblemDetail` (RFC 7807 — ver [`_shared/api-standards.md`](../../_shared/api-standards.md)):

```java
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
    @ApiResponse(responseCode = "404", description = "Cliente não localizado",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "422", description = "Dados de entrada inválidos")
})
```

## Boas práticas

- `operationId` explícito quando houver geração de client.
- `@Hidden` em endpoint de controle interno.
- **Produção:** Swagger UI protegida (VPN/auth) ou desabilitada — ver
  [`_shared/api-security.md`](../../_shared/api-security.md) §4. Habilitada em `dev`/`hml`.
