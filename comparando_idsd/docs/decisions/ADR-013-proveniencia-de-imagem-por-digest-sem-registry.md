---
id: ADR-013
type: ADR
status: accepted
date: 2026-09-09
supersedes: —
superseded-by: —
---

# ADR-013 — Proveniência de imagem por digest, sem registry enquanto não houver publicação

## Decisão

Toda imagem base é referenciada por `nome:tag@sha256:<digest>`. **Não se adota
registry** — nem interno, nem gerenciado — enquanto a execução for exclusivamente
local e não houver publicação em ambiente compartilhado. `latest` é proibido em
qualquer referência, e imagem construída localmente é marcada com a revisão de
código que a originou.

## Motivação

Duas perguntas apareceram juntas e têm respostas opostas, o que é a razão de
estarem no mesmo registro.

Fixação por tag é insuficiente: tag é mutável, e `eclipse-temurin:25-jre` de hoje
não é a de daqui a três meses. O build deixa de ser reprodutível **em silêncio** —
nada falha, nada avisa, e a diferença aparece como comportamento inexplicado em uma
máquina só. Digest resolve isso e custa um passo explícito de atualização.

Registry é o inverso: seria infraestrutura normatizada sem existir. A avaliação
pedida pelo demandante concluiu contra o registry interno on-premise que ele
cogitou — não porque a escolha estivesse errada em si, mas porque publicação,
retenção e política de tag de release descrevem um fluxo que hoje ninguém executa.
Regra que ninguém consegue conferir olhando um diff não é norma, é opinião, e a
biblioteca inteira existe para não produzir isso.

**Problema que resolve:**
Separa o que precisa de garantia agora (reprodutibilidade do build) do que só
precisará quando houver produção (proveniência de artefato publicado), sem deixar o
segundo implícito.

**Restrições consideradas:**
- Execução exclusivamente em Docker local; sem deploy e sem ambiente compartilhado
  (ADR-008, ADR-009).
- Política de CVE transversal já exige varredura e tratativa — ela se aplica à
  imagem base tanto quanto a dependência de aplicação.
- Não há CI, então a atualização de digest é feita por pessoa ou por ferramenta de
  atualização de dependência.

## Consequências

**Positivas:**
- Build reprodutível: a mesma revisão de código produz a mesma imagem em qualquer
  máquina.
- Atualização de imagem base vira evento visível no histórico — commit próprio, com
  razão declarada —, e é assim que a política de CVE fica auditável.
- Nenhuma norma escrita sobre infraestrutura inexistente.

**Negativas / trade-offs:**
- Atualizar o digest é passo manual enquanto não houver ferramenta de atualização
  automática configurada. O risco é ficar para trás em correção de segurança, e a
  mitigação é a varredura obrigatória do `definition-of-done`.
- Sem registry, não há como reproduzir exatamente a imagem **da aplicação** de uma
  revisão passada sem reconstruí-la. Aceitável enquanto não há deploy; deixa de ser
  no dia em que houver.
- A dívida fica nomeada e alguém precisa reabri-la no momento certo, que é uma
  dependência de disciplina.

**Downstream afetado:**
- `infra/docker/stack.md` (tabela de imagens), `coding-standards.md` §2 e
  `definition-of-done.md` §3.
- Reabrir quando existir ambiente compartilhado, publicação fora da máquina de quem
  desenvolve, ou CI que construa imagem.

## Alternativas Consideradas

### Alternativa 1 — Registry interno on-premise agora
**Descartada porque:** normatizaria publicação, retenção e tag de release para um
fluxo que não existe. O custo não é o servidor: é a coleção passar a conter regras
não verificáveis, que envelhecem sem ninguém perceber e ensinam que norma desta
biblioteca é aspiracional.

### Alternativa 2 — Tag semântica completa, sem digest
**Descartada porque:** mais leve de manter e insuficiente no que importa. Não
garante reprodutibilidade nem detecta troca de conteúdo sob a mesma tag, que é
exatamente o modo de falha silencioso que o digest existe para fechar.
