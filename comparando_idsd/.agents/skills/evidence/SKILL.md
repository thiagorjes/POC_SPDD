---
name: evidence
description: >
  Sela o pacote de evidências no release com hash — versões, gates,
  classificação e overrides, rastreabilidade, ordem de verificação, envelopes
  de NFR, achados, tentativas, custo e intervenções humanas. É cartório e não
  narrador; registro ausente reprova o pacote e nunca é reconstruído. Última
  etapa de qualquer flow.
camada: execution
satisfaz-gates: [GATE-EVIDENCIA, GATE-DADOS, GATE-RECONCILIACAO]
template: .agents/templates/evidence-template.md
input-artifacts:
  - docs/intent/{{FEATURE}}-classification.md
  - docs/tasks/{{FEATURE}}-tasks.md
output-artifacts:
  - docs/evidence/{{FEATURE}}/manifest.json
  - docs/evidence/{{FEATURE}}/evidence.md
---

## Objetivo

Consolidar e selar o que já foi registrado ao longo da execução, de forma que
um auditor externo consiga responder, meses depois: o que foi pedido, quem
decidiu, o que foi verificado, por quem, e sob quais versões de skill e policy.

## O que esta skill não é

Ela **não gera** o registro. Cada etapa escreve o seu, incrementalmente, no
momento em que acontece. Esta skill lê, confere e sela.

A distinção é a razão de ser da cláusula 4.10 e precisa ficar clara para quem
executar a skill: **registro ausente é falha do pacote, não lacuna a
preencher.** Reconstruir por memória, por inferência ou relendo o código produz
um documento que parece evidência e não é — é narrativa retroativa, e é
exatamente o que a auditoria precisa detectar.

Se um registro falta, a saída correta é listar em "Registros ausentes", reprovar
o selo e apontar qual etapa deveria tê-lo escrito.

## Argumentos

- `"nome-da-feature"` — sela o pacote da feature
- `--dry-run` — confere sem selar, para ver o que falta antes do release
- `--reconciliar` — modo de flow de emergência (ver abaixo)

## Pré-condições

- Todas as etapas do flow, exceto as marcadas `posterior`, concluídas.
- Existe uma tag ou candidata de release.

## Workflow

### Fase 1 — Conformidade de gates

Leia o flow em `governance/flows/[flow].yaml` e a policy de gates. Para cada
gate exigido pela classe, localize a evidência declarada no catálogo e execute o
verificador. Gate humano exige registro de aprovador e data.

Gate reprovado não bloqueia a **geração** do pacote — bloqueia o **selo**. O
pacote sai com o gate marcado como reprovado e o release não acontece.

### Fase 2 — Versões

Registre versão e hash de skills, policies, catálogo de gates e modelos usados.
A mesma spec produz resultado diferente sob skills diferentes; sem esta seção a
evidência não é reproduzível e a auditoria não consegue explicar divergências
entre duas execuções aparentemente iguais.

### Fase 3 — Ordem de verificação

Para cada cenário, extraia do histórico do repositório o commit do teste e o
commit da implementação. Teste depois da implementação é violação da 4.9.1 e
reprova o GATE-VERIFICACAO-INDEPENDENTE — mesmo que o teste passe.

Esta é a única verificação do pacote que se apoia no git em vez de em artefato,
porque é a única cuja prova é temporal.

### Fase 4 — Consolidação

Transcreva, sem interpretar: resultados de cenário, envelopes de NFR medidos,
achados de revisão e status, tentativas e custo por task, e toda intervenção
humana. Análise é de `/analyze` e `/code-review`; aqui só entra o registro.

### Fase 5 — Varredura de dados sensíveis

Verifique que nenhum dado real de cliente aparece em prompt, artefato ou
evidência (IDSD 4.10.1). Ocorrência diferente de zero reprova o GATE-DADOS, e
este gate não admite waiver.

### Fase 6 — Selo

Se "Registros ausentes" está vazio e nenhum gate bloqueante reprovou, calcule o
hash do pacote e grave `manifest.json`. O selo é imutável: correção posterior
gera novo pacote com novo hash, referenciando o anterior.

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/evidence/validate-rules.json \
  --artifact docs/evidence/[feature]/evidence.md
```

## Modo reconciliação

Em flow de emergência, os gates marcados `posterior` viram dívida com prazo
definido na policy. O pacote sela com a dívida **explícita e visível**, e um
segundo pacote é emitido quando a reconciliação se completa.

Dívida vencida é bloqueante para o próximo release do sistema — não para o
release emergencial que a originou.

## Handoff

Fim do flow. Registre em `memory/state.md` o hash do pacote e o status do
release.
