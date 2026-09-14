---
id: SDR-005
type: SDR
status: accepted
date: 2026-09-14
supersedes: —
superseded-by: —
---

# SDR-005 — Substituição de fluxo serializada por bloqueio pessimista do projeto

## Decisão

`PUT /v1/projetos/{projetoId}/etapas` toma **bloqueio pessimista sobre a linha de
`projeto`** (`SELECT ... FOR UPDATE`, `LockModeType.PESSIMISTIC_WRITE`) como
primeira operação da transação, antes de ler o fluxo vigente. A leitura do
conjunto de etapas, as recusas de RN-001 e RN-020, o deslocamento para a faixa de
trabalho e a reatribuição de ordens acontecem todos sob esse bloqueio. Duas
requisições concorrentes ao mesmo projeto são **serializadas**: a segunda lê o
fluxo já substituído pela primeira e decide sobre ele.

A rota **não** ganha estado de origem declarado e **não** devolve `409`. Não há
`@Version` em `Etapa`. Do lado do cliente nada muda: o contrato de entrada e de
saída permanece o que `contracts/sessao-e-projetos.md` já descreve.

## Motivação

ACH-05 da revisão de TASK-02.2 mostrou que a rota não tinha controle algum de
concorrência: dois `PUT` simultâneos liam o mesmo fluxo vigente, e o segundo
calculava sobre um conjunto já obsoleto. A etapa criada pelo primeiro não está no
conjunto do segundo, logo não é arquivada nem reordenada — perda silenciosa da
escrita do primeiro, ou colisão com `etapa_projeto_ordem_unico` e `500`
intermitente. A transação garante atomicidade; isolamento de decisão, não.

**Por que SDR-002 não se aplica aqui.** O mecanismo daquela decisão depende de o
solicitante declarar o estado de que parte, e ele funciona porque a unidade de
escrita é **uma linha que já existe** — a tarefa — cuja versão o cliente carrega.
Nesta rota a unidade é o **conjunto**, e o dano concorrente se manifesta por
**ausência**: a etapa que o outro criou não está no corpo de quem perdeu. Versão
otimista em `Etapa` não detecta inserção alheia — não há linha sobre a qual
conflitar. Só o bloqueio do agregado resolve, e o agregado é o projeto.

**Por que a Alternativa 2 de SDR-002 foi recusada lá e aceita aqui.** Lá o
argumento era transação longa segurando linha em board colaborativo, com taxa de
conflito baixa e custo de conflito igual a uma releitura. Aqui a operação é rara,
administrativa, exige `CONFIGURAR`, dura o que dura uma substituição de conjunto
pequeno (teto de 100 etapas, `Etapa.MAXIMO_DE_ETAPAS`) e trava **um único
projeto**. O perfil é o oposto do que motivou a recusa.

**Restrições consideradas:**
- Os cenários congelados de RF-017 — SCN-017.1, .2 e .3 — não descrevem disputa,
  e a suíte congelada envia o corpo sem campo de origem ou de versão. Exigir campo
  novo quebraria a suíte e seria escopo novo, que volta ao `/prd` e não se decide
  aqui.
- A linha bloqueada é a mesma que SDR-004 já incrementa para gerar o `seq`. As
  duas serializações compartilham o ponto de contenção em vez de criar um segundo,
  e a ordem de aquisição é idêntica — não há ciclo possível entre elas.
- `backend/java/database.md` §5 desaconselha bloqueio pessimista em caminho
  quente. Esta rota não é caminho quente, e o desvio fica declarado aqui.

## Consequências

**Positivas:**
- A última substituição vence inteira e explicitamente, em vez de vencer por
  metade. Não há perda silenciosa nem `500` intermitente.
- Nada muda no cliente e nada muda na suíte congelada.
- O deslocamento para a faixa de trabalho deixa de precisar de qualquer suposição
  sobre simultaneidade: sob o bloqueio ele é o único escritor daquele projeto.

