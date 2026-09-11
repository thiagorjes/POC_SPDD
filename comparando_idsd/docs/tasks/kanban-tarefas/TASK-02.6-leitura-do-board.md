# TASK-02.6 — Leitura do board

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.5
- **Cenários cobertos:** SCN-003.1, SCN-003.2
- **Origem:** RF-003, RN-002, RN-008, RN-015, RNF-009

#### Contexto

O board é a leitura central do produto e a que sustenta o envelope de desempenho
das consultas. Ele devolve as três dimensões em campos separados, e é essa
separação que impede o cliente de recolapsá-las — o defeito que a modelagem
inteira existe para evitar.

#### O que deve ser feito

- [ ] Implementar `GET /v1/projetos/{projetoId}/board`.
- [ ] Devolver etapas na ordem, com raias e cartões, incluindo etapa vazia com
      lista vazia.
- [ ] Devolver `seq` do projeto no corpo.
- [ ] Preencher `esperaTomada`, `impedimento` e `permanencia` como blocos
      independentes, sem nenhum campo de soma.
- [ ] Montar a resposta com número fixo de consultas, sem consulta por cartão.
- [ ] Devolver `404` para projeto sem participação.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/BoardController.java` | criar | rota de leitura |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/BoardQuery.java` | criar | consultas de projeção |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/BoardResposta.java` | criar | registro de saída |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`GET /v1/projetos/{projetoId}/board`

- **Saída `200`:**
  `{ seq, etapas: [ { id, nome, ordem, terminal, raias: [ { id, nome, tarefas: [ <cartão> ] } ] } ] }`.
- **Cartão:** a mesma forma fixada na criação de tarefa.
- Etapa sem tarefa vem na lista com `tarefas: []` — exibir a etapa vazia, não
  omiti-la.
- `esperaTomada` e `impedimento` podem estar **ambos** preenchidos, e a soma nunca
  é calculada nem devolvida.
- **`condicao` nunca vale `IMPEDIDA`.** O impedimento sai no bloco próprio do
  cartão e coexiste com qualquer condição não terminal. O cliente lê as três
  dimensões em campos separados: a etapa pela posição na lista, a `condicao`, e o
  bloco de impedimento presente ou ausente.
- `seq` é o último número de sequência do projeto, e é o que o cliente compara com
  o recebido pelo canal de tempo real para detectar lacuna.
- **`404`** projeto inexistente ou sem participação — nunca `403`, para não
  revelar existência.

Consultas usadas: uma para etapas e raias, uma para as tarefas do projeto, uma
para os intervalos abertos e uma para os impedimentos abertos. A montagem é feita
em memória.

#### Guia técnico — pontos de atenção

- **Não devolva total de tempo, nem calcule soma das séries.** Um campo de soma
  no cartão é o caminho mais curto para violar a regra que separa as três séries.
- **Consulta por cartão estoura o envelope de desempenho.** O número de consultas
  não pode crescer com a quantidade de tarefas.
- **`decorrido` é calculado a partir de `desde` no instante da leitura**, não
  armazenado.
- **A etapa vazia é informação.** Omiti-la esconde do time uma etapa configurada.
- **Etapas arquivadas não entram no board.**

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | O board devolve as etapas na ordem configurada, com os cartões em suas etapas e raias | leitura após percurso conhecido |
| 2 | Etapa sem tarefa aparece com lista vazia | configurar etapa sem tarefa e ler |
| 3 | Cartão com espera e impedimento simultâneos traz os dois blocos preenchidos e nenhuma soma | inspeção do corpo |
| 4 | Nenhum campo do corpo agrega as séries entre si | inspeção do contrato de saída |
| 5 | O número de consultas ao banco não cresce com a quantidade de tarefas | contagem de consultas com 5 e com 50 tarefas |
| 6 | Projeto sem participação devolve `404` | requisição com sujeito sem participação |
| 7 | `seq` corresponde ao último evento do projeto | comparação com o log |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
