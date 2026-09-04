---
description: "Monta o contexto mínimo necessário para a etapa seguinte e registra toda lacuna como evidência em vez de inferir. Terceira etapa da camada de Controle, reexecutada antes de cada etapa de Execução. Use após /classify e sempre que uma etapa precisar de insumo que ainda não está em disco."
applyTo: "**"
---

<!-- GERADO por .agents/scripts/generate_platform.py — nao editar. -->
<!-- Fonte: .agents/skills/context/SKILL.md -->

# /context

## Objetivo

Reunir o insumo que a próxima etapa precisa e, principalmente, **tornar visível
o que falta**. A cláusula 4.6 é categórica: o resolvedor de contexto registra a
lacuna, não a preenche por inferência.

Esta é a skill onde a tentação de alucinar é maior, porque preencher um campo
com algo plausível parece produtivo. Não é: é a origem de quase todo defeito de
especificação que só aparece na implementação.

Na Fase 1 do IDSD isto é uma skill; na Fase 2 vira componente do runtime.

## Argumentos

- `"etapa"` — monta contexto para uma etapa específica (ex: `/context prd`)
- (sem argumento) — monta para a próxima etapa do flow resolvido
- `revisar` — reavalia lacunas registradas, marcando as que foram resolvidas

## Pré-condições

- `docs/intent/[feature]-classification.md` existe e o flow está resolvido.

## Workflow

### Fase 1 — Determinar o que a etapa precisa

Leia o `input-artifacts` da skill da etapa alvo. Esse é o piso, não o teto:
acrescente o que o domínio exigir (regra vigente, contrato de sistema vizinho,
decisão anterior registrada em DR).

### Fase 2 — Resolver

Para cada item, preencha fonte e trecho relevante.

**Fonte válida:** caminho de arquivo no repositório, URL, nome de pessoa,
identificador de ticket. **Fonte inválida:** "conhecimento do modelo", "prática
comum de mercado", "padrão da indústria", ou fonte omitida. Se a fonte é
inválida, o item não está resolvido — é lacuna.

Marque a confiança: **alta** quando a fonte afirma diretamente; **média** quando
exige interpretação; **baixa** quando é indício. Confiança baixa é lacuna
disfarçada — trate como lacuna.

### Fase 3 — Registrar lacunas

Para cada lacuna: o que falta, quem provavelmente sabe, qual etapa ela bloqueia,
status. Lacuna que bloqueia a etapa alvo impede o início dela — reporte e pare.

### Fase 4 — Suposições operacionais

Apenas para lacunas **não bloqueantes**. Cada suposição precisa de: o que se
assume, o que acontece se estiver errada, um dono humano e um prazo de
confirmação. Suposição sem dono e sem prazo não é suposição, é invenção — vira
lacuna bloqueante.

### Fase 5 — Saída

Salve em `docs/context/[feature]-context.md`.

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/context/validate-rules.json \
  --artifact docs/context/[feature]-context.md
```

## Nunca

- Preencher lacuna por plausibilidade.
- Resumir domínio sem citar fonte.
- Copiar dado real de cliente para qualquer campo (IDSD 4.10.1) — referencie o
  identificador do registro, nunca o conteúdo.
- Decidir. Este artefato reúne insumo; decidir é de `/shape`, `/prd` ou
  `/techspec`.

## Handoff

Se há lacuna bloqueante: reporte quem precisa responder e pare o flow.
Caso contrário, próximo comando: a etapa alvo.
