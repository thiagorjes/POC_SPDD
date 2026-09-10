# Pesquisa técnica — kanban-tarefas

_Data: 2026-09-09 | Alimenta: `kanban-tarefas-techspec.md`_

> Registro das incertezas técnicas da Fase 2 do `/techspec`. Uma seção por
> incerteza: o requisito do PRD que a origina, as opções pesadas, a decisão e o
> impacto na TechSpec. Fecha com o que permanece em aberto.

---

## I-01 — Conflito entre a coleção `backend/java` e os ADRs desta feature

**Origina-se de:** RF-020, RNF-001, RNF-002 (via ADR-004), e da própria
pré-condição do `/techspec` de seguir `guidelines.yaml`.

**O que era desconhecido:** qual das duas fontes prevalece quando a coleção fixa
Java 21, Oracle e OpenShift e os ADRs aceitos fixam Java 25, PostgreSQL e Docker
on-premise.

| Opção | Prós | Contras |
| --- | --- | --- |
| ADRs prevalecem nos três eixos | Preserva ADR-004 e o desenho de RNF-001/002; a coleção continua governando desenho de código | Java fora da linha LTS da coleção; H2 deixa de servir em teste |
| Coleção prevalece integralmente | Aderência à plataforma corporativa | Supera ADR-004; broadcast multi-instância cai em polling sob a proibição de broker do ADR-002 |
| Oracle com Java 25 e Docker | — | Mesmo custo sobre ADR-004, sem a aderência que justificaria pagá-lo |

**Decisão:** ADRs prevalecem nos três eixos; coleção governa todo o resto.
Registrada em **ADR-009**. Confirmada pelo demandante em 2026-09-09.

**Impacto na TechSpec:** Seções 1, 3 e 7. Gera I-04 como consequência direta.

---

## I-02 — Como manter três séries de tempo distintas sob imutabilidade e envelope de consulta

**Origina-se de:** RN-007, RN-008, RN-019, RNF-008, RNF-009.

**O que era desconhecido:** RNF-008 proíbe alterar registro já gravado; RNF-009
exige p95 ≤ 2 s com 12 meses e 5.000 tarefas; RN-019 exige somar episódios
distinguíveis de tarefa reaberta. Um modelo único que satisfaça os três não é
óbvio, e ADR-002 fecha a saída fácil ao proibir cache.

| Opção | Prós | Contras |
| --- | --- | --- |
| Log de eventos imutável + projeção de intervalos | Imutabilidade literal; consulta rápida; projeção descartável e reconstruível; RN-014 vira estrutural | Duas escritas por transição; divergência possível se alguém escrever fora do caminho canônico |
| Tabela de intervalos como fonte única | Simples; consulta trivial | Fechar intervalo escreve na linha existente — colide de frente com RNF-008 |
| Event sourcing puro, agregação por consulta | Imutabilidade perfeita; nenhuma projeção a manter | RNF-009 exposto; caberia em 2 s só com cache, que ADR-002 proíbe |

**Decisão:** log imutável com projeção de intervalos. Registrada em **SDR-001**.
Confirmada pelo demandante em 2026-09-09.

**Impacto na TechSpec:** Seções 3 e 5; `kanban-tarefas/data-model.md`.

Achado colateral que a pesquisa produziu e que virou parte da decisão: a projeção
de intervalos **não precisa de coluna de pessoa**. Isso transforma RN-014 — a
proibição de agregar tempo por pessoa — de disciplina de quem escreve a consulta
em propriedade do esquema. Era a regra do PRD com maior risco de erosão silenciosa
ao longo do tempo, porque nenhum teste falha quando alguém acrescenta um filtro.

---

## I-03 — Concorrência, origem declarada e idempotência: um mecanismo ou três

**Origina-se de:** RN-012, RN-013, RN-031, RNF-004.

**O que era desconhecido:** `_shared/api-standards.md` §4 sugere
`Idempotency-Key` para POST reenviável, e §2 reserva `409` para versão otimista
divergente. Não estava claro se idempotência e concorrência deveriam ser dois
controles ou um.

| Opção | Prós | Contras |
| --- | --- | --- |
| Estado de origem declarado, com bloqueio otimista | Um mecanismo para as três regras; devolve estado corrente a quem perde, como SCN-020.3 exige | Corpo de escrita mais verboso; cliente precisa manter o estado lido |
| `Idempotency-Key` + versão otimista separada | Aderente à letra de `api-standards.md` §4 | Duas trilhas; a chave não detecta mudança feita por outra pessoa, que é o caso de RN-012 |
| Bloqueio pessimista | Sem conflito a tratar | Transação longa em board colaborativo; contraria `database.md` §5 |

**Decisão:** estado de origem declarado, com bloqueio otimista. Registrada em
**SDR-002**.

**Impacto na TechSpec:** Seções 4 e 5; todos os contratos de escrita.

---

## I-04 — Banco de teste e ferramenta de E2E

**Origina-se de:** RF-020, RNF-001, RNF-002, RNF-006, e dos 14 cenários que o PRD
tipa como `e2e`. Consequência direta de I-01.