**Negativas / trade-offs:**
- Duas pessoas configurando o mesmo projeto ao mesmo tempo: a segunda sobrescreve
  a primeira sem aviso. É perda de escrita **decidida**, e não acidental — a
  alternativa que avisaria exige campo de origem no contrato, que é escopo do
  `/prd`. Fica como gatilho de reabertura: se o produto passar a querer aviso, a
  emenda é ao PRD e esta decisão é revista.
- Requisição que espera o bloqueio pode estourar tempo sob contenção patológica.

  **Emenda de 2026-09-14 (ACH-01 da reexecução de TASK-02.2).** A redação
  original dizia que o tempo de espera era limitado pelo `timeout` de transação
  já vigente. **Não havia `timeout` de transação vigente algum** — nem
  `lock_timeout`, nem `statement_timeout`, nem
  `idle_in_transaction_session_timeout`, nem configuração de pool. A frase
  descrevia uma garantia inexistente, e era ela que tornava aceitável a linha
  acima: sem teto, bloqueio não é serialização, é contenção convertida em
  indisponibilidade — cada requisição em espera retém conexão do pool, o pool
  acaba, e `db` está no grupo de readiness, de modo que a instância sai do
  tráfego. Alcançável por um único sujeito autenticado com `CONFIGURAR`.

  O teto passa a ser **decidido aqui e declarado em três camadas**:
  `lock_timeout` de 5 s na sessão (quanto se espera para entrar),
  `@Transactional(timeout = 10)` na rota (quanto se pode segurar depois de
  entrar) e `connection-timeout` de 5 s no pool (quanto se espera por conexão).
  Espera esgotada responde `503` com `Retry-After`, e não `500`: nada no pedido
  está errado, e o teto que acabou de nascer não pode aparecer como defeito.

  O teto **não** vai no hint `jakarta.persistence.lock.timeout`: o dialeto
  PostgreSQL do Hibernate só traduz espera zero (`FOR UPDATE NOWAIT`), e valor
  positivo seria um teto que o código afirma ter e o banco não aplica.

  `statement_timeout` fica **fora**, declaradamente: alcança toda consulta do
  sistema e não há medição para escolher o número. Pendente de medição, não
  esquecido.

**Downstream afetado:**
- TechSpec Seções 2 e 5; `contracts/sessao-e-projetos.md` (nota de concorrência).
- `EtapaService.substituirFluxo` e `ProjetoRepository` (consulta com `@Lock`).
  **Cumprido com desvio de porta:** o bloqueio ficou em
  `SubstituicaoDeFluxo.bloquearProjeto`, fragmento que `EtapaService` já possui,
  porque `EtapaServiceTest` é suíte congelada e fixa `new
  EtapaService(EtapaRepositorio)`. A decisão é cumprida inteira; muda a porta.
- Configuração: `application.yml` — os três tetos acima. `TratadorDeErro` — o
  `503` da espera esgotada.
- `/tests`: verificação além dos cenários — dois `PUT` concorrentes sobre o mesmo
  projeto terminam com o fluxo do segundo, íntegro, e sem `500`.
  **Entregue, recusada na reexecução e corrigida em 2026-09-14:**
  `SubstituicaoDeFluxoConcorrenteIT` não ficava vermelha se o bloqueio fosse
  removido (ACH-02 a ACH-04). O predicado passou a ser o status **`200` exato**, e
  a assimetria está medida nas duas direções que importam — sem o bloqueio, e com
  o bloqueio depois da leitura do fluxo vigente. Item que permanece: a espera
  esgotada tem código próprio e ainda não tem verificação que o alcance.

## Emenda de 2026-09-14 (ACH-11 da reexecução de TASK-02.2) — o corpo que traz `id`

A decisão acima descreve o caso em que os corpos concorrentes criam etapas novas.
**Falta o outro:** duas requisições que carregam os `id` do fluxo que ambas leram.
Serializadas, a segunda encontra um fluxo cujas etapas foram arquivadas pela
primeira, e `exigirQueOsIdentificadoresSejamDoFluxo` a recusa com `422
etapa-fora-do-fluxo`.

