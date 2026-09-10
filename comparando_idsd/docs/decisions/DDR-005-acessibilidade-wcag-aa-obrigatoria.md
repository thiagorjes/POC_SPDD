---
id: DDR-005
type: DDR
status: accepted
date: 2026-09-04
supersedes: DDR-003
superseded-by: —
---

# DDR-005 — Acessibilidade WCAG 2.1 AA obrigatória e feedback realinhado ao design system

## Decisão

**WCAG 2.1 AA é obrigatório** em `kanban-tarefas`, e não dívida a endereçar
depois. Decorrem daí três exigências que valem como contrato de design:

1. **Mover tarefa entre etapas tem equivalente por teclado.** O caminho
   alternativo ao arrasto previsto no DDR-002 deixa de ser conveniência e passa
   a ser obrigação: sem ele, a operação central do produto fica inacessível.
2. **Mudança de estado é anunciada sem interromper a leitura em andamento** —
   tarefa que chega aguardando tomada, impedimento aberto, e recusa por ação
   concorrente.
3. **Ícone sem texto exige rótulo acessível**, foco visível em todo elemento
   interativo, e contraste verificado inclusive nas cores que a coleção ainda
   não tokenizou.

Os padrões de feedback do DDR-003 são mantidos em substância e realinhados aos
componentes da coleção: atenção obrigatória em diálogo, informação de baixa
relevância em aviso efêmero, espera de duração variável com esqueleto de
conteúdo, espera curta com indicador de progresso no próprio controle.

## Motivação

A Fase 0 do `/design` encontrou o DDR-003 dispensando acessibilidade formal por
se tratar de "sistema interno/POC", o Design Brief v1.0 declarando WCAG AA, e o
design system da coleção declarando WCAG 2.1 AA como alvo com garantias já
implementadas. Três fontes, três respostas.

**Problema que resolve:**
Fecha a divergência no sentido mais defensável. Com a adoção do design system
(DDR-004), a maior parte do custo de acessibilidade já vem paga: foco visível,
navegação por teclado via Radix, rótulo obrigatório em ícone sem texto e estados
de carregamento anunciados são propriedades dos primitivos herdados.

**Restrições consideradas:**
- A premissa de "sistema interno" que sustentava o DDR-003 continua verdadeira;
  o que mudou foi o custo, que caiu com DDR-004.
- O board é operado por arrasto, a interação mais hostil a teclado e a leitor de
  tela que existe — e é a operação central do produto.
- A direção aprovada aposta que o board se torne o lugar de trabalho diário
  (H-01). Ferramenta de uso diário prolongado inacessível a um integrante do
  time exclui essa pessoa do processo inteiro.

## Consequências

**Positivas:**
- O gate de acessibilidade passa a ter critério objetivo contra o qual o
  `/code-review` verifica.
- A dívida que o DDR-003 reconhecia deixa de ser criada.

**Negativas / trade-offs:**
- O equivalente por teclado do arrasto é trabalho real de design e de
  implementação, e não vem pronto na coleção.
- Verificar contraste das cores não tokenizadas da coleção pode exigir alterar a
  biblioteca compartilhada, que governa outros sistemas.

**Downstream afetado:**
- Design Brief seção 7, e todo protótipo gerado.
- DDR-002, cujo caminho alternativo ao arrasto passa a ser obrigatório.
- `/prd`: envelope de acessibilidade vira requisito não funcional verificável.
- `/tests` e `/code-review`: passam a ter o que verificar.

## Alternativas Consideradas

### Alternativa 1 — Manter o DDR-003 e dispensar WCAG
**Descartada porque:** a justificativa original era custo, e o custo caiu com a
adoção do design system. Manter a dispensa preservaria a dívida sem economia
correspondente.

### Alternativa 2 — WCAG AA apenas nas telas de leitura, dispensando o board
**Descartada porque:** o board é o produto. Acessibilidade que exclui a operação
central não é acessibilidade parcial, é ausência dela com aparência de
conformidade.
