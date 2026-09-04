---
name: shape
description: Converge o discovery em uma direção decidida — problema escolhido, alternativas descartadas com motivo, fronteira de escopo, personas priorizadas e métrica de sucesso. Produz decisão, não especificação. Suporta modo ingestão para Lean Inception, RFP e specs herdadas. Use após /discovery, antes de /solution.
camada: execution
satisfaz-gates: [GATE-DIRECAO]
template: .agents/templates/shape-brief-template.md
input-artifacts:
  - docs/intent/{{FEATURE}}-intent.md
  - docs/discovery/{{FEATURE}}-discovery.md
  - docs/context/{{FEATURE}}-context.md
output-artifacts:
  - docs/shape/{{FEATURE}}-brief.md
---

## Objetivo

Fechar o primeiro diamante. O `/discovery` divergiu — levantou dores,
enquadramentos alternativos, contexto. Esta skill escolhe: **um** problema,
**uma** direção, **uma** fronteira.

O produto é uma decisão de negócio aprovada por humano. Sem esta etapa, a
exploração de solução e a prototipação acontecem sobre direção não aprovada, e
o retrabalho só aparece no gate de spec — mais tarde e mais caro.

## Argumentos

- (sem argumento) — conduz a convergência para a feature ativa
- `"nome-da-feature"` — feature específica
- `--ingestao <caminho>` — parte de um artefato externo em vez de entrevistar
- `revisar` — reabre o brief após devolução do `/prd` ou do `/solution`

## Pré-condições

- `docs/discovery/[feature]-discovery.md` existe — ou modo ingestão com artefato
  externo informado.
- `docs/context/[feature]-context.md` sem lacuna bloqueante.

## Workflow

### Fase 1 — Levantar as alternativas reais

Extraia do discovery os enquadramentos possíveis do problema. Se houver menos de
dois, o discovery não divergiu o suficiente: devolva antes de continuar.

Variação cosmética não é alternativa. "Fazer com wizard" e "fazer com formulário
longo" são a mesma direção com apresentações diferentes — isso é `/design`. São
alternativas de verdade quando mudam quem é atendido, que dor é resolvida, ou
até onde vai a entrega.

### Fase 2 — Convergir

Uma pergunta de cada vez, com o gestor:

1. "Entre estes enquadramentos, qual problema vamos resolver agora?"
2. "O que fica de fora, e o que fica adiado com gatilho de retomada?"
3. "Que persona e que jornada vêm primeiro, e por quê essa?"
4. "Como vamos saber, depois de entregue, se a aposta estava certa?"

Para cada alternativa descartada, registre o motivo **e** a condição de
reabertura. Descarte sem condição de reabertura é descarte que ninguém revisita.

### Fase 3 — Métrica de sucesso

Exija linha de base, alvo, prazo de leitura e se a instrumentação já existe.
Métrica sem linha de base não é métrica, é desejo. Se a instrumentação não
existe, isso é uma restrição a registrar — ela vira trabalho no `/tasks`.

### Fase 4 — Modo ingestão

Quando a entrada é Lean Inception, RFP ou spec de fornecedor, você não conduz
levantamento: você **lê e reorganiza**. Duas regras invioláveis:

1. **Saída de workshop é hipótese, não requisito.** Tudo que veio do ritual
   externo e não foi validado entra em "Hipóteses a validar", com experimento e
   critério de descarte. Sem marcação, hipótese vira regra de negócio com o
   mesmo peso de um requisito levantado — e ninguém depois consegue separar.
2. **Revisão técnica de workshop é restrição conhecida, nunca decisão
   arquitetural.** A Lean Inception coloca dev na sala, o que é bom; o problema
   é o `/techspec` herdar uma escolha que nunca passou por gate técnico. Registre
   em "Restrições técnicas herdadas" com a coluna "passou por gate técnico" em
   não — o `/techspec` pode contrariá-la com justificativa.

Se o artefato externo já traz sequenciamento de incrementos (o Canvas MVP da
Lean Inception traz), não o reconstrua: aponte para ele, e o `/tasks --ingestao`
o consome como base do DAG de épicos.

### Fase 5 — Gate de direção

Salve o brief e apresente ao aprovador humano. O gate é humano — nenhum script
aprova direção. Registre nome, data e ressalvas.

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/shape/validate-rules.json \
  --artifact docs/shape/[feature]-brief.md
```

## Fronteiras

Você responde **o quê** e **até onde**. Não responde *como se comporta*
(`/solution`), *o que precisa ser verdade* (`/prd`) nem *com o quê*
(`/techspec`).

O sinal mais confiável de que o brief invadiu o PRD é o aparecimento de
numeração de requisito. Zero RF, zero RNF, zero RN, zero Gherkin.

## Handoff

Próximo comando: `/solution`.
