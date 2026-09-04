---
description: "Explora contexto, dores, personas e enquadramentos alternativos do problema. É a divergência do primeiro diamante — abre o leque, não escolhe. Recebe a Intent já validada em vez de elicitá-la. Use após /context, antes de /shape."
applyTo: "**"
---

<!-- GERADO por .agents/scripts/generate_platform.py — nao editar. -->
<!-- Fonte: .agents/skills/discovery/SKILL.md -->

# /discovery

## Objetivo

Entender o problema mais fundo do que o demandante o formulou, e produzir **mais
de uma leitura possível** dele. O valor desta etapa está na divergência: se ao
final houver um único enquadramento, o `/shape` não terá o que convergir e a
decisão de direção vira carimbo.

Esta skill **não elicita a intenção** — isso já aconteceu em `/intent`, e
refazê-lo aqui produz duas versões da mesma coisa. Você recebe a Intent pronta
e investiga o que está em volta dela.

## Argumentos

- (sem argumento) — explora a feature ativa
- `"nome-da-feature"` — feature específica
- `atualizar` — reabre o discovery existente com novas evidências

## Pré-condições

- `docs/intent/[feature]-intent.md` validado pelo demandante.
- `docs/context/[feature]-context.md` sem lacuna bloqueante.

## Workflow

### Fase 0 — Absorver o que já existe

Leia a Intent e o Contexto. **Não repita pergunta cuja resposta já está em
disco** — repetir sinaliza que o agente não leu, e queima a paciência do
gestor justamente antes das perguntas que importam.

### Fase 1 — Situação atual

Uma pergunta de cada vez:

1. "Como isso é feito hoje, do começo ao fim?"
2. "Onde as pessoas improvisam? Planilha paralela, combinado informal, retrabalho."

A segunda pergunta é a mais produtiva da etapa. Gambiarra é requisito não dito
que já foi validado pela prática — e quase nunca aparece quando se pergunta
"o que você precisa".

### Fase 2 — Dores e evidências

Para cada dor: quem sente, com que frequência, e **qual a evidência**. Dado,
ticket, reclamação registrada, observação de campo. "É sabido que" não é
evidência — sem fonte, registre como suposição a validar, não como dor
comprovada.

Pergunte também o custo de conviver com a dor. Dor sem custo é incômodo, e
incômodo não sustenta prioridade.

### Fase 3 — Divergir os enquadramentos

Esta é a fase que justifica a skill. A partir das dores levantadas, formule ao
menos dois enquadramentos diferentes do problema.

Enquadramentos são diferentes quando mudam **quem é atendido** ou **que dor é
resolvida** — não quando mudam a apresentação. "Reduzir o tempo de aprovação" e
"eliminar a necessidade de aprovação" são enquadramentos distintos; "aprovar por
e-mail" e "aprovar no sistema" são a mesma coisa com interface diferente.

Para cada um, registre o que ficaria sem resposta caso ele fosse o escolhido. É
essa coluna que dá matéria ao `/shape`.

### Fase 4 — Contradições e perguntas em aberto

Quando duas fontes discordam, **registre a contradição** e aponte quem decide.
Resolver contradição cedo, por conta própria, é a forma mais comum de o agente
tomar uma decisão de negócio sem perceber que a tomou.

### Fase 5 — Saída

Salve progressivamente em `docs/discovery/[feature]-discovery.md`.

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/discovery/validate-rules.json \
  --artifact docs/discovery/[feature]-discovery.md
```

## Fronteiras

Você **abre**. Quem fecha é o `/shape`. Nenhum requisito numerado, nenhuma
solução, nenhuma tela, nenhuma tecnologia — e nenhuma métrica com alvo, porque
definir alvo já é decidir.

Se o discovery contradiz a Intent, isso é um achado a levar ao `/shape`. Não
corrija a Intent por conta própria: ela pertence ao demandante (IDSD 4.2).

## Handoff

Próximo comando: `/shape`.
