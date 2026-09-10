# TASK-04.3 — Ortogonalidade da marca nas operações de etapa e de tomada

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-04.2
- **Cenários cobertos:** SCN-006.3, SCN-007.4, SCN-008.3
- **Origem:** RN-002, RN-009, RN-032, RN-033

#### Contexto

As operações que esta task verifica já existem: mover, assumir e devolver foram
implementadas antes de o impedimento existir. O que falta é a prova de que
nenhuma delas encosta na terceira dimensão. É trabalho de fechamento, e é onde a
regressão aparece — quem mexer nessas rotas depois vai encontrar aqui o teste que
o impede de apagar a marca por engano.

#### O que deve ser feito

- [ ] Verificar que a movimentação preserva a marca e o intervalo de
      impedimento, com a contagem correndo.
- [ ] Verificar que a tomada de tarefa impedida é aceita sem verificação
      adicional e mantém a marca.
- [ ] Verificar que a devolução ao pool preserva o impedimento aberto.
- [ ] Ajustar o que divergir nas rotas existentes, sem introduzir verificação de
      impedimento em nenhuma delas.
- [ ] Garantir que nenhuma escrita sobre tarefa fora da resolução alcance a
      marca.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/tarefa/MovimentoService.java` | alterar | apenas se divergir; nenhuma verificação nova |
| `backend/src/main/java/<pkg>/internal/tarefa/TomadaService.java` | alterar | idem |
| `backend/src/main/java/<pkg>/internal/tempo/AplicadorDeIntervalos.java` | alterar | confirma que os eventos de etapa e tomada não tocam o intervalo de impedimento |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Comportamento esperado, por operação:

| Operação | Efeito sobre a marca | Efeito sobre o intervalo de impedimento |
| --- | --- | --- |
| mover de etapa | nenhum | nenhum; a contagem segue correndo |
| assumir | nenhum | nenhum |
| devolver ao pool | nenhum | nenhum |
| renomear etapa | nenhum | nenhum |
| remover participação | nenhum | nenhum |
| registrar desfecho | apaga | fecha |

A movimentação fecha `PERMANENCIA` e `ESPERA_TOMADA` na origem e abre as duas no
destino, e **não** toca em `IMPEDIMENTO`. O cartão devolvido continua com o bloco
de impedimento preenchido.

A tomada de tarefa impedida não tem pré-condição a verificar: a marca não vive em
`tarefa`, e quem assume assume também o trabalho de destravar.

#### Guia técnico — pontos de atenção

- **A tentação aqui é acrescentar verificação, e ela é o defeito.** Recusar
  movimentação ou tomada por causa da marca materializa a recomendação que foi
  expressamente recusada.
- **Só duas operações são restringidas pela marca**, e nenhuma delas está nesta
  task: concluir e encerrar sem conclusão, ambas no épico seguinte.
- **O intervalo de impedimento guarda a etapa da abertura**, e ela não muda quando
  a tarefa se move. A série de impedimento por etapa é atribuída à etapa em que o
  travamento começou.
- **Esta task existe porque a regressão é provável.** Os testes que ela produz são
  a rede que segura as três rotas quando alguém as alterar por outro motivo.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Mover tarefa impedida preserva a marca e a contagem de impedimento | leitura do cartão e do intervalo após o movimento |
| 2 | A etapa registrada no intervalo de impedimento não muda quando a tarefa se move | inspeção do intervalo |
| 3 | Tarefa impedida no pool pode ser assumida | tomada aceita, marca presente após a operação |
| 4 | Devolver tarefa impedida mantém o impedimento aberto e contando | leitura após a devolução |
| 5 | Nenhuma operação além do registro do desfecho apaga a marca | percurso que exercita todas as operações da tabela |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
