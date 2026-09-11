# TASK-07.5 — Frontend: minha fila

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-06.3, TASK-07.1
- **Cenários cobertos:** SCN-014.1, SCN-014.3
- **Origem:** RF-014, tela TL-06, DDR-005, DDR-006

#### Contexto

É a tela que responde "o que espera por mim" em todos os projetos, e o suporte
mais direto da hipótese de adesão de que a direção inteira depende. Se ela não
for melhor que varrer três canais de mensagem à mão, o produto não é adotado.

#### O que deve ser feito

- [ ] Implementar `/minha-fila` consumindo a fila entre projetos.
- [ ] Exibir a lista de tarefas aguardando tomada com projeto, etapa e tempo de
      espera.
- [ ] Exibir a lista de impedimentos sob responsabilidade de quem consulta.
- [ ] Oferecer a inversão da ordenação.
- [ ] Exibir a fila vazia com bloco próprio, **sem nenhum tempo** e sem zeros.
- [ ] Oferecer assumir direto da fila, inclusive em item com impedimento aberto.
- [ ] Garantir conformidade WCAG 2.1 AA.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/app/minha-fila/page.tsx` | criar | tela TL-06 |
| `frontend/components/fila/ItemDeFila.tsx` | criar | item com espera e ação de assumir |
| `frontend/lib/api/fila.ts` | criar | consulta com ordenação |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/`, e todo arquivo de
verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Protótipo de referência:
`docs/design/kanban-tarefas/prototypes/TL-06-minha-fila.html`, que já traz o item
de fila com impedimento aberto e a ação de assumir oferecida.

Chamada: `GET /v1/fila?ordem=maiorEspera|menorEspera`, com as duas listas no
corpo. Assumir usa a mesma rota de tomada do board, com o bloco de origem montado
a partir do item.

Fila vazia devolve as duas listas vazias e nenhum tempo. A tela mostra o bloco
próprio, e nunca zero.

#### Guia técnico — pontos de atenção

- **Não exiba zero na fila vazia.** "Ainda não há" e "é zero" são afirmações
  diferentes, e essa é a tela em que a confusão custa mais caro.
- **Item com impedimento aberto continua com assumir oferecido.** Esconder a ação
  deixa a tarefa parada no pool sem ninguém a reclamar.
- **A espera de tomada é estado de primeira classe**, com apresentação própria.
- **A ordenação padrão coloca o mais parado no topo**, que é o ponto da tela.
- **Nenhuma informação apenas por cor**, inclusive a marca de impedimento no item.
- A ordenação da fila ficou deliberadamente em aberto para observação com o time;
  entregue o padrão e a inversão, e não invente uma terceira ordem.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | A fila reúne tarefas de todos os projetos, com a mais parada no topo | percurso com tarefas em dois projetos |
| 2 | A inversão da ordenação funciona na tela | interação com o controle de ordem |
| 3 | Fila vazia exibe o bloco próprio, sem nenhum tempo e sem zeros | percurso com usuário sem tarefas |
| 4 | Assumir direto da fila registra o responsável e retira o item | percurso em navegador |
| 5 | Item com impedimento aberto oferece assumir | inspeção do item |
| 6 | A tela passa em auditoria de acessibilidade AA | verificação automatizada sem violação de nível AA |
| 7 | Nenhuma informação é transmitida apenas por cor | inspeção em modo monocromático |
| 8 | **RNF-005** — TL-06 é funcional a 1280 px e a 1024 px de largura, sem perda de ação nem rolagem horizontal não indicada | verificação automatizada nas duas larguras extremas (ACH-16 da revisão de TASK-01.7) |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
