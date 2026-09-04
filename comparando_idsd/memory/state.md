# Estado Operacional — IDSD
_Atualizado em: 2026-09-03_

> Estado atual do sistema e das demandas em andamento.
> Para princípios estáveis e Decision Records, veja [constitution.md](constitution.md).

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
| kanban-tarefas | `feature` | feature / risco alto / impacto sistema | `/shape` | GATE-DIRECAO atravessado — direção E-01+E-02 aprovada, 5 alternativas descartadas, 5 hipóteses a validar. Próximo: `/solution` |

---

## Gates atravessados

| Demanda | Gate | Data | Quem aprovou |
| --- | --- | --- | --- |
| kanban-tarefas | GATE-INTENT | 2026-09-04 | Thiago Goncalves Cavalcante (demandante) |
| kanban-tarefas | GATE-CLASSIFY | 2026-09-04 | agente (sem override) |
| kanban-tarefas | GATE-CONTEXT | 2026-09-04 | agente — 0 lacunas bloqueantes |
| kanban-tarefas | GATE-DIRECAO | 2026-09-04 | Thiago Cavalcante (aprovador) — sem ressalvas |

---

## Devoluções

> Contadores do anti-ping-pong. Ver o orçamento vigente na policy.

| Demanda | Por par | Por diamante | Total |
| --- | --- | --- | --- |

---

## Pendências conhecidas

> O que está verde hoje é validação estrutural. Nenhum flow foi executado ponta
> a ponta ainda.

| # | Pendência | Efeito no teste |
| --- | --- | --- |
| 01 | Ambiente integrado efêmero para o gate E2E de épico multi-sistema. | Enquanto não existir, esse gate é aprovação manual. |
| 02 | `infra/docker` declarada no `guidelines.yaml` como stub. | Rede, volumes, healthcheck, segredo, registry e política de tag sem norma. Containerização hoje só é coberta pelo `definition-of-done.md` de cada stack. |
| 03 | `frontend/react` é stub na biblioteca; não governa este sistema. | Nenhum, enquanto nenhum sistema declarar a coleção. |
| 04 | Não existe mapa de domínios do sistema, embora o template de classificação o pressuponha. | O eixo Domínio de `kanban-tarefas` foi derivado da Intent e é provisório; reconciliar quando o mapa existir. |

---

## Histórico

