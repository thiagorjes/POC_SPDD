---
name: implement
description: >
  Executa uma task até a suíte congelada passar, sem tocar nos cenários nem nos
  step definitions. Trabalha dentro do escopo de arquivo declarado na task,
  registra cada tentativa no histórico e para quando o orçamento acaba. Use após
  /tests, uma task por vez.
camada: execution
satisfaz-gates: []
input-artifacts:
  - docs/tasks/{{FEATURE}}/TASK-*.md
  - docs/tests/{{FEATURE}}-verificacao.md
  - docs/techspec/{{FEATURE}}-techspec.md
output-artifacts:
  - docs/tasks/{{FEATURE}}/TASK-*.md
---

## Objetivo

Fazer a suíte passar. Você recebe uma task auto-contida e uma suíte escrita
antes de você existir; entrega código de produção e o histórico do que
aconteceu.

Não é sua função julgar se a task está bem especificada — é sua função **parar**
quando ela não está. Task com decisão em aberto volta ao `/techspec`; cenário que
o código não consegue satisfazer volta ao `/prd` por emenda. Preencher a lacuna
sozinho é o modo mais comum de o pipeline entregar algo que ninguém aprovou.

## A restrição

`restricao: sem_escrita_em_feature_e_step_defs`.

Os `.feature` e os step definitions estão fora do seu alcance. Não os edite, não
os "ajuste", não adicione `skip`, não relaxe uma asserção. Se um teste está
errado, isso é um achado — registre no histórico e escale; não conserte.

O espelho está do outro lado: o `/tests` escreveu sem ver implementação. As duas
restrições juntas é que sustentam o GATE-VERIFICACAO-INDEPENDENTE.

## Argumentos

- `TASK-xx.y` — task a executar (obrigatório)
- `--tentativas N` — override do orçamento herdado do flow
- `--ci` — não interativo: falha em vez de perguntar
- `continuar` — retoma uma task com histórico de tentativas anteriores

## Pré-condições

- `docs/tasks/[feature]/TASK-xx.y-*.md` existe e passa no `check_task_files`.
- `docs/tests/[feature]-verificacao.md` existe, com a suíte congelada e a
  execução Red registrada.
- As dependências declaradas na task estão concluídas.
- A suíte roda. Se ela não roda antes de você começar, o problema não é seu
  código — pare e devolva ao `/tests`.

## Workflow

### Fase 0 — Ler a task, e só a task

Leia o arquivo individual da task. Ele foi escrito para bastar sozinho; se você
precisa abrir PRD ou TechSpec para saber o que fazer, a task está incompleta —
registre isso no histórico antes de continuar, porque é a informação mais útil
que esta execução vai produzir para a próxima.

Leia também o escopo proibido. Ele não é decorativo.

### Fase 1 — Confirmar o Red

Rode apenas os testes dos cenários cobertos pela task. Eles devem falhar por
ausência de implementação. Se algum já passa, algo está errado — a suíte, a task
ou o escopo. Escale.

### Fase 2 — Implementar

Trabalhe apenas nos arquivos declarados. Siga a assinatura, os nomes de campo e
as mensagens literais transcritas na task: elas são contrato, não sugestão.

Todo caminho é relativo a `systems/[sistema]/`.

Nada de código morto, nada de funcionalidade não pedida, nada de refatoração
oportunista fora do escopo — cada linha a mais amplia o que o revisor precisa
ler, e capacidade de revisão é o gargalo do modelo.

### Fase 3 — Verde

Rode a suíte da task. Depois rode a suíte inteira do sistema: quebrar teste de
outra task é regressão, não detalhe.

Se ficou verde alterando algo fora do escopo, não ficou verde.

### Fase 4 — Registrar

Atualize o histórico da task **a cada tentativa**, no momento em que ela ocorre:
o que foi tentado, o que falhou, o que decidiu. Ao final, atualize `Status`.

O histórico é a fonte do `/evidence`. Reconstruído no fim, não vale como
evidência e reprova o selo.

Commit com o ID no título: `feat: TASK-03.4 — descrição`. O estado não vem de
quem trabalhou; vem do git.

### Fase 5 — Orçamento esgotado

Ao consumir a última tentativa, **pare**. Registre o estado real, o que já
funciona, o que falta e a sua melhor hipótese sobre a causa.

Isso não é falha do processo — é o caminho normal com outro valor no campo
`executor`. Um humano assume, o commit continua trazendo o ID, os testes
continuam rodando e o gate é idêntico.

## Validação

```
python .agents/skills/implement/scripts/check_escopo.py \
  --task docs/tasks/[feature]/TASK-xx.y-*.md
```

Confere, contra o git, que nada fora do escopo declarado foi tocado e que os
`.feature` e step definitions permanecem intactos.

## Handoff

Próximo comando: `/code-review TASK-xx.y`. Se foi a última task do épico, abra
o PR do épico.
