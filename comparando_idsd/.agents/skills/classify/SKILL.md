---
name: classify
description: Classifica a demanda por tipo, domínio, risco e impacto, e resolve qual flow se aplica. Conservadora por construção. Segunda etapa da camada de Controle. Use logo após /intent, e novamente sempre que uma etapa posterior revelar impacto maior que o classificado.
camada: control
satisfaz-gates: [GATE-CLASSIFY]
template: .agents/templates/classification-template.md
input-artifacts:
  - docs/intent/{{FEATURE}}-intent.md
output-artifacts:
  - docs/intent/{{FEATURE}}-classification.md
---

## Objetivo

Decidir qual flow governa esta execução. A classificação não é rótulo
administrativo: ela determina o conjunto de gates obrigatórios, e portanto
determina quanto rigor a execução vai exigir.

## Argumentos

- (sem argumento) — classifica a feature ativa
- `"nome-da-feature"` — classifica uma feature específica
- `reclassificar` — registra mudança de classe descoberta em etapa posterior

## Pré-condições

- `docs/intent/[feature]-intent.md` existe e passou no GATE-INTENT.
  - Exceção: flow de emergência, onde a Intent é posterior. Nesse caso a
    classificação é feita a partir do relato do incidente e o campo de Intent
    de origem aponta para a dívida de reconciliação.

## Workflow

### Fase 1 — Atribuir os quatro eixos

Leia a Intent. Para cada eixo, atribua e justifique em uma linha.

| Eixo | Valores |
| --- | --- |
| Tipo | feature, bug, dependencia, migracao, emergencia |
| Domínio | conforme o mapa de domínios do sistema |
| Risco | baixo, medio, alto |
| Impacto | local, sistema, multi-sistema |

**Regra do conservadorismo:** na dúvida entre duas classes, escolha a de maior
exigência de gates. Errar para mais custa tempo; errar para menos custa a
auditoria. Aplique também a cada eixo isoladamente — dúvida entre risco médio e
alto resolve em alto.

Sinais que empurram para alto risco, independentemente do tamanho: movimenta
dinheiro, altera permissão, apaga ou migra dado, toca autenticação, tem
obrigação regulatória, ou o rollback não é trivial.

### Fase 2 — Resolver o flow

Leia `governance/policies/gates-por-classe.yaml` e selecione o flow em
`governance/flows/` cujo campo `atende` inclui o tipo classificado.

Copie para o artefato a lista de gates obrigatórios herdados — núcleo
inegociável mais os gates da classe. **Não invente gate e não omita gate**: a
lista vem da policy, você apenas transcreve.

Se nenhum flow registrado atende a classe, pare e reporte. Criar flow é ato de
governança, não desta skill.

### Fase 3 — Override

Só quando um humano discordar da classificação automática. Registre
classificação original, nova, autor, motivo e data. Override sem autor e motivo
é inválido e reprova o gate.

Você pode recomendar um override; não pode aplicá-lo sozinho.

### Fase 4 — Saída

Salve em `docs/intent/[feature]-classification.md` a partir do template.

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/classify/validate-rules.json \
  --artifact docs/intent/[feature]-classification.md
```

## Reclassificação

Obrigatória quando qualquer etapa posterior revela impacto maior que o
classificado. A reclassificação **retroage**: os gates da nova classe passam a
valer mesmo que a execução já esteja adiantada, e gate já vencido sob a classe
antiga precisa ser refeito se a nova classe o exigir de forma mais estrita.

Registre na tabela de reclassificação o que revelou a mudança — é essa coluna
que permite calibrar a classificação inicial ao longo do tempo.

## Fronteiras

Risco não é tamanho. Uma alteração de uma linha em cálculo de cobrança é alto
risco e esforço mínimo; uma tela nova de relatório é baixo risco e esforço
grande. Estimativa é assunto de `/tasks`.

Regras negativas completas no template.

## Handoff

Próximo comando: `/context`.