**O que era desconhecido:** `backend/java/testing.md` §4 prescreve H2, que não
implementa `LISTEN/NOTIFY`; e `frontend/nextjs/testing.md` não declara nenhuma
ferramenta de navegador, embora o PRD exija duas sessões simultâneas em três
cenários.

| Opção | Prós | Contras |
| --- | --- | --- |
| Testcontainers + Playwright | Testa o dialeto e o mecanismo reais; cobre acessibilidade no mesmo percurso; coerente com "tudo em Docker" | Suíte mais lenta; Docker vira pré-requisito de qualquer teste; ferramenta nova no workspace |
| H2 + verificação de `LISTEN/NOTIFY` só em ambiente integrado | Suíte rápida | Empurra a verificação do mecanismo central de RF-020 para o gate que a pendência 01 registra como aprovação manual — ou seja, não aconteceria |
| Cypress | Difundido | Múltiplas sessões simultâneas desconfortáveis; SCN-020.1 e SCN-020.3 sairiam artificiais |
| Rebaixar `e2e` para integração | Nenhuma ferramenta nova | Os cenários estão congelados **com o tipo de teste declarado**; exigiria emenda motivada por conveniência |

**Decisão:** Testcontainers com a imagem do `compose`, e Playwright para os
cenários `e2e`. Registrada em **SDR-003**. Confirmada pelo demandante em
2026-09-09.

**Impacto na TechSpec:** Seção 7; `kanban-tarefas/quickstart.md`.

---

## I-05 — Entrega da notificação por usuário em múltiplas instâncias

**Origina-se de:** RF-014, RF-020, RNF-002.

**O que era desconhecido:** ADR-004 já registrou, nas Consequências, que
`convertAndSendToUser` isolado por instância não garante entrega quando a pessoa
está conectada a uma instância diferente da que processou o evento. O ADR deixou o
mecanismo em aberto para a implementação. SCN-020.2 depende dele: um item precisa
chegar à fila de quem está com ela aberta, sem recarregar.

| Opção | Prós | Contras |
| --- | --- | --- |
| Único canal `board_events`, cada instância decide o que retransmite às suas sessões | Sem componente novo; consistente com ADR-002 e ADR-004; uma só trilha de broadcast | Toda instância recebe todo evento; custo cresce com o número de instâncias |
| Relay de broker STOMP compartilhado | Roteamento nativo por usuário | Introduz broker, que ADR-002 proíbe nesta fase |
| Canal `LISTEN/NOTIFY` por usuário | Roteamento preciso | Uma conexão de escuta por pessoa conectada; não escala para as 300 sessões de RNF-002 |

**Decisão:** canal único, com a decisão de destino tomada em cada instância a
partir das sessões que ela mantém. O envelope de RNF-002 — dezenas a centenas de
sessões, alvo de 300 — está muito abaixo do ponto em que a retransmissão total
pesa. Não gera DR próprio: é o detalhamento que ADR-004 explicitamente delegou.

**Impacto na TechSpec:** Seções 5 e 6.

---

## I-06 — Custo das consultas de RF-015 e RF-016 sobre a projeção

**Origina-se de:** RNF-009.

**O que era desconhecido:** se a projeção de intervalos, sem cache, cabe em
p95 ≤ 2 s com 12 meses de histórico e 5.000 tarefas por projeto.

**Pesquisa:** o volume esperado é de ordem de dezenas de milhares de linhas de
intervalo por projeto — cada tarefa produzindo alguns intervalos por etapa
percorrida, mais episódios de reabertura. Agregação com filtro por projeto e por
tipo de série, sobre índice composto `(projeto, etapa, tipo, inicio)`, opera sobre
volume dessa ordem com folga dentro do envelope.

**Decisão:** agregação sob demanda sobre a projeção, **sem** pré-agregação por
período. Não gera DR: é aplicação direta de `backend/java/database.md` §4, e
introduzir um segundo nível de materialização sem evidência de necessidade seria
otimização especulativa.

**Condição de reabertura:** se a medição de RNF-009 reprovar em carga sintética,
pré-agregar por etapa e dia é o próximo passo, e aí sim vira DR.

**Impacto na TechSpec:** Seções 3 e 7.

---

## Incertezas não resolvidas

| # | Incerteza | Por que não foi resolvida | Bloqueante |
| --- | --- | --- | --- |
| I-07 | Comportamento da conexão de escuta `LISTEN/NOTIFY` sob queda prolongada do banco, e se o retry com backoff previsto no ADR-004 basta para não perder evento durante a janela | Só se resolve medindo, com o ambiente de contêineres de pé. A rede de segurança já existe no desenho — resincronização por `seq` na reconexão, prevista no próprio ADR-004 | Não. Vai para a Seção 10 |
| I-08 | Se Playwright e Testcontainers devem subir para a biblioteca compartilhada de guidelines, e sob que forma | É decisão de `/guidelines`, não de `/techspec`. Escrevê-la aqui como norma geral é o que a regra negativa desta skill proíbe | Não. Vai para a Seção 10 |
