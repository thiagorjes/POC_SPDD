# TASK-07.1 — Fila pessoal entre projetos

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-014.1, SCN-014.2, SCN-014.3
- **Origem:** RF-014, RN-024, RN-025, BDR-002

#### Contexto

A fila é o suporte mais direto da hipótese de adesão: ela responde "o que espera
por mim" atravessando todos os projetos, e é o que substitui a varredura manual
de três canais de mensagem. Não recebe projeto: se recebesse, deixaria de
resolver o problema que existe para resolver.

#### O que deve ser feito

- [ ] Implementar `GET /v1/fila`, atravessando todos os projetos em que a pessoa
      participa.
- [ ] Aceitar a ordenação por maior e por menor espera, com maior espera como
      padrão.
- [ ] Devolver a lista de tarefas aguardando tomada com o tempo de espera.
- [ ] Devolver a lista de impedimentos sob a responsabilidade de quem consulta.
- [ ] Devolver as duas listas vazias e **nenhum tempo** quando não há nada na
      fila.
- [ ] Devolver listas vazias para quem só tem leitura no projeto.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/fila/FilaController.java` | criar | rota sem projeto no caminho |
| `backend/src/main/java/br/com/idsd/kanban/internal/fila/FilaQuery.java` | criar | consulta entre projetos |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`GET /v1/fila` — atravessa **todos** os projetos em que a pessoa participa. Não
recebe projeto.

- **Consulta:** `?ordem=maiorEspera|menorEspera`, padrão `maiorEspera`, o mais
  parado no topo.
- **Saída `200`:**
  `{ "aguardandoTomada": [ { "tarefaId", "titulo", "projeto": { "id", "nome" }, "etapa": { "id", "nome" }, "esperaDesde", "esperaDecorrida" } ], "impedimentosSobMinhaResponsabilidade": [ { "tarefaId", "titulo", "projeto", "motivo", "impedidaDesde", "impedimentoDecorrido" } ] }`
- A segunda lista traz os impedimentos abertos dos projetos em que a pessoa tem
  permissão de desbloqueio; nos projetos **sem ninguém** com a permissão, traz
  para todos os participantes.
- Fila vazia: `200` com as duas listas vazias e **nenhum tempo** no corpo. Zero
  não é resposta aqui.
- Quem só tem leitura no projeto recebe as duas listas vazias: não há o que
  assumir.

#### Guia técnico — pontos de atenção

- **Não acrescente projeto ao caminho nem como filtro obrigatório.** A fila é
  entre projetos por definição.
- **Fila vazia não devolve zero.** "Ainda não há" e "é zero" são afirmações
  diferentes, e devolver zero mente com número.
- **O índice parcial sobre a condição de espera é o que sustenta esta consulta**;
  sem ele a varredura cresce com o total de tarefas do sistema.
- **A lista de impedimentos depende da permissão por projeto**, e o caso sem
  ninguém com ela precisa continuar alcançável.
- **Nenhum agregado por pessoa é produzido aqui** — a fila lista tarefas, não
  mede pessoas.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | A fila reúne tarefas de todos os projetos da pessoa, com a mais parada no topo | percurso com tarefas em dois projetos |
| 2 | A ordenação inversa coloca a de menor espera no topo | requisição com a outra ordem |
| 3 | Fila vazia devolve listas vazias e nenhum tempo no corpo | inspeção do corpo |
| 4 | Impedimentos aparecem para quem tem permissão de desbloqueio | comparação entre sujeitos |
| 5 | Em projeto sem ninguém com a permissão, o impedimento aparece para todos os participantes | projeto configurado sem esses papéis |
| 6 | Quem só tem leitura recebe as duas listas vazias | requisição com papel de leitura |
| 7 | A consulta não devolve nenhum agregado por pessoa | inspeção do contrato de saída |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
