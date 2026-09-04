---
name: tests
description: >
  Escreve a suíte de verificação a partir dos cenários congelados, antes da
  implementação e sem acesso a ela. Produz step definitions, testes e o plano de
  verificação, e congela a suíte. Satisfaz a verificação independente e o
  congelamento do Gherkin. Use após /tasks e antes de /implement.
camada: execution
satisfaz-gates: [GATE-VERIFICACAO-INDEPENDENTE, GATE-GHERKIN-CONGELADO]
template: .agents/templates/verificacao-template.md
input-artifacts:
  - docs/prd/{{FEATURE}}-prd.md
  - docs/prd/{{FEATURE}}/*.feature
  - docs/techspec/{{FEATURE}}-techspec.md
  - docs/tasks/{{FEATURE}}-tasks.md
output-artifacts:
  - docs/tests/{{FEATURE}}-verificacao.md
  - features/{{FEATURE}}/*.feature
---

## Objetivo

Traduzir contrato em suíte executável. Você recebe cenários congelados e tasks
planejadas, e entrega os step definitions e os testes que dirão, depois, se a
implementação cumpre o que foi acordado.

Você roda **antes** do `/implement` e **sem ler código de produção**. Essa é a
única coisa que torna a verificação independente: uma suíte escrita depois, por
quem viu a implementação, tende a descrever o que o código faz em vez do que o
contrato exige — e passa em verde sem verificar nada.

## A restrição

`restricao: sem_acesso_a_implementacao`.

Na prática:

- Não abra arquivos de produção da feature. Se precisar do formato de um retorno,
  ele está no contrato da TechSpec; se não está, é lacuna de spec — devolva.
- Não escreva código de produção, nem "só um stub para o teste compilar". Stub
  necessário é teste mal desenhado ou contrato incompleto.
- A execução inicial é **Red**: os testes de cenário falham por ausência de
  implementação. Teste que passa antes de existir implementação está verificando
  outra coisa — investigue antes de seguir.

E o espelho disso, do outro lado: a task do `/implement` declara os `.feature` e
os step definitions como escopo proibido. As duas restrições juntas é que fecham
o gate; sozinha, cada uma é apenas uma boa intenção.

## Argumentos

- (sem argumento) — feature ativa
- `"nome-da-feature"` — feature específica
- `TASK-xx.y` — restringe a suíte às tasks indicadas
- `--ci` — não interativo: sem plano para confirmar, falha em vez de perguntar
- `audit` — exceção: código já existe (flow de dependência, migração ou legado).
  Registre a justificativa na declaração de independência; o gate passa a ser
  avaliado sabendo que a suíte não é independente.

## Pré-condições

- PRD aprovado no gate de spec, com `.feature` gerados.
- `docs/tasks/[feature]-tasks.md` validado.
- `guidelines.yaml` declara ao menos uma coleção com `testing.md`. Sem ela não
  há convenção a seguir — pare e rode `/guidelines`. Os transversais em
  `_shared/` não substituem: eles não decidem framework nem estrutura de suíte.

## Workflow

### Fase 0 — Leitura

PRD e `.feature`, estratégia de testes da TechSpec, tasks (tipo de teste
declarado por cenário) e `testing.md`. Nada mais.

### Fase 1 — Congelamento do Gherkin

Confira que os `.feature` no repositório são idênticos aos aprovados no gate de
spec. Divergência sem emenda registrada reprova o GATE-GHERKIN-CONGELADO — e não
se conserta editando o `.feature` para bater com o que você prefere.

```
python .agents/skills/tests/scripts/check_congelamento.py \
  --prd docs/prd/[feature]-prd.md \
  --features docs/prd/[feature]
```

### Fase 2 — Plano de verificação

Preencha o plano antes de escrever teste: escopo por cenário, tipo, arquivo de
destino, dependências a simular e os testes que existem além do Gherkin —
propriedade, carga, fronteira numérica, o que o RNF exige.

Fora do modo `--ci`, apresente o plano e pergunte o que falta. Suíte grande
refeita por escopo mal calibrado é o desperdício mais comum desta etapa.

### Fase 3 — Suíte

Um teste por cenário congelado, no mínimo. O nome do teste descreve o
comportamento, nunca a implementação. Salve incrementalmente.

Simulação de dependência tem motivo declarado no plano. Mock sem motivo vira
teste que verifica o mock.

### Fase 4 — Execução Red

Rode a suíte. O resultado esperado é: todos os testes de cenário falhando por
ausência de implementação, nenhum falhando por erro na própria suíte. Registre a
tabela de execução inicial.

Em modo `audit` este resultado não se aplica; registre o que de fato ocorreu.
Teste que falha em `audit` é possível defeito real — reporte, não conserte o
teste para ficar verde.

### Fase 5 — Congelar e validar

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/tests/validate-rules.json \
  --artifact docs/tests/[feature]-verificacao.md
```

A partir daqui a suíte não é alterada pela implementação. Alteração legítima
exige emenda de cenário no PRD, com o ID preservado, ou desvio aprovado por
revisor humano — em qualquer caso, registrada na tabela de congelamento.

## Handoff

Próximo comando: `/implement TASK-xx.y`.
