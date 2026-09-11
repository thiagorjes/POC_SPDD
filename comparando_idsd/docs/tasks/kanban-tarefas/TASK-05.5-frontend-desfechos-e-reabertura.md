# TASK-05.5 — Frontend: desfechos na ficha e diálogo de reabertura

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-04.4, TASK-05.4
- **Cenários cobertos:** SCN-011.1, SCN-011.3, SCN-012.2, SCN-013.1
- **Origem:** RF-011, RF-012, RF-013, telas TL-04 e TL-10, DDR-005, DDR-007

#### Contexto

A ficha ganha as duas saídas do fluxo e o diálogo de reabertura, que é a única
ação do produto visível para uma só pessoa. É também a tela em que a marca de
impedimento passa a desabilitar controles — e só os dois que o contrato de fato
recusa.

#### O que deve ser feito

- [ ] Exibir e habilitar concluir e encerrar sem conclusão conforme o estado da
      tarefa.
- [ ] Desabilitar **as duas** quando houver impedimento aberto, com justificativa
      visível e a mesma razão.
- [ ] Implementar o diálogo de reabertura, visível apenas para o Product Owner.
- [ ] Exibir a tarefa concluída e a encerrada com apresentação distinta e sem
      contagens correndo.
- [ ] Tratar a recusa por permissão exibindo a razão, sem sugerir caminho
      alternativo.
- [ ] Garantir conformidade WCAG 2.1 AA.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/components/tarefa/Desfechos.tsx` | criar | concluir e encerrar, com as recusas |
| `frontend/components/tarefa/DialogoReabertura.tsx` | criar | tela TL-10, sobre a ficha |
| `frontend/lib/api/desfechos.ts` | criar | conclusão, encerramento e reabertura |
| `frontend/components/tarefa/Ficha.tsx` | alterar | integra os desfechos |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/`, e todo arquivo de
verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Protótipos de referência:
`docs/design/kanban-tarefas/prototypes/TL-04-detalhe-da-tarefa.html` e
`TL-10-reabrir-tarefa-concluida.html`.

Chamadas: `POST /v1/tarefas/{tarefaId}/conclusao` com `{ origem }`;
`POST /v1/tarefas/{tarefaId}/encerramento` com `{ origem, motivo }`;
`POST /v1/tarefas/{tarefaId}/reabertura` com `{ origem, motivo }`.

Com impedimento aberto, **as duas** ações de saída ficam desabilitadas, com a
mesma justificativa: registrar o desfecho do impedimento antes. Sair do fluxo em
dois passos é deliberado.

O diálogo de reabertura só aparece em tarefa concluída e só para quem tem o papel
de Product Owner. Esconder é conveniência de interface; a recusa real é do
serviço.

#### Guia técnico — pontos de atenção

- **Não deixe encerrar habilitado com impedimento aberto.** Oferecer o que o
  serviço recusa entrega um controle que responde erro — pior que restringir
  demais.
- **A justificativa precisa estar visível junto do controle**, e não só na
  mensagem de erro depois do clique.
- **Concluída e encerrada são estados diferentes** e precisam de apresentação
  distinta; nenhuma contagem segue correndo em qualquer das duas.
- **Nenhuma informação apenas por cor**, inclusive a distinção entre os dois
  desfechos.
- **Controle desabilitado precisa continuar perceptível e explicado** para
  tecnologia assistiva.
- **A reabertura devolve a tarefa à primeira etapa** — a tela precisa dizer isso
  antes de confirmar, porque é efeito não óbvio.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Mover o cartão para a etapa terminal conclui a tarefa e a exibe como concluída | percurso em navegador |
| 2 | Com impedimento aberto, concluir e encerrar estão desabilitados com a mesma justificativa visível | inspeção da ficha |
| 3 | Participante comum não consegue encerrar e vê a razão | percurso com papel sem permissão |
| 4 | O diálogo de reabertura só aparece para o Product Owner em tarefa concluída | comparação entre papéis |
| 5 | Reabrir devolve a tarefa à primeira etapa e a tela avisa disso antes de confirmar | percurso em navegador |
| 6 | Nenhuma contagem segue correndo em tarefa concluída ou encerrada | inspeção do cartão |
| 7 | As telas passam em auditoria de acessibilidade AA, com os controles desabilitados explicados | verificação automatizada e leitura por tecnologia assistiva |
| 8 | **RNF-005** — TL-10 é funcional a 1280 px e a 1024 px de largura, sem perda de ação nem rolagem horizontal não indicada | verificação automatizada nas duas larguras extremas (ACH-16 da revisão de TASK-01.7) |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
