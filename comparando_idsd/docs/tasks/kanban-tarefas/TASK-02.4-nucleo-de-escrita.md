# TASK-02.4 — Núcleo de escrita: evento, sequência e projeção

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.3
- **Cenários cobertos:** SCN-004.1
- **Origem:** SDR-001, SDR-004, RNF-008, TechSpec Seção 5

#### Contexto

Toda escrita sobre tarefa do sistema inteiro passa por um caminho único: grava o
evento no log, obtém a sequência do banco, atualiza as projeções na mesma
transação e registra a publicação para depois do commit. Esta task cria esse
caminho uma vez. Todas as operações dos épicos seguintes o reutilizam, e é por
isso que ela precede a primeira delas.

#### O que deve ser feito

- [ ] Implementar o serviço de escrita de evento com a sequência obtida no
      banco, dentro da transação.
- [ ] Implementar o aplicador de intervalos: abrir e fechar `PERMANENCIA`,
      `ESPERA_TOMADA` e `IMPEDIMENTO` conforme o tipo de evento.
- [ ] Gravar log e projeção **na mesma transação**.
- [ ] Definir a porta de publicação de evento e registrar a publicação em
      `afterCommit`, com implementação vazia por enquanto.
- [ ] Implementar a rotina administrativa de reconstrução da projeção, com
      bloqueio consultivo por projeto.
- [ ] Garantir que nenhum repositório exponha atualização ou remoção sobre o
      log.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/tarefa/RegistradorDeEvento.java` | criar | grava evento, obtém a sequência, delega intervalos |
| `backend/src/main/java/<pkg>/internal/tempo/AplicadorDeIntervalos.java` | criar | abre e fecha intervalos por tipo de evento |
| `backend/src/main/java/<pkg>/shared/EventoBoardPublisher.java` | criar | porta; implementação vazia nesta task |
| `backend/src/main/java/<pkg>/internal/tempo/ReconstrutorDeProjecao.java` | criar | rotina administrativa idempotente |
| repositórios de evento e intervalo | criar | sem métodos de atualização ou remoção sobre o log |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Fluxo de uma escrita sobre tarefa, ponta a ponta:

1. O controller recebe a requisição e valida **formato** com Bean Validation.
   Nenhuma decisão de negócio nele.
2. O serviço abre transação curta, carrega a tarefa sob bloqueio otimista e
   reavalia a origem declarada contra o estado corrente. Divergência encerra em
   `409` com o estado atual; efeito já aplicado encerra em `200` sem novo evento.
3. Verifica a permissão sobre a participação no projeto — nunca sobre papel
   global e nunca sobre dado vindo do cliente.
4. Obtém a sequência do evento por
   `UPDATE projeto SET seq_atual = seq_atual + 1 WHERE id = ? RETURNING seq_atual`,
   na mesma transação.
5. Aplica a regra, grava `evento_tarefa` e atualiza `tarefa`,
   `intervalo_tarefa` e `impedimento` na mesma transação.
6. Registra em sincronização de transação, no gancho de após o commit, a
   publicação pela porta de eventos.

Catálogo de tipos de evento e o efeito de cada um sobre os intervalos:

| Tipo | Fecha intervalo | Abre intervalo |
| --- | --- | --- |
| `TAREFA_CRIADA` | — | `PERMANENCIA`, `ESPERA_TOMADA` |
| `TAREFA_ASSUMIDA` | `ESPERA_TOMADA` | — |
| `TAREFA_DEVOLVIDA` | — | `ESPERA_TOMADA` |
| `TAREFA_MOVIDA` | `PERMANENCIA`, `ESPERA_TOMADA` | `PERMANENCIA`, `ESPERA_TOMADA` |
| `IMPEDIMENTO_ABERTO` | — | `IMPEDIMENTO` |
| `IMPEDIMENTO_ANOTADO` | — | — |
| `IMPEDIMENTO_RESOLVIDO` | `IMPEDIMENTO` | — |
| `TAREFA_CONCLUIDA` | todos abertos | — |
| `TAREFA_ENCERRADA_SEM_CONCLUSAO` | `PERMANENCIA`, `ESPERA_TOMADA` | — |
| `TAREFA_REABERTA` | — | `PERMANENCIA`, `ESPERA_TOMADA`, com episódio incrementado |

Reconstrução da projeção: lê `evento_tarefa` em ordem de `id`, por projeto, e
reescreve `tarefa`, `intervalo_tarefa` e `impedimento` do zero. É idempotente e
não toca no anel de verdade. Toma bloqueio consultivo por projeto
(`pg_advisory_xact_lock` sobre o identificador), e escrita que chegue durante a
janela recebe `409`.

#### Guia técnico — pontos de atenção

- **A sequência vem do banco, nunca de contador em memória.** Contador por
  instância produz número duplicado com duas instâncias escrevendo no mesmo
  projeto, e duplicata torna a lacuna indetectável — quebra exatamente a rede de
  segurança do mecanismo de tempo real.
- **A contenção é o preço da contiguidade, e é aceita.** O incremento serializa
  as escritas do mesmo projeto, e só delas.
- **Rollback não pode deixar buraco.** É por isso que o contador é coluna e não
  sequência do banco.
- **`TAREFA_MOVIDA` não fecha `IMPEDIMENTO`.** É a linha mais fácil de errar
  deste catálogo.
- **A publicação sai depois do commit, jamais dentro da transação.** Evento
  anunciado e depois revertido é pior que evento atrasado.
- **Um único publicador.** Não existe gatilho no esquema; a porta é o caminho.
- **Reconstruir concorrentemente produz o defeito que a rotina cura.** O
  bloqueio consultivo não é opcional.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Duas escritas concorrentes no mesmo projeto recebem sequências distintas e contíguas | teste de integração com duas transações simultâneas |
| 2 | Transação revertida não consome sequência | escrita que falha e leitura do contador |
| 3 | Log e projeção commitam juntos | falha injetada na projeção deixa o log intacto e sem o evento |
| 4 | A publicação só ocorre após o commit | espião na porta registra a chamada depois da confirmação |
| 5 | Nenhum caminho exposto consegue alterar ou apagar evento | tentativa por cada método de repositório e por comando direto |
| 6 | Reconstruir a projeção depois de um percurso completo reproduz o estado capturado | executar percurso, capturar, reconstruir, comparar |
| 7 | Escrita durante a janela de reconstrução recebe `409` | teste com bloqueio tomado |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
