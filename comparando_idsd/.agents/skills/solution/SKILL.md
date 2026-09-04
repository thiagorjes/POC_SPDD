---
name: solution
description: Explora o comportamento da solução sem nenhuma tecnologia — fluxos de operação, estados e transições, semântica de contrato e regras de borda. É a divergência do segundo diamante e funciona para fluxos com e sem tela. Use após /shape, antes de /design e /prd.
camada: execution
satisfaz-gates: []
template: .agents/templates/solution-template.md
input-artifacts:
  - docs/shape/{{FEATURE}}-brief.md
  - docs/context/{{FEATURE}}-context.md
output-artifacts:
  - docs/solution/{{FEATURE}}-solution.md
---

## Objetivo

Abrir o leque de comportamentos possíveis dentro da direção já aprovada. É o
lado divergente do segundo diamante, e é o que dá D2 aos fluxos que não têm
tela — sem esta skill, uma feature puramente de backend salta do brief direto
para o PRD e os casos de borda nascem inventados.

O produto não é uma decisão fechada: é o mapa de comportamento e as questões em
aberto que o `/prd` vai fechar.

## Argumentos

- (sem argumento) — explora a feature ativa
- `"nome-da-feature"` — feature específica
- `revisar` — reabre após devolução do `/prd` ou do `/design`

## Pré-condições

- `docs/shape/[feature]-brief.md` aprovado no gate de direção.
- `docs/context/[feature]-context.md` sem lacuna bloqueante.

## O teste de fronteira

Antes de escrever qualquer frase, aplique: **se trocar a tecnologia torna a
frase falsa, a frase é de techspec e não pertence aqui.**

Pertence: "a operação é idempotente e rejeita duplicata em 24h devolvendo o
resultado anterior."
Não pertence: "chave de idempotência em Redis com TTL de 24h."

O comportamento é o mesmo; a segunda frase amarra a implementação. Use o teste
sempre que estiver em dúvida — ele resolve quase todos os casos.

## Workflow

### Fase 1 — Fluxos de operação

Um bloco por fluxo priorizado no brief. Passos em linguagem de negócio, com ator
e gatilho explícitos. Para cada passo, registre o que precisa ser verdade antes
dele — é essa coluna que revela pré-condição escondida.

Caminhos alternativos não são opcionais: todo fluxo tem pelo menos um.

### Fase 2 — Estados e transições

Nomeie os estados pelo significado que têm para o negócio, não por status
técnico. Registre estado inicial, estados terminais e, principalmente, as
**transições proibidas** — o que nunca pode acontecer é a informação que mais se
perde entre etapas e a que mais gera defeito.

### Fase 3 — Semântica de contrato

Para cada operação: o que entra e o que sai em termos de significado, se é
idempotente, que efeito colateral produz. Sem protocolo, sem formato, sem nome
de endpoint.

### Fase 4 — Regras de borda

Percorra a lista mínima e responda cada uma: vazio, duplicado, concorrente, fora
de ordem, parcial, expirado, sem permissão. "Não se aplica" é resposta válida,
mas precisa ser escrita — omissão não é resposta.

Para cada regra, a coluna "por que não é o óbvio" força a distinguir a decisão
real do comportamento default. Se todas forem óbvias, provavelmente a exploração
foi rasa.

### Fase 5 — Opções de comportamento

Esta seção é a divergência. Onde houver mais de um comportamento defensável,
registre as opções, recomende uma e marque em que etapa a escolha será fechada.
Um artefato de `/solution` sem nenhuma questão em aberto quase sempre significa
que o agente decidiu sozinho o que caberia ao `/prd` decidir com o gestor.

### Fase 6 — Saída

Salve em `docs/solution/[feature]-solution.md`.

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/solution/validate-rules.json \
  --artifact docs/solution/[feature]-solution.md
```

## Fronteiras

Você responde **como se comporta**. Não responde qual direção seguir
(`/shape` — se a exploração mostrar que a direção está errada, devolva, não
decida), como aparece na tela (`/design`), o que precisa ser verdade e ser
verificável (`/prd`), nem com o quê (`/techspec`).

Entidade aqui é vocabulário de negócio, não esquema de dados.

## Handoff

Próximo comando: `/design` se a feature tem interface; caso contrário, `/prd`.