**Fica decidido que esse é o desfecho pretendido, e não um efeito colateral.** É a
mesma escolha que a decisão principal já fez por outro caminho: a última
configuração vence inteira, e quem decidiu sobre um fluxo que já não existe não
tem como ter a intenção respeitada — preservar os `id` que ela nomeia significaria
desarquivar etapas que a primeira requisição removeu de propósito. O `422` diz
exatamente isso, e o `detail` já manda recarregar.

Note-se o que a decisão **não** é: não é "conflito", e por isso não é `409`. Nada
no estado do projeto colide com o pedido; o pedido é que se refere a coisas que
deixaram de existir. `409` convidaria o cliente a retentar o mesmo corpo, que
falharia de novo pela mesma razão.

Downstream: `/tests` — o caminho concorrente **com `id`** precisa de verificação
própria, que fixe o `422` e o slug. **Entregue em 2026-09-14, com a premissa
corrigida:** dois corpos que carregam *os mesmos* `id` são ambos válidos, porque
nada foi arquivado entre um e outro. O `422` exige que a primeira requisição
**reduza** o fluxo, e é assim que `SubstituicaoDeFluxoConcorrenteIT` o alcança.
Sob concorrência o par de status não é fixado — qual chega primeiro não é
propriedade do sistema —, e o que se assere é que `409` e `5xx` são impossíveis.

## Emenda de 2026-09-14 (ACH-08 da reexecução de TASK-02.2) — permissão resolvida fora da transação de escrita

`EtapaController` resolve a permissão de `CONFIGURAR` antes de chamar o serviço, e
portanto **fora** da transação que escreve. Entre a decisão e a escrita há janela
em que a participação pode ser revogada: a substituição grava com uma permissão
que já não vale.

**Fica decidido manter assim, e a razão não é custo de implementação.** Mover a
resolução para dentro da transação fecharia esta janela e abriria outra igual do
lado de fora — a permissão continuaria sendo lida uma vez, só que mais tarde —, e
o único desenho que a fecha de verdade é reavaliar a permissão no commit, contra a
linha de participação travada. Isso põe a tabela de participação dentro da seção
crítica de toda escrita do sistema, e não apenas desta rota.

O que torna a janela aceitável **nesta** rota, e o que a tornaria inaceitável em
outra, está nomeado para que a próxima decisão não precise redescobri-lo: a janela
é de milissegundos; a operação é administrativa e rara; quem a executa tinha a
permissão há instantes, de modo que o cenário exige revogação concorrente ao uso; e
o dano é uma configuração de fluxo, que a pessoa seguinte com a permissão desfaz
substituindo o fluxo de novo. Nenhuma das quatro vale para uma rota que mova
dinheiro, conceda acesso ou apague série de tempo — e é aí que esta decisão deve
ser reaberta em vez de citada.

**Consequência registrada, não resolvida.** Não há downstream de código; há o
critério acima.

## Alternativas Consideradas

### Alternativa 1 — `@Version` em `Etapa`, com `409` na divergência
**Descartada porque:** não cobre o caso. O dano é a etapa **criada** pelo outro,
que não está no conjunto de quem perdeu e portanto não tem versão a conferir.
Cobriria apenas renomeação concorrente da mesma etapa, que é o caso menos grave, e
deixaria a perda silenciosa exatamente onde ela está.

### Alternativa 2 — Versão do fluxo declarada no corpo, ao modo de SDR-002
**Descartada porque:** exige campo novo em contrato de escrita que os cenários
congelados já exercitam sem ele — quebraria a suíte congelada — e institui
comportamento de produto (avisar quem perdeu a corrida de configuração) que o PRD
não pede. Decidir isso aqui seria o `/techspec` decidindo *o quê*.

### Alternativa 3 — Confiar no índice único e traduzir a violação para `409`
**Descartada porque:** o índice só acusa colisão de ordem. A perda silenciosa —
etapa alheia não arquivada, ordens coerentes entre si — passa pelo índice sem
violar nada. Traduzir a violação continua valendo como rede (é o que TASK-02.2
implementou em `TratadorDeErro`), mas como rede, não como mecanismo.
