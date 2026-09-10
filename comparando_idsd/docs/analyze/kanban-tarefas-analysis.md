# Análise de consistência — kanban-tarefas
_Data: 2026-09-10 | Modo: --pre-implement_
_Artefatos analisados: intent, classificação, contexto, shape brief, solução, design (brief + screen-map + protótipos), PRD v1.5, TechSpec v1.7 e seus anexos, plano de tasks e arquivos de task, plano de verificação, ADRs, BDRs, SDRs e DDRs_

> Esta análise não corrige nada. Ela localiza a divergência e diz qual artefato
> é o dono da correção. Emendar aqui esconderia o defeito no lugar errado.

Nona execução, terceira em modo `--pre-implement`. A oitava reprovou por um
bloqueante — INC-22, escopo órfão — e registrou uma divergência de transcrição
fora da tabela de achados. Os dois foram tratados no mesmo dia.

O demandante decidiu **instituir as duas sinalizações do fluxo não configurado
como comportamento requerido**, que era uma das duas saídas legítimas, e pediu
que a marca durável ganhasse cenário congelado. O escopo desta execução é essa
emenda e sua propagação.

Aprova. Zero achados, zero waiver.

---

## Artefatos e versões

| Artefato | Caminho | Última alteração | Considerado |
| --- | --- | --- | --- |
| Intent | docs/intent/kanban-tarefas-intent.md | 2026-09-04 15:13 | sim |
| Contexto | docs/context/kanban-tarefas-context.md | 2026-09-09 (emendado) | sim |
| Shape Brief | docs/shape/kanban-tarefas-brief.md | 2026-09-04 16:23 | sim |
| Solução | docs/solution/kanban-tarefas-solution.md | 2026-09-09 10:22 | sim |
| Design — brief | docs/design/kanban-tarefas-design-brief.md | 2026-09-10 (TL-11) | sim |
| Design — screen-map | docs/design/kanban-tarefas/screen-map.md | 2026-09-10 (INC-22) | sim |
| Design — protótipos | docs/design/kanban-tarefas/prototypes/ | 2026-09-10 (TL-11, TL-02) | sim |
| PRD | docs/prd/kanban-tarefas-prd.md | 2026-09-10 (v1.5) | sim |
| TechSpec | docs/techspec/kanban-tarefas-techspec.md | 2026-09-10 (v1.7) | sim |
| Plano de tasks e arquivos de task | docs/tasks/kanban-tarefas-tasks.md e docs/tasks/kanban-tarefas/ | 2026-09-10 | sim |
| Plano de verificação | docs/tests/kanban-tarefas-verificacao.md | 2026-09-10 (v1.2) | sim |
| Decision Records | docs/decisions/ (13 ADR, 2 BDR, 4 SDR, 7 DDR) | 2026-09-10 (SDR-003 recontado) | sim |
| Coleção `infra/docker` | requirements/guidelines/infra/docker/ | 2026-09-09 15:19 | sim |

- **Artefato desatualizado em relação a montante:** nenhum pela data

Não há inversão de frescor. A emenda de hoje é a menor que esta cadeia já
absorveu — um cenário novo, uma regra estendida, um requisito com a regra
acrescentada às aplicáveis, um campo de saída — e foi propagada numa passagem
por PRD, TechSpec, contrato, design, plano de tasks, arquivo de task e plano de
verificação.

A varredura desta execução repetiu deliberadamente a da oitava, que foi a que
produziu resultado: partir de cada superfície do protótipo e perguntar qual
regra a obriga. Repetir a varredura que acabou de achar algo é o teste mais
barato de que a correção fechou o buraco em vez de deslocá-lo. Ela devolveu zero
desta vez, e a razão é verificável: as duas superfícies que estavam órfãs
aparecem agora nomeadas dentro da regra que declara o custo que elas pagam.

A segunda varredura é a que valia a pena depois de um achado de escopo órfão:
seguir o **campo novo** de ponta a ponta. Ele sai do contrato, entra no cenário
congelado, aparece no arquivo de task com a proibição explícita de o cliente
inferi-lo, e chega à tela pelo screen-map. Não há ponto em que ele exista sem
origem nem origem sem realização.

---

## Cobertura da cadeia

Cada elo precisa ser total nas duas direções. Elemento sem origem é escopo que
ninguém pediu; elemento sem destino é requisito que ninguém vai entregar.

| Elo | Órfãos a montante | Órfãos a jusante | Situação |
| --- | --- | --- | --- |
| direção → requisito | 0 | 0 | ok |
| regra de borda → cenário | 0 | 0 | ok |
| tela/estado → requisito | 0 | 0 | ok |
| requisito → decisão técnica | 0 | 0 | ok |
| RNF → estratégia de verificação | 0 | 0 | ok |

**direção → requisito.** Fecha. Nada mudou de direção nesta emenda: ela
instituiu como requisito comportamento que já estava desenhado, e não abriu
frente nova.

**tela/estado → requisito.** Fecha, e era o único elo aberto. Os dois órfãos de
ontem têm origem agora, e a origem está na regra certa e não numa regra
inventada para acomodá-los: é RN-038 que declara o custo dos dois passos
obrigatórios, e é ela que passa a obrigar as duas sinalizações que o pagam. O
requisito da lista de projetos ganhou a regra entre as aplicáveis, a descrição
que a realiza e a origem no protótipo correspondente; o requisito de criação de
projeto ganhou, na descrição, a frase que ancora o desfecho bem-sucedido.

**regra de borda → cenário.** As dez bordas do `/solution` seguem cobertas.
Nenhuma borda criada nem revogada.

