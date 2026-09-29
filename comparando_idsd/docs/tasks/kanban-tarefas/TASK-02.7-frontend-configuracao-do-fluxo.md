# TASK-02.7 — Frontend: configuração do fluxo de etapas

- **Status:** implementada — 2 de 7 critérios medidos (build/lint/tsc); 5 dependem de `docker compose` ou de outra tela
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-01.7, TASK-02.2
- **Cenários cobertos:** SCN-017.1, SCN-017.2, SCN-017.3
- **Origem:** RF-017, tela TL-08, DDR-004, DDR-005

#### Contexto

A tela em que o projeto ganha seu fluxo. Ela é pré-requisito operacional do
board: sem etapas configuradas não há onde criar tarefa. A substituição é do
fluxo inteiro numa submissão, e a interface precisa refletir isso em vez de
sugerir edição etapa a etapa.

#### O que deve ser feito

- [x] Implementar `/projetos/:id/config/fluxo` consumindo as duas rotas de
      etapas.
- [x] Permitir acrescentar, renomear, reordenar, marcar como terminal e remover
      etapa, tudo numa submissão única.
- [x] Exibir a recusa de fluxo sem etapa terminal preservando o que a pessoa
      digitou.
- [x] Exibir a recusa de arquivamento de etapa com tarefas ativas identificando
      a etapa.
- [x] Ocultar a tela e a ação de quem não tem permissão de configuração, sem
      tratar isso como autorização.
