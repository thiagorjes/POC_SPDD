---
id: ADR-012
type: ADR
status: accepted
date: 2026-09-09
supersedes: —
superseded-by: —
---

# ADR-012 — Suíte em contêiner acessa o daemon do host por socket montado; DinD descartado

## Decisão

O serviço de teste recebe o **socket do daemon Docker do host** montado. Os
contêineres que a suíte cria em tempo de execução são irmãos do serviço de teste,
não filhos, e ficam na mesma rede que ele. Docker-in-Docker está descartado. A
montagem existe apenas em `compose.test.yaml`, e é aceita **somente** em
desenvolvimento e CI.

## Motivação

A estratégia de teste do workspace usa banco real em contêiner no lugar de banco em
memória (SDR-003 do sistema IDSD), porque o mecanismo verificado — notificação
assíncrona pelo próprio banco — não existe em banco em memória, e um teste verde
sobre ele teria aparência de prova sem sê-lo. Como toda execução acontece em
contêiner (ADR-008), a suíte precisa criar contêineres de dentro de um contêiner, e
isso tem exatamente dois caminhos.

**Problema que resolve:**
Sem uma decisão declarada, cada sistema resolve isso sozinho na primeira vez que a
suíte não roda, e resolve sob pressão — normalmente concedendo mais privilégio do
que precisaria, sem registrar que concedeu.

**Restrições consideradas:**
- Não há orquestrador nem runner gerenciado; o alvo é Docker local (ADR-009).
- A suíte precisa ser executável por quem desenvolve, com um comando.
- Montar o socket concede ao serviço de teste o equivalente a **root no host** —
  quem controla o daemon controla a máquina.
- Ciclo de teste lento é ciclo contornado; cache entre execuções é requisito, não
  conforto.

## Consequências

**Positivas:**
- A suíte roda com a mesma imagem de banco do ambiente de desenvolvimento, o que é
  o ponto inteiro de ter tirado o banco em memória.
- Cache de camadas e de dependências preservado entre execuções, porque o daemon é
  o mesmo.
- Sem contêiner privilegiado, que é o que DinD exigiria.

**Negativas / trade-offs:**
- Privilégio efetivo de root no host para o serviço de teste. É o custo aceito, e é
  por isso que o escopo está restrito por escrito a desenvolvimento e CI, com a
  proibição explícita em ambiente compartilhado.
- Os contêineres criados pela suíte não são resolvidos pelo nome de serviço do
  Compose, por serem irmãos. A rede compartilhada é obrigatória, e esquecê-la
  produz falha de conexão que parece problema de aplicação.
- Contêiner órfão sobrevive a uma suíte interrompida, porque o daemon é do host.

**Downstream afetado:**
- `infra/docker/testing.md` §3 e `definition-of-done.md` item 13.
- `docs/techspec/kanban-tarefas/quickstart.md` §3, que tomou esta decisão
  localmente e a declarou como rascunho da coleção.
- Requisito de CI: runner com daemon acessível ao job.

## Alternativas Consideradas

### Alternativa 1 — Docker-in-Docker
**Descartada porque:** custa uma camada de armazenamento inteira e perde o cache
entre execuções, o que encarece o ciclo a ponto de as pessoas contornarem a suíte
— e suíte contornada não verifica nada. Além disso exige contêiner privilegiado,
que não é obviamente menos perigoso que o socket montado: troca um risco explícito
e escopado por um difuso.

### Alternativa 2 — Rodar a suíte fora de contêiner, direto na máquina
**Descartada porque:** contradiz ADR-008 e reintroduz o pré-requisito de toolchain
local que a containerização removeu. O ambiente passaria a ter dois modos de
execução, e o que falha é sempre o que ninguém usa no dia a dia.
