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

---

## Gates atravessados

| Demanda | Gate | Data | Quem aprovou |
| --- | --- | --- | --- |

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

---

## Histórico

| Data | Mudança |
| --- | --- |
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
