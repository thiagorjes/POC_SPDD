# TASK-06.2 — Participação e papéis, com devolução ao pool na remoção

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-019.1, SCN-019.2, SCN-019.3
- **Origem:** RF-019, RN-024, RN-027, BDR-001, BDR-002

#### Contexto

A participação é o que determina toda autorização do produto: papéis são
acumuláveis e valem por projeto. Remover alguém não pode deixar tarefas órfãs
seguradas por quem já não está — elas voltam ao pool na mesma transação, com o
registro de quem havia assumido preservado no histórico.

#### O que deve ser feito

- [ ] Implementar `GET /v1/projetos/{projetoId}/participacoes`.
- [ ] Implementar `PUT /v1/projetos/{projetoId}/participacoes/{usuarioId}` com
      papéis acumuláveis, valendo imediatamente.
- [ ] Recusar com `422` papel fora do catálogo fechado.
- [ ] Implementar `DELETE /v1/projetos/{projetoId}/participacoes/{usuarioId}`,
      devolvendo ao pool, na mesma transação, toda tarefa assumida pela pessoa.
- [ ] Preservar no log quem havia assumido, com o evento de devolução sem ator.
- [ ] Garantir que a permissão de desbloqueio pertence aos papéis existentes, sem
      papel novo.
- [ ] Exigir permissão de configuração em todas as rotas.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/ParticipacaoController.java` | criar | três rotas |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/ParticipacaoService.java` | criar | papéis e remoção com devolução |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/CatalogoDePapeis.java` | criar | catálogo fechado e matriz de permissões |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Todas as rotas exigem permissão de configuração.

`GET /v1/projetos/{projetoId}/participacoes`

- **Saída `200`:** `[ { id, usuario: { id, nome, email }, papeis: [] } ]`.

`PUT /v1/projetos/{projetoId}/participacoes/{usuarioId}`

- **Entrada:** `{ papeis: [ "dev", "product_owner" ] }` — acumuláveis.
- **Saída `200`:** a participação resultante. Vale imediatamente.
- **`422`** papel fora do catálogo fechado.
- **Efeito no canal de eventos:** as inscrições da pessoa naquele projeto são
  invalidadas na mesma transação. "Vale imediatamente" precisa valer também para
  o que já está aberto, ou a perda de papel só surtiria efeito na expiração do
  token. A implementação do canal chega no épico seguinte; deixe o ponto de
  extensão pronto e chamado.

`DELETE /v1/projetos/{projetoId}/participacoes/{usuarioId}`

- **Saída `204`.**
- **Efeito obrigatório na mesma transação:** toda tarefa assumida pela pessoa no
  projeto volta a `AGUARDANDO_TOMADA` na etapa em que está, com evento
  `TAREFA_DEVOLVIDA` de ator nulo, e o registro de quem havia assumido é
  preservado no log.
- Cada devolução gera publicação como qualquer outra mudança de estado.
- **As sessões de tempo real da pessoa naquele projeto são derrubadas na mesma
  transação** — mesma observação sobre o ponto de extensão.

A permissão de desbloqueio pertence aos papéis de administração do projeto e de
Product Owner; nenhum papel novo é criado para ela.

#### Guia técnico — pontos de atenção

- **A devolução na remoção é obrigatória e transacional.** Fazê-la depois, em
  rotina separada, deixa uma janela em que a tarefa está presa a quem já não
  participa.
- **O evento de devolução automática não tem ator**, porque não houve pessoa
  agindo — e o log continua registrando quem havia assumido.
- **Papéis são acumuláveis**, e a autorização é a união das permissões.
- **O catálogo é fechado**: papel desconhecido é recusa, nunca criação implícita.
- **Nada disso concede alcance a outro projeto.** A participação é sempre por
  projeto.
- **Deixe os pontos de invalidação de sessão chamados desde já**, mesmo vazios: é
  o que impede que o épico do canal precise reabrir esta rota.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Papel atribuído vale imediatamente na requisição seguinte | atribuir e exercer a permissão sem novo login |
| 2 | Papéis acumuláveis somam permissões | sujeito com dois papéis exerce as duas capacidades |
| 3 | Papel fora do catálogo é recusado com `422` | requisição com papel inventado |
| 4 | Remover participação devolve ao pool todas as tarefas assumidas pela pessoa | leitura das tarefas após a remoção |
| 5 | O log preserva quem havia assumido, e o evento de devolução não tem ator | leitura do histórico |
| 6 | A devolução e a remoção acontecem na mesma transação | falha injetada não deixa estado parcial |
| 7 | A permissão de desbloqueio existe nos papéis previstos, sem papel novo | inspeção do catálogo e da matriz |
| 8 | Participante sem permissão de configuração recebe `403` nas três rotas | requisição por papel comum |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
