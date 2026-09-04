---
name: tasks
description: >
  Distribui os cenários congelados do PRD em épicos verticais e tasks
  auto-contidas, com dependências explícitas, escopo de arquivo declarado e
  instrução precisa o bastante para ser executada sem reabrir PRD ou TechSpec.
  Satisfaz o gate de rastreabilidade pela invariante cenário↔épico. Use após
  /techspec e /analyze.
camada: execution
satisfaz-gates: [GATE-RASTREABILIDADE]
template: .agents/templates/tasks-template.md
input-artifacts:
  - docs/prd/{{FEATURE}}-prd.md
  - docs/prd/{{FEATURE}}/*.feature
  - docs/techspec/{{FEATURE}}-techspec.md
  - docs/analyze/{{FEATURE}}-analysis.md
output-artifacts:
  - docs/tasks/{{FEATURE}}-tasks.md
  - docs/tasks/{{FEATURE}}/TASK-*.md
---

## Objetivo

Converter especificação em plano de execução. Você recebe cenários congelados e
uma TechSpec revisada; produz épicos verticais e tasks que uma pessoa ou um
agente executa sem consultar mais nada.

O gate que você satisfaz é o de rastreabilidade, e ele é aritmético: **todo
cenário do PRD é entregue por exatamente um épico; todo épico entrega pelo menos
um cenário**. Não é declaração de boa intenção — é função total, verificada por
script. É também o critério objetivo de fatia vertical.

## O risco desta skill

Task vaga é o defeito mais caro do pipeline, porque só aparece na execução,
depois que o orçamento de tentativas já foi consumido. "Implementar o serviço de
cálculo conforme a TechSpec" não é task: é o adiamento da leitura que você
deveria ter feito.

O padrão de qualidade é **cópia integral**: assinatura de método, nome de campo,
mensagem de erro literal, modo de arredondamento. Duplicar informação da
TechSpec dentro da task é intencional — a task é o contrato de execução, e o
custo de mantê-la sincronizada é menor que o custo de uma tentativa perdida.

## Argumentos

- (sem argumento) — feature ativa
- `"nome-da-feature"` — feature específica
- `--ci` — modo não interativo: sem perguntas, decisões pelo default do flow,
  falha em vez de perguntar
- `ingestao` — recebe backlog pronto de fora e o reconcilia com os cenários
- `update` — revisão preservando IDs já atribuídos

## Pré-condições

- `docs/prd/[feature]-prd.md` aprovado no gate de spec, com cenários congelados.
- `docs/techspec/[feature]-techspec.md` existe.
- `docs/analyze/[feature]-analysis.md` existe, gerado em modo `--pre-tasks`, sem
  achado bloqueante em aberto. Achado bloqueante se resolve no PRD ou na
  TechSpec — nunca é compensado aqui.
- Se a Intent marca interface visual: `docs/design/[feature]/screen-map.md`
  existe. Como o `/design` roda antes do `/prd`, a existência do screen-map já
  não distingue nada — o disparo do gate `--pre-tasks` é `intent.tem_interface`,
  não a presença do arquivo.

## Workflow

### Fase 0 — Leitura

Leia PRD, os `.feature`, a TechSpec e a análise. Monte, antes de escrever
qualquer coisa, a lista completa de cenários. Ela é o denominador: no final,
cada linha precisa ter exatamente um épico ao lado.

### Fase 1 — Épicos verticais

Agrupe cenários em épicos pelo critério do **mínimo implantável e testável**.
Camada técnica não é épico: "Épico de banco" e "Épico de UI" não entregam
cenário nenhum sozinhos e adiam a primeira verificação real.

Cada épico declara sistema, cenários entregues, dependências e o que fica
funcionando ao final.

Dimensionamento:

| Faixa | Comportamento |
| --- | --- |
| ≤ 8 tasks | alvo — siga sem comentar |
| 9 a 12 | aceito |
| 13 a 20 | avise, sugira corte, peça confirmação |
| > 20 | bloqueie e exija divisão |

O teto existe porque as tasks do épico rodam em série, porque um PR de vinte
commits excede o que um revisor lê com atenção, e porque épico grande adia a
verificação que o fatiamento existe para antecipar.

### Fase 2 — Tasks

IDs: `TASK-[EPICO].[SEQ]`, sequenciais na ordem de dependência. Task
multi-sistema não existe — toda task pertence a um único sistema; o épico é que
pode atravessar sistemas, com o sufixo `[sistema]` no título da task.

**IDs não são reordenáveis.** Inserir épico consome o próximo número livre; a
ordem mora no grafo, não no identificador. Depois do merge do PR da spec, ID
atribuído é imutável — renumerar quebra a evidência e o histórico de commits.

Todo caminho de arquivo é relativo a `systems/[sistema]/`.

Para cada task, três blocos de guia técnico:

1. **Estrutura de arquivos** — o que criar, o que alterar, e o que é **proibido
   tocar**. Os `.feature` e os step definitions entram sempre na lista de
   proibidos: eles vêm do `/tests`, que rodou antes e sem ver a implementação.
   Deixar a task livre para editá-los destrói a independência da verificação.
2. **Padrão a seguir** — aponte um arquivo análogo que já existe no repositório
   e transcreva as assinaturas, nomes de campo e mensagens literais.
3. **Pontos de atenção** — armadilhas conhecidas.

Critérios de aceite são verificáveis e **não incluem step definitions**. A task
declara dependência da suíte; não a produz.

Campos de execução: `executor` (`agente` | `humano` | `misto`) e `tentativas`
vêm do flow, com override por task. Com executor humano, `esforço` (P/M/G) volta
a ser obrigatório e `tentativas` perde sentido.

Salve o documento consolidado **a cada épico concluído** e gere, também
obrigatoriamente, um arquivo auto-contido por task em
`docs/tasks/[feature]/TASK-[EPICO].[SEQ]-[slug].md`. É esse arquivo que o
`/implement` consome; ele precisa bastar sozinho.

### Fase 3 — Grafo e cobertura

Desenhe o grafo de dependências e marque quais épicos correm em paralelo. Dentro
do épico a ordem é serial: as tasks compartilham o PR.

Preencha a tabela de cobertura de cenários e valide:

```
python .agents/skills/tasks/scripts/check_cobertura.py \
  --tasks docs/tasks/[feature]-tasks.md \
  --prd docs/prd/[feature]-prd.md
```

Cenário órfão e cenário em dois épicos reprovam o GATE-RASTREABILIDADE.

### Fase 4 — Multi-sistema

Se a feature afeta dois ou mais sistemas, registre a ordem de merge entre
repositórios derivada das dependências, com a compatibilidade retroativa exigida
de cada passo. Em workspace de sistema único, omita.

### Fase 5 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/tasks/validate-rules.json \
  --artifact docs/tasks/[feature]-tasks.md
```

O canvas **não** é escrito aqui. Ele é derivado das fontes e verificado pelo
check `canvas-drift`; escrever nele à mão reintroduz a divergência que a
derivação existe para eliminar.

## Histórico da task

Cada arquivo de task tem tabela de histórico, preenchida **no momento em que o
fato ocorre** — tentativa, bloqueio, decisão, desvio. É daqui que o `/evidence`
recolhe tentativas, custo e intervenção humana. Registro reconstruído no fim não
vale como evidência e reprova o selo.

## Modo ingestão

Backlog vindo de fora entra como proposta, não como verdade. Reconcilie contra
os cenários: item sem cenário correspondente é escopo não especificado — devolva
ao `/prd`; cenário sem item é lacuna do backlog — crie a task.

## Handoff

Próximo comando: `/tests`.