**requisito → decisão técnica.** Fecha nas duas direções. O campo novo na saída
da listagem tem requisito, regra e cenário; a decisão de derivá-lo no servidor
por existência, em vez de devolver a coleção de etapas, está registrada com o
motivo na tabela de revisões da TechSpec e repetida como ponto de atenção no
arquivo de task, que é onde quem implementa vai lê-la.

**RNF → estratégia de verificação.** O cenário novo é de `integração` e entrou
na contagem em todos os lugares que a carregam — PRD, TechSpec, plano de tasks,
plano de verificação e o SDR que registra a escolha de ferramenta. A busca pela
contagem anterior devolve apenas ocorrências em notas históricas datadas, que
descrevem a emenda passada e não devem mudar. O universo de onze telas não foi
tocado: a emenda não criou tela.

---

## Achados

**Nenhum.** Zero achados, zero bloqueantes, zero waiver.

---

**Tipos:**

- **contradição** — dois artefatos afirmam coisas incompatíveis. O mais grave,
  porque cada um parece coerente lido isoladamente.
- **lacuna** — algo exigido a montante não aparece a jusante.
- **ambiguidade** — texto que duas pessoas competentes leem de formas
  diferentes. Vira defeito na implementação, não aqui.
- **duplicação** — a mesma decisão escrita em dois lugares, livre para divergir.
- **escopo órfão** — existe a jusante sem origem a montante.

---

## Verificação dos achados das execuções anteriores

Os vinte e dois achados das oito execuções anteriores foram verificados no
artefato dono, em disco. Todos fechados. O de ontem e o item fora da tabela:

- **INC-22.** RN-038 passou a obrigar as duas sinalizações, nomeando cada uma e
  dizendo o que se perde sem elas. O requisito da lista de projetos lista a
  regra entre as aplicáveis, descreve a marca e aponta a origem no protótipo. O
  cenário congelado novo verifica a marca nos dois sentidos — projeto sem etapa
  marcado, projeto configurado não marcado —, que é a forma que impede o teste
  de passar marcando tudo. O requisito de criação de projeto ancora o estado de
  sucesso. A reconfirmação do gate de spec registra a decisão de negócio e nomeia
  a alternativa recusada.
- **Divergência de transcrição no arquivo de task de frontend.** O campo do
  corpo de criação está grafado da mesma forma nos quatro lugares em que aparece
  na cadeia. A busca pela grafia divergente devolve zero em todo o `docs/`.

Dois pontos foram reconferidos, pela mesma razão de sempre — são os mais
expostos a reabertura silenciosa:

- **Cenário congelado preexistente.** Nenhum ID, redação ou tipo foi alterado.
  A emenda é estritamente aditiva, e a tabela de emendas do PRD registra a linha
  nova com o motivo.
- **RN-014.** O campo acrescentado à saída da listagem é booleano sobre o
  projeto, não sobre pessoa. Nenhuma superfície nova agrega tempo por pessoa.

---

## Procedência e hipóteses

| Verificação | Resultado |
| --- | --- |
| Inferências do agente pendentes de confirmação | 0 |
| Hipóteses sem experimento, critério ou prazo | 0 |
| Suposições operacionais vencidas | 0 |

A contagem volta a zero. As duas sinalizações deixaram de ser escolha da etapa
de design apresentada com aparência de decidido e passaram a ser decisão do
demandante, registrada na reconfirmação do gate com a alternativa recusada
nomeada — retirá-las e aceitar o custo de RN-038 exatamente como estava escrito.

Vale registrar a assimetria que a decisão deixou, porque ela é deliberada e não
descuido: só a marca na relação de projetos ganhou cenário congelado. O estado
de sucesso da criação tem requisito, mas é verificado por critério de aceite de
task e não pela suíte. A razão está escrita no PRD — a marca é a sinalização
durável, a única que ainda alcança quem precisa agir depois que o desfecho da
criação saiu da tela, e é ela que precisa sobreviver a uma refatoração de tela
sem que ninguém perceba. Um cenário para o estado de sucesso seria `e2e`, e
`e2e` é a única classe cujo custo esta cadeia vem contendo desde o `/techspec`.

H-03 — entrevista direta com um desenvolvedor e um gestor de outro time —
permanece aberta com critério e prazo declarados, e por isso fora da contagem.

---

## Veredicto

- **GATE-CONSISTENCIA:** aprovado
- **Bloqueantes em aberto:** 0
- **Aprovado por:** agente `/analyze`, 2026-09-10 — sem waiver

Nada mais bloqueia o `/implement` do lado da cadeia de especificação. A pendência
que resta é de outra natureza e está declarada no plano de verificação: a
execução Red é previsão e não medição, porque o projeto nasce na primeira task.
Substituí-la por número medido é a primeira obrigação da primeira task fechada —
número previsto que ninguém troca por número medido é a forma mais discreta de
um gate virar formulário.

Duas execuções seguidas acharam a mesma classe de defeito com o sinal trocado:
comportamento de produto instituído pela etapa que desenha, ao lado do que o
demandante decidiu e com a mesma aparência de decidido. A lição que fica não é
sobre este produto: a mitigação que o agente inventa para pagar um custo que o
demandante aceitou é, ela própria, escopo, e precisa ser perguntada em vez de
entrar pelo protótipo.

---

## Fora deste artefato — regras negativas

- **Correção de qualquer artefato** — pertence ao dono declarado no achado.
- **Requisito, regra ou cenário novo** — pertence ao `/prd`.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Task ou estimativa** — pertence ao `/tasks`.
- **Reabertura de direção** — pertence ao `/shape`; achado que questiona a
  direção escolhida é devolução, não decisão tomada aqui.
