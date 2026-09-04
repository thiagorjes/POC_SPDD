---
name: analyze
description: >
  Confronta os artefatos da cadeia — intent, shape, solução, design, PRD e
  TechSpec — em busca de contradição, lacuna, ambiguidade, duplicação e escopo
  órfão. Localiza e devolve; não corrige. Satisfaz o gate de consistência. Use
  após /techspec, antes de /tasks.
camada: execution
satisfaz-gates: [GATE-CONSISTENCIA]
template: .agents/templates/analysis-template.md
input-artifacts:
  - docs/intent/{{FEATURE}}-intent.md
  - docs/shape/{{FEATURE}}-brief.md
  - docs/solution/{{FEATURE}}-solution.md
  - docs/prd/{{FEATURE}}-prd.md
  - docs/techspec/{{FEATURE}}-techspec.md
output-artifacts:
  - docs/analyze/{{FEATURE}}-analysis.md
---

## Objetivo

Encontrar as divergências entre artefatos antes que elas virem código. Cada
etapa da cadeia foi escrita em um momento diferente, por um contexto diferente;
todas parecem coerentes lidas isoladamente. Sua função é ler em conjunto.

O achado mais valioso é a **contradição**, justamente porque nenhum dos dois
lados parece errado sozinho.

## O que você não faz

Você não corrige. Cada achado nomeia o artefato dono da correção, e é lá que ela
acontece — depois a análise é reexecutada.

Consertar aqui esconde o defeito no lugar errado: o PRD continua dizendo uma
coisa, a TechSpec outra, e a análise passa a afirmar que estão de acordo.

## Argumentos

- (sem argumento) — feature ativa, modo `--pre-tasks`
- `--pre-tasks` — gate antes do planejamento (padrão)
- `--pre-implement` — reexecução após mudança em artefato de spec
- `--ci` — não interativo
- `--verbose` — inclui os trechos divergentes; sem ele, apenas local e descrição

## Pré-condições

- PRD aprovado no gate de spec e TechSpec existente.
- Se a Intent marca interface visual, o design existe. **O disparo é
  `intent.tem_interface`, não a presença do `screen-map.md`** — como o `/design`
  roda antes do `/prd`, a existência do arquivo já não distingue nada.

## Workflow

### Fase 0 — Frescor

Compare a data do último commit de cada artefato. Artefato a jusante mais antigo
que o de montante é suspeito imediato: alguém mudou a spec e não propagou.
Registre antes de analisar conteúdo; isso muda como você lê o resto.

### Fase 1 — Cobertura da cadeia

Para cada elo, conte os órfãos nas duas direções:

| Elo | A montante sem destino | A jusante sem origem |
| --- | --- | --- |
| direção → requisito | direção decidida que virou nada | requisito que não serve à direção |
| regra de borda → cenário | borda respondida sem cenário | cenário sem borda correspondente |
| tela/estado → requisito | estado do protótipo sem requisito | requisito de UI sem tela |
| requisito → decisão técnica | RF sem cobertura na TechSpec | componente sem RF |
| RNF → estratégia de verificação | RNF sem forma de medir | — |

A perda mais comum do pipeline é a cobertura de borda que se dissolve entre
`/solution` e `/prd`. Procure ali primeiro.

### Fase 2 — Achados

Classifique por tipo e severidade, e declare o dono. Descrição precisa o
bastante para a devolução não precisar de conversa.

Ambiguidade é achado de verdade, não preciosismo: texto que duas pessoas
competentes leem de formas diferentes vira defeito na implementação, e lá custa
dez vezes mais.

### Fase 3 — Procedência e hipóteses

Conte inferências do agente pendentes de confirmação, hipóteses sem experimento
ou prazo, e suposições operacionais vencidas. Qualquer contagem diferente de
zero é bloqueante: a spec está apoiada em algo que ninguém confirmou.

### Fase 4 — Veredicto

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/analyze/validate-rules.json \
  --artifact docs/analyze/[feature]-analysis.md
```

Bloqueante em aberto reprova o GATE-CONSISTENCIA e impede o `/tasks`. Waiver é
permitido pela policy para este gate, mas exige aprovador e prazo nomeados — e
o débito reaparece no `/evidence`.

## Handoff

Aprovado: `/tasks`.
Reprovado: o artefato dono de cada achado, e depois `/analyze` de novo.
