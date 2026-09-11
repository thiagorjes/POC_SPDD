# TASK-04.2 — Registro do desfecho do impedimento

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-04.1
- **Cenários cobertos:** SCN-010.1, SCN-010.2, SCN-010.3
- **Origem:** RF-010, RN-032, BDR-002

#### Contexto

O registro do desfecho é a **única** operação do sistema que apaga a marca de
impedimento e encerra sua contagem. Nenhuma outra o faz, e o esquema garante isso
porque a marca não é coluna de tarefa. Não há condição a restaurar: a abertura
nunca alterou condição nenhuma.

#### O que deve ser feito

- [ ] Implementar `POST /v1/tarefas/{tarefaId}/impedimentos/{impedimentoId}/resolucao`.
- [ ] Fechar o intervalo de `IMPEDIMENTO` e gravar o desfecho e quem o resolveu.
- [ ] Deixar condição e etapa como estiverem.
- [ ] Autorizar quem tem permissão de desbloqueio **ou** quem abriu o
      impedimento.
- [ ] Absorver a resolução repetida com `200`, sem alterar nada nem o tempo
      registrado.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/impedimento/ImpedimentoController.java` | alterar | acrescenta a rota de resolução |
| `backend/src/main/java/br/com/idsd/kanban/internal/impedimento/ImpedimentoService.java` | alterar | fechamento e autorização |
| `backend/src/main/java/br/com/idsd/kanban/internal/impedimento/ResolucaoRequisicao.java` | criar | origem e desfecho |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`POST /v1/tarefas/{tarefaId}/impedimentos/{impedimentoId}/resolucao`

- **Entrada:** `{ origem, desfecho }`.
- **Saída `200`:** fecha o intervalo de `IMPEDIMENTO` e apaga a marca. A condição
  e a etapa ficam como estiverem — não há condição a restaurar, porque a abertura
  do impedimento nunca a alterou.
- **Autorizado a:** quem tem permissão de desbloqueio **ou** quem abriu o
  impedimento. A segunda metade é regra de serviço sobre quem abriu, e não
  permissão de papel.
- Já resolvido: `200`, nada alterado, tempo registrado inalterado.

O evento gravado é `IMPEDIMENTO_RESOLVIDO`, com o desfecho em `dados`.

#### Guia técnico — pontos de atenção

- **Não devolva a tarefa a nenhuma condição anterior.** A promessa de "restaurar
  a condição" era falsa desde sempre, e implementá-la corromperia o estado.
- **A autorização tem duas metades de naturezas diferentes**: uma é permissão de
  papel, a outra é a identidade de quem abriu. Resolver ambas no mesmo verificador
  de papel deixaria a segunda de fora.
- **Resolver de novo não reescreve o instante do fechamento.** Reescrevê-lo
  aumentaria retroativamente o tempo de impedimento.
- **Só esta operação apaga a marca.** Se aparecer um segundo caminho que a apague,
  a regra deixou de ser verdadeira.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Registrar o desfecho apaga a marca e encerra a contagem de impedimento | leitura do cartão e do intervalo |
| 2 | A condição e a etapa permanecem as mesmas após a resolução | comparação antes e depois |
| 3 | Quem tem permissão de desbloqueio resolve | requisição com cada um dos papéis que a possuem |
| 4 | Quem abriu o impedimento resolve mesmo sem a permissão | requisição pelo autor da abertura |
| 5 | Quem não tem nenhuma das duas condições recebe `403` | requisição por terceiro sem permissão |
| 6 | Resolver de novo devolve `200` sem alterar o tempo registrado | comparação da duração do intervalo |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