| Data | Mudança |
| --- | --- |
| 2026-09-04 | `/shape` executada e GATE-DIRECAO atravessado por Thiago Cavalcante, sem ressalvas. Direção: E-01+E-02 em conjunto — registrar o estado e avisar o próximo responsável são a mesma ação. E-01 puro foi descartado por não cobrir o atraso de handoff que o demandante apontou como o efeito mais caro (C-04), apesar de ser o enquadramento sobre o qual os 12 DRs e os 9 protótipos foram construídos; a incorporação de E-02 os preserva. As três contradições da Intent foram resolvidas pelo demandante: C-01 mantém a notificação interna rígida com a consequência assumida, e a adesão virou a hipótese H-01, de que a direção inteira depende; C-02 e C-03 viraram **restrição estrutural** — tempo por etapa nunca agregado por pessoa, gestor externo somente-leitura —, e não mais acordo de uso. Métrica sem linha de base por decisão: o primeiro período de uso a produz, nenhuma instrumentação existe e ela vira trabalho no `/tasks`. O demandante questionou se o restante dos requisitos seria atacado; nada saiu do escopo, mas a revisão expôs que raias e painel agregado estavam implícitos e que o envelope de RNF não tinha ponteiro — ambos corrigidos no brief. 5 alternativas descartadas com condição de reabertura, nenhuma adiada. |
| 2026-09-04 | `/discovery` executada para `kanban-tarefas`, com entrevista ao demandante (S-01 confirmada: ele responde pelas três perspectivas, consultando gestor e time; sem entrevista direta com dev e gestor — L-03 segue aberta). Dois achados que nenhuma fonte anterior trazia: os canais em uso são três (chat, e-mail e WhatsApp) e o PO consolida tudo à mão em planilha depois de varrê-los. O atraso relatado está nos handoffs — "atraso em review, validação etc., tudo começa tarde" —, não na ausência de registro central, o que contradiz o enquadramento único do PRD (C-04). Cinco enquadramentos registrados; o do PRD e do material de design é apenas um deles, o que permite ao `/shape` escolhê-lo por decisão em vez de inércia. Cinco contradições levadas ao `/shape`, das quais duas nascem dentro da própria Intent: notificação só interna contra uma equipe que vive nos canais externos (C-01), e visibilidade ao gestor contra a recusa de que ele interfira (C-02). |
| 2026-09-04 | `/context revisar`: o demandante depositou o discovery de 2026-08-24 em `requirements/discovery/kanban-tarefas-discovery.md`. L-01 resolvida — o artefato existia fora do workspace. Três itens novos no contexto resolvido, incluindo o uso simultâneo da mesma tarefa por participantes diferentes, que nenhuma outra fonte trazia. L-02 deixou de ser inferência e passou a ter confirmação literal: o discovery declara que não há dados quantitativos. L-03 foi reescrita — o artefato não registra participantes nem método, então não se distingue persona levantada com usuário de persona redigida pelo PO. Registrado que o discovery já sai com hipótese de solução e enquadramento único, o que o torna insumo e não substituto da etapa. |
| 2026-09-04 | `/context` executada para a etapa `/discovery`: 11 itens resolvidos com fonte em disco, 6 lacunas registradas, nenhuma bloqueante, 4 suposições operacionais com dono e prazo. Achado relevante: o discovery que o PRD cita três vezes em §9 não existe no workspace, e não há nenhuma medição do estado atual — a dor tem um caso ilustrativo, não uma linha de base. Duas correções no artefato foram necessárias para o validador: a nota do próprio template contém a expressão que o filtro de fonte inválida barra, e `check_lacunas.py` conta como bloqueante toda lacuna cuja coluna de bloqueio não esteja literalmente vazia — prosa explicativa ali reprova. |
| 2026-09-04 | `/classify` executada para `kanban-tarefas`: feature / risco alto / impacto sistema, flow `feature.yaml`, 14 gates herdados (4 do núcleo + 10 da classe). Risco alto por dois sinais explícitos na Intent — autorização configurável em runtime e autenticação federada. Impacto mantido em `sistema` após consideração: as dependências externas são consumidas, não alteradas, e elevar o eixo acionaria o gate E2E multi-sistema sem um segundo sistema a verificar. Sem override e sem waiver. Registrada a pendência 04 — o eixo Domínio foi derivado da Intent porque não existe mapa de domínios. |
| 2026-09-04 | `/intent` executada para `kanban-tarefas`, em modo ingestão a pedido do demandante: capturada de `requirements/prd/kanban-tarefas-prd.md` v1.0 e do design brief, sem entrevista de abertura. Quatro lacunas que as fontes não cobriam foram fechadas com o demandante — ausência de prazo e orçamento (custo é métrica do processo, não teto), dois efeitos colaterais indesejados, manutenção dos KPIs qualitativos e vigência do PRD apesar do `Status: Draft`. Três termos foram reescritos por regra negativa do artefato (nome de produto não entra na Intent). GATE-INTENT atravessado. |
| 2026-09-04 | `/guidelines` executada em modo ingestão. Biblioteca compartilhada assumida em `requirements/guidelines` (já organizada por stack); criados o índice `README.md` que faltava, o stub `infra/docker` e o `guidelines.yaml` do sistema. A coleção plana do CRUDAO que ocupava a raiz da biblioteca foi movida para `requirements/_legado/guidelines-crudao/` — duplicava `_shared` + `backend/java` + `frontend/nextjs` em versão mais pobre, e um dos arquivos contradizia ADR-006. Dois achados dela viraram norma: introspecção JavaBeans em campo `eFinal` e resolução eager do issuer OIDC na subida do contexto Spring. Os 12 DRs de `requirements/decisions/` foram copiados para `docs/decisions/` com caixa normalizada e indexados na constitution. `check_guidelines.py --sistema .`: 4 coleções conformes, 3 avisos, nenhuma falha. |
| 2026-09-03 | Sistema inicializado via `init.py` |
| 2026-09-04 | `validate.py` deixou de exigir invocação a partir da raiz: o script do custom step resolve contra `SISTEMA_RAIZ`, os argumentos continuam relativos ao cwd. Sem isso a fixture `demo-techspec.md` era impossível de rodar de qualquer diretório. Verificada nos dois sentidos: cobertura completa exit 0, RF faltando na matriz exit 1. |
| 2026-09-04 | Detecção de artefato desatualizado deixou de depender do Artifact Registry (que esta stack não tem) e passou a ser derivada do disco: `stale_check: "mtime"` nos 15 rules com etapa anterior, `check_stale_mtime` no `validate.py`, saída como AVISO. `state.md` e `constitution.md` ficam fora da comparação. Fixtures órfãs `valid_techspec.md`/`invalid_techspec.md` removidas. |
| 2026-09-04 | `/design` migrada com diff mínimo: só a Fase 6 (canvas) saiu, as demais renumeraram, Artifact Registry virou Demandas ativas. Método de trabalho (Fase 0 de detecção, entrevista condicional, prototipador obrigatório) preservado integralmente. |
| 2026-09-04 | `/techspec`: recuperadas três perdas da reescrita — check de `stale` no PRD na Fase 0, perguntas literais dos blocos A/B/C, salvamento por seção e `check_rf_coverage` inline ao fechar a Seção 9. |
| 2026-09-03 | `/techspec` migrada para o contrato novo: canvas removido das saídas, Artifact Registry trocado pelas tabelas do `state.md`, limite "como, nunca o quê" com devolução ao `/prd`, âncoras de seção declaradas como contrato do `derive_canvas.py`, bloco de regras negativas no template, `check_rf_coverage` deixou de passar em silêncio com caminho errado. |
| 2026-09-03 | `check_guidelines.py`: separada falha (invariante de disco) de aviso (cheiro na prosa). Os checks de checklist e hedge eram falsos positivos contra a biblioteca real — o de checklist saiu, o de hedge virou aviso. |
| 2026-09-03 | `/guidelines` migrada para o modelo de biblioteca compartilhada por stack; `guidelines.yaml` passa a ser o vínculo; `techspec`, `tests`, `design` e os templates de review/verificação atualizados; `derive_canvas.normas()` lê o vínculo em vez de varrer diretório. |
| 2026-09-03 | 17 skills e 6 flows registrados; `techspec` e `design` migradas para o contrato novo no frontmatter; isenção `LEGADO` esvaziada. |
