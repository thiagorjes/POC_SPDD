---
id: ADR-011
type: ADR
status: accepted
date: 2026-09-09
supersedes: —
superseded-by: —
---

# ADR-011 — Migração de schema em serviço dedicado, fora do boot da aplicação

## Decisão

A migração de schema roda em um **serviço próprio do Compose**, que executa até o
fim e sai. As instâncias da aplicação sobem com validação de schema, sem permissão
de migrar, e dependem da migração por `condition: service_completed_successfully`.
Vale para todo sistema governado pela coleção `infra/docker`.

## Motivação

A prática corrente era rodar a migração no arranque da aplicação, o que funciona
enquanto existe exatamente uma instância. O sistema IDSD tem um requisito não
funcional cujo teste sobe **três** instâncias simultâneas para verificar broadcast
multi-pod, e nesse cenário as instâncias disputam o lock de migração: a que espera
pode estourar o `start_period` do healthcheck antes de a outra terminar, e o
ambiente falha na subida por uma razão que não tem nada a ver com o que está sendo
testado.

**Problema que resolve:**
Elimina a disputa em vez de dimensioná-la. O modo de falha anterior é intermitente
e sensível ao tamanho da migração — passa hoje, falha quando o schema crescer, e
falha primeiro na máquina mais lenta, que é onde ninguém está olhando.

**Restrições consideradas:**
- O ambiente inteiro roda em Compose, sem orquestrador (ADR-008, ADR-009).
- Flyway com `ddl-auto=validate` já é a decisão de versionamento de schema
  (ADR-005); esta decisão muda **quem** executa, não a ferramenta.
- O ambiente precisa subir do zero com um comando, sem passo manual.
- Registrado como Q-010 na TechSpec de `kanban-tarefas`, com a decisão exigida
  antes de `/implement`.

## Consequências

**Positivas:**
- A subida deixa de ter condição de corrida, independentemente do número de
  réplicas.
- A falha de migração passa a ser legível: o serviço de migração sai com código
  diferente de zero e a aplicação nem chega a tentar subir.
- É o desenho que sobrevive a uma eventual migração para orquestrador, onde ele
  vira um Job de inicialização — nada precisa ser repensado.

**Negativas / trade-offs:**
- Um serviço a mais no `compose`, e um passo a mais para quem lê o arquivo pela
  primeira vez.
- A aplicação perde a capacidade de se autocorrigir contra um banco desatualizado:
  se alguém subir a aplicação sem rodar a migração, ela falha na validação em vez
  de migrar. Isso é deliberado — falhar é o comportamento correto.

**Downstream afetado:**
- `infra/docker/architecture.md` §6 e `definition-of-done.md` item 11.
- `docs/techspec/kanban-tarefas/quickstart.md` §3, que descrevia a migração no boot
  e registrava a questão em aberto.
- Q-010 da TechSpec de `kanban-tarefas` fecha com esta decisão.
- **ADR-008**, cuja redação original afirmava que o Flyway continuava aplicando
  migrations no boot. Emendado em 2026-09-09 pelo achado INC-15 — a omissão desta
  linha foi o que deixou o DR contradizendo a decisão por um dia.

## Alternativas Consideradas

### Alternativa 1 — Manter a migração no boot com `start_period` folgado
**Descartada porque:** transforma o problema em um número mágico calibrado por
tentativa. O valor que funciona hoje deixa de funcionar quando a migração crescer,
e a falha volta na forma mais cara possível — intermitente, dependente de máquina,
e com mensagem que aponta para healthcheck em vez de para migração.

### Alternativa 2 — Uma instância privilegiada que migra e as demais que esperam
**Descartada porque:** exige distinguir instâncias que deveriam ser idênticas, e a
"instância que migra" vira um papel implícito que ninguém declara em lugar nenhum.
Reintroduz por configuração exatamente o acoplamento que separar o serviço remove.
