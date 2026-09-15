# TASK-02.4 — Núcleo de escrita: evento, sequência e projeção

- **Status:** concluída (2 de 3 tentativas) — 13 achados da revisão fechados; medição de integração pendente de ambiente
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
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/RegistradorDeEvento.java` | criar | grava evento, obtém a sequência, delega intervalos |
| `backend/src/main/java/br/com/idsd/kanban/internal/tempo/AplicadorDeIntervalos.java` | criar | abre e fecha intervalos por tipo de evento |
| `backend/src/main/java/br/com/idsd/kanban/shared/EventoBoardPublisher.java` | criar | porta; implementação vazia nesta task |
| `backend/src/main/java/br/com/idsd/kanban/internal/tempo/ReconstrutorDeProjecao.java` | criar | rotina administrativa idempotente |
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
| 2026-09-15 | tentativa 1 — implementada | Sete arquivos de produção: `RegistradorDeEvento` (seq por `UPDATE ... RETURNING` dentro da transação, `persist`+`flush`, publicação em `afterCommit`, `409` `reconstrucao-em-curso` por `pg_try_advisory_xact_lock`), `AplicadorDeIntervalos` (o catálogo da seção 4 transcrito **uma única vez**, fecha antes de abrir, tudo no `ocorridoEm` do evento), `ReconstrutorDeProjecao` (bloqueio consultivo bloqueante, uma transação por projeto), `EventoBoardPublisher` com a implementação silenciosa, mais os enums `TipoDeEvento` e `TipoDeIntervalo`; e quatro alterados — `EventoTarefa` (tipo tipado, construtor de nascimento e o record `Novo`), `IntervaloTarefa` (construtor de abertura, `fechar`, `reescrever` restrito à reconstrução), e os dois repositórios, **nenhum deles publicando alteração ou remoção do log**. A transação da reconstrução é `TransactionTemplate` e não `@Transactional`: `reconstruirTudo` chama `reconstruir` pelo `this`, que não passa pelo proxy — a anotação seria ignorada e o bloqueio `xact` soltaria no fim da própria consulta que o toma, apagando a janela sem escrita **sem que nada falhasse**. A remoção do excedente não pôde ser "apaga tudo e insere de novo": `impedimento.intervalo_id` é FK não adiável, de modo que a rotina casa linha gravada com linha reexecutada por `(tarefa_id, tipo)`, reescreve no lugar, apaga o excedente e só então insere o que falta. **Medição:** `compile` em BUILD SUCCESS e **28 testes unitários, 28 verdes** — idêntico à base, zero regressão —, com `-Dmaven.compiler.release=21` porque o JDK do host é 21 contra o 25 do projeto; aferição, não a build. **O Docker sumiu da máquina outra vez**, e sem ele nenhum teste de integração sobe: os **sete critérios de aceite permanecem sem medição**. As duas classes que os exercitariam dependem de rotas de TASK-02.5 e dos EPIC-03/04/07 de qualquer modo — como TASK-02.3, esta é task de infraestrutura e não acende cenário. O sinal positivo disponível é outro: os arquivos de teste que não compilam caem de **4 para 3**, porque `RegistradorDeEvento` e `AplicadorDeIntervalos` passaram a resolver nos três, o que é evidência direta de que o contrato bate com a suíte congelada. |
| 2026-09-15 | achado — quem chama o aplicador | A tabela de arquivos da task diz que o registrador "delega intervalos". A suíte congelada prova o contrário: `TomadaServiceTest` e `ImpedimentoServiceTest` constroem os serviços de domínio **com `AplicadorDeIntervalos` como colaborador** e estipulam `registrar(any())` com **um** argumento. **A suíte prevaleceu**, porque está fora do alcance desta skill; a divergência está anotada no javadoc de `RegistradorDeEvento`. Destino `/tasks`. |
| 2026-09-15 | achado bloqueante — `recuarInicioDoIntervalo` torna o critério 6 insatisfazível | `Cenario.recuarInicioDoIntervalo` desloca **apenas** `intervalo_tarefa.inicio` e nunca `evento_tarefa.ocorrido_em`. Uma reconstrução dirigida pelo log não tem como reproduzir um início que o log não contém, e `ReconstrucaoDaProjecaoIT` compara por igualdade exata. Nenhuma implementação correta passa. Destino `/tests`. |
| 2026-09-15 | achado bloqueante — `TRUNCATE intervalo_tarefa` colide com a FK | `ReconstrucaoDaProjecaoIT.apagarAProjecao()` trunca `intervalo_tarefa` enquanto `impedimento.intervalo_id` a referencia por FK não adiável: o PostgreSQL recusa sem `CASCADE`. Destino `/tests`. |
| 2026-09-15 | achado — a seção 9 promete mais do que o log carrega | A TechSpec diz que a reconstrução "reescreve `tarefa`, `intervalo_tarefa` e `impedimento` do zero". Não é alcançável: `tarefa.raia_id` não aparece em coluna nem em documento de evento nenhum do catálogo da seção 4; `impedimento.id` é um `uuid` que evento algum carrega, de modo que recriar a linha trocaria o identificador já devolvido ao cliente; e `titulo`/`descricao` vivem em `evento_tarefa.dados`, cujo formato nenhum escritor fixou. O escopo implementado é `intervalo_tarefa`, declarado no javadoc. Destino `/techspec`. |
| 2026-09-15 | achado menor — dois arquivos fora da tabela | `TipoDeEvento` e `TipoDeIntervalo` são exigidos pelo mapeamento `@Enumerated(STRING)` das duas entidades e não constam da tabela de arquivos da task. Destino `/tasks`. |
| 2026-09-15 | tentativa 2 — achados da revisão | **13 dos 16 achados de destino `/implement` fechados.** O bloqueante ACH-01 saiu numa palavra e é a correção que importa: o escritor passa a tomar `pg_try_advisory_xact_lock_shared` em vez do exclusivo. A relação que o desenho exige é **assimétrica** — escritores convivem entre si e nenhum convive com a reconstrução —, e é isso que compartilhado contra exclusivo expressa; o reconstrutor segue no modo exclusivo, que espera os compartilhados saírem e barra os que chegarem depois. Sem isso o critério 1 falhava por construção, e qualquer participante do projeto impedia os demais de escrever só mantendo transações abertas. **ACH-06 e ACH-19 fecharam juntos, pela mesma mudança:** o `UPDATE ... RETURNING` foi envolvido em `WITH ... SELECT`, porque o Hibernate classifica a consulta pelo verbo inicial e ler o resultado de um comando que ele considera de escrita funciona por acidente, não por contrato — e o ponto de que todo o caminho de escrita depende não pode depender de comportamento não especificado. Com a CTE, projeto inexistente devolve zero linhas, e sai como `404` em vez de `500`. **ACH-12 fechou pela metade, e a metade que falta está declarada:** a coerência tarefa↔projeto passa a ser conferida antes de tudo — sem ela um `Novo` com o `projetoId` alheio grava no log de outro projeto **e consome a sequência dele**, o que é lacuna permanente e indistinguível de mensagem perdida para o board de quem nada fez. Já conferir `atorId` contra o principal autenticado acoplaria o caminho único de escrita ao contexto de segurança web e o quebraria para o job e para o arnês, que legitimamente não têm principal: o lugar é a borda, que nasce em TASK-02.5. **ACH-17 idem:** o gate administrativo do reconstrutor inverte o usual — ausência de autenticação autoriza, porque a rotina é de processo; o que ele impede é chegar ali por dentro de uma requisição autenticada qualquer, que é o que aconteceria no dia em que ela ganhasse rota. Os demais: `dados` ganha teto e validação de JSON (ACH-15), com a ressalva escrita de que distinguir dado de cliente de dado de serviço não é validação de forma e pertence à borda; exceção no `afterCommit` passa a morrer no gancho e virar log (ACH-13), porque `500` sobre escrita já comitada convida a repetir o que, num log imutável, grava de novo; o log passa a ser reexecutado em lotes de 500 com `em.clear()` entre eles (ACH-14) — seguro ali e só ali, porque nada daquela fase é entidade gerenciada e os intervalos ainda não foram carregados; a remoção do excedente vai em lotes de 1000 (ACH-18); a chave do bloqueio passa a sair de SHA-256 e não do `xor` das metades, que era construtível (ACH-16); `condicaoOrigem`/`condicaoDestino` viram `Condicao` (ACH-10) — nenhum consumidor dos getters, verificado antes; e os dois textos que afirmavam a perna de banco de RNF-008 fora de vigor foram corrigidos, porque TASK-02.9 a pôs em vigor no commit anterior (ACH-07), mais o comentário do `flush` (ACH-09) e a hipótese da projeção bem-formada (ACH-08), que não existia: todo campo mutável é sobrescrito e o que sobra é apagado, de modo que projeção corrompida sai da rotina igual a projeção ausente. **Medição:** `compile` verde e **28 testes unitários, 28 verdes** — idêntico à base, zero regressão —, seguem exatamente os mesmos 3 arquivos de teste que não compilam por dependerem de tasks posteriores, nenhum novo. **O Docker continua ausente e ACH-05 segue aberto:** os sete critérios de aceite permanecem sem medição, e com eles a verificação de que ACH-01 de fato passou. **Três achados não fecharam aqui, por não serem desta mão:** ACH-02 (`/techspec`), ACH-03 e ACH-04 (`/tests`, os dois defeitos da suíte congelada). |
