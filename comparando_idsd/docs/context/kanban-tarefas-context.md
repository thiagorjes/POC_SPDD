# Contexto — kanban-tarefas

> Artefato de Controle. Monta o contexto mínimo de cada etapa e **registra
> lacuna como evidência em vez de inferir**.
> IDSD 4.6 — **Gate:** GATE-CONTEXT

- **Data:** 2026-09-04
- **Etapa alvo:** `/discovery`

---

## Contexto resolvido

| Item | Fonte | Trecho relevante | Confiança |
| --- | --- | --- | --- |
| Intenção validada pelo demandante | `docs/intent/kanban-tarefas-intent.md` | Resultado esperado, motivação, 16 limites declarados, 10 itens fora de escopo. Validada em 2026-09-04. | Alta |
| Classe e gates da execução | `docs/intent/kanban-tarefas-classification.md` | feature / risco alto / impacto sistema; flow `feature.yaml`; 14 gates. | Alta |
| Dor relatada e sua evidência | `requirements/prd/kanban-tarefas-prd.md` §1 | Acompanhamento por reports manuais em email e chat, cruzados à mão; caso registrado de demanda parada por 5 dias por impedimento não visto. | Alta |
| Discovery anterior, citado pelo PRD | `requirements/discovery/kanban-tarefas-discovery.md` (2026-08-24, facilitador `/discovery`) | Problema, duas personas com frustrações e modo de uso, três objetivos de negócio, hipótese de solução e restrições. É o artefato que a §9 do PRD referenciava. | Alta |
| Natureza da evidência da dor | `requirements/discovery/kanban-tarefas-discovery.md` §1 | "Evidência qualitativa/anedótica"; mensagens de impedimento perdidas "em meio a outros comunicados (ex.: spam de email), gerando cobranças recorrentes entre a equipe"; **"Não há dados quantitativos levantados até o momento."** | Alta |
| Modo de uso da persona principal | `requirements/discovery/kanban-tarefas-discovery.md` §2 | "uso diário e recorrente, com atualizações frequentes e eventualmente simultâneas na mesma tarefa por diferentes participantes". | Alta |
| Público declarado | `requirements/prd/kanban-tarefas-prd.md` §1 e §2 | Devs e liderança técnica; gestores de outros times que precisam de visibilidade sem participar da execução. | Alta — como **declaração do demandante**; não é pesquisa com os próprios usuários |
| Restrições e premissas já assumidas | `requirements/prd/kanban-tarefas-prd.md` §7 | Notificação só interna; sem timesheet; single-tenant; sem dependência entre projetos. | Alta |
| Decisões técnicas já tomadas antes do discovery | `docs/decisions/` (12 DRs: ADR-001 a ADR-008, BDR-001, DDR-001 a DDR-003) | Stack, armazenamento, modelo de RBAC, broadcast de eventos, versionamento de schema, bootstrap de admin, empacotamento, tokens de design, mecânica do board, padrões de feedback. | Alta |
| Normas que governam o sistema | `guidelines.yaml` → `requirements/guidelines/` (`_shared`, `backend/java`, `frontend/nextjs`, `infra/docker`) | 4 coleções vinculadas; `infra/docker` é stub. | Alta |
| Material de design já produzido | `requirements/design/kanban-tarefas-design-brief.md` e `requirements/design/kanban-tarefas/` | Identidade visual, inventário de 8 telas, fluxos, estados por tela, acessibilidade; 9 protótipos HTML navegáveis e `design-tokens.json`. | Alta |
| Questão de design ainda aberta | `requirements/design/kanban-tarefas-design-brief.md` §8 | Densidade do card no board — compacto (TL-03) vs expandido (TL-03b); "decisão a ser validada com o time após revisão do protótipo". | Alta |
| Modo de trabalho da execução | `docs/intent/kanban-tarefas-intent.md`, resposta do demandante em 2026-09-04 | "será feito por IA e o custo é parte da avaliação do processo" — sem prazo e sem orçamento; o custo é métrica observada, não teto. | Alta |
| Efeitos que o demandante recusa | `docs/intent/kanban-tarefas-intent.md` | Não virar instrumento de cobrança individual do dev; gestor não passar a interferir na execução; lead-time mede o processo, não a pessoa. | Alta |

