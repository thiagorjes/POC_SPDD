---
id: DDR-004
type: DDR
status: accepted
date: 2026-09-04
supersedes: DDR-001
superseded-by: —
---

# DDR-004 — Adoção do design system da coleção Next.js como fonte de verdade visual

## Decisão

A identidade visual de `kanban-tarefas` é integralmente herdada de
`requirements/guidelines/frontend/nextjs/design-system.md` e do
`design-tokens.json` da mesma coleção, declarada no `guidelines.yaml` deste
sistema. Ação primária `#004B8D`, Inter Variable com escala fechada de oito
degraus e pesos 400/500/600, raio base 16px, alturas de controle 44/36,
espaçamento com base 16px, mecânica Radix + CVA + `cn()` sobre os primitivos de
`components/ui/`.

O produto entrega **tema claro e escuro**, viabilizado pelo uso exclusivo de
tokens semânticos em superfície e texto.

Nenhum valor visual é decidido no Design Brief deste sistema. O brief aponta
para a coleção.

## Motivação

A Fase 0 do `/design` encontrou três fontes visuais em desacordo: a coleção de
guidelines que o próprio sistema declara governá-lo, o DDR-001 (`#0d6efd`,
Roboto, apenas tema claro) e o Design Brief v1.0 de 2026-08-25, que dizia Inter
e contradizia o DDR que o originou.

**Problema que resolve:**
Elimina a divergência entre a norma declarada e o material produzido, e evita
que o `/code-review` — que revisa contra a coleção — reprove sistematicamente um
frontend construído sobre outra base.

**Restrições consideradas:**
- `guidelines.yaml` declara `frontend/nextjs` como coleção governante.
- O design system foi extraído por engenharia reversa de aplicação em produção:
  os componentes existem, estão testados e têm catálogo vivo.
- A Intent exige interface responsiva para desktop nos navegadores da equipe, o
  que o design system já atende com sua abordagem desktop-first.

## Consequências

**Positivas:**
- O frontend herda primitivos prontos em vez de recriá-los, com acessibilidade e
  comportamento de teclado já resolvidos.
- Tema escuro passa a ser possível sem trabalho adicional.
- A norma e a prática deixam de divergir.

**Negativas / trade-offs:**
- Os nove protótipos e o `design-tokens.json` de agosto perdem validade visual e
  precisam ser refeitos. O conteúdo de fluxo e estados deles permanece útil.
- O produto passa a carregar a identidade institucional da coleção, abrindo mão
  de identidade própria.
- A coleção não tem papel semântico para **impedimento**, condição central deste
  produto. Resolver isso exige ou criar token na biblioteca compartilhada, que
  governa outros sistemas, ou reutilizar um papel de significado impreciso —
  registrado como QD-01 no Design Brief.

**Downstream afetado:**
- Design Brief, protótipos e tokens deste sistema.
- `/techspec`: arquitetura de frontend passa a assumir Radix, CVA e os
  primitivos da coleção.
- `/code-review`: revisa contra a coleção, sem exceção declarada.

## Alternativas Consideradas

### Alternativa 1 — Manter o DDR-001
**Descartada porque:** preservaria os protótipos de agosto ao custo de o sistema
contrariar a coleção que ele mesmo declara no `guidelines.yaml`. A economia é
imediata; o conflito é permanente.

### Alternativa 2 — Híbrido: mecânica da coleção, paleta do DDR-001
**Descartada porque:** exigiria tokenizar a paleta antiga como par claro/escuro
dentro da coleção, criando uma segunda identidade que a biblioteca teria de
manter. O ganho seria estético, e nenhuma fonte registrou exigência de
identidade própria para este produto.
