# Desvios — cobranca

## Sumário

| DEV | Artefato de spec | Direção | Status |
| --- | --- | --- | --- |
| DEV-01 | PRD §RN-004 | spec corrigida | resolvido |

---

## DEV-01 — Janela de idempotência

- **Detectado em:** 2026-09-03
- **Artefato de spec:** docs/prd/cobranca-prd.md §RN-004
- **Local no código:** billing/idempotency.py:88
- **Cenário afetado:** SCN-004.2
- **O que a spec diz:** duplicata é rejeitada por 24h.
- **O que o código faz:** rejeita por 48h.
- **Quem decidiu:** Thiago (product owner)
- **Direção:** spec corrigida
- **Por quê:** a janela de 24h foi copiada do sistema legado sem checar o prazo de estorno do adquirente, que é de 48h.
- **Status:** resolvido

---

## Fora deste artefato — regras negativas

- **Não** corrige a spec nem o código.
