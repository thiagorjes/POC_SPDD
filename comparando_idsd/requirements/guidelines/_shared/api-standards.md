# Padrões de API HTTP (transversal)

> Regras de contrato HTTP válidas para qualquer API do workspace, independente da stack.
> Autenticação e autorização: ver [`api-security.md`](api-security.md).

## 1. Versionamento

### Versionamento da API (exposição)

- Versão **major** no path: `/v1/...`, `/v2/...`. Sem versão em header ou query string.
- Uma nova major só é criada em **breaking change** de contrato (remoção/renome de campo,
  mudança de tipo, mudança de semântica de status, remoção de endpoint).
- Adições retrocompatíveis (campo opcional novo, novo endpoint, novo valor de enum tolerado
  pelo cliente) **não** geram nova major.
- Versões antigas ficam em depreciação anunciada (header `Deprecation` + `Sunset`) por um
  período mínimo acordado com os consumidores antes de serem removidas.

### Versionamento do artefato (SemVer 2.0.0)

O número de versão do serviço/lib segue **`MAJOR.MINOR.PATCH`**:

| Incremento | Quando |
|---|---|
| `MAJOR` | Mudança incompatível de contrato público (API HTTP, assinatura de lib) |
| `MINOR` | Funcionalidade nova retrocompatível |
| `PATCH` | Correção de bug retrocompatível |

- Pré-lançamento: `1.4.0-rc.1`. Build metadata: `+20260901`.
- `0.y.z` = instável; qualquer coisa pode mudar.
- A versão da API HTTP (`/v1`) e a versão SemVer do artefato são independentes:
  o artefato pode ir de `1.x` a `2.x` sem mudar `/v1` se o contrato HTTP não quebrou.

## 2. Códigos de status HTTP

### Sucesso

| Código | Uso |
|---|---|
| `200 OK` | GET com corpo; PUT/PATCH que retorna o recurso atualizado |
| `201 Created` | POST que cria recurso — incluir header `Location` |
| `202 Accepted` | Processamento assíncrono aceito, ainda não concluído |
| `204 No Content` | DELETE com sucesso; PUT/PATCH sem corpo de resposta |

### Erro do cliente (4xx)

| Código | Uso |
|---|---|
| `400 Bad Request` | Sintaxe/JSON inválido, parâmetro malformado |
| `401 Unauthorized` | Token ausente, inválido ou expirado |
| `403 Forbidden` | Autenticado, mas sem permissão para o recurso/ação |
| `404 Not Found` | Recurso inexistente (ou oculto por falta de permissão, quando não se quer revelar existência) |
| `405 Method Not Allowed` | Método HTTP não suportado pela rota |
| `409 Conflict` | Conflito de estado (duplicidade, versão otimista divergente) |
| `410 Gone` | Recurso/endpoint removido permanentemente |
| `415 Unsupported Media Type` | `Content-Type` não suportado |
| `422 Unprocessable Entity` | JSON válido, mas viola regra de validação/negócio |
| `429 Too Many Requests` | Rate limit — incluir `Retry-After` |

### Erro do servidor (5xx)

| Código | Uso |
|---|---|
| `500 Internal Server Error` | Falha inesperada não tratada |
| `502 Bad Gateway` | Resposta inválida de sistema a montante |
| `503 Service Unavailable` | Indisponibilidade temporária / dependência fora — incluir `Retry-After` |
| `504 Gateway Timeout` | Timeout em integração a montante |

**Regra:** validação de formato → `400`; validação de regra de negócio → `422`;
falha de permissão → `403` (nunca `200` com corpo de erro).

## 3. Corpo de erro — Problem Details (RFC 7807 / 9457)

Toda resposta de erro usa `application/problem+json` com, no mínimo:

```
{
  "type": "https://errors.<org>/validacao-conta",
  "title": "Conta inválida",
  "status": 422,
  "detail": "O dígito verificador da conta 12345-6 não confere",
  "instance": "/v1/extratos/12345-6",
  "traceId": "b7f3a1c2d4e5"
}
```

- `type` é URI estável por categoria de erro (pode ser documentação).
- `detail` é específico da ocorrência e **não** vaza stack trace, SQL, host interno nem dado sensível.
- `traceId` correlaciona com os logs (ver [`logging-and-levels.md`](logging-and-levels.md)).
- Erros de validação com múltiplos campos: incluir array `errors` com `{ field, message }`.

## 4. Convenções de contrato

- Recursos no plural, kebab-case: `/v1/ordens-pagamento`.
- Datas/hora em ISO-8601 UTC (`2026-09-01T12:30:00Z`).
- Paginação por `page` + `size` (0-based) ou cursor; resposta inclui `totalElements` / `totalPages` ou `nextCursor`.
- Filtros e ordenação via query string (`?status=ATIVO&sort=dataCriacao,desc`).
- Campos monetários: valor inteiro em centavos **ou** string decimal com escala fixa — nunca `float`.
- Nomes de campo em `camelCase` no JSON.
- Idempotência: POST que pode ser reenviado aceita header `Idempotency-Key`.

## 5. Headers padrão

| Header | Direção | Uso |
|---|---|---|
| `Authorization: Bearer <jwt>` | request | Token de acesso |
| `X-Correlation-Id` | request/response | Correlação ponta a ponta; se ausente, o serviço gera |
| `Location` | response | URI do recurso criado (`201`) |
| `Retry-After` | response | `429`, `503` |
| `Deprecation` / `Sunset` | response | Endpoint/versão em depreciação |

---

## Checklist

- [ ] Versão major no path; breaking change ⇒ nova major, não alteração da atual
- [ ] Versão do artefato segue SemVer 2.0.0
- [ ] Status codes conforme tabelas (400 formato / 422 negócio / 403 permissão)
- [ ] Erros em `application/problem+json` sem vazar interno
- [ ] `traceId`/`X-Correlation-Id` presentes e correlacionados ao log
- [ ] Datas ISO-8601 UTC; dinheiro sem `float`; paginação padronizada