> **Fonte** é caminho de arquivo, URL ou pessoa. O que o agente supõe saber não
> conta: sem fonte apontável, o item é lacuna.

## Lacunas

> A parte mais importante deste artefato. Lacuna registrada é evidência;
> lacuna preenchida por inferência é violação.

| # | O que falta | Quem sabe | Bloqueia qual etapa | Status |
| --- | --- | --- | --- | --- |
| L-01 | O discovery que o PRD cita não existe em disco. §9 do PRD remete a ele três vezes ("ver discovery") para justificar por que os KPIs ficaram qualitativos, e uma varredura do workspace não encontra nenhum artefato de discovery. | Thiago Goncalves Cavalcante (demandante) | — | **Resolvida em 2026-09-04** — o demandante depositou o artefato em `requirements/discovery/kanban-tarefas-discovery.md`. Ele existia fora do workspace; a remissão do PRD não era a trabalho não feito. |
| L-02 | Nenhuma medição do estado atual. A dor é relatada com um caso ilustrativo (5 dias), mas não há quantidade: quantos impedimentos por período, quanto tempo médio se perde, quantas tarefas em curso, quantos projetos. Sem isso não há linha de base contra a qual comparar depois. | Devs e liderança técnica; possivelmente extraível dos canais de email e chat em uso | — | Aberta, não bloqueante — **confirmada pela fonte**: o discovery declara em §1 que "não há dados quantitativos levantados até o momento" |
| L-03 | Quem foi ouvido no discovery, e se `/discovery` pode entrevistar dev e gestor diretamente agora. O artefato traz duas personas com frustrações e modo de uso, mas não registra participantes, método nem data de entrevista — o campo de facilitador é a própria skill. Não é possível distinguir persona levantada com usuários de persona redigida pelo Product Owner. | Thiago Goncalves Cavalcante (demandante) | — | Aberta, não bloqueante sob S-01 |
| L-04 | Como o acompanhamento funciona hoje, em detalhe. O discovery acrescentou dois traços — as mensagens se perdem "em meio a outros comunicados (ex.: spam de email)" e há "cobranças recorrentes entre a equipe" —, mas o ritual segue sem descrição: há reunião diária? quem consolida? o que dispara o aviso de impedimento hoje? | Devs e liderança técnica | — | Aberta, não bloqueante — parcialmente respondida |
| L-05 | Se existe ferramenta de acompanhamento em uso hoje da qual haja dado a migrar. Nenhuma fonte menciona uma; nenhuma fonte nega. | Thiago Goncalves Cavalcante (demandante) | — | Aberta, não bloqueante sob S-03 |
| L-06 | Mapa de domínios do sistema. Registrado como pendência 04 em `memory/state.md`; o eixo Domínio da classificação foi derivado da Intent. | Governança do workspace | — | Aberta, não bloqueante |

- **Lacunas bloqueantes em aberto:** 0
  <!-- Se > 0, a etapa alvo não pode iniciar. -->

Nenhuma lacuna bloqueia `/discovery`. Três das restantes (L-02, L-04, L-05) são
precisamente o material que essa etapa existe para levantar; registrá-las aqui é
o que impede que `/discovery` as trate como já sabidas.

Efeito de cada uma adiante, já que a coluna de bloqueio está vazia por
construção: L-04 é insumo direto do `/discovery`; L-02 empobrece o `/shape` e
deixa o `/evidence` sem número contra o qual comparar; L-03 está coberta por
S-01; L-05 é gatilho de reclassificação se a resposta for positiva; L-06 não
afeta nenhuma etapa desta demanda.

### Nota sobre o discovery recebido

O artefato entregue em 2026-09-04 é o discovery de 2026-08-24, produzido na
stack anterior. Ele resolve L-01 e entra como **insumo** desta execução, não
como substituto da etapa `/discovery` do flow — três razões, todas verificáveis
no próprio arquivo:

