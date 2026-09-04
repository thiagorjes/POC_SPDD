# REASONS Canvas — demo

<!-- GERADO por .agents/scripts/derive_canvas.py — nao editar. Altere a fonte e regenere. -->

> Projeção das fontes. Não há transição manual DRAFT → READY: ou o
> canvas está em sincronia e o check `canvas-drift` passa, ou não passa.

---

## Fontes

| Artefato | Conteúdo |
| --- | --- |
| `docs/prd/demo-prd.md` | 85b6c710e904 |
| `docs/solution/demo-solution.md` | d5f79743ceb8 |
| `docs/techspec/demo-techspec.md` | 0911b3e23c7b |
| `docs/tasks/demo-tasks.md` | 95f8da9f6477 |
| `docs/review/demo/EPIC-01-review.md` | e8aaaea08f2d |

---

## R — Requirements

_Fonte: docs/prd/demo-prd.md_

Registrar recusa de pedido.

**Requisitos:**
- RF-001 — Registrar motivo da recusa

---

## E — Entities

_Fonte: docs/solution/demo-solution.md, docs/techspec/demo-techspec.md_

- Recusa: registro do motivo.

Tabela recusa.

---

## A — Approach

_Fonte: docs/techspec/demo-techspec.md_

Serviço isolado.

---

## S — Structure

_Fonte: docs/techspec/demo-techspec.md_

Síncrono.

Nenhuma.

---

## O — Operations

_Fonte: docs/tasks/demo-tasks.md_

**EPIC-01 — Recusa**
- TASK-01.1 — Persistir motivo

---

## N — Norms

_Fonte: guidelines.yaml_

- `backend/demo`
  - `backend/demo/testing.md`
- `_shared` (transversais)
  - `_shared/logging-and-levels.md`

---

## S — Safeguards

_Fonte: docs/review/demo/_

<!-- EPIC-01-review.md -->
| Guardrail | Origem | Onde |
| --- | --- | --- |
| Validar motivo | ACH-01 | guidelines/coding.md |

---

## Âncoras não resolvidas

Nenhuma.
