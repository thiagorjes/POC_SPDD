# SDR-004 — Sequência de eventos gerada no banco e publicador único do broadcast

- **Status:** Aceito
- **Data:** 2026-09-09
- **Supera parcialmente:** ADR-004, no ponto em que atribui o contador de `seq` a cada pod
- **Origem:** comitê de análise da `/techspec` — achado convergente do architect e do database

---

## Contexto

ADR-004 realiza RF-020 e RNF-002 por `LISTEN/NOTIFY`, e usa um número de sequência
por projeto (`seq`) como rede de segurança: o cliente detecta lacuna e resincroniza
por `GET /v1/projetos/{id}/board`. Duas coisas ficaram sem dono no desenho.

**Quem gera o `seq`.** ADR-004 atribui o contador a cada pod. Com duas instâncias
escrevendo no mesmo projeto, dois eventos distintos recebem o mesmo número, ou as
sequências divergem por instância. Duplicata torna a lacuna **indetectável** — quebra
justamente o mecanismo que o ADR nomeia para RNF-002. O bloqueio otimista de SDR-002
não protege: ele é por linha de `tarefa`, e duas transições em tarefas diferentes do
mesmo projeto passam sem conflito, que é exatamente o caso em que `MAX(seq)+1`
colidiria.

**Quem publica o `NOTIFY`.** A TechSpec Seção 5 e o ADR-004 mandam publicar em
`afterCommit` pela porta `EventoBoardPublisher`; o modelo de dados previa um gatilho
no `INSERT` do evento. Dois publicadores gerariam dois broadcasts por mudança.

## Decisão

**1. `seq` é atribuído no banco, dentro da transação de escrita**, por
`UPDATE projeto SET seq_atual = seq_atual + 1 ... RETURNING`, com índice único sobre
`(projeto_id, seq)` em `evento_tarefa`.

**2. O publicador é único: a porta `EventoBoardPublisher`, em `afterCommit`.** Não
há gatilho de `NOTIFY` no esquema.

**3. A janela de perda entre commit e publicação é fechada no servidor, não só no
cliente.** Ao (re)assumir o `LISTEN`, a instância varre `evento_tarefa` por `seq`
acima do último publicado conhecido e retransmite o que faltou.

**4. O cliente tolera lacuna transitória.** A regra deixa de ser "lacuna ⇒ resync
imediato" e passa a ser "lacuna persistente após janela curta ⇒ resync, com jitter".

## Motivação

O item 1 não tem alternativa dentro de ADR-002: sem broker, o único árbitro de ordem
compartilhado entre instâncias é o próprio banco. `SEQUENCE` do PostgreSQL resolveria
a duplicidade e não a lacuna, porque não é transacional — rollback deixaria buraco
permanente e todo cliente resincronizaria para sempre. O contador em linha de
`projeto` reverte com a transação.

O custo é real e é aceito conscientemente: **a atribuição contígua serializa toda
escrita do mesmo projeto**. É contenção por projeto, não global, e o board de um
projeto não é caminho de escrita de alta concorrência. Ela precisa estar escrita
aqui, e não ser descoberta em `/implement`.

O item 2 escolhe entre duas opções que não eram equivalentes. O gatilho tem uma
vantagem que o desenho anterior não reconhecia: o `NOTIFY` seria atômico com o
commit. A porta em `afterCommit` pode perder a publicação se a instância morrer entre
o commit e o envio. Escolheu-se a porta porque ela preserva a trocabilidade que
ADR-004 declara (Redis ou broker em fase futura sem tocar no esquema) — mas a
vantagem que se abre mão obriga o item 3. Sem ele, a escolha seria pior que o gatilho,
não melhor.

O item 4 responde a um efeito de segunda ordem: ordem de atribuição não é ordem de
commit, então um cliente pode ver `n+1` antes de `n` legitimamente. Com 50 sessões no
projeto, "resync imediato" é rajada sincronizada contra o mesmo banco que atende a
escrita — pressão direta sobre RNF-001 causada pelo mecanismo que existe para
protegê-lo.

## Alternativas descartadas

**`evento_tarefa.id` global como `seq`.** Resolveria unicidade e ordem sem contenção
por projeto, mas abre mão da contiguidade: o cliente não consegue distinguir lacuna
real de número consumido por outro projeto, e perde-se a detecção inteira.

**Contador por pod, como estava.** Descartado por inválido em multi-instância, que é
o cenário de RNF-002.

**Confiar só na resincronização client-side.** Deixaria a janela de perda do
`afterCommit` sem cobertura no servidor; a divergência entre instâncias seria
silenciosa até alguém reconectar.

## Consequências

- Escritas do mesmo projeto serializam na atualização do contador. Se algum projeto
  vier a ter concorrência de escrita alta o bastante para isso doer, a reabertura é
  trocar contiguidade por `id` global e mudar a estratégia de detecção do cliente.
- A varredura de retomada do listener é código novo, com teste próprio.
- `EventoBoardPublisher` deixa de ser abstração decorativa: é onde a janela de perda
  é tratada.
