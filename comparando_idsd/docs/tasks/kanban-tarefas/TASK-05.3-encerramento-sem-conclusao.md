# TASK-05.3 — Encerramento sem conclusão

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-05.2
- **Cenários cobertos:** SCN-012.1, SCN-012.2, SCN-012.3, SCN-012.4
- **Origem:** RF-012, RN-011, RN-016, RN-018, RN-032

#### Contexto

Encerrar sem conclusão é a saída para a tarefa que não vai mais ser feita, e
substitui a exclusão do cartão: apagar destruiria o tempo por etapa que o produto
existe para medir. É a segunda das duas únicas restrições que a marca de
impedimento impõe — sair do fluxo exige, antes, alguém dizer como o impedimento
terminou.

#### O que deve ser feito

- [ ] Implementar `POST /v1/tarefas/{tarefaId}/encerramento`.
- [ ] Passar a condição a `ENCERRADA_SEM_CONCLUSAO`, fechando `PERMANENCIA` e
      `ESPERA_TOMADA`, preservando o histórico.
- [ ] Exigir permissão de configuração; `403` para participante comum, sem
      alterar a condição.
- [ ] Recusar com `422` quando houver impedimento aberto, exigindo o desfecho
      antes.
- [ ] Tornar o estado terminal absoluto: nenhuma rota o reverte.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/tarefa/EncerramentoController.java` | criar | rota de encerramento |
| `backend/src/main/java/<pkg>/internal/tarefa/EncerramentoService.java` | criar | aplicação e recusas |
| `backend/src/main/java/<pkg>/internal/tarefa/VerificadorDeMarca.java` | alterar | terceira chamadora da verificação compartilhada |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`POST /v1/tarefas/{tarefaId}/encerramento`

- **Entrada:** `{ origem, motivo }`.
- **Saída `200`:** condição `ENCERRADA_SEM_CONCLUSAO`, intervalos de
  `PERMANENCIA` e `ESPERA_TOMADA` fechados, histórico preservado. Não há
  intervalo de `IMPEDIMENTO` a fechar aqui, porque o encerramento com impedimento
  aberto nunca é aceito.
- **Exige permissão de configuração.** `403` para participante comum, sem alterar
  a condição.
- **`422` com impedimento aberto**, com `detail` exigindo o desfecho registrado
  antes. A condição não é alterada e o impedimento permanece aberto. Fechar o
  intervalo em cascata atribuiria ao sistema um desfecho que ninguém afirmou, e o
  agregado passaria a conter esse número.
- Estado terminal absoluto: nenhuma rota o reverte.

O evento gravado é `TAREFA_ENCERRADA_SEM_CONCLUSAO`, com o motivo em `dados`.

#### Guia técnico — pontos de atenção

- **Nunca feche o impedimento em cascata.** É a decisão de negócio central desta
  task, e a razão pela qual sair do fluxo com a tarefa travada exige dois passos —
  isso é deliberado, não uma aspereza a corrigir.
- **Este evento fecha duas séries, não três**, precisamente porque nunca é gravado
  com a marca acesa.
- **Encerrar não apaga nada.** A tarefa continua no histórico e no tempo por
  etapa; não existe exclusão de cartão no produto.
- **Encerrada não reabre.** Só a tarefa concluída volta ao fluxo.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Encerrar sem conclusão cessa as contagens de permanência e de espera de tomada | consulta aos intervalos abertos |
| 2 | O histórico da tarefa é preservado após o encerramento | leitura do detalhe |
| 3 | Participante comum recebe `403` e a condição não muda | comparação do estado antes e depois |
| 4 | Encerrar com impedimento aberto é recusado com `422` exigindo o desfecho | inspeção do corpo do erro |
| 5 | Após a recusa o impedimento permanece aberto e contando | leitura do intervalo |
| 6 | Tarefa encerrada não se move e não reabre | tentativas recusadas com `422` |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
