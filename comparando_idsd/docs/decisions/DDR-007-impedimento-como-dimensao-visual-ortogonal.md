---
id: DDR-007
type: DDR
status: accepted
date: 2026-09-09
supersedes: —
superseded-by: —
---

# DDR-007 — Impedimento como dimensão visual ortogonal, que só desabilita o que o contrato recusa

> **Revisão de 2026-09-09 (achado INC-14).** A decisão central não mudou: impedimento
> continua sendo dimensão própria, ortogonal à etapa e à condição. Mudou o que está a
> jusante dela. A emenda v1.2 do PRD estendeu RN-011, que passou a recusar **duas**
> operações — concluir e encerrar sem conclusão —, com cenário congelado próprio para a
> segunda (SCN-012.4). O texto original deste DR dizia "exceto concluir" e "só a conclusão
> é recusada", redação correta quando foi escrita e falsa a partir da v1.2. Emendado no
> lugar em vez de superado: superar registraria uma decisão nova onde houve apenas
> conformação a uma regra decidida em outro artefato.

## Decisão

A marca de impedimento é renderizada como **dimensão própria**, ao lado da etapa
e da condição de trabalho, e nunca no lugar de nenhuma delas. Em toda tela:

1. **Não existe a condição "impedida".** O que a interface exibe é a condição de
   trabalho — aguardando tomada, em curso, concluída, encerrada sem conclusão — e,
   separadamente, a marca de impedimento quando há impedimento sem desfecho.
2. **A marca desabilita exatamente duas ações: concluir e encerrar sem conclusão.**
   Mover, devolver, assumir, renomear e remover participação continuam oferecidos
   com impedimento aberto. As duas recusas dizem por quê, e a razão é a mesma nas
   duas: tirar a tarefa do fluxo com o intervalo aberto exigiria inventar um
   desfecho que ninguém afirmou (B-04). Nenhuma tela oferece ação que o contrato
   recuse — oferecer e ser recusado pelo serviço é pior que não oferecer.
3. **O contador de impedimento corre em série própria**, em paralelo aos de
   permanência e de espera de tomada. Nenhuma tela apresenta soma nem descreve as
   outras contagens como "travadas".
4. **Só o registro do desfecho apaga a marca.** Nenhuma outra interação a remove,
   e nenhuma tela sugere que remova.

## Motivação

O protótipo TL-04 desabilitava "mover de etapa" em cartão com impedimento aberto,
com nota explicando que a tarefa fica congelada até o desfecho. A nota
materializava a recomendação que o agente havia feito em Q-01 e que **o
demandante rejeitou** em 2026-09-09: mover é permitido para facilitar a operação,
e o custo de interpretabilidade do agregado foi transferido para a medição, que
apura as três séries separadamente.

**Problema que resolve:**
O protótipo é insumo de implementação. Enquanto contradisse o contrato congelado,
qualquer pessoa que o tomasse como referência reintroduziria o comportamento
recusado — e o faria de boa-fé, porque a nota do próprio protótipo justificava a
restrição com um argumento que soa correto isoladamente. Foi o achado INC-09 da
análise de consistência.

**Restrições consideradas:**
- RN-002, RN-003 e RN-032 do PRD v1.2: impedimento é a terceira dimensão, coexiste
  com qualquer condição não terminal e só o desfecho o apaga.
- RN-009: a movimentação não encerra nem reinicia a contagem de impedimento.
- RN-033: a tomada é aceita com impedimento aberto.
- RN-011 (PRD v1.2): concluir e encerrar sem conclusão são as duas — e as únicas —
  operações que a marca impede.
- SCN-012.4: a recusa do encerramento é cenário congelado, e precisa de estado
  correspondente na interface.
- RN-008: as três séries nunca são somadas — o que proíbe descrever a permanência
  como "travada" enquanto o impedimento corre.
- QD-01: o papel semântico de impedimento é novo na coleção `frontend/nextjs` e
  ainda precisa ser acrescentado a ela antes do uso.

## Consequências

**Positivas:**
- O protótipo deixa de ser o único artefato da cadeia que contradiz o contrato
  congelado, que era a situação registrada no fechamento do `/techspec` v1.2.
- A regra fica legível na própria interface: a ficha de TL-04 exibe as três
  dimensões em campos separados, o que torna difícil alguém colapsá-las de novo.
- SCN-007.4 ganha materialização — TL-06 mostra um item de fila com impedimento
  aberto e o botão "Assumir" oferecido.

**Negativas / trade-offs:**
- O cartão fica mais denso: precisa acomodar condição, marca e dois contadores
  simultâneos, e a densidade já está decidida — DM-02 fixou o compacto e
  descartou a variação expandida. O aperto é assumido, não uma pergunta em
  aberto; a saída, se ele se mostrar caro em uso, é hierarquia visual dentro do
  compacto, não reabrir a variação.
- Dois contadores lado a lado no mesmo cartão convidam à leitura somada, que é o
  que RN-008 proíbe. A mitigação é textual e será testada com o time.

**Downstream afetado:**
- Design Brief seções 5, 6.1, 6.2 e 6.3; `screen-map.md`.
- Protótipos TL-03, TL-03b, TL-04 e TL-06.
- `/tasks`: o papel semântico de impedimento continua sendo trabalho na coleção
  compartilhada (QD-01, Q-007 da TechSpec).

## Alternativas Consideradas

### Alternativa 1 — Manter "mover" desabilitado em cartão impedido
**Descartada porque:** contraria decisão explícita do demandante em Q-01 e uma
regra de negócio vigente com cenário congelado (RN-009, SCN-006.3). O argumento
que a sustentava — sobreposição das séries torna o agregado ininterpretável — foi
considerado e recusado por quem decide: a sobreposição é deliberada, e RN-008
existe justamente para impedir que ela seja resolvida por soma.

### Alternativa 2 — Exibir "Impedida" como valor da condição e manter a ação habilitada
**Descartada porque:** corrigiria a ação e preservaria o defeito de fundo. Uma
tarefa pode estar aguardando tomada **e** impedida ao mesmo tempo, e um campo
único obrigaria a interface a escolher qual dos dois estados mentir. É a mesma
razão pela qual `IMPEDIDA` saiu do domínio de `condicao` no modelo de dados.

### Alternativa 3 — Habilitar "mover" sem explicar a mudança no protótipo
**Descartada porque:** a nota anterior tinha um argumento persuasivo, e removê-la
em silêncio deixaria a próxima pessoa livre para reconstruí-lo. A nota nova diz
que a decisão foi tomada, por quem, e contra qual recomendação.
