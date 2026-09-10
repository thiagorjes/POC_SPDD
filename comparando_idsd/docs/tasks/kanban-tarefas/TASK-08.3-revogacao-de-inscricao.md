# TASK-08.3 — Revogação de inscrição na mudança e na remoção de participação

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-08.2
- **Cenários cobertos:** SCN-019.4
- **Origem:** RF-019, RN-015, RNF-004

#### Contexto

Este é o fecho da única brecha estrutural do canal: a inscrição sobrevive à
mudança que deveria encerrá-la. Sem esta task, remover alguém de um projeto
revoga o acesso pelas rotas e não revoga pelo canal — a pessoa continua vendo o
board mudar em tempo real. É a diferença entre autorização vigente e autorização
lembrada.

#### O que deve ser feito

- [ ] Invalidar as inscrições afetadas **na mesma transação** que altera papéis
      de participação.
- [ ] Derrubar as sessões e as inscrições **na mesma transação** que remove a
      participação.
- [ ] Aplicar a invalidação em todas as instâncias, e não apenas na que atendeu
      a requisição.
- [ ] Manter a fila pessoal coerente: o que deixou de ser visível para de chegar.
- [ ] Não exigir reconexão do cliente para que a revogação tenha efeito.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/tempo_real/RevogacaoDeInscricao.java` | criar | invalidação e derrubada |
| `backend/src/main/java/<pkg>/internal/tempo_real/OuvinteDeEventos.java` | alterar | tratamento do aviso de revogação |
| `backend/src/main/java/<pkg>/internal/participacao/ParticipacaoService.java` | alterar | acionamento na mesma transação |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Dois gatilhos, com efeitos diferentes:

- **Alteração de papéis** (`PUT /v1/projetos/{projetoId}/participacoes/{usuarioId}`):
  invalida as inscrições do sujeito naquele projeto **na mesma transação** da
  alteração. Se os papéis novos ainda dão acesso, o cliente se reinscreve e
  segue; se não dão, a reinscrição é recusada pela regra da TASK-08.1.
- **Remoção de participação**
  (`DELETE /v1/projetos/{projetoId}/participacoes/{usuarioId}`, resposta `204`):
  além da devolução obrigatória das tarefas assumidas, **derruba as sessões e as
  inscrições do sujeito naquele projeto, na mesma transação**.

Como a revogação alcança as outras instâncias: pelo mesmo canal já usado para os
eventos, com um tipo próprio que carrega projeto e sujeito. Cada instância aplica
a invalidação sobre os seus assinantes locais. Nada de comunicação direta entre
instâncias.

"Na mesma transação" é literal: se a alteração de participação for revertida, a
revogação não pode ter acontecido; se ela for confirmada, o efeito no canal não
pode ficar para depois.

A fila pessoal acompanha — tarefa de projeto ao qual o sujeito perdeu acesso
deixa de chegar em `/user/queue/fila`.

#### Guia técnico — pontos de atenção

- **Não deixe a revogação para o próximo evento nem para a reconexão.** É
  exatamente a janela que esta task existe para fechar.
- **Não revogue só na instância que atendeu a requisição.** A sessão da pessoa
  removida está, com grande probabilidade, em outra.
- **Não confunda alterar papéis com remover participação.** O primeiro invalida
  a inscrição, o segundo derruba a sessão e ainda devolve as tarefas.
- **Não emita evento de board para a pessoa removida** como "aviso" da remoção —
  isso entrega, no último instante, o dado que se está revogando.
- **Não esqueça a fila pessoal**, que é o destino mais fácil de deixar para trás
  por não ser endereçado por projeto.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Alterar papéis invalida a inscrição do sujeito no projeto sem esperar reconexão | sessão aberta durante a alteração |
| 2 | Remover participação derruba a sessão do sujeito naquele projeto | sessão aberta durante a remoção |
| 3 | A revogação alcança sessão conectada em outra instância | teste com duas instâncias |
| 4 | Alteração revertida não revoga inscrição nenhuma | transação forçada a falhar |
| 5 | Após a remoção, nenhum evento do projeto chega ao sujeito | escrita no projeto logo após a remoção |
| 6 | A fila pessoal deixa de trazer tarefas do projeto perdido | leitura da fila após a remoção |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
