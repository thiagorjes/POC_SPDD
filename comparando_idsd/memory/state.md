# Estado Operacional — IDSD
_Atualizado em: 2026-09-14_

> Estado atual do sistema e das demandas em andamento.
> Para princípios estáveis e Decision Records, veja [constitution.md](constitution.md).
> Para o registro integral de gates e o histórico narrativo, veja
> [historico.md](historico.md) — arquivo de consulta, não carregado por padrão.

---

## Sistema

| Campo | Valor |
| --- | --- |
| Nome | IDSD |
| Cenário | Novo (greenfield) |
| Guidelines | vinculadas — `guidelines.yaml` → `requirements/guidelines` (`_shared`, `backend/java`, `frontend/nextjs`, `infra/docker`) |
| Inicializado em | 2026-09-03 |

Os flows disponíveis não são listados aqui: eles vivem em `governance/flows/`,
e essa é a única fonte. Copiar a cadeia de etapas para cá cria uma segunda
versão que envelhece em silêncio — foi o que aconteceu na stack anterior.

Regenerar os derivados: `python .agents/scripts/init.py`.
Verificar sincronia: `python .agents/scripts/check_drift.py`.

---

## Demandas ativas

| Demanda | Flow | Classe | Etapa atual | Estado |
| --- | --- | --- | --- | --- |
| kanban-tarefas | `feature` | feature / risco alto / impacto sistema | `/code-review TASK-02.2` — depois TASK-02.3 | Cadeia liberada: PRD v1.5, TechSpec v1.11, plano de verificação v1.2, 8 épicos / 43 tasks, 70/70 cenários congelados cobertos. **Nenhum achado de revisão em aberto.** |

**Escopo congelado.** 70 cenários — 9 `e2e`, 56 `integração`, 5 `unitário` —,
mais 14 verificações além dos cenários e 4 desvios declarados.

**Progresso.** EPIC-01 fechado (TASK-01.1 a 01.8; a ordem executada não foi a
numérica, porque 01.7 depende de 01.8). EPIC-02 aberto com TASK-02.1 concluída
e revisada, e TASK-02.2 concluída e ainda não revisada.

**Suíte.** 168 testes, 70 verdes / 98 vermelhos. Os vermelhos são a linha de
base — cenários de tasks ainda não implementadas —, e a suíte **não compila** na
árvore de trabalho, porque os cenários unitários nomeiam classes de tasks
posteriores; a medição exige excluí-los em cópia descartável. São 4 os arquivos
que restam nessa condição, um a menos desde que TASK-02.2 criou `EtapaService`,
`FluxoRequisicao` e `RegraDeNegocioViolada`.

**4 dos 5 testes de `ConfiguracaoDoFluxoIT` seguem vermelhos por dependência e
não por defeito:** eles exercitam o board e a criação de tarefa, que nascem em
TASK-02.6 e TASK-02.5. O fluxo de etapas só fica verificável de ponta a ponta
quando o épico fechar — é a mesma razão que mantém GATE-NFR reprovado.

**Pendência de spec viva:** Q-012 — decisão sobre claim ausente exige migration
nova e um recuo no autoprovisionamento fora do plano vigente. Dono `/tasks`,
não bloqueante.

---

## Gates atravessados

> Uma linha por gate, com o veredito final. O detalhe de cada achado está em
> [historico.md](historico.md); os relatórios de revisão, em `docs/review/`.

