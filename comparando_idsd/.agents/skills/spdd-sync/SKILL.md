---
name: spdd-sync
description: >
  Confronta o código entregue com a especificação que o originou e decide, com
  o humano, qual dos dois lados está errado — corrigir a spec ou corrigir o
  código. Registra cada desvio e nunca resolve sozinha. Use após /code-review,
  quando o comportamento implementado divergiu do que estava especificado.
camada: execution
satisfaz-gates: []
template: .agents/templates/deviations-template.md
input-artifacts:
  - docs/prd/{{FEATURE}}-prd.md
  - docs/techspec/{{FEATURE}}-techspec.md
  - docs/tasks/{{FEATURE}}-tasks.md
  - docs/spdd/{{FEATURE}}-canvas.md
output-artifacts:
  - docs/spdd/{{FEATURE}}-deviations.md
---

## Objetivo

Fechar o ciclo. A especificação foi escrita antes de existir código; o código
descobriu coisas que a especificação não sabia. Onde os dois discordam, alguém
tem de decidir qual está errado — e essa decisão precisa ficar registrada, não
absorvida em silêncio pelo lado que foi mais fácil de mudar.

Etapa opcional: só faz sentido se houver divergência. Sem divergência, ela não
roda.

## O que esta skill não é mais

Ela não sincroniza o REASONS Canvas. O canvas passou a ser **derivado** das
fontes por `.agents/scripts/derive_canvas.py`, e a divergência entre canvas e
fontes é detectada pelo check `canvas-drift`, não por conversa. Se o canvas
está fora de sincronia, a correção é regenerar — não é assunto desta skill.

O que sobrou aqui é o único problema que nenhum script resolve: **o código faz
uma coisa, a spec diz outra, e é preciso decidir quem estava certo.**

## O risco desta skill

Corrigir a spec por conveniência. Ajustar o PRD para descrever o que o código
faz é sempre o caminho mais barato, e destrói o valor de ter especificado
antes — a spec vira relatório do passado.

Por isso a direção `spec corrigida` exige justificativa que responda **por que
a spec estava errada**, não "porque o código ficou assim". Justificativa que
apenas descreve o código é recusada.

## Argumentos

- (sem argumento) — feature ativa, todos os desvios
- `--epico EPIC-NN` — restringe ao escopo de um épico
- `--ci` — não interativo: apenas detecta e lista; não decide nada

## Pré-condições

- `/code-review` concluído para o escopo analisado. Antes disso, divergência é
  trabalho em andamento, não desvio.
- Suíte congelada executada. Cenário falhando não é desvio — é defeito, e volta
  para `/implement`.

## Workflow

### Fase 0 — Delimitar

Levante o escopo real: arquivos tocados pelo épico, cenários cobertos, tasks
concluídas. Desvio fora desse escopo não é desta feature; registre como achado
solto e não o resolva aqui.

### Fase 1 — Detecção

Compare, nesta ordem — do contrato mais forte para o mais fraco:

1. **Cenários congelados × comportamento** — cenário passando por caminho
   diferente do especificado (mesmo resultado, regra diferente) é o desvio mais
   perigoso, porque a suíte verde o esconde.
2. **Regras de negócio do PRD × código** — regra ausente, invertida ou com
   limite diferente.
3. **Decisões da TechSpec × implementação** — decisão arquitetural declarada e
   não seguida.
4. **Escopo de arquivo da task × diff real** — código entregue fora do escopo
   que a task declarou.

Cada desvio recebe local no código, local na spec e o par "o que a spec diz" /
"o que o código faz". Se você não consegue escrever esse par em uma frase cada,
ainda não entendeu o desvio — investigue antes de registrar.

### Fase 2 — Decisão

Um desvio por vez, com o humano. Três direções possíveis:

- **spec corrigida** — a spec estava errada. Exige dizer por quê. Se o cenário
  congelado muda, a emenda é registrada no plano de verificação com o ID
  preservado; ID de cenário não é reaproveitado nem renumerado.
- **código corrigido** — volta como task para `/implement`, com os cenários
  intocados.
- **aceito com prazo** — exige data limite e responsável nomeado. Sem os dois,
  não é aceite: é desvio pendente.

Você não escolhe a direção. Você apresenta o desvio, o custo de cada lado e a
sua recomendação — a decisão é do humano e o nome dele vai no registro.

### Fase 3 — Registro

Escreva `docs/spdd/{{FEATURE}}-deviations.md` a partir do template, um DEV por
desvio, salvando a cada decisão em vez de acumular para o final. IDs de desvio
são sequenciais e imutáveis depois de escritos.

### Fase 4 — Propagação

- Direção `spec corrigida`: aplique a correção no artefato dono e **regenere o
  canvas** (`derive_canvas.py --feature <nome>`); confirme com
  `check_drift.py`.
- Direção `código corrigido`: registre a task de correção e informe que o épico
  volta ao ciclo de implementação.
- Atualize `memory/state.md` com a contagem de desvios por direção e os
  pendentes.

Desvio pendente ao fim da execução é reportado explicitamente ao humano — não
fica implícito no arquivo.

## Saída no chat

Contagem por direção, lista de pendentes e o caminho do arquivo. Nunca o
conteúdo dos desvios nem trechos do código divergente.

## Handoff

Sem desvio pendente: `/evidence`.
Com desvio `código corrigido`: `/implement` na task de correção, e depois
`/code-review` de novo.
Com desvio `spec corrigida`: o artefato dono, `derive_canvas.py` e
`check_drift.py` antes de seguir.