- Ele já contém uma **Hipótese de Solução** (§4) que descreve o sistema que o
  PRD viria a especificar. O primeiro diamante pede divergência; um discovery
  que sai com uma solução única não deixa o que convergir no `/shape`.
- Ele apresenta **um único enquadramento** do problema — "falta centralização" —
  sem enquadramentos alternativos descartados.
- Ele **não registra participantes nem método**, o que é a lacuna L-03.

Nada disso o desqualifica: é material real e detalhado, e as personas e o modo
de uso simultâneo são insumo que não existia. A decisão de rodar `/discovery` em
modo ingestão a partir dele, ou de reabrir a divergência, é da etapa seguinte,
não deste artefato.

## Suposições operacionais

> Só para lacunas **não bloqueantes** que a etapa pode atravessar assumindo algo.
> Cada suposição precisa de um dono e de um momento de confirmação.

| # | Suposição | Se estiver errada | Dono | Confirmar até |
| --- | --- | --- | --- | --- |
| S-01 | O demandante responde pelas três perspectivas (dev, liderança técnica, gestor) durante o `/discovery`, e a etapa roda sem entrevista direta com dev e gestor. | O discovery produz enquadramentos derivados de uma só perspectiva — a de quem já formulou a solução. É exatamente o modo de falha que a divergência do primeiro diamante deveria evitar: sai um leque estreito, e `/shape` converge para o que já estava decidido. | Thiago Goncalves Cavalcante | Antes de encerrar `/discovery` |
| S-02 | Os três KPIs permanecem qualitativos, sem meta numérica, conforme o demandante confirmou em 2026-09-04. | Sem linha de base nem meta, não há como afirmar ao final que o problema foi resolvido — só que o software foi entregue. `/evidence` registra entrega, não efeito. | Thiago Goncalves Cavalcante | Antes de `/shape` fechar a métrica de sucesso |
| S-03 | Não há dado histórico a migrar de ferramenta anterior (L-05). | A classificação muda: deixa de ser apenas `feature` e passa a envolver `migracao`, com gates adicionais que retroagem. | Thiago Goncalves Cavalcante | Antes de `/prd` |
| S-04 | O material de design existente (brief, 9 protótipos, tokens) descreve a mesma intenção que está sendo processada agora, e não uma versão anterior dela. | `/design` reaproveitaria protótipo que não corresponde ao que `/shape` e `/solution` decidirem, e a divergência só apareceria na implementação. | Thiago Goncalves Cavalcante | Antes de `/design` |

## Sistemas e artefatos em jogo

| Sistema | Papel nesta execução | Guidelines |
| --- | --- | --- |
| kanban-tarefas — backend | Sistema a construir: API, regras de fluxo, autorização, cálculo de lead-time, propagação de eventos | `requirements/guidelines/backend/java/` |
| kanban-tarefas — frontend | Sistema a construir: board, dashboard, telas de administração | `requirements/guidelines/frontend/nextjs/` + `frontend/_shared/` |
| Provedor de identidade corporativo | Dependência externa consumida, não alterada; autentica os usuários | Fora do escopo de guidelines deste sistema |
| Plataforma de contêineres corporativa | Dependência externa de execução; alvo de empacotamento | `requirements/guidelines/infra/docker/` — **stub**, não elaborada |
| Biblioteca compartilhada de guidelines | Norma que governa a implementação e serve de base ao `/code-review` | `guidelines.yaml` declara 4 coleções |

---

## Fora deste artefato — regras negativas

- **Resposta inventada para uma lacuna.** Se o agente não tem fonte, o campo é
  lacuna. Preencher por plausibilidade é a violação exata que a 4.6 proíbe.
- **Decisão de qualquer tipo.** Este artefato reúne insumo; quem decide é
  `/shape`, `/prd` ou `/techspec`.
- **Requisito, regra de negócio ou cenário** → `/prd`.
- **Resumo do domínio escrito pelo agente sem citar fonte.** Se não dá para
  apontar de onde veio, não entra.
- **Dado real de cliente.** Nunca, em nenhum campo (IDSD 4.10.1). Use
  referência ao registro, não o conteúdo.
