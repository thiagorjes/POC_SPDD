---
id: BDR-002
type: BDR
status: accepted
date: 2026-09-09
supersedes: —
superseded-by: —
---

# BDR-002 — Desbloqueio de impedimento como permissão de papéis existentes, sem papel novo

## Decisão

RN-024 não introduz papel novo no catálogo do BDR-001. A permissão de desbloqueio
— receber o destaque do impedimento aberto e declará-lo resolvido — é atribuída
aos papéis **`product_owner`** e **`project_admin`**, por projeto, como qualquer
outra permissão do catálogo fechado. Quem sinalizou o impedimento também pode
resolvê-lo, independentemente de papel, conforme SCN-010.2.

## Motivação

BDR-001 fechou o catálogo de papéis e descartou explicitamente RBAC configurável
por projeto. Acrescentar um papel a cada responsabilidade nova erode essa decisão
por acúmulo, e um sinalizador no vínculo pessoa↔projeto criaria a segunda trilha
de autorização que a Alternativa 1 do BDR-001 já havia recusado.

**Problema que resolve:**
Dar dono ao impedimento sem expandir o modelo de autorização.

**Restrições consideradas:**
- RN-024 tem fallback: projeto que não configurou o papel torna o impedimento
  visível a **todos** os participantes, e SCN-019.3 verifica exatamente isso. O
  fallback continua alcançável — um projeto cujos participantes sejam apenas `dev`
  e `gestor`, administrado pelo admin global, não tem ninguém com a permissão. O
  cenário não fica órfão.
- RN-015 continua valendo: `gestor` é somente-leitura e nunca recebe a permissão,
  nem o destaque acionável.
- BDR-001 mantém `admin` global fora do vínculo por projeto.

## Consequências

**Positivas:**
- Nenhuma entidade nova de autorização; a checagem por projeto que BDR-001 já
  obriga passa a cobrir também o desbloqueio.
- O modelo mental de quem administra o projeto não cresce.

**Negativas / trade-offs:**
- Na maioria dos projetos haverá alguém com a permissão, então o fallback de
  RN-024 será raro na prática. Ele deixa de ser um caminho comum e passa a ser um
  caso de borda — verificado, mas pouco exercitado em produção.
- Um projeto que queira separar "quem desbloqueia" de "quem administra" não
  consegue, sem que a decisão seja revista.

**Downstream afetado:**
- TechSpec Seções 3 e 8; `data-model.md`, na tabela de permissão por papel.
- `/tasks`: a permissão entra no mesmo mecanismo de checagem por projeto do
  BDR-001, sem trabalho de modelagem próprio.

## Alternativas Consideradas

### Alternativa 1 — Papel `desbloqueador` no catálogo
**Descartada porque:** o demandante optou por não expandir o catálogo. O ganho
seria tornar o fallback de RN-024 mais frequente e permitir separar desbloqueio de
administração — nenhum dos dois foi levantado como necessidade concreta.

### Alternativa 2 — Sinalizador booleano no vínculo pessoa↔projeto
**Descartada porque:** cria autorização fora do catálogo de papéis, exatamente o
que BDR-001 recusou ao descartar RBAC configurável.