| Demanda | Gate | Data | Veredito |
| --- | --- | --- | --- |
| kanban-tarefas | GATE-INTENT | 2026-09-04 | aprovado — Thiago Goncalves Cavalcante (demandante) |
| kanban-tarefas | GATE-CLASSIFY | 2026-09-04 | aprovado — agente, sem override |
| kanban-tarefas | GATE-CONTEXT | 2026-09-04 | aprovado — 0 lacunas bloqueantes |
| kanban-tarefas | GATE-DIRECAO | 2026-09-04 | aprovado — Thiago Cavalcante, sem ressalvas |
| kanban-tarefas | GATE-SPEC | 2026-09-09 | aprovado — 4 reconfirmações de emenda até a v1.5 (2026-09-10); H-03 aberta por decisão da etapa |
| kanban-tarefas | GATE-PROVENIENCIA | 2026-09-09 | aprovado — 1 inferência (RNF-009) confirmada pelo demandante |
| kanban-tarefas | GATE-CONSISTENCIA | 2026-09-10 | **aprovado** na 9ª execução (`--pre-implement`) — 0 achados, 0 waiver. As 8 anteriores reprovaram; os 22 achados estão fechados |
| kanban-tarefas | GATE-RASTREABILIDADE | 2026-09-09 | aprovado — invariante cenário↔épico fechada nas duas direções |
| kanban-tarefas | GATE-VERIFICACAO-INDEPENDENTE | 2026-09-10 | aprovado — 0 arquivos de produção lidos ou escritos |
| kanban-tarefas | GATE-GHERKIN-CONGELADO | 2026-09-10 | aprovado — 70/70 cobertos, 0 cenários alterados desde o gate de spec |
| kanban-tarefas | GATE-REVISAO-TECNICA | parcial | **reprovado** enquanto o épico não fecha. 8 revisões parciais (TASK-01.1 a 01.8, 02.1), 94 achados, 21 bloqueantes — **todos fechados** |
| kanban-tarefas | GATE-NFR | parcial | **reprovado** — os envelopes só são mensuráveis no fechamento de épico. RNF-004 é o único já medido dentro |

---

## Devoluções

> Contadores do anti-ping-pong. Ver o orçamento vigente na policy.

| Demanda | Por par | Por diamante | Total |
| --- | --- | --- | --- |

---

## Pendências conhecidas

| # | Pendência | Efeito |
| --- | --- | --- |
| 01 | Ambiente integrado efêmero para o gate E2E de épico multi-sistema. | Enquanto não existir, esse gate é aprovação manual. |
| 03 | `frontend/react` é stub na biblioteca; não governa este sistema. | Nenhum, enquanto nenhum sistema declarar a coleção. |
| 04 | Não existe mapa de domínios do sistema, embora o template de classificação o pressuponha. | O eixo Domínio de `kanban-tarefas` é provisório; reconciliar quando o mapa existir. |
| 05 | `.agents/skills/design/SKILL.md` (Fase 3) prescreve seções diferentes das que `validate-rules.json` exige. | Todo Design Brief escrito conforme a skill reprova no validador. Corrigir a fonte — provavelmente a lista da SKILL.md. |
| 06 | `check_proveniencia.py` reprova dúvida cujo Status não **comece** literalmente com `resolv`/`fechad`; negrito quebra. | Marcação em negrito bloqueia o GATE-SPEC sem que a causa apareça na mensagem. |
| 11 | `check_task_files` exige ao menos um item `- [ ]`, de modo que **task concluída sempre reprova**. | O validador do plano de tasks nunca mais sai em exit 0, e o sinal deixa de distinguir defeito de progresso. |
| 12 | `check_escopo.py` compara `git diff --name-only` com a tabela de arquivos, e a raiz git é `SPDD_puro` — todo caminho volta prefixado por `comparando_idsd/`. Dono: ferramenta. | A pré-condição do `/code-review` é inverificável. (A metade de notação, o marcador `<pkg>`, foi resolvida em 2026-09-11.) |
| 18 | `check_escopo.py` imprime `ERRO:` para cada arquivo fora do escopo e **sai em exit 0**. Descoberto em TASK-02.2. Dono: ferramenta. | O validador não reprova nada. Somado à pendência 12, que o faz acusar todo arquivo, o sinal é ruído que sai verde — a pior combinação: quem automatizar o gate por código de saída nunca verá defeito algum. |
| 14 | SCN-002.4 é cenário congelado sem teste; `check_independencia` reprova. Não é defeito do validador. **Não fechou em TASK-02.2, como se previa.** A task entregou o lado de produção — `fluxoConfigurado` é emitido, derivado por `EXISTS` na mesma consulta —, mas o que falta é o **teste**, e escrevê-lo é do `/tests`. Dono corrigido: `/tests`. | O plano de verificação não sai em exit 0. |
| 16 | Em `docker/compose.yaml`, `PORTA_IDENTIDADE` e `IDENTIDADE_HOSTNAME` são independentes: porta deslocada sem hostname deslocado manda o navegador para outra stack da máquina. Dono `/tasks`/infra. | O sintoma aparece a um serviço de distância da causa. Custou uma sessão de diagnóstico em 2026-09-11. |
| 17 | O validador da TechSpec lê a menção a `TASK-01.6` em `kanban-tarefas-techspec.md:389` como planejamento de task. É rastreabilidade, não plano. Dono: ferramenta. | Toda revisão da TechSpec sai com um erro que não corresponde a defeito. |

