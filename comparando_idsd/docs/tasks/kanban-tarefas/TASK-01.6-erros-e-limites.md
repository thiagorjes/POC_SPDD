# TASK-01.6 — problem+json, negação por padrão e limite de requisições

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.4
- **Cenários cobertos:** SCN-002.3
- **Origem:** RNF-004, RNF-010, TechSpec Seção 8

#### Contexto

Todo erro do sistema sai no mesmo formato, e é esse formato que os cenários
congelados exercitam nos épicos seguintes. A task também institui a negação por
padrão e o limite de requisições, que aqui pesa mais do que num sistema comum:
cada escrita aceita dispara difusão de evento para todas as instâncias e todas
as sessões, então o custo de uma requisição abusiva é amplificado pelo desenho
de tempo real, e os envelopes de desempenho não têm outra proteção.

#### O que deve ser feito

- [ ] Implementar o tratador global de exceções produzindo
      `application/problem+json` em toda resposta de erro.
- [ ] Incluir `traceId` em todo corpo de erro e propagar o identificador de
      correlação por requisição.
- [ ] Fazer rota não mapeada negar por padrão.
- [ ] Implementar o limite de requisições por sujeito: **120 leituras** e
      **30 escritas por minuto**, com `429` e `Retry-After` em problem+json.
- [ ] Aplicar o limite também por origem.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/shared/TratadorDeErro.java` | criar | tratador global |
| `backend/src/main/java/<pkg>/shared/ProblemaDetalhado.java` | criar | corpo de erro |
| `backend/src/main/java/<pkg>/shared/FiltroDeCorrelacao.java` | criar | identificador por requisição |
| `backend/src/main/java/<pkg>/shared/LimiteDeRequisicoes.java` | criar | contagem por sujeito e por origem |
| `backend/src/main/java/<pkg>/config/SegurancaConfig.java` | alterar | negação por padrão, registro do filtro |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Forma do corpo de erro, literal:

```
{
  "type": "https://errors.idsd/<slug>",
  "title": "<frase curta em linguagem de negócio>",
  "status": 0,
  "detail": "<o que aconteceu, em linguagem de negócio>",
  "instance": "/v1/<recurso>",
  "traceId": "<...>"
}
```

Códigos comuns, válidos para todos os contratos do sistema:

| Código | Quando |
| --- | --- |
| `400` | corpo malformado ou parâmetro desconhecido |
| `401` | sem token, token inválido ou expirado |
| `403` | sem permissão |
| `404` | recurso inexistente, ou existente em projeto sem participação |
| `409` | estado de origem divergente |
| `422` | regra de negócio violada |
| `429` | limite de requisições excedido, com `Retry-After` |
| `503` | provedor de identidade indisponível, com `Retry-After` |

Envelope numérico do limite: 120 leituras e 30 escritas por minuto **por
sujeito**.

#### Guia técnico — pontos de atenção

- **`detail` fala a linguagem do negócio.** Mensagem técnica no `detail` vaza
  desenho interno e não ajuda quem lê.
- **Nunca inclua no corpo de erro dado do recurso negado.** É o mesmo raciocínio
  que faz o projeto de terceiro responder `404`.
- **Rota não mapeada nega por padrão.** Configuração permissiva com exceções
  ponto a ponto inverte o ônus e falha em silêncio quando alguém acrescenta uma
  rota.
- **O tratador não pode engolir o conflito de estado.** O corpo de `409` carrega
  um bloco adicional que uma task posterior acrescenta; deixe o ponto de
  extensão previsto em vez de serializar um corpo fixo.
- O limite conta por sujeito autenticado; requisição sem token já é recusada
  antes por `401`.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Todo erro sai em `application/problem+json` com `traceId` | uma requisição por código da tabela acima |
| 2 | Rota não mapeada é negada | requisição a caminho inexistente não vaza pilha nem devolve `200` |
| 3 | A 121ª leitura no mesmo minuto devolve `429` com `Retry-After` | teste de integração com relógio controlado |
| 4 | A 31ª escrita no mesmo minuto devolve `429` com `Retry-After` | idem |
| 5 | O corpo de erro não contém dado do recurso negado | inspeção do corpo em resposta `404` de projeto de terceiro |
| 6 | O identificador de correlação aparece no log e no corpo | comparação entre `traceId` da resposta e a linha de log |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
