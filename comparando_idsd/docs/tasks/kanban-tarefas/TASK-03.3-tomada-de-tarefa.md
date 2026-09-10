# TASK-03.3 — Tomada de tarefa

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-03.1
- **Cenários cobertos:** SCN-007.1, SCN-007.2, SCN-007.3
- **Origem:** RF-007, RN-007, RN-030, RN-033

#### Contexto

Assumir é o ato que fecha a espera de tomada e transforma a fila em trabalho. É
também o ponto de disputa mais provável do produto: duas pessoas clicam no mesmo
cartão, e quem perde precisa saber quem ganhou — não apenas que houve erro.

#### O que deve ser feito

- [ ] Implementar `POST /v1/tarefas/{tarefaId}/tomada`.
- [ ] Registrar responsável e instante da tomada, fechando `ESPERA_TOMADA`.
- [ ] Absorver a repetição de quem já assumiu com `200`, sem novo evento e sem
      alterar o instante.
- [ ] Devolver `409` com o responsável atual quando outra pessoa assumiu antes.
- [ ] Aceitar a tomada de tarefa com impedimento aberto, sem verificação
      adicional.
- [ ] Exigir participação no projeto.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/tarefa/TomadaController.java` | criar | rota de tomada |
| `backend/src/main/java/<pkg>/internal/tarefa/TomadaService.java` | criar | aplicação e disputa |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`POST /v1/tarefas/{tarefaId}/tomada`

- **Entrada:** `{ origem }`.
- **Saída `200`:** cartão com `responsavel` e `assumidaEm`; fecha
  `ESPERA_TOMADA`.
- Repetir por quem já assumiu: `200`, sem novo evento, `assumidaEm` inalterado.
- **`409`** quando outra pessoa assumiu antes, com `estadoAtual.responsavel`
  preenchido.
- **Impedimento aberto não é pré-condição.** A tomada é aceita, a marca
  permanece, e quem assume assume também o trabalho de destravar. Não há
  verificação a fazer: a marca não vive em `tarefa`.

O evento gravado é `TAREFA_ASSUMIDA`, com o ator preenchido.

#### Guia técnico — pontos de atenção

- **Não acrescente verificação de impedimento.** Recusar a tomada de tarefa
  impedida deixaria a tarefa parada no pool sem ninguém a reclamar, que é
  exatamente o oposto do efeito desejado.
- **`assumidaEm` não é reescrito na repetição.** Reescrevê-lo falsearia o tempo
  de trabalho e é a forma silenciosa de quebrar a idempotência.
- **A tomada fecha `ESPERA_TOMADA` e não abre nada.** A permanência segue
  correndo, sem interrupção.
- **O `409` precisa nomear quem assumiu.** Sem o nome, a tela não tem o que
  mostrar e o cenário reprova.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Assumir registra o responsável e encerra a contagem de espera de tomada | leitura do cartão e do intervalo |
| 2 | Assumir de novo devolve `200` sem novo evento e com o mesmo instante | contagem de eventos e comparação do instante |
| 3 | Segunda pessoa recebe `409` com o nome de quem assumiu | inspeção do corpo do erro |
| 4 | Tarefa com impedimento aberto pode ser assumida e a marca permanece | tomada seguida de leitura do cartão |
| 5 | A permanência não é interrompida pela tomada | intervalo de permanência continua aberto e com o mesmo início |
| 6 | Quem não participa do projeto recebe `404` | requisição com sujeito sem participação |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
