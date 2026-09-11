# TASK-06.1 — Consulta e substituição das raias

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-018.1, SCN-018.2
- **Origem:** RF-018, RN-023

#### Contexto

A raia organiza visualmente o board e nada mais: não restringe transição e não
entra em nenhuma agregação. Essa ausência é a regra, e ela é garantida no esquema
— nenhuma tabela do anel de projeção referencia raia.

#### O que deve ser feito

- [ ] Implementar `GET` e `PUT /v1/projetos/{projetoId}/raias`, na mesma forma e
      com a mesma permissão das etapas.
- [ ] Substituir o conjunto inteiro numa operação, criando, renomeando,
      reordenando e arquivando.
- [ ] Manter a tarefa acessível quando a raia dela é arquivada.
- [ ] Não permitir que raia restrinja transição.
- [ ] Não aceitar filtro por raia em nenhuma rota de consulta agregada.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/RaiaController.java` | criar | duas rotas |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/RaiaService.java` | criar | substituição transacional |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`GET` e `PUT /v1/projetos/{projetoId}/raias`, mesma forma e mesma permissão da
configuração de etapas: leitura devolve `[ { id, nome, ordem } ]` sem as
arquivadas; a escrita substitui o conjunto inteiro, item sem `id` cria e item
existente omitido é arquivado.

Raia não restringe transição e não entra em agregação: **nenhuma rota de consulta
agregada aceita filtro por raia**, e é a ausência que precisa ser verificada.

Tarefa cuja raia foi arquivada continua acessível, e aparece no board fora das
raias vigentes.

#### Guia técnico — pontos de atenção

- **Não acrescente filtro por raia às consultas agregadas**, nem "por simetria"
  com o filtro por etapa. A ausência é a regra.
- **Arquivar raia não pode esconder tarefa.** Perder o acesso à tarefa por causa
  de uma mudança visual seria destruir trabalho por engano de configuração.
- **A raia não participa de nenhuma decisão de transição.**
- **A substituição é transacional**, pela mesma razão das etapas.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Raias configuradas aparecem no board como agrupamento | leitura do board após a configuração |
| 2 | Mover tarefa entre etapas independe da raia | movimento em tarefa de qualquer raia |
| 3 | Arquivar a raia mantém a tarefa acessível | leitura da tarefa e do board após o arquivamento |
| 4 | Nenhuma rota de consulta agregada aceita filtro por raia | tentativa de filtro é recusada em cada rota |
| 5 | A configuração exige a mesma permissão da configuração de etapas | requisição por participante sem ela recebe `403` |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
