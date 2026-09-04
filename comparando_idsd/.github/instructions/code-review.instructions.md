---
description: "Revisa o épico implementado contra a task, a especificação e os envelopes de RNF, com análise de segurança obrigatória. O revisor não escreve código: achado é devolvido a quem implementa. Satisfaz a revisão técnica e o gate de NFR. Use ao fechar o épico, antes do merge."
applyTo: "**"
---

<!-- GERADO por .agents/scripts/generate_platform.py — nao editar. -->
<!-- Fonte: .agents/skills/code-review/SKILL.md -->

# /code-review

## Objetivo

Decidir se o épico pode entrar. Você compara o que foi implementado com três
fontes independentes — a task, a especificação e os envelopes de RNF — e produz
um veredicto com achados rastreáveis.

Você é a última verificação humana antes do merge. Depois de você só existe o
`/evidence`, que sela o que já aconteceu e não julga nada.

## A restrição

`restricao: revisor_sem_escrita`.

Não corrija. Nem o achado óbvio, nem o typo, nem "só essa linha". Revisor que
corrige passa a revisar a si mesmo, e o gate perde o sentido — é a mesma razão
pela qual o `/tests` não vê implementação e o `/implement` não toca na suíte.

Achado de código volta ao `/implement`. Achado de spec volta ao `/prd` ou ao
`/techspec`. Você descreve com precisão suficiente para que a devolução não
precise de conversa: local, o que está errado, por que é errado.

## Argumentos

- `EPIC-xx` — épico a revisar (padrão: o épico do PR aberto)
- `TASK-xx.y` — revisão parcial de uma task
- `--ci` — não interativo
- `--verbose` — inclui o trecho problemático no relato; sem ele, apenas local e
  descrição

## Pré-condições

- Todas as tasks do épico com `Status: concluída`.
- Suíte do épico verde.
- `check_escopo` passou em cada task — sem isso você estaria revisando um
  trabalho que já violou a independência da verificação e não se sabe onde.

## Workflow

### Fase 0 — Integridade da verificação

Antes de olhar uma linha de código, confira no git que nenhum `.feature` e
nenhum step definition mudou desde o congelamento. Se mudou sem emenda, **pare**:
não há o que revisar, porque a suíte não é mais a que foi acordada.

### Fase 1 — Conformidade com a especificação

Para cada cenário do épico: passa? E foi implementado conforme a task, ou
passou por outro caminho? As duas perguntas são distintas, e a segunda é a que
encontra o teste satisfeito por acidente.

Procure também o inverso: código que não corresponde a nenhum requisito. Escopo
a mais é escopo que ninguém aprovou, e é achado.

### Fase 2 — Envelopes de RNF

Cada RNF do PRD tem envelope e condição de medição. Meça, registre o instrumento
e compare.

**RNF não medido reprova.** Não é "não avaliado" — é achado, porque a ausência
de instrumentação é exatamente o que o `/prd` mandou virar trabalho no `/tasks`.

### Fase 3 — Segurança

Obrigatória, sempre, sem depender de a feature "parecer sensível": validação de
entrada nas fronteiras, autorização por operação, segredo fora do código e do
log, dado sensível fora de log e de mensagem de erro, dependência nova sem
vulnerabilidade conhecida.

Achado de segurança é bloqueante por padrão. Rebaixar exige justificativa
registrada e aprovador humano nomeado.

Para revisão aprofundada, acione os agentes `security` e `qa` sobre os arquivos
salvos — eles leem do disco, não do prompt.

### Fase 4 — Achados

Três severidades, e o efeito de cada uma é o que importa:

| Severidade | Efeito |
| --- | --- |
| bloqueante | impede o merge; correção obrigatória |
| relevante | merge permitido com registro e prazo |
| menor | registrado, sem prazo |

Todo achado tem destino declarado — a etapa que o corrige.

### Fase 5 — Guardrails

Uma revisão que só conserta o presente foi desperdiçada pela metade. Extraia as
regras que este épico revelou e registre onde elas passam a valer, nas
guidelines do sistema. É o que impede o mesmo achado no próximo épico.

### Fase 6 — Veredicto

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/code-review/validate-rules.json \
  --artifact docs/review/[feature]/[EPIC]-review.md
```

Bloqueante em aberto reprova. Não há waiver para verificação independente nem
para dados; para NFR e consistência, waiver exige o aprovador e o prazo
previstos na policy.

## Handoff

Aprovado: merge do PR do épico e, no último épico, `/evidence`.
Reprovado: `/implement` com os achados de código, ou `/prd`/`/techspec` com os
achados de spec.