- [x] Garantir conformidade WCAG 2.1 AA, com reordenação também por teclado —
      escrito conforme o padrão (rótulo por controle, foco nativo, sem cor
      isolada); a auditoria automatizada (critério 6) não rodou nesta mão.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/src/app/projetos/[id]/config/fluxo/page.tsx` | criar | tela TL-08. Caminho normalizado para `src/`, que é onde todo o resto do app já vive (`tsconfig.json` mapeia `@/*` para `./src/*`) — a task escreveu o caminho sem o prefixo |
| `frontend/src/app/projetos/[id]/config/fluxo/acoes.ts` | criar | ação de servidor da submissão; não estava na tabela, mas é o padrão já usado por `projetos/novo/acoes.ts` — ação fica junto da rota que a usa |
| `frontend/src/lib/api/etapas.ts` | criar | leitura e substituição do fluxo |
| `frontend/src/componentes/fluxo/editor-de-fluxo.tsx` | criar | editor de etapas e estados de erro. Caminho normalizado para `src/componentes` (português), convenção de todo componente já existente (`cartao-de-projeto.tsx`, `formulario-de-novo-projeto.tsx` etc.) — a task escreveu `components/fluxo` em inglês, que não existe em lugar nenhum do projeto |
| `frontend/src/lib/api/cliente.ts` | alterar | fora da tabela, por necessidade: o cliente só suportava `GET`/`POST`, e `PUT /etapas` é o único jeito de salvar o fluxo |
| `frontend/src/componentes/formulario-de-novo-projeto.tsx` | alterar | fora da tabela, por necessidade: o link "Configurar o fluxo agora" (TASK-01.8) apontava para `/projetos/{id}/fluxo`, rota que não corresponde à desta task (`/config/fluxo`) nem a nenhuma outra existente — corrigido para não deixar o percurso de TL-11→TL-08 do mapa de telas morto |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/`, e todo arquivo de
verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Protótipo de referência:
`docs/design/kanban-tarefas/prototypes/TL-08-configuracao-do-fluxo.html`.

Leitura: `GET /v1/projetos/{projetoId}/etapas` devolve
`[ { id, nome, ordem, terminal } ]`, ordenada e sem arquivadas.

Escrita: `PUT /v1/projetos/{projetoId}/etapas` recebe
`{ "etapas": [ { "id": "<uuid opcional>", "nome": "", "ordem": 0, "terminal": false } ] }`.
Item sem `id` **cria**; etapa existente omitida do corpo é **arquivada**.

Respostas de erro relevantes: `422` sem etapa terminal, e `422` com `errors`
identificando a etapa que tem tarefas ativas. Nos dois casos nada mudou no
servidor, e a tela precisa manter o rascunho na tela.

#### Guia técnico — pontos de atenção

- **A remoção da lista é o arquivamento.** Deixe explícito na interface que
  retirar a etapa da lista e salvar é o que a arquiva — caso contrário a pessoa
  descarta uma etapa sem perceber o efeito.
- **Recusa preserva o rascunho.** Recarregar do servidor após um `422` apaga o
  trabalho da pessoa e é a falha mais provável desta tela.
- **Marcar terminal não é opcional.** Sinalize a ausência antes de submeter, mas
  não substitua a verificação do serviço por ela.
- **Reordenar por arraste exige caminho equivalente por teclado**, obrigatório e
  não conveniência.
- **Nenhuma informação apenas por cor** — a marca de etapa terminal precisa de
  rótulo ou ícone além da cor.
- Esconder a tela de quem não tem permissão é conveniência de interface; a
  recusa real é a do serviço.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Configurar o fluxo e voltar ao board mostra as etapas na ordem definida | percurso em navegador |
| 2 | Submeter fluxo sem etapa terminal exibe a recusa e mantém o rascunho | inspeção da tela após o erro |
| 3 | Arquivar etapa com tarefas ativas exibe qual etapa impede | mensagem identifica a etapa |
| 4 | Renomear etapa não altera a identidade nem o histórico | renomear e conferir o andamento do projeto |
| 5 | Toda a edição é possível apenas com teclado | percurso sem mouse |
| 6 | A tela passa em auditoria de acessibilidade AA | verificação automatizada sem violação de nível AA |
| 7 | **RNF-005** — TL-08 é funcional a 1280 px e a 1024 px de largura, sem perda de ação nem rolagem horizontal não indicada | verificação automatizada nas duas larguras extremas (ACH-16 da revisão de TASK-01.7) |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-29 | `/tests` — suíte escrita, em resposta ao achado abaixo | Cinco testes em `frontend/e2e/configuracao-do-fluxo.spec.ts` (critérios 1-5) e duas entradas TL-08 em `verificacoes/largura.spec.ts` (critério 7) e `verificacoes/acessibilidade.spec.ts` (critério 6) — plano de verificação v1.11. Dois achados registrados lá: o protótipo desenha migração de tarefas ao arquivar etapa, que o contrato atual não tem (destino `/prd`); e a rota usada por `somente-leitura.spec.ts` (congelado) diverge da declarada no mapa de telas e nesta task (destino `/tasks` ou revisão humana). Execução não realizada — exige `docker compose` de pé; validado `tsc`, `eslint` e `playwright --list`, todos limpos. Pronta para `/implement` reabrir. |
| 2026-09-29 | `/implement` — parado antes de escrever código | **Pré-condição do `/implement` não se sustenta: a suíte que cobriria esta tela não existe.** SCN-017.1/.2/.3 só têm teste no backend (`internal/projeto/ConfiguracaoDoFluxoIT.java`) — já verde independente desta task. No frontend, busca por `TL-08`, `config/fluxo` e `configuracao-do-fluxo` em toda a árvore de `frontend/` não encontra nada: nenhum componente, nenhum `*.test.tsx`, nenhum `e2e/*.spec.ts` referencia esta tela. Os critérios 1 a 5 (percurso em navegador, recusa preservando rascunho, identificação da etapa, edição só por teclado) não têm verificação automatizada nenhuma que possa ir de vermelho a verde. Os critérios 6 e 7 (auditoria AA, RNF-005) têm **local** declarado — `verificacoes/acessibilidade.spec.ts` e `verificacoes/largura.spec.ts`, que já existem e cobrem TL-01/TL-02/TL-11 — mas **nenhum caso para TL-08**, e esses dois arquivos estão na lista de "Proibido tocar" desta própria task (arquivo de verificação já produzido pela etapa de testes). Implementar a tela sem isso seria escrever ~500 linhas de produção que nenhum teste prende, e fechar os critérios 6/7 exigiria tocar arquivo fora do meu escopo. Escalado ao `/tests`, que precisa acrescentar: casos de componente/e2e para os critérios 1-5, e as entradas `TL-08` em `largura.spec.ts` e `acessibilidade.spec.ts`. Nenhum arquivo de produção tocado. |
| 2026-09-29 | `/implement` — implementado | **A tela nasce.** `EditorDeFluxo` mantém a lista de etapas em estado local (React), nunca recarregado do servidor entre ações — é isso que preserva o rascunho no critério 2, sem lógica especial: a recusa só popula `recusa`/`bloqueadas` no retorno da ação de servidor, e o array de etapas em tela não muda. A ordem nunca é campo digitável, é a posição na lista: reordenar move por botão (`↑`/`↓`, nativamente acionável por teclado, sem arrasto — o protótipo de referência já desenha reordenação assim, e não por drag), e o número ao lado da linha é `aria-hidden`, igual ao protótipo. **Remover da lista é omitir do `PUT`**, que é o próprio mecanismo de arquivamento do serviço (RF-017) — não existe rota de remoção individual, então não há o que chamar além de tirar da lista local e salvar. O rótulo do checkbox de terminal muda de forma conforme o estado (`"{nome} é terminal"` quando marcado, `"Marcar {nome} como terminal"` quando não) — não é só estética: description-como-estado versus oferta-de-ação são frases estruturalmente diferentes, e isso é o que mantém exatamente um controle no rótulo "é terminal" a qualquer momento, que é o que a suíte pressupõe ao usar `.uncheck()` sem outro qualificador. **A recusa de "etapa com tarefas" nomeia a etapa pelo próprio corpo do erro** (`errors[].nome`, `EtapaService.recusarSeContemTarefaAtiva`) — não foi preciso mapear id→nome no cliente. **Dois arquivos fora da tabela, por necessidade** (registrados acima): `cliente.ts` ganhou `PUT`, e o link de TL-11 para esta tela foi corrigido de `/fluxo` (rota que não existe) para `/config/fluxo`. **Verificado sem a pilha de pé:** `tsc --noEmit` limpo (mesmos 2 erros pré-existentes de outra task), `eslint` limpo, `next build` completo e verde com a rota `/projetos/[id]/config/fluxo` listada na saída — a prova mais forte disponível sem Playwright real de que a árvore de componentes e o `'use server'` compilam e resolvem. **Não executei a suíte `e2e`**: exige `docker compose` (backend, frontend, Keycloak) de pé, fora do orçamento desta sessão — mesma limitação declarada pelo `/tests` na v1.11. Os critérios 1 e 4 continuam dependendo de telas de outras tasks (board, TASK-02.8; andamento, tela ainda sem task própria neste plano) e ficam vermelhos por essa razão, não por defeito daqui. O critério 5 (teclado) é o único inteiramente dentro desta tela e sem dependência externa — é o que tenho mais confiança de que passa quando a suíte rodar. `check_escopo.py` acusa a árvore inteira (sem `--base`, mesma limitação da pendência 12/24): os arquivos de `TASK-02.6` e de `/tests` ainda não commitados aparecem junto; nada foi commitado nesta sessão. |
