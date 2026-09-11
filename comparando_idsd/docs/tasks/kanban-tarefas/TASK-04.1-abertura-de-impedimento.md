# TASK-04.1 — Abertura e anotação de impedimento

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-009.1, SCN-009.2, SCN-009.3
- **Origem:** RF-009, RN-010, RN-024, BDR-002

#### Contexto

Abrir impedimento acende a terceira dimensão e nada mais: a condição de trabalho
não muda, e a contagem de impedimento passa a correr em paralelo às outras duas.
Impedir de novo uma tarefa já impedida não cria segundo impedimento nem reinicia
o relógio — anexa informação ao que já está aberto.

#### O que deve ser feito

- [ ] Implementar `POST /v1/tarefas/{tarefaId}/impedimentos`.
- [ ] Abrir o impedimento com o intervalo de `IMPEDIMENTO`, registrando quem
      abriu e a etapa vigente.
- [ ] Não alterar a condição nem a etapa na abertura.
- [ ] Anexar o motivo às anotações quando já houver impedimento aberto,
      devolvendo `200` e sem reiniciar a contagem.
- [ ] Recusar com `422` motivo ausente ou em branco.
- [ ] Destacar o impedimento para quem tem a permissão de desbloqueio no projeto;
      sem ninguém com ela, para todos os participantes.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/impedimento/ImpedimentoController.java` | criar | rota de abertura |
| `backend/src/main/java/br/com/idsd/kanban/internal/impedimento/ImpedimentoService.java` | criar | abertura e anotação |
| `backend/src/main/java/br/com/idsd/kanban/internal/impedimento/ImpedimentoRequisicao.java` | criar | origem e motivo |
| `backend/src/main/java/br/com/idsd/kanban/internal/impedimento/DestaqueDeImpedimento.java` | criar | resolve a quem o impedimento se destaca |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`POST /v1/tarefas/{tarefaId}/impedimentos`

- **Entrada:** `{ origem, motivo }`.
- **Saída `201`** quando abre; **`200`** quando já havia impedimento aberto e a
  informação foi anexada.
- **A condição não muda.** Abrir impedimento acende a terceira dimensão e nada
  mais: a tarefa em curso continua em curso, a que aguardava tomada continua
  aguardando, e as duas contagens seguem correndo em paralelo à de impedimento.
- Já impedida: nenhum segundo impedimento é criado, o motivo entra em `anotacoes`
  e a contagem **não** reinicia. O índice único parcial do esquema garante isso
  mesmo sob concorrência.
- **`422`** motivo ausente ou em branco.
- Após aceitar, o impedimento se destaca para quem tem a permissão de desbloqueio
  no projeto; **sem ninguém com ela, para todos os participantes**.

O evento gravado é `IMPEDIMENTO_ABERTO` na primeira vez e `IMPEDIMENTO_ANOTADO`
nas seguintes; o segundo não abre nem fecha intervalo. O intervalo de
`IMPEDIMENTO` guarda a etapa vigente na abertura.

#### Guia técnico — pontos de atenção

- **Não mexa em `condicao`.** É o erro que a promoção do impedimento a dimensão
  própria existe para tornar impossível, e o valor `IMPEDIDA` não existe no
  domínio.
- **A concorrência é resolvida pelo esquema.** Duas aberturas simultâneas: uma
  cria, a outra colide no índice único parcial e vira anotação. Não tente
  resolver isso só no serviço.
- **Reiniciar a contagem na segunda abertura falseia a série** e é a forma
  silenciosa de perder a medida do travamento.
- **O instantâneo da etapa no intervalo é obrigatório**, ou o bloco de
  impedimento por etapa das consultas agregadas cai num grupo nulo.
- **O caminho sem ninguém com a permissão precisa continuar alcançável** — é a
  saída que impede a tarefa impedida de ficar sem destinatário.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Abrir impedimento acende a marca e inicia a contagem, sem alterar a condição | leitura do cartão antes e depois |
| 2 | Motivo em branco é recusado com `422` e nada é gravado | contagem de eventos antes e depois |
| 3 | Impedir tarefa já impedida não cria segundo impedimento e não reinicia a contagem | comparação do início do intervalo |
| 4 | O motivo da segunda abertura fica registrado nas anotações | leitura do detalhe da tarefa |
| 5 | Duas aberturas simultâneas produzem um único impedimento | teste de concorrência |
| 6 | O impedimento se destaca para quem tem permissão de desbloqueio | leitura por sujeito com e sem a permissão |
| 7 | Sem ninguém com a permissão no projeto, o destaque alcança todos os participantes | projeto configurado sem esses papéis |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
