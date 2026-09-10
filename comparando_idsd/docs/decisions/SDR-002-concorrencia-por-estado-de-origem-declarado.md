---
id: SDR-002
type: SDR
status: accepted
date: 2026-09-09
supersedes: —
superseded-by: —
---

# SDR-002 — Concorrência e idempotência por estado de origem declarado, sem chave de idempotência

## Decisão

Toda escrita sobre uma tarefa carrega no corpo o **estado de origem que o
solicitante acredita ser o corrente** — etapa e condição —, conforme RN-013. O
serviço reavalia esse estado contra o corrente dentro da transação, sob bloqueio
otimista da linha da tarefa. Origem divergente devolve `409 Conflict` com o estado
corrente no corpo do `problem+json`; origem coincidente com o efeito já aplicado é
**absorvida sem novo evento** e devolve `200`, satisfazendo RN-031 sem header
`Idempotency-Key`.

## Motivação

RN-012 manda a primeira ação prevalecer e a segunda ser recusada com o estado
corrente devolvido a quem perdeu. RN-013 manda toda transição declarar de onde
parte. RN-031 manda toda escrita ser idempotente. As três descrevem o mesmo
mecanismo visto de ângulos diferentes, e implementá-las separadamente produziria
três controles sobrepostos.

**Problema que resolve:**
Evitar que idempotência e controle de concorrência virem duas trilhas
independentes — uma por header, outra por versão —, que é a origem clássica de
comportamento inconsistente entre endpoints.

**Restrições consideradas:**
- `_shared/api-standards.md` §4 oferece `Idempotency-Key` como opção para POST
  reenviável. Aqui ela é dispensável: o estado de origem declarado já identifica
  a solicitação de forma única e semanticamente mais forte, porque também detecta
  o caso em que outra pessoa mudou o estado no intervalo.
- `409 Conflict` é o código que `api-standards.md` reserva para versão otimista
  divergente. SCN-005.3, SCN-007.3 e SCN-020.3 exigem que o corpo traga o estado
  atual, não uma mensagem genérica.
- RNF-004 exige que nenhuma escrita se complete com verificação apenas do cliente.
  A reavaliação acontece no serviço, sobre dado do banco.

## Consequências

**Positivas:**
- Um único mecanismo cobre RN-012, RN-013 e RN-031, e o mesmo teste o verifica nos
  três ângulos.
- Quem perde a corrida recebe informação útil — quem assumiu, em que etapa está —
  em vez de um erro. É o que SCN-020.3 pede e o que torna a recusa compreensível.

**Negativas / trade-offs:**
- Todo contrato de escrita ganha campos de origem, o que torna o corpo mais
  verboso e obriga o cliente a manter o estado que leu.
- Cliente que perdeu o estado precisa reler antes de agir. É custo real, e é
  também o que impede escrita às cegas.

**Downstream afetado:**
- TechSpec Seções 4 e 5; todos os contratos de escrita em `contracts/`.
- `/tests`: o cenário de recusa concorrente precisa de duas sessões reais, o que
  se reflete na escolha de ferramenta de E2E (SDR-003).

## Alternativas Consideradas

### Alternativa 1 — `Idempotency-Key` no header, com versão otimista à parte
**Descartada porque:** duas trilhas para o mesmo problema. A chave resolve reenvio
do mesmo cliente, mas não diz nada sobre outra pessoa ter mudado o estado — que é
o caso de RN-012 e o mais frequente na operação real do board.

### Alternativa 2 — Bloqueio pessimista da tarefa durante a escrita
**Descartada porque:** transação longa segurando linha em board colaborativo, o
oposto do que `backend/java/database.md` §5 prescreve, e sem ganho: a taxa de
conflito real é baixa e o custo do conflito, quando ocorre, é uma releitura.
