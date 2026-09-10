---
id: SDR-003
type: SDR
status: accepted
date: 2026-09-09
supersedes: —
superseded-by: —
---

# SDR-003 — Testcontainers no lugar de H2, e Playwright para os cenários E2E

## Decisão

Os testes de repositório e de integração sobem **PostgreSQL em contêiner via
Testcontainers**, usando a mesma imagem do `compose` de desenvolvimento, no lugar
do H2 que `backend/java/testing.md` §4 prescreve. Os **8** cenários que o PRD tipa
como `e2e` são verificados com **Playwright**, ferramenta que nenhuma coleção
declara hoje. Toda execução — aplicação, banco, provedor de identidade e suíte —
acontece em contêiner. O serviço de teste recebe o socket Docker do host montado,
para que o Testcontainers possa criar contêineres irmãos — DinD foi descartado
pelo custo de camada e pela perda de cache. A versão da imagem PostgreSQL vem de
variável única, consumida pelo `compose` e pelo Testcontainers.

> **Correção de 2026-09-09 (comitê de análise).** A primeira versão deste DR
> dizia "14 cenários `e2e`". O número errado era da TechSpec, não do PRD. A
> escolha do Playwright não dependia da massa e permanece: SCN-020.1 e 020.3
> exigem duas sessões simultâneas sobre o mesmo board, e SCN-015.2 exige provar
> que uma ação não é oferecida nem aceita.

> **Atualização de 2026-09-09 (emenda do PRD v1.1).** São **9** os cenários
> `e2e`: SCN-001.1, 001.2, 003.1, 014.2, 015.2, **019.4**, 020.1, 020.2 e 020.3.
> SCN-019.4 nasceu do achado INC-06 e é `e2e` pelo mesmo motivo dos demais —
> exige o socket de uma sessão aberto enquanto outra altera a participação.
> O total do PRD passa a 65 cenários: 9 `e2e`, 51 `integração` e 5 `unitário`.
> A segunda emenda (PRD v1.2, achados INC-11 e INC-12) acrescentou SCN-012.4, de
> `integração`, levando o total a **66 cenários: 9 `e2e`, 52 `integração` e 5
> `unitário`**. A terceira emenda (PRD v1.3, criação de projeto) acrescentou
> SCN-022.1 a SCN-022.3, todos de `integração`, levando o total a **69 cenários:
> 9 `e2e`, 55 `integração` e 5 `unitário`**. A quarta emenda (PRD v1.5, achado
> INC-22) acrescentou SCN-002.4, também de `integração`, levando o total a
> **70 cenários: 9 `e2e`, 56 `integração` e 5 `unitário`**. Nenhum cenário `e2e`
> foi acrescentado em nenhuma das quatro emendas, então a justificativa do
> Playwright permanece inalterada.

## Motivação

O desvio de banco do ADR-009 não é neutro para teste. H2 não implementa
`LISTEN/NOTIFY`, e é sobre `LISTEN/NOTIFY` que ADR-004 constrói RF-020, RNF-001 e
RNF-002. Um `@DataJpaTest` verde em H2 diria muito pouco sobre o comportamento que
importa, e diria com aparência de prova.

Quanto ao E2E: a coleção `frontend/nextjs` declara Jest, Testing Library e
Storybook, e nenhuma ferramenta de navegador. SCN-020.1, SCN-020.2 e SCN-020.3
exigem **duas sessões simultâneas** observando o mesmo board; SCN-015.2 exige
verificar que ação de escrita não é apresentada e é recusada por outro caminho.
Nada disso se verifica em jsdom.

**Problema que resolve:**
Impedir que a suíte passe contra um substituto que não tem a característica sob
verificação — o modo mais silencioso de uma suíte verde não significar nada.

**Restrições consideradas:**
- Pedido explícito do demandante, 2026-09-09: rodar tudo em Docker.
- `backend/java/testing.md` §5 já alerta que contexto Spring completo exige o
  provedor de identidade no ar. Com tudo em contêiner, isso deixa de ser um passo
  manual do README e passa a ser o `compose` de teste.
- RNF-006 exige zero violação WCAG 2.1 AA nas 11 telas; Playwright integra
  verificação automatizada de acessibilidade no mesmo percurso do E2E, em vez de
  criar uma segunda suíte.

## Consequências

**Positivas:**
- O teste de repositório passa a exercitar o dialeto, os índices e a semântica de
  intervalo realmente usados em produção.
- O broadcast multi-instância de ADR-004 vira verificável: duas instâncias em
  contêiner, um `NOTIFY`, duas sessões observando.
- RNF-006 e os cenários de acessibilidade são cobertos pelo mesmo percurso.

**Negativas / trade-offs:**
- Suíte mais lenta que H2 em memória, e Docker vira pré-requisito para rodar
  qualquer teste — inclusive na máquina de quem desenvolve.
- Playwright é ferramenta nova no workspace. Se outros sistemas precisarem de E2E,
  esta decisão deveria virar guideline em vez de ficar escondida nesta feature —
  ver Downstream.

**Downstream afetado:**
- TechSpec Seção 7; `quickstart.md`.
- `/guidelines`: proposta de acrescentar E2E à coleção `frontend/nextjs` e de
  registrar Testcontainers como alternativa legítima ao H2 quando o banco de
  produção tiver semântica que o H2 não reproduz. Enquanto isso não acontece,
  ambas as escolhas valem só para este sistema — é o limite que a regra negativa
  do `/techspec` impõe a norma de aplicação geral.
- `/tests`: os cenários tipados `unitário` continuam em Mockito puro, sem
  contêiner, e é isso que mantém o ciclo curto de quem implementa.

## Alternativas Consideradas

### Alternativa 1 — Manter H2 e testar `LISTEN/NOTIFY` só em ambiente integrado
**Descartada porque:** empurra para o gate E2E de épico — que a pendência 01 do
`state.md` registra como aprovação manual por falta de ambiente efêmero — a
verificação do mecanismo central de RF-020. Sem ambiente, a verificação não
aconteceria.

### Alternativa 2 — Cypress no lugar de Playwright
**Descartada porque:** múltiplas sessões simultâneas são desconfortáveis no seu
modelo de execução, e é exatamente o que SCN-020.1 e SCN-020.3 exigem. O teste
sairia artificial ou verificaria menos do que o cenário afirma.

### Alternativa 3 — Rebaixar os cenários `e2e` para integração e componente
**Descartada porque:** os cenários estão congelados desde o GATE-SPEC **com o tipo
de teste declarado**. Rebaixá-los exigiria emenda, e a emenda seria motivada por
conveniência de ferramenta, não por mudança no que precisa ser verdade.
