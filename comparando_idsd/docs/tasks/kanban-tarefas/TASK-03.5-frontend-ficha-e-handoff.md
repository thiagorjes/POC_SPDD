# TASK-03.5 — Frontend: ficha da tarefa com mover, assumir e devolver

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** G
- **Depende de:** TASK-02.8, TASK-03.2, TASK-03.4
- **Cenários cobertos:** SCN-005.1, SCN-005.3, SCN-007.1, SCN-007.3, SCN-008.1
- **Origem:** RF-005, RF-006, RF-007, RF-008, telas TL-03 e TL-04, DDR-002, DDR-005, DDR-006

#### Contexto

A ficha da tarefa é onde o handoff acontece, e o arraste no board é o caminho
rápido para a mesma operação. É a primeira tela que envia escrita, então é aqui
que o envelope de origem declarada e o tratamento do conflito ganham forma
visível — e é do conflito que depende a confiança no board compartilhado.

#### O que deve ser feito

- [ ] Implementar a ficha da tarefa como painel lateral sobre o board.
- [ ] Exibir as três dimensões em campos separados: etapa, condição de trabalho e
      marca de impedimento.
- [ ] Implementar mover, assumir e devolver, enviando o bloco de origem
      declarada.
- [ ] Implementar o arraste no board com destaque das colunas de destino válidas.
- [ ] Oferecer caminho equivalente por teclado para toda movimentação.
- [ ] Tratar o conflito exibindo o que aconteceu e quem fez, e recarregando o
      board.
- [ ] Garantir conformidade WCAG 2.1 AA.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/components/tarefa/Ficha.tsx` | criar | tela TL-04, painel lateral |
| `frontend/components/board/Arraste.tsx` | criar | arraste com destaque de destino válido |
| `frontend/lib/api/tarefas.ts` | criar | movimentos, tomada e devolução |
| `frontend/components/feedback/Conflito.tsx` | criar | apresentação do estado atual após conflito |
| `frontend/components/board/Cartao.tsx` | alterar | acesso à ficha e ação rápida de assumir |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/`, e todo arquivo de
verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Protótipo de referência:
`docs/design/kanban-tarefas/prototypes/TL-04-detalhe-da-tarefa.html`, que traz as
três dimensões em campos separados na ficha.

Chamadas: `POST /v1/tarefas/{tarefaId}/movimentos` com `{ origem, etapaDestinoId }`;
`POST /v1/tarefas/{tarefaId}/tomada` com `{ origem }`;
`DELETE /v1/tarefas/{tarefaId}/tomada` com `{ origem }` no corpo.

O bloco de origem é montado a partir do cartão em tela:
`{ etapaId, condicao, versao }`.

No `409`, o corpo traz `estadoAtual` com etapa, condição, responsável, instante
da tomada e versão. A tela precisa dizer o que aconteceu e quem fez, e atualizar
o board — não basta informar que houve erro.

Destinos válidos para o arraste: etapa seguinte, etapa anterior e primeira etapa
do fluxo. Etapa não alcançável não recebe destaque e não aceita a soltura.

#### Guia técnico — pontos de atenção

- **O arraste nunca é o único caminho.** O menu de movimentação é obrigação de
  acessibilidade, não conveniência: sem ele a operação central do produto fica
  inacessível.
- **Cartão com impedimento aberto continua movível e assumível.** Desabilitar
  essas ações materializa a recomendação que foi expressamente recusada.
- **As três dimensões ficam em campos separados na ficha.** Colapsá-las de novo
  é o modo de falha conhecido desta tela.
- **Conflito não é erro genérico.** Mostrar apenas "não foi possível" reprova
  cenário congelado.
- **Nenhuma informação apenas por cor**, inclusive o destaque das colunas de
  destino válidas.
- Esconder ação de quem não tem permissão é conveniência; a recusa real é do
  serviço.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Mover um cartão pelo board o leva à etapa de destino e reinicia a contagem exibida | percurso em navegador |
| 2 | A mesma movimentação é possível apenas com teclado | percurso sem mouse |
| 3 | Assumir a partir do cartão registra o responsável e some com a contagem de espera | percurso em navegador |
| 4 | Duas sessões disputando a mesma tarefa: a perdedora vê quem assumiu e o board atualizado | percurso de ponta a ponta com duas sessões |
| 5 | Movimento com estado desatualizado exibe o que aconteceu, não um erro genérico | percurso de ponta a ponta com duas sessões |
| 6 | Devolver deixa o cartão no pool da mesma etapa, com a espera recomeçada | percurso em navegador |
| 7 | A ficha exibe etapa, condição e impedimento em campos distintos | inspeção da tela |
| 8 | As telas passam em auditoria de acessibilidade AA | verificação automatizada sem violação de nível AA |
| 9 | **RNF-005** — TL-04 é funcional a 1280 px e a 1024 px de largura, sem perda de ação nem rolagem horizontal não indicada | verificação automatizada nas duas larguras extremas (ACH-16 da revisão de TASK-01.7) |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
