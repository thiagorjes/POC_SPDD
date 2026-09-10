---
id: DDR-006
type: DDR
status: accepted
date: 2026-09-04
supersedes: —
superseded-by: —
---

# DDR-006 — Espera de tomada como estado visível de primeira classe, com fila própria

## Decisão

A **espera de tomada** — o intervalo entre a tarefa chegar a uma etapa e alguém
assumi-la — é tratada na interface como estado de primeira classe, não como
ausência de responsável. Isso se materializa em dois lugares:

1. **No cartão**, dentro do board: a tarefa exibe que aguarda tomada e há quanto
   tempo, de forma permanente enquanto durar a espera.
2. **Em uma tela própria** (`/minha-fila`, TL-06), que reúne tudo o que aguarda
   tomada pelo usuário **em todos os projetos a que ele tem acesso**, sem que
   ele precise abrir board algum.

A espera é contada e exibida separadamente do tempo de trabalho na etapa,
conforme Q-09 da exploração de solução.

## Motivação

O enquadramento E-02 entrou na direção porque o demandante localizou o atraso
nos handoffs: "atraso em review, validação etc., tudo começa tarde". A dor não é
que o trabalho demore, é que ele **não comece**.

**Problema que resolve:**
Sem esta decisão, saber que a vez chegou exigiria abrir cada board e procurar. A
direção aprovada afirma que registrar o estado e avisar o próximo responsável
são a mesma ação — e essa afirmação só se sustenta na interface se o aviso
chegar a quem age sem exigir busca. Como a notificação é interna e não integra
canal externo (restrição rígida da Intent), a fila é o único lugar onde o aviso
efetivamente alcança alguém.

**Restrições consideradas:**
- Notificação apenas interna, não negociável.
- Nada expira nem escala por prazo (B-08): a fila mostra e conta, não aciona.
- A tarefa aguardando tomada não tem responsável nomeado (Q-10): ela fica no
  pool da etapa, o que torna a fila necessariamente uma visão por elegibilidade,
  não por atribuição.
- Tempo agregado por pessoa é proibido por restrição estrutural — a fila mostra
  espera por tarefa, nunca desempenho de quem assume.

## Consequências

**Positivas:**
- H-01, a hipótese de adesão de que a direção depende, ganha o suporte mais
  direto possível: existe um lugar onde a pessoa vê o que espera por ela.
- O tempo de espera acumulado vira evidência observável — é ele que permitirá
  reabrir E-04 (escalonamento com prazo) com dado, em vez de suposição.

**Negativas / trade-offs:**
- Uma tela a mais no escopo, e ela atravessa projetos, o que a torna a única
  visão do produto que não é escopada por projeto — com implicação direta de
  permissão.
- Como não há responsável nomeado, a mesma tarefa aparece na fila de várias
  pessoas ao mesmo tempo, o que torna a recusa por ação concorrente (B-03) uma
  ocorrência esperada e não excepcional.
- Exibir contador contínuo em muitos cartões pode virar ruído visual — registrado
  como QD-03 no Design Brief.

**Downstream afetado:**
- Design Brief seções 4, 5 e 6; protótipos TL-03, TL-03b e TL-06.
- `/prd`: a fila e o cálculo de elegibilidade viram cenário.
- `/techspec`: a visão atravessa projetos e depende da participação do usuário
  em cada um.

## Alternativas Consideradas

### Alternativa 1 — Espera visível apenas no cartão, sem tela dedicada
**Descartada porque:** obriga a varrer os boards para descobrir o que espera por
você. É exatamente o "cruzamento manual" que a Intent quer eliminar, transposto
do e-mail para dentro do produto.

### Alternativa 2 — Raia dedicada a "aguardando tomada" dentro de cada etapa
**Descartada porque:** colide com Q-08, que mantém a raia como agrupamento livre
definido por cada projeto. Fixar semântica de sistema em uma raia contraria o
princípio de que o fluxo acompanha o processo real de cada time.

### Alternativa 3 — Nomear o próximo responsável ao mover a tarefa
**Descartada em Q-10:** aproxima-se do hábito atual de avisar uma pessoa, mas
recria o ponto único de falha quando ela está ausente. Registrada como a
primeira escolha a revisitar caso H-01 seja refutada por falta de adesão.
