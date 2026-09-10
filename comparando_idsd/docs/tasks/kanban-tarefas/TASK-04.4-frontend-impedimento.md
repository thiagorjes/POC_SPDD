# TASK-04.4 — Frontend: marca no cartão e bloco de impedimento na ficha

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-03.5, TASK-04.3
- **Cenários cobertos:** SCN-003.3, SCN-009.1, SCN-010.1
- **Origem:** RF-009, RF-010, telas TL-03 e TL-04, DDR-005, DDR-007

#### Contexto

O impedimento entra na interface como dimensão visual ortogonal: ele se soma ao
cartão sem substituir a condição, e só desabilita o que o contrato de fato
recusa. É a tela em que o cartão passa a exibir duas contagens simultâneas, e
onde o convite a somá-las precisa ser ativamente combatido pelos rótulos.

#### O que deve ser feito

- [ ] Exibir a marca de impedimento no cartão como dimensão própria, junto da
      condição e sem substituí-la.
- [ ] Exibir as duas contagens simultâneas com rótulos inequívocos e sem
      qualquer total.
- [ ] Implementar o bloco de impedimento na ficha: abrir, anexar motivo e
      registrar desfecho.
- [ ] Manter habilitadas mover, assumir e devolver em cartão com impedimento
      aberto.
- [ ] Definir o papel semântico de impedimento na extensão local do tema.
- [ ] Destacar na fila e no board os impedimentos de quem pode desbloquear.
- [ ] Garantir conformidade WCAG 2.1 AA.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/components/tarefa/BlocoImpedimento.tsx` | criar | abertura, anotações e desfecho |
| `frontend/components/board/Cartao.tsx` | alterar | marca e segunda contagem |
| `frontend/lib/api/impedimentos.ts` | criar | abertura e resolução |
| `frontend/lib/tema/papeis.ts` | criar | papel semântico de impedimento, local a este sistema |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/`, e todo arquivo de
verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Protótipos de referência:
`docs/design/kanban-tarefas/prototypes/TL-03-board-cartao-compacto.html` e
`TL-04-detalhe-da-tarefa.html`, cuja ficha exibe as três dimensões em campos
separados.

Chamadas: `POST /v1/tarefas/{tarefaId}/impedimentos` com `{ origem, motivo }`,
`201` ao abrir e `200` ao anexar; e
`POST /v1/tarefas/{tarefaId}/impedimentos/{impedimentoId}/resolucao` com
`{ origem, desfecho }`.

No cartão, o bloco `impedimento` traz `{ desde, decorrido, motivo }` e coexiste
com `esperaTomada`. A condição nunca vale `IMPEDIDA`.

O papel semântico de impedimento não existe na coleção compartilhada de design e
é definido na extensão local do tema deste sistema. Ele é condição legítima do
trabalho, e não falha de quem registra — não reutilize o papel destrutivo.

#### Guia técnico — pontos de atenção

- **Nunca exiba total das contagens.** Dois contadores no mesmo cartão convidam à
  leitura somada; os rótulos precisam nomear cada série.
- **Não desabilite mover, assumir nem devolver.** O contrato aceita as três em
  tarefa impedida, e oferecer menos do que o serviço permite trava a operação.
- **A marca não substitui a condição no cartão** — as duas aparecem lado a lado.
- **Não reutilize o papel destrutivo do tema.** Impedimento não é erro.
- **Nenhuma informação apenas por cor:** a marca precisa de ícone e rótulo,
  inclusive porque os tons de azul do tema são próximos entre si.
- **A ficha precisa mostrar o histórico de anotações**, ou a segunda abertura vira
  informação perdida.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Cartão com espera e impedimento exibe as duas contagens, rotuladas e sem total | inspeção visual e do DOM |
| 2 | A marca de impedimento aparece sem substituir a condição | inspeção do cartão |
| 3 | Abrir impedimento pela ficha acende a marca sem mudar a condição exibida | percurso em navegador |
| 4 | Registrar o desfecho apaga a marca e encerra a contagem na tela | percurso em navegador |
| 5 | Mover, assumir e devolver seguem habilitados em cartão impedido | inspeção dos controles |
| 6 | Quem pode desbloquear vê o destaque do impedimento | comparação entre sujeitos com e sem a permissão |
| 7 | A marca é perceptível sem depender de cor | inspeção em modo monocromático |
| 8 | As telas passam em auditoria de acessibilidade AA | verificação automatizada sem violação de nível AA |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