**Resolvidas** (registro em [historico.md](historico.md)): 02 coleção
`infra/docker` elaborada · 07 `check_gherkin` em bloco cercado · 08 propagação
na TechSpec · 09 rota de criação de projeto · 10 `{{FEATURE}}` com sufixo
`-verificacao` · 13 `---` lido como patch no `/code-review` · 15 RNF-005 sem
dono.

---

## Histórico

> Registro integral, com as razões de cada decisão, em
> [historico.md](historico.md). Abaixo, só a linha do tempo.

| Data | Mudança |
| --- | --- |
| 2026-09-14 | `/implement TASK-02.2` — consulta e substituição do fluxo de etapas. A substituição é atômica e roda em **dois passos**, porque `etapa_projeto_ordem_unico` é único parcial e não é adiável: o passo intermediário desloca as ordens vigentes antes de reatribuí-las, exatamente como ACH-07 da revisão de 02.1 previu na migration. `fluxoConfigurado` passa a ser emitido em `GET /v1/projetos`, derivado por `EXISTS` na mesma consulta, e `AusenciaDeNMaisUmIT` segue verde. Critério 10 devolve à tela de criação a frase que ACH-07 de 01.7 retirou, e `fluxoConfigurado` deixa de ser opcional no tipo do cliente. 168 testes, 70/98 — nenhuma regressão. Descoberta a pendência 18 |
| 2026-09-14 | Os 14 achados da revisão de TASK-02.1 fechados. Destaques: TechSpec v1.11 decide a garantia **estreita** da raia (`tarefa.raia_id` existe; o que nenhuma tabela carrega é raia na série de tempo); repositórios trocam `JpaRepository` por `Repository`, tornando a ausência de remoção física propriedade da interface; migration nova em vez de editar a aplicada, para não quebrar o checksum do Flyway; `EsquemaDoFluxoIT` dá ao esquema a cobertura que `ddl-auto=validate` não alcança, com poder de falha provado. 163 testes, 64/99 — nenhuma regressão |
| 2026-09-14 | `/code-review TASK-02.1` — 14 achados, 4 bloqueantes. Todos na distância entre o que os arquivos afirmam sobre si e o que garantem |
| 2026-09-11 | `/implement TASK-02.1` — EPIC-02 aberto: esquema de `etapa` e `raia`, entidades e repositórios |
| 2026-09-11 | EPIC-01 fechado. Revisões de TASK-01.5 a 01.8 conduzidas e todos os achados corrigidos, inclusive os 6 bloqueantes de TASK-01.7 (redirecionador aberto, CVEs de produção, cabeçalhos de segurança) e os 2 de TASK-01.8 (escrita antes do `403`, transação única sem poder de falha) |
| 2026-09-10 | `/implement` de TASK-01.1 a 01.6 e as respectivas revisões; ambiente Docker, segurança, sessão autenticada, RBAC e tratamento de erro em pé |
| 2026-09-10 | `/tests` — suíte congelada v1.0; `/analyze --pre-implement` aprova o GATE-CONSISTENCIA na 9ª execução |
| 2026-09-09 | `/prd` → `/techspec` → `/analyze` → `/tasks`: PRD congelado no GATE-SPEC, TechSpec com 4 SDRs, 8 épicos e 43 tasks. Coleção `infra/docker` elaborada (ADR-011/012/013) |
| 2026-09-04 | `/intent` → `/classify` → `/context` → `/discovery` → `/shape` → `/solution` → `/design`: demanda `kanban-tarefas` percorre a camada de Controle e o primeiro diamante |
| 2026-09-03 | Sistema inicializado via `init.py`; skills e flows migrados para o contrato novo |
