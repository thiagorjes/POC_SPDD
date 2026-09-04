---
description: "Converge a exploração de solução e o protótipo em escopo de entrega — regras de negócio, requisitos funcionais e não-funcionais, critérios de aceite em Gherkin com ID estável e procedência declarada por regra. Sai no gate de spec, a partir do qual os cenários viram contrato congelado. Use após /solution e /design."
applyTo: "**"
---

<!-- GERADO por .agents/scripts/generate_platform.py — nao editar. -->
<!-- Fonte: .agents/skills/prd/SKILL.md -->

# /prd

## Objetivo

Fechar o segundo diamante. Você recebe uma direção aprovada, um mapa de
comportamento e — quando há interface — um protótipo navegável. Sua função é
transformar isso em **o que precisa ser verdade**: regras, requisitos e
critérios de aceite verificáveis, cada um com procedência declarada.

O Gherkin que sai daqui é contrato. Ele alimenta o `/tests`, que escreve os step
definitions sem ver a implementação, e é distribuído pelos épicos no `/tasks`.
Cenário mal escrito aqui vira defeito caro três etapas adiante.

## O risco desta skill

Rodando depois do `/solution` e do `/design`, o PRD tende a virar transcrição.
Ele só se justifica se mantiver trabalho autoral próprio:

- **RNF** — latência, volume, retenção, disponibilidade. Ninguém antes tratou.
- **Regras invisíveis na tela** — autorização, auditoria, cálculo, compliance.
  O protótipo não as mostra e a exploração de solução raramente as detalha.
- **O Gherkin** — transformar comportamento descrito em cenário verificável é
  autoria, não cópia.

Se o PRD não tiver essas três coisas, ele é redundante e deve ser encurtado, não
inflado.

## Argumentos

- (sem argumento) — feature ativa
- `"nome-da-feature"` — feature específica
- `--cenarios N` — número alvo de cenários por RF (padrão: definido na Fase 1)
- `emenda` — abre emenda de cenário já congelado
- `revisar` — reabre antes do gate

## Pré-condições

- `docs/solution/[feature]-solution.md` existe.
- `docs/shape/[feature]-brief.md` aprovado no gate de direção.
- Se a Intent marca interface visual: `docs/design/[feature]/screen-map.md`
  existe. Sem ele, os estados de tela viram invenção — bloqueie e devolva.

## Workflow

### Fase 1 — Parametrizar

Pergunte, antes de começar: "Quantos cenários por RF? O padrão é caminho feliz,
um caminho alternativo e uma borda — três." Registre a resposta; ela governa a
Fase 4 inteira.

### Fase 2 — Escopo

Derive o escopo da fronteira do Shape Brief. Se você precisa divergir do brief,
justifique na tabela — divergência silenciosa entre brief e PRD é o defeito mais
difícil de detectar depois, porque os dois documentos parecem coerentes
isoladamente.

### Fase 3 — Regras e requisitos

Regras de negócio primeiro, requisitos depois: o requisito referencia a regra, e
não o contrário. Fonte das regras: a semântica de contrato e as regras de borda
do `/solution`, mais as regras invisíveis que só existem aqui.

Para cada RF, quando houver protótipo, registre a **origem no protótipo** —
tela e estado. É esse campo que garante que o cenário descreve algo que existe.

### Fase 4 — Critérios de aceite em Gherkin

Um bloco Gherkin por cenário, com ID no formato `SCN-[RF].[SEQ]` — o RF é o
número do requisito, sem prefixo. `SCN-001.2` é o segundo cenário do RF-001.

Regras do cenário:

- **Um comportamento por cenário.** Dois "Então" sobre coisas diferentes são
  dois cenários.
- **Sem detalhe de implementação.** "Quando o usuário confirma o pedido", não
  "Quando o endpoint POST /pedidos retorna 201".
- **Verificável sem ambiguidade.** Se duas pessoas podem discordar sobre se o
  cenário passou, ele não está pronto.
- **Tipo de teste declarado** — unitário, integração ou e2e. É o que permite ao
  `/tests` distribuir a suíte e ao `/tasks` dimensionar os épicos.

Percorra as regras de borda do `/solution`: cada uma respondida com algo
diferente de "não se aplica" deve ter cenário correspondente. Cobertura de
borda que se perde entre `/solution` e `/prd` é a perda mais comum do pipeline.

Salve os blocos também como `.feature` em `docs/prd/[feature]/`.

### Fase 5 — RNF com envelope

Cada RNF precisa de envelope e condição de medição. "Deve ser rápido" não é
RNF. "p95 abaixo de 300 ms com 200 usuários simultâneos" é.

Se a instrumentação para medir não existe, registre — vira trabalho no `/tasks`.

### Fase 6 — Procedência

Cada regra e cada requisito recebe exatamente um tipo:

| Tipo | Quando usar |
| --- | --- |
| informada | o demandante ou especialista afirmou |
| derivada | consequência lógica de outra regra declarada |
| extraída de legado | lida do sistema atual, do código ou dos dados |
| hipótese a validar | veio de workshop ou suposição; exige experimento |
| inferida pelo agente | você preencheu porque parecia certo |

**`inferida pelo agente` é permitida e deve ser usada sem constrangimento** —
mentir sobre a procedência é infinitamente pior do que inferir. Mas cada
ocorrência entra na tabela de pendências e precisa de confirmação humana antes
do gate. É a primeira coisa que uma auditoria vai olhar.

`hipótese a validar` exige experimento, critério de descarte e prazo. Sem isso,
a hipótese vira regra de negócio com o mesmo peso de uma informada — que é
exatamente o problema que o modo ingestão do `/shape` tenta evitar.

### Fase 7 — Gate de spec

Salve, valide e apresente ao aprovador humano.

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/prd/validate-rules.json \
  --artifact docs/prd/[feature]-prd.md
```

Dúvida material em aberto bloqueia o gate. Inferência não confirmada bloqueia o
gate.

Aprovado, os cenários **congelam**. A partir daí, alteração exige emenda
registrada na tabela de emendas — e o `/code-review` verifica isso contra o git.

## Emendas

Modo `emenda`: registre data, cenário, o que mudou, motivo e aprovador. O ID do
cenário **não muda** — cenário emendado mantém o ID, porque a rastreabilidade
até o commit depende disso. Cenário eliminado é marcado como revogado, nunca
apagado.

## Handoff

Próximo comando: `/techspec`.
