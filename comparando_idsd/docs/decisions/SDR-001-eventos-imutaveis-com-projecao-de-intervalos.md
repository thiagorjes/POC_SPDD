---
id: SDR-001
type: SDR
status: accepted
date: 2026-09-09
supersedes: —
superseded-by: —
---

# SDR-001 — Eventos imutáveis como fonte de verdade, com projeção de intervalos para as três séries de tempo

## Decisão

O histórico da tarefa é um **log append-only de eventos de domínio**: nada nele é
alterado ou removido depois de gravado. As três séries de tempo que RN-008 obriga
a manter distintas — permanência na etapa, espera de tomada e impedimento — são
**projeções derivadas** desse log, materializadas numa tabela de intervalos
reconstruível a qualquer momento a partir dele. Os episódios de RN-019 caem como
sequência natural no log; o agregado consulta a projeção, nunca o log.

## Motivação

RNF-008 exige que nenhuma operação do produto altere ou remova evento já
registrado. RNF-009 exige p95 ≤ 2 s nas consultas de RF-015 e RF-016 com 12 meses
de histórico e 5.000 tarefas. RN-019 exige que o tempo por etapa de uma tarefa
reaberta some episódios distinguíveis. As três pressões não se resolvem no mesmo
modelo sem separar quem responde pela verdade de quem responde pela leitura.

**Problema que resolve:**
Manter imutabilidade literal do histórico sem pagar o custo de agregar sobre o log
inteiro a cada consulta — e sem introduzir cache, que ADR-002 proíbe nesta fase.

**Restrições consideradas:**
- ADR-002 proíbe cache e broker. A projeção é uma tabela do próprio PostgreSQL,
  não um componente novo de infraestrutura.
- RN-008 proíbe somar as três séries entre si. A projeção guarda cada uma com tipo
  próprio; não existe coluna de total.
- RN-014 proíbe qualquer agregação por pessoa. A projeção de intervalos **não tem
  coluna de pessoa** — quem assumiu vive no log, que não é a fonte dos agregados.
  A proibição passa a ser estrutural, não um filtro que alguém pode esquecer.
- RN-031 exige idempotência de toda escrita. O evento carrega o estado de origem
  declarado (ver SDR-002); repetir a mesma solicitação a partir do mesmo estado não
  produz evento novo.

## Consequências

**Positivas:**
- RNF-008 é satisfeito literalmente, não por convenção: a tabela de eventos não
  tem caminho de `UPDATE` nem de `DELETE` no produto.
- A projeção é descartável. Defeito de cálculo em série de tempo se corrige
  reconstruindo, sem perda de histórico e sem migração de dado.
- RN-014 deixa de depender de disciplina de quem escreve a consulta.

**Negativas / trade-offs:**
- Duas escritas por transição, e a projeção precisa ser atualizada na mesma
  transação do evento, sob pena de o board exibir um estado e o agregado outro.
- Existe divergência possível entre log e projeção se alguém escrever na projeção
  fora do caminho canônico. Mitigado por reconstrução verificável — o teste de
  integração reconstrói a projeção e compara.
- Consulta ad hoc ao histórico bruto é mais trabalhosa do que leria uma tabela de
  estado corrente.

**Downstream afetado:**
- TechSpec Seções 3 e 5; `data-model.md`.
- `/tasks`: a rotina de reconstrução da projeção é entregável próprio, não um
  script solto — é o que sustenta a alegação de que a projeção é descartável.

## Alternativas Consideradas

### Alternativa 1 — Tabela de intervalos como fonte única de verdade
**Descartada porque:** fechar um intervalo escreve na linha que já existe, e isso
é exatamente o que RNF-008 proíbe. O envelope diz "nenhuma operação do produto
altera evento já registrado" — um intervalo com `fim` preenchido depois é um
registro alterado, por mais natural que a operação pareça.

### Alternativa 2 — Event sourcing puro, séries agregadas por consulta
**Descartada porque:** imutabilidade perfeita, mas RNF-009 fica exposto. Agregar
12 meses de log a cada consulta de RF-016 exigiria cache para caber em 2 s, e
cache é justamente o que ADR-002 proíbe nesta fase. A projeção materializada é a
forma de obter o mesmo resultado dentro da restrição vigente.
