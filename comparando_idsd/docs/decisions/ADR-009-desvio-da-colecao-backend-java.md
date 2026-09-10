---
id: ADR-009
type: ADR
status: accepted
date: 2026-09-09
supersedes: —
superseded-by: —
---

# ADR-009 — Desvio declarado da coleção `backend/java` em três eixos: linguagem, banco e plataforma

## Decisão

O sistema IDSD executa em **Java 25**, persiste em **PostgreSQL** e é empacotado e
operado em **Docker on-premise**, divergindo dos três eixos correspondentes de
`requirements/guidelines/backend/java/stack.md` (Java 21, Oracle, OpenShift +
Nexus). Em **todo o resto** a coleção continua governando sem exceção: Screaming
Architecture com domínios sob `internal/<dominio>`, records para DTO, entidade JPA
que nunca sai do service, `@RestControllerAdvice` com `ProblemDetail`, rota `/v1`,
Flyway como único caminho de schema, transação curta e somente-leitura em consulta,
índice em toda chave estrangeira, `MockMvc` no controller e Mockito no service.

## Motivação

A própria `stack.md` prescreve que desvio de stack é exceção legítima **desde que
haja ADR aprovado**. Este é esse ADR. Ele não abre precedente para os demais
sistemas do workspace: a coleção segue sendo a norma, e este documento registra
por que um sistema específico não a segue em três pontos.

**Problema que resolve:**
A coleção e os ADRs desta feature — todos aceitos antes desta etapa — discordavam
em três eixos, e a discordância não era estilística. Sem uma decisão registrada, a
TechSpec escolheria em silêncio e a divergência reapareceria como achado no
`/analyze` ou, pior, como surpresa na implementação.

**Restrições consideradas:**
- ADR-004 realiza RF-020 e RNF-001 com `LISTEN/NOTIFY`, mecanismo que existe no
  PostgreSQL e não tem equivalente direto em Oracle. Adotar Oracle superaria
  ADR-004 e reabriria o desenho de RNF-001 e RNF-002 sem que nenhuma dor do
  produto o exigisse.
- ADR-002 proíbe cache e broker nesta fase. Sem `LISTEN/NOTIFY` e sem broker, o
  broadcast multi-instância cairia em polling — que não atende p95 ≤ 2 s de
  RNF-001 sem carga desproporcional.
- ADR-008 já decidiu contêiner como forma de entrega, e a operação é on-premise.
  OpenShift e Nexus corporativo pressupõem uma plataforma que este sistema não usa.
- Identidade **não** conflita: RH-SSO é Keycloak. `_shared/api-security.md`,
  ADR-003 e ADR-006 convergem e valem integralmente.

## Consequências

**Positivas:**
- ADR-004 permanece de pé, e com ele todo o desenho de RF-020, RNF-001 e RNF-002.
- O que a coleção decide sobre desenho de código — que é a maior parte dela e a
  parte que de fato governa a qualidade do que se escreve — continua valendo, sem
  ambiguidade sobre o que vale e o que não vale.

**Negativas / trade-offs:**
- Java 25 não é LTS na linha que a coleção fixou. O custo de acompanhar versão
  passa a ser deste sistema.
- Flyway com dialeto PostgreSQL diverge dos scripts de exemplo da coleção; a
  nomenclatura `V<ANO><MES><DIA><HORA>__descricao.sql` é mantida.
- H2 deixa de servir como banco de teste — ver SDR-003. Este é o efeito colateral
  mais concreto do desvio, e é o que obriga uma segunda decisão.
- Se o sistema um dia for para a plataforma corporativa, os três eixos voltam à
  mesa de uma vez.

**Downstream afetado:**
- TechSpec Seções 1, 3 e 7.
- `/tasks`: composição Docker com PostgreSQL e Keycloak como pré-requisito de
  qualquer execução, inclusive de teste.
- `/guidelines`: se outros sistemas on-premise surgirem, a coleção pode precisar
  de uma variante — hoje ela pressupõe a plataforma corporativa em três pontos que
  não são de desenho de código.

## Alternativas Consideradas

### Alternativa 1 — Coleção prevalece integralmente (Java 21, Oracle, OpenShift)
**Descartada porque:** superaria ADR-004 e obrigaria a redesenhar o broadcast
multi-instância sob a proibição de broker do ADR-002, sem nenhum ganho para o
produto. Aderência de plataforma não é objetivo deste sistema, que é on-premise
por decisão anterior.

### Alternativa 2 — Oracle com Java 25 e Docker
**Descartada porque:** incorre no mesmo custo da Alternativa 1 sobre ADR-004 — a
perda do `LISTEN/NOTIFY` — sem sequer obter em troca a aderência à plataforma
corporativa, que era a única justificativa daquela opção.
