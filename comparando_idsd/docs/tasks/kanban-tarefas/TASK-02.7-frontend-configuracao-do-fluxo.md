# TASK-02.7 — Frontend: configuração do fluxo de etapas

- **Status:** pendente
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

- [ ] Implementar `/projetos/:id/config/fluxo` consumindo as duas rotas de
      etapas.
- [ ] Permitir acrescentar, renomear, reordenar, marcar como terminal e remover
      etapa, tudo numa submissão única.
- [ ] Exibir a recusa de fluxo sem etapa terminal preservando o que a pessoa
      digitou.
- [ ] Exibir a recusa de arquivamento de etapa com tarefas ativas identificando
      a etapa.
- [ ] Ocultar a tela e a ação de quem não tem permissão de configuração, sem
      tratar isso como autorização.
- [ ] Garantir conformidade WCAG 2.1 AA, com reordenação também por teclado.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/app/projetos/[id]/config/fluxo/page.tsx` | criar | tela TL-08 |
| `frontend/lib/api/etapas.ts` | criar | leitura e substituição do fluxo |
| `frontend/components/fluxo/` | criar | editor de etapas e estados de erro |

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

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
