# Legado — coleção plana de guidelines do CRUDAO

Estes seis arquivos eram a coleção única de guidelines do sistema CRUDAO, no
formato anterior à biblioteca compartilhada por stack. Foram retirados da raiz
de `requirements/guidelines/` em 2026-09-04 porque duplicavam, em versão mais
pobre, o que `_shared/`, `backend/java/` e `frontend/nextjs/` já decidem — e
duplicação divergente é o defeito que a biblioteca existe para evitar.

Estão aqui como histórico. **Não são norma vigente.** Nenhum sistema deve
declará-los em `guidelines.yaml`.

## O que foi aproveitado

| Achado | Destino |
|---|---|
| Introspecção JavaBeans quebra em campo `eFinal` (achado TASK-01.1) | `backend/java/coding-standards.md` § Nomenclatura |
| OAuth2 client resolve o issuer OIDC eagerly na subida do contexto (achado TASK-02.3) | `backend/java/testing.md` §5 |

## O que não foi aproveitado, e por quê

- **Decisões de arquitetura do CRUDAO** (STOMP por projeto, multi-instância,
  `LISTEN/NOTIFY`): são do sistema, não da stack. Vivem nos ADRs e na TechSpec.
- **`observability.md`** (rotação de log a cada 5MB, sem APM): configuração de
  deploy de um sistema, não norma de stack.
- **`security.md`**: contradiz ADR-006 — descrevia fallback de autenticação
  própria, que foi descartado.
- **Padrões de lint "default, sem regra customizada"**: as coleções por stack
  fixam ferramenta e limite concretos, que é o que dá verificador.
