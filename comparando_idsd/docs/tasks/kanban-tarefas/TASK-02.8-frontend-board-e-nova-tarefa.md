# TASK-02.8 — Frontend: board e criação de tarefa

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** G
- **Depende de:** TASK-02.6, TASK-02.7
- **Cenários cobertos:** SCN-003.1, SCN-003.2, SCN-004.1, SCN-004.2, SCN-004.3
- **Origem:** RF-003, RF-004, telas TL-03 e TL-05, DDR-002, DDR-005, DDR-006

#### Contexto

O board é a tela central do produto e a que fecha a fatia vertical deste épico.
O cartão precisa exibir as três dimensões separadas desde já, mesmo que o
impedimento só ganhe operação num épico posterior: um cartão que colapse as
dimensões é o defeito que a modelagem inteira existe para evitar, e corrigi-lo
depois custa mais que fazê-lo certo agora.

#### O que deve ser feito

- [ ] Implementar `/projetos/:id/board` consumindo a leitura do board.
- [ ] Renderizar colunas por etapa na ordem, incluindo etapa vazia.
- [ ] Renderizar raias como agrupamento horizontal dentro das colunas.
- [ ] Implementar o cartão compacto exibindo etapa, condição e marca de
      impedimento em campos separados.
- [ ] Exibir os contadores de permanência, de espera de tomada e de impedimento
      sem nenhuma soma entre eles.
- [ ] Implementar a criação de tarefa em TL-05, com as recusas de título em
      branco e de projeto sem fluxo.
- [ ] Garantir conformidade WCAG 2.1 AA.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/app/projetos/[id]/board/page.tsx` | criar | tela TL-03 |
| `frontend/app/projetos/[id]/tarefas/nova/page.tsx` | criar | tela TL-05 |
| `frontend/components/board/Cartao.tsx` | criar | três dimensões em campos separados |
| `frontend/components/board/Coluna.tsx` | criar | etapa, com estado vazio próprio |
| `frontend/lib/api/board.ts` | criar | leitura do board e criação de tarefa |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/`, e todo arquivo de
verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Protótipos de referência:
`docs/design/kanban-tarefas/prototypes/TL-03-board-cartao-compacto.html` e
`TL-05-nova-tarefa.html`. O cartão compacto é o padrão decidido;
`TL-03b-board-cartao-expandido.html` é **variação descartada** e não é insumo de
implementação.

Corpo consumido:
`{ seq, etapas: [ { id, nome, ordem, terminal, raias: [ { id, nome, tarefas: [ <cartão> ] } ] } ] }`.

Cartão:
`{ id, titulo, condicao, raiaId, responsavel, versao, esperaTomada: { desde, decorrido } | null, impedimento: { desde, decorrido, motivo } | null, permanencia: { desde, decorrido } }`.

`condicao` nunca vale `IMPEDIDA`. As três dimensões são lidas assim: a etapa pela
coluna, a condição pelo campo `condicao`, e o impedimento pela presença do bloco
próprio. `seq` é guardado para a comparação com o canal de tempo real, que chega
num épico posterior.

Criação: `POST /v1/projetos/{projetoId}/tarefas` com `{ titulo, descricao?, raiaId? }`,
`201` com o cartão; `422` para título em branco e `422` para projeto sem fluxo,
este último orientando configurar as etapas antes.

#### Guia técnico — pontos de atenção

- **Nunca exiba um total de tempo no cartão.** As três séries correm em paralelo
  e somá-las produz número sem significado. Dois contadores no mesmo cartão
  convidam à leitura somada, e é justamente por isso que os rótulos precisam ser
  inequívocos.
- **Espera de tomada é estado de primeira classe**, com apresentação própria — não
  é ausência de responsável.
- **A marca de impedimento é dimensão ortogonal**: ela coexiste com qualquer
  condição não terminal e não substitui a condição no cartão.
- **Etapa vazia é renderizada como coluna com estado próprio**, nunca omitida.
- **Nenhuma informação transmitida apenas por cor** — espera e impedimento
  precisam de ícone e rótulo textual além do tom, e isso resolve a semelhança
  entre os azuis do tema.
- **Projeto sem fluxo leva à configuração**, e a recusa da criação precisa
  oferecer esse caminho.
- Arraste, quando existir, sempre com caminho equivalente por teclado; nesta task
  o cartão ainda não se move.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | O board exibe as colunas na ordem e os cartões em suas etapas e raias | percurso em navegador |
| 2 | Etapa sem tarefa aparece como coluna com estado vazio | configurar etapa sem tarefa e abrir o board |
| 3 | Cartão com espera e impedimento simultâneos exibe os dois contadores e nenhum total | inspeção visual e do DOM |
| 4 | Criar tarefa a faz aparecer na primeira coluna, aguardando tomada | percurso de ponta a ponta |
| 5 | Título em branco é recusado com mensagem na própria tela | tentativa de submissão |
| 6 | Projeto sem fluxo recusa a criação e oferece configurar as etapas | percurso em projeto recém-criado |
| 7 | As duas telas passam em auditoria de acessibilidade AA | verificação automatizada sem violação de nível AA |
| 8 | Nenhuma informação é transmitida apenas por cor | inspeção dos estados em modo monocromático |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
