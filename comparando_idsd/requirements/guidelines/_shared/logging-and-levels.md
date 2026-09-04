# Logging e Níveis (transversal)

> Como e o que registrar, em qualquer stack. Implementação concreta (biblioteca de log,
> anotações) em `backend/<stack>/coding-standards.md`.

## 1. Níveis

| Nível | Quando usar | Em produção |
|---|---|---|
| `ERROR` | Falha com impacto no usuário ou na integridade de dados; requer ação | ativo, alertável |
| `WARN` | Anomalia recuperável, degradação silenciosa, uso de fallback, retry | ativo |
| `INFO` | Evento relevante do fluxo normal (início/fim de operação de negócio, decisão importante) | ativo |
| `DEBUG` | Diagnóstico detalhado (payloads resumidos, ramos de decisão) | **desativado** |
| `TRACE` | Passo a passo fino, só investigação local | desativado |

Regras:

- Não registrar a mesma falha em vários níveis/camadas (log-and-throw duplicado). Registre
  **uma vez**, no ponto onde há contexto suficiente, e propague.
- `ERROR` e `WARN` sempre com contexto acionável: identificador da operação, do recurso e a causa.
- Exceção registrada em `ERROR` inclui o stack trace (objeto da exceção), nunca só `.getMessage()`.
- Nada de log dentro de loop quente sem _rate limiting_.

## 2. Formato

- **Structured logging** (JSON) em ambientes gerenciados; uma linha por evento.
- Campos mínimos: `timestamp` (ISO-8601 UTC), `level`, `logger`, `message`, `correlationId`, `service`, `env`.
- `correlationId` / `traceId` propagado de ponta a ponta e igual ao retornado no corpo de erro
  (ver [`api-standards.md`](api-standards.md)) e no header `X-Correlation-Id`.
- Mensagem em texto fixo + parâmetros estruturados; não concatenar valores na string
  (`"conta {} bloqueada", conta` — não `"conta " + conta + " bloqueada"`).

## 3. Proibido registrar (mesmo em DEBUG)

- Senhas, tokens (JWT, refresh, API key), secrets, headers `Authorization`
- CPF/CNPJ, dados de cartão, dados bancários completos, dados pessoais sensíveis sem mascaramento
- Payload íntegro de request/response com qualquer um dos itens acima
- Query com parâmetros contendo dado sensível

Quando o dado for necessário para diagnóstico, **mascarar** (`123.***.***-**`, `**** **** **** 1234`).

## 4. Correlação e rastreabilidade

- Todo request de entrada gera ou propaga `correlationId`; ele acompanha logs, chamadas a
  integrações externas (header repassado) e mensagens assíncronas.
- IDs de negócio relevantes (conta, pedido, remessa) vão como campos estruturados, não só na mensagem.

---

## Checklist

- [ ] Nível correto (ERROR impacto/ação · WARN recuperável · INFO fluxo · DEBUG off em prod)
- [ ] Falha registrada uma única vez, com contexto acionável e stack trace
- [ ] Log estruturado com `correlationId` propagado ponta a ponta
- [ ] Nenhum secret / dado pessoal / bancário sem máscara em qualquer nível
- [ ] Mensagem parametrizada, sem concatenação de valores
