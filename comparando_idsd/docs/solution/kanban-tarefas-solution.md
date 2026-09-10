# Exploração de Solução — kanban-tarefas

> D2 divergente, universal — vale para fluxos com e sem tela.
> Responde *como se comporta*, sem nenhuma tecnologia.
> Sem gate próprio: alimenta o `/prd`, que fecha no GATE-SPEC.

- **Shape Brief:** `docs/shape/kanban-tarefas-brief.md`
- **Data:** 2026-09-04

> **Teste de fronteira com o `/techspec`:** se trocar a tecnologia torna a frase
> falsa, a frase é de techspec e não pertence aqui.
> Pertence: "a operação é idempotente e rejeita duplicata em 24h devolvendo o
> resultado anterior". Não pertence: a mesma frase amarrada ao mecanismo de
> armazenamento e ao tempo de vida da chave que a implementaria.

---

## Nota de leitura

A direção aprovada tem uma consequência que atravessa todo este documento:
**registrar o estado e avisar o próximo responsável são a mesma ação**. Não
existe, em nenhum fluxo abaixo, um passo "notificar" separado do passo que muda
o estado. Quando aparecer a expressão *o fluxo passa a exibir*, leia-a como
efeito da transição, nunca como uma segunda operação que alguém precisa lembrar
de executar.

Os fluxos estão na ordem de prioridade das jornadas do brief.

---

## Fluxos de operação

### Fluxo: Registrar avanço da tarefa

- **Ator:** Desenvolvedor
- **Gatilho:** o desenvolvedor terminou o que lhe cabia na etapa atual
- **Resultado esperado:** a tarefa está na etapa seguinte, e quem responde por
  essa etapa passa a ver que há trabalho esperando por ele

| # | Passo | Quem faz | O que precisa ser verdade antes |
| --- | --- | --- | --- |
| 1 | Localiza a tarefa no fluxo do projeto | Desenvolvedor | Ele tem acesso de participação ao projeto; a tarefa existe e não está encerrada |
| 2 | Indica que a tarefa avança para a etapa seguinte | Desenvolvedor | A etapa de destino é alcançável a partir da atual, no fluxo configurado daquele projeto |
| 3 | O sistema encerra a contagem de permanência na etapa de origem e inicia a da etapa de destino | Sistema | A transição foi aceita |
| 4 | A tarefa passa a constar como pendente de tomada na nova etapa, sem responsável individual atribuído | Sistema | A etapa de destino não é terminal |
| 5 | Quem responde pela etapa de destino passa a ver a tarefa como esperando por ele | Sistema | Existe ao menos uma pessoa com acesso de participação ao projeto |

**Caminhos alternativos:**

| Condição | O que acontece |
| --- | --- |
| A etapa de destino não é alcançável a partir da atual | A transição é recusada e a tarefa permanece onde está, com a razão explicitada. Ver Q-02 — se o fluxo admite transição livre ou apenas adjacente |
| A tarefa está impedida | A transição é permitida (Q-01 decidida): o impedimento acompanha a tarefa e não bloqueia o movimento. O tempo de impedimento é apurado como série própria, separada do tempo de permanência na etapa |
| Outra pessoa moveu a mesma tarefa entre a leitura e a ação | Ver B-03 |
| O fluxo do projeto foi reconfigurado e a etapa atual não existe mais | Ver B-06 |
| O desenvolvedor tem acesso somente-leitura ao projeto | A ação não lhe é oferecida, e é recusada se tentada por outro caminho (B-07) |
| A etapa de destino é terminal | A tarefa é encerrada: a contagem de permanência para em definitivo e ela deixa de aparecer como trabalho em curso |

---

### Fluxo: Sinalizar impedimento

- **Ator:** Desenvolvedor
- **Gatilho:** o desenvolvedor não consegue prosseguir por causa externa a ele
- **Resultado esperado:** a tarefa está visivelmente travada, com o motivo
  registrado, e quem pode desbloquear sabe disso sem que ninguém precise avisar
  por outro canal

| # | Passo | Quem faz | O que precisa ser verdade antes |
| --- | --- | --- | --- |
| 1 | Declara que a tarefa está impedida e registra o motivo | Desenvolvedor | A tarefa está em curso e não encerrada; ela ainda não tem impedimento aberto |
| 2 | O sistema marca a tarefa como impedida, preservando a etapa em que ela está | Sistema | O motivo foi informado — impedimento sem motivo não se distingue de tarefa parada |
| 3 | O sistema inicia a contagem do tempo de impedimento, separada da contagem de permanência na etapa | Sistema | — |
| 4 | A tarefa passa a figurar com destaque para quem responde pelo desbloqueio no projeto | Sistema | Há alguém com esse papel no projeto (ver Q-04) |
| 5 | O impedimento permanece visível enquanto não for resolvido — não é um aviso que passa | Sistema | — |

**Caminhos alternativos:**

| Condição | O que acontece |
| --- | --- |
| A tarefa já tem impedimento aberto | Não se abre um segundo; o registro existente recebe informação adicional (B-02) |
| Ninguém no projeto tem papel de desbloqueio | O impedimento é registrado assim mesmo e fica visível a todos os participantes. Impedimento que não se registra por falta de destinatário é exatamente a falha de hoje |
| O motivo não foi informado | A sinalização é recusada |
| A tarefa é encerrada enquanto o impedimento está aberto | Ver B-04 |

---

### Fluxo: Assumir tarefa que chegou à minha etapa

- **Ator:** Reviewer, validador ou qualquer participante responsável pela etapa
- **Gatilho:** a tarefa entrou em uma etapa pela qual essa pessoa responde
- **Resultado esperado:** a tarefa deixa de estar esperando e passa a estar sendo
  trabalhada, com dono conhecido — este é o fluxo que materializa o handoff, a
  razão pela qual E-02 entrou na direção

| # | Passo | Quem faz | O que precisa ser verdade antes |
| --- | --- | --- | --- |
| 1 | Vê que há tarefa esperando na etapa pela qual responde | Participante | A tarefa foi movida para essa etapa e ninguém a assumiu |
| 2 | Assume a tarefa | Participante | Ele tem acesso de participação ao projeto |
| 3 | O sistema registra quem assumiu e a partir de quando | Sistema | A tarefa ainda não tinha sido assumida por outro |
| 4 | A tarefa deixa de constar como esperando tomada | Sistema | — |

**Caminhos alternativos:**

| Condição | O que acontece |
| --- | --- |
| Duas pessoas assumem simultaneamente | Apenas uma prevalece, e a outra é informada de quem assumiu — sem perda silenciosa (B-03) |
| Ninguém assume por muito tempo | Nada acontece automaticamente nesta entrega: escalonamento com prazo é E-04, descartado no brief. O tempo de espera fica contado e visível, que é o que permite reabrir E-04 com evidência |
| Quem assumiu precisa devolver | A tarefa volta a constar como esperando tomada na mesma etapa, e o tempo de espera recomeça |

---

### Fluxo: Desbloquear tarefa impedida

- **Ator:** Liderança técnica, ou quem responde pelo desbloqueio no projeto
- **Gatilho:** existe impedimento aberto
- **Resultado esperado:** a tarefa volta a fluir, e fica registrado quanto tempo
  ela esteve travada e por quê

| # | Passo | Quem faz | O que precisa ser verdade antes |
| --- | --- | --- | --- |
| 1 | Vê a tarefa impedida e o motivo | Liderança técnica | Existe impedimento aberto |
| 2 | Age sobre a causa — fora do sistema, em geral | Liderança técnica | — |
| 3 | Declara o impedimento resolvido, registrando o desfecho | Liderança técnica | O impedimento está aberto |
| 4 | O sistema encerra a contagem de impedimento e a tarefa volta a fluir na etapa em que estava | Sistema | — |

**Caminhos alternativos:**

| Condição | O que acontece |
| --- | --- |
| O próprio desenvolvedor resolve o que o travava | Ele mesmo declara resolvido — o desbloqueio não é privativo da liderança |
| O impedimento é resolvido movendo a tarefa de volta a uma etapa anterior | Permitido (Q-01 decidida): a tarefa retrocede carregando a marca de impedimento, que é encerrada à parte. O alcance do retrocesso continua governado por Q-02 |
| O impedimento se revela permanente | A tarefa é encerrada sem conclusão; o tempo de impedimento é preservado no histórico |

---

### Fluxo: Consultar andamento e tempo por etapa

- **Ator:** Product Owner (visão agregada) e gestor de outro time (somente
  leitura)
- **Gatilho:** necessidade de saber como está o trabalho
- **Resultado esperado:** a resposta vem do próprio sistema, sem que ninguém
  precise consolidar nada — é o que substitui a planilha

| # | Passo | Quem faz | O que precisa ser verdade antes |
| --- | --- | --- | --- |
| 1 | Consulta o andamento de um projeto ou do conjunto de projetos a que tem acesso | PO ou gestor | Tem ao menos acesso de leitura ao projeto |
| 2 | Vê o trabalho distribuído pelas etapas, o que está impedido e há quanto tempo | Sistema | — |
| 3 | Vê o tempo de permanência agregado **por etapa e por projeto** | Sistema | Há histórico acumulado; sem uso anterior, não há o que agregar |
| 4 | Nenhuma visão apresenta tempo agregado por pessoa | Sistema | Restrição estrutural do brief, não preferência de apresentação |

**Caminhos alternativos:**

| Condição | O que acontece |
| --- | --- |
| Não há histórico ainda | A visão informa a ausência de base, em vez de exibir zero. Zero e "ainda não medido" significam coisas opostas, e é o primeiro período de uso que estabelece a linha de base |
| O gestor tenta alterar qualquer coisa | Recusado. É a fronteira estrutural entre visibilidade e interferência (B-07) |
| O projeto tem poucas tarefas concluídas | O agregado é exibido com a ressalva de baixa massa, não omitido |

---

### Fluxo: Configurar o fluxo de etapas e as raias do projeto

- **Ator:** Responsável pela configuração do projeto
- **Gatilho:** o processo real do time não corresponde ao fluxo configurado
- **Resultado esperado:** o fluxo do projeto reflete como aquele time trabalha,
  sem afetar outros projetos

| # | Passo | Quem faz | O que precisa ser verdade antes |
| --- | --- | --- | --- |
| 1 | Define as etapas do projeto, a ordem entre elas e quais são terminais | Responsável pela configuração | Tem permissão de configuração naquele projeto |
| 2 | Define as raias que separam o trabalho dentro de cada etapa | Responsável pela configuração | — |
| 3 | Define quem participa e com que permissão naquele projeto | Responsável pela configuração | As pessoas já existem no sistema |
| 4 | A alteração vale a partir dali, sem reescrever o histórico já acumulado | Sistema | — |

**Caminhos alternativos:**

| Condição | O que acontece |
| --- | --- |
| Há tarefas em curso na etapa que se quer remover | Ver B-06 — é a borda mais perigosa deste fluxo |
| A configuração deixaria o projeto sem etapa terminal | Recusada: sem etapa terminal nada nunca conclui, e o tempo por etapa perde sentido |
| Renomear uma etapa | Permitido; o histórico segue a identidade da etapa, não o nome (ver B-06) |

---

## Estados e transições

A tarefa tem **duas dimensões de estado simultâneas**, e tratá-las como uma só é
o erro que mais custaria adiante. A **etapa** é definida por cada projeto e
varia entre projetos. A **condição** é transversal, igual em todo o sistema, e é
sobre ela que a tabela abaixo fala.

| Estado | Significado para o negócio | Transições que saem | Quem pode disparar |
| --- | --- | --- | --- |
| Aguardando tomada | A tarefa chegou a uma etapa e ninguém a assumiu. O tempo contado aqui é a espera de handoff — a dor que originou E-02 | → Em curso; → Impedida; → Encerrada sem conclusão | Qualquer participante do projeto |
| Em curso | Alguém assumiu e está trabalhando nela | → Aguardando tomada (na etapa seguinte, ou devolvida na mesma); → Impedida; → Concluída; → Encerrada sem conclusão | Quem assumiu, ou outro participante do projeto |
| Impedida | O trabalho não pode prosseguir por causa externa, com motivo registrado. A etapa é preservada — impedimento não é uma etapa, é uma condição sobre ela | → volta à condição anterior (Aguardando tomada ou Em curso); → Encerrada sem conclusão | Quem responde pelo desbloqueio, ou quem sinalizou |
| Concluída | Chegou a uma etapa terminal de sucesso. Toda contagem cessa | — | — |
| Encerrada sem conclusão | Saiu do fluxo sem ter sido concluída — cancelada, obsoleta ou impedimento permanente. Distinta de Concluída porque contaminaria o tempo por etapa se fossem a mesma coisa | — | Participante com permissão de encerrar (ver Q-05) |

- **Estado inicial:** Aguardando tomada, na primeira etapa do fluxo do projeto.
  Não existe estado de rascunho: tarefa que existe é trabalho que existe.
- **Estados terminais:** Concluída; Encerrada sem conclusão.
- **Transições proibidas:**
  - Sair de **Encerrada sem conclusão**. O que saiu do fluxo sem conclusão não
    retorna: abre-se outra tarefa.
  - Sair de **Concluída**, com uma exceção decidida pelo demandante em
    2026-09-04 (Q-06): **o Product Owner pode reabrir uma tarefa concluída**.
    Ninguém mais pode. A reabertura é registrada como retorno no histórico, e a
    consequência para a medição está declarada em Q-06 — o tempo por etapa de
    uma tarefa reaberta passa a ser soma de episódios, e precisa ser lido como
    tal no agregado.
  - Ir para Concluída sem passar por uma etapa terminal do fluxo do projeto.
  - Concluir tarefa com impedimento aberto. O impedimento precisa ter desfecho
    registrado — do contrário o tempo de impedimento fica em aberto para sempre
    e o agregado mente.
  - Mover uma tarefa para etapa que não pertence ao fluxo do projeto dela.
  - Qualquer transição disparada por quem tem apenas acesso de leitura. É a
    restrição estrutural que separa visibilidade de interferência.
  - Alterar retroativamente tempo já contado. O histórico é registro, não campo.
  - Atribuir a uma pessoa a tarefa de um projeto do qual ela não participa.

---

## Semântica de contrato

> Comportamento observável de cada operação, independente de protocolo.

| Operação | Entrada (significado) | Saída (significado) | Idempotente | Efeito colateral |
| --- | --- | --- | --- | --- |
| Avançar tarefa de etapa | Qual tarefa, para qual etapa, e a partir de que etapa o solicitante acredita que ela está | A tarefa em sua nova etapa e condição | Sim quanto ao alvo: pedir a mesma transição a partir do mesmo estado de origem duas vezes deixa a tarefa no mesmo lugar e não recontabiliza tempo | Encerra e inicia contagens de permanência; torna a tarefa visível como espera para a etapa de destino |
| Assumir tarefa | Qual tarefa | Quem assumiu e desde quando | Sim se quem repete é quem já assumiu; recusada com identificação do atual dono se for outra pessoa | Encerra a contagem de espera de handoff |
| Devolver tarefa assumida | Qual tarefa | A tarefa aguardando tomada na mesma etapa | Sim | Reinicia a contagem de espera |
| Sinalizar impedimento | Qual tarefa e o motivo | A tarefa em condição de impedida, com impedimento aberto | Sim: já havendo impedimento aberto, não se cria um segundo | Inicia contagem de impedimento; torna a tarefa destacada para quem desbloqueia |
| Resolver impedimento | Qual impedimento e o desfecho | A tarefa de volta à condição anterior | Sim: resolver o já resolvido não altera nada nem recontabiliza | Encerra a contagem de impedimento |
| Encerrar tarefa | Qual tarefa e se houve conclusão | A tarefa em estado terminal | Sim | Cessa todas as contagens em definitivo |
| Consultar andamento | Escopo de consulta — um projeto ou o conjunto acessível | Distribuição do trabalho pelas etapas, impedimentos abertos e há quanto tempo | Sim, por ser leitura | Nenhum. Consulta nunca altera estado, inclusive não marca nada como visto |
| Consultar tempo por etapa | Escopo e período | Tempo de permanência agregado por etapa e por projeto | Sim | Nenhum. **Nunca agregável por pessoa** — restrição estrutural, não filtro ausente |
| Configurar fluxo do projeto | Etapas, ordem, terminais e raias | O fluxo vigente do projeto | Sim se a configuração submetida for idêntica à vigente | Passa a valer para transições futuras; não reescreve histórico |
| Conceder ou alterar participação no projeto | Quem, em qual projeto, com que permissão | A participação vigente | Sim | Muda o que a pessoa pode ver e fazer, dali em diante |

Nenhuma operação de escrita se completa apenas com verificação feita do lado de
quem chama: toda transição é reavaliada contra o estado corrente antes de ser
aceita. Isso é comportamento, não escolha de implementação — vale qualquer que
seja a tecnologia.

---

## Regras de borda

| # | Situação | Comportamento esperado | Por que não é o óbvio |
| --- | --- | --- | --- |
| B-01 | **Vazio** — projeto sem tarefa, etapa sem tarefa, consulta sem histórico | Distinguir explicitamente "nada aqui" de "ainda não medido". Um projeto novo não tem tempo por etapa igual a zero: ele não tem tempo por etapa | O óbvio seria exibir zero. Zero é uma afirmação sobre o mundo — diria que as tarefas atravessam as etapas instantaneamente. Como não há linha de base (o primeiro período de uso é que a produz), confundir os dois corrompe justamente a medição que sustenta a métrica de sucesso |
| B-02 | **Duplicado** — mesma sinalização de impedimento repetida, ou mesma transição pedida duas vezes | Não gerar segundo registro nem recontabilizar tempo. Informação adicional se anexa ao impedimento aberto | O óbvio seria criar outro registro por ser outro clique. Mas dois impedimentos abertos na mesma tarefa tornam ambíguo qual contagem vale, e o tempo de impedimento é insumo direto do agregado |
| B-03 | **Concorrente** — duas pessoas agem sobre a mesma tarefa ao mesmo tempo. O discovery registra isso como modo de uso real, não como exceção rara | Uma ação prevalece; a outra é **recusada e informada com o estado atual**, nunca sobrescrita em silêncio. Quem perdeu vê o que aconteceu e decide de novo | O óbvio é o último a escrever vencer, que é como quase tudo se comporta. Aqui isso apaga o trabalho de alguém sem aviso — e reintroduz, dentro do produto, exatamente a perda silenciosa de informação que motivou o projeto. Ver Q-03 |
| B-04 | **Parcial** — tarefa encerrada com impedimento aberto, ou impedimento sem desfecho | Recusar o encerramento enquanto houver impedimento sem desfecho registrado | O óbvio seria encerrar em cascata, fechando o impedimento junto. Isso produziria tempo de impedimento com desfecho inventado pelo sistema, e o agregado passaria a incluir número que ninguém afirmou |
| B-05 | **Fora de ordem** — pedido de transição baseado em um estado que já mudou | Recusar com base no estado corrente, e informar qual é. A transição declara de onde o solicitante pensa que está partindo | O óbvio é aceitar porque a etapa de destino é válida. Mas "avançar" a partir de uma etapa que já não é a atual pode significar retroceder sem que ninguém tenha pedido isso |
| B-06 | **Reconfiguração com trabalho em curso** — a etapa a remover tem tarefas nela | Ver Q-07. Preliminarmente: a etapa não desaparece do histórico, e nenhuma tarefa fica em etapa inexistente | O óbvio seria bloquear a remoção, ou mover tudo para a etapa anterior. Ambos escondem a decisão de negócio de quem deveria tomá-la, e o histórico já acumulado precisa continuar interpretável mesmo depois da mudança |
| B-07 | **Sem permissão** — gestor somente-leitura tenta alterar; participante age em projeto do qual não participa | Recusar toda escrita e não expor a ação como disponível. Recusar também a leitura de projeto ao qual não se tem acesso | Não é o óbvio porque deixar de oferecer a ação a quem não pode executá-la parece suficiente. A restrição aqui é estrutural: é o que impede o gestor de "passar a interferir na execução", efeito que o demandante recusou |
| B-08 | **Expirado** | **Não se aplica.** Nada neste sistema expira por decurso de prazo: impedimento não vence, tarefa não caduca, espera de handoff não escala sozinha. Escalonamento com prazo é E-04, descartado no brief. O tempo de espera é contado e exibido, não acionado | Registrado explicitamente porque a ausência de expiração é decisão, não esquecimento — e porque a evidência acumulada aqui é o que permitirá reabrir E-04 |
| B-09 | **Tarefa impedida há muito tempo** | Permanece impedida e visível, com o tempo acumulado à mostra. Nenhuma ação automática | Complementa B-08. O caso dos 5 dias tenta puxar para o escalonamento automático; a direção aposta que a visibilidade permanente basta, e H-02 é a hipótese que verifica isso |
| B-10 | **Pessoa removida do projeto com tarefa assumida** | A tarefa volta a aguardar tomada na etapa em que está; o histórico de quem a assumiu é preservado | O óbvio seria manter a atribuição, ou apagá-la. Manter deixa trabalho com dono que não pode agir; apagar destrói histórico |

---

## Opções de comportamento consideradas

> Isto é a divergência de D2. Sem alternativas, não houve exploração.

| Questão em aberto | Opção A | Opção B | Recomendação | Decidir em |
| --- | --- | --- | --- | --- |
| **Q-01** — Tarefa impedida pode mudar de etapa? | Não: impedida é congelada na etapa, e só volta a fluir depois de resolvido. O tempo de impedimento fica inequívoco | Sim: impedimento é uma marca que acompanha a tarefa, e ela pode retroceder ou avançar enquanto travada | Recomendação era **A**. **DECIDIDO pelo demandante em 2026-09-09: opção B** — impedimento é marca que acompanha a tarefa, e mover é permitido para facilitar a operação, inclusive o retorno ao backlog. Custo assumido: o tempo de impedimento passa a se sobrepor ao tempo de permanência na etapa, e o agregado precisa apurá-los como séries distintas para continuar interpretável — vira requisito no `/prd`, não detalhe de implementação | **Decidida.** `/prd` formaliza |
| **Q-02** — Transição livre entre etapas ou apenas para adjacentes? | Apenas adjacentes na ordem configurada, com retrocesso de uma etapa permitido | Livre para qualquer etapa do fluxo do projeto | **A**, com retrocesso permitido. Livre é mais simples de construir e mais difícil de interpretar: se qualquer etapa alcança qualquer outra, a ordem configurada não descreve o processo e o tempo por etapa perde comparabilidade. Contrapeso real: o processo de um time pode ter desvio legítimo | `/prd` |
| **Q-03** — Como resolver a ação concorrente (B-03) | Recusar a segunda ação e mostrar o estado atual a quem perdeu | Aceitar as duas em ordem de chegada quando não forem conflitantes, e recusar só o conflito real | **A** para transição de etapa e tomada de tarefa; **B** é defensável para o que não muda o fluxo, como anotar motivo. A escolha muda o que o usuário sente no uso diário, que o discovery diz ser frequente e simultâneo | `/prd`, com reflexo em `/design` |
| **Q-04** — Quem é o destinatário do impedimento? | Papel de desbloqueio configurado por projeto | Todos os participantes do projeto veem, sem destinatário nomeado | **A**, com **B** como fallback quando o papel não estiver configurado. Sem destinatário reproduz-se a dor D-02 — a mensagem vai para o ambiente, não para quem age. Mas destinatário único cria ponto de falha quando a pessoa está ausente | `/prd` |
| **Q-05** — Quem pode encerrar tarefa sem conclusão? | Qualquer participante do projeto | Apenas quem tem permissão de configuração no projeto | **B.** Encerrar sem conclusão remove trabalho do fluxo e do agregado; é a única operação que apaga demanda em vez de movê-la | `/prd` |
| **Q-06** — Tarefa concluída pode ser reaberta? | Não: abre-se outra, vinculada à primeira | Sim, com o retorno registrado no histórico | Recomendação era **A**. **DECIDIDO pelo demandante em 2026-09-04: opção B, restrita ao Product Owner** — só ele reabre. A restrição de quem responde à objeção que motivava A (encerramento perde sentido se qualquer um desfaz), e o custo permanece: tempo por etapa de tarefa reaberta é soma de episódios e o agregado precisa distinguir isso | **Decidida.** `/prd` formaliza |
| **Q-07** — O que acontece com tarefas em etapa removida na reconfiguração (B-06)? | Recusar a remoção enquanto houver tarefa na etapa; quem configura precisa esvaziá-la | Permitir, exigindo que se indique para qual etapa as tarefas existentes migram | **B.** A opção A é mais segura e transfere ao usuário um trabalho manual que o sistema poderia conduzir; a B mantém a decisão com quem configura, que é quem sabe o que aquelas tarefas significam | `/prd` |
| **Q-08** — Raias separam o quê? | Agrupamento livre definido por projeto, sem semântica fixa | Semântica fixa do sistema, por exemplo por tipo de trabalho ou por prioridade | **A.** Fixar semântica contraria o princípio de que o fluxo acompanha o processo real de cada time. Contrapeso: sem semântica, raias não são comparáveis entre projetos e não entram no agregado | `/prd`, com reflexo em `/design` |
| **Q-09** — A espera de handoff é contada como parte do tempo da etapa, ou separada? | Parte do tempo da etapa de destino | Contada à parte, como espera de tomada, distinta do tempo de trabalho efetivo | **B.** É a distinção que dá resposta a "tudo começa tarde": separar espera de trabalho é o que permite mostrar que o tempo se perde entre as etapas, e não dentro delas. A opção A torna o dado mais simples e cega justamente para o problema escolhido | **Decidida** pelo demandante em 2026-09-04: opção B. `/prd` formaliza |
| **Q-10** — Tarefa aguardando tomada tem responsável? | Não: fica no pool da etapa até alguém assumir | Sim: quem move já indica quem é o próximo responsável | **A**, coerente com E-02 e com a estrutura de pool descrita nos fluxos. Mas **B** é o que mais se aproxima do hábito atual do time, em que se avisa uma pessoa; se H-01 for refutada por falta de adesão, esta é a primeira escolha a revisitar | **Decidida** pelo demandante em 2026-09-04: opção A. `/prd` formaliza |

---

## Entidades e vocabulário

> Nomes que o negócio usa. Vira insumo direto da dimensão E do canvas.

| Termo | Definição | Não confundir com |
| --- | --- | --- |
| Projeto | Contexto de trabalho com fluxo próprio, participantes próprios e permissões próprias. É a fronteira de tudo: não há dependência entre projetos | Organização ou cliente — o sistema é de tenant único |
| Tarefa | Unidade de trabalho que percorre o fluxo de um projeto | Impedimento, que é uma condição da tarefa, não outra tarefa |
| Etapa | Posição da tarefa no fluxo do projeto, definida por quem configura aquele projeto. Pode ser terminal | Condição da tarefa. Impedida não é etapa; é condição sobre a etapa atual |
| Fluxo do projeto | Conjunto ordenado de etapas e as transições que a ordem admite | Processo real do time, que o fluxo tenta representar e pode representar mal |
| Raia | Agrupamento visual do trabalho dentro das etapas, definido por projeto | Etapa. Raia não é posição no processo e não gera tempo agregado |
| Impedimento | Registro de que a tarefa não pode prosseguir por causa externa, com motivo, início e desfecho | Atraso. Tarefa atrasada não está impedida; está esperando |
| Espera de tomada | Tempo entre a tarefa chegar a uma etapa e alguém assumi-la. É onde o atraso de handoff aparece | Tempo de trabalho na etapa |
| Tempo de permanência | Quanto tempo a tarefa ficou em cada etapa, agregável por etapa e por projeto | Esforço, horas trabalhadas ou produtividade de pessoa. O sistema não mede nenhum dos três, por restrição estrutural |
| Participação | Vínculo de uma pessoa com um projeto, com a permissão que ela tem ali. Acumulável entre projetos | Conta de acesso ao sistema, que é única e vem do provedor de identidade corporativo |
| Acesso de leitura | Participação que permite ver e não permite alterar nada | Participação restrita. Não é "menos" participação: é a fronteira que impede o gestor de interferir na execução |
| Encerrada sem conclusão | Tarefa que saiu do fluxo sem ter sido concluída | Concluída. Somá-las corromperia o tempo por etapa |

---

## Devolução ao `/shape`

Nenhuma. A exploração não encontrou nada que torne a direção aprovada
inexequível. Duas observações que o `/prd` deve carregar consigo:

- **Q-06, Q-09 e Q-10 já foram decididas pelo demandante em 2026-09-04**, antes
  do `/design`, e estão marcadas como tais na tabela. Q-09 e Q-10 seguiram a
  recomendação; Q-06 foi decidida contra ela, permitindo reabertura de tarefa
  concluída **restrita ao Product Owner**. Isso altera as transições proibidas e
  precisa aparecer no `/design` — existe uma ação que só uma pessoa vê.
- **Q-10** mantém a espera sem responsável nomeado, que é o oposto do hábito
  atual do time. Continua pressionando H-01, a hipótese de adesão de que a
  direção inteira depende.
- **Q-01** foi decidida em 2026-09-09 contra a recomendação: a tarefa impedida
  pode mudar de etapa. A operação ganha liberdade, e o custo recai inteiro sobre
  a medição — tempo de impedimento e tempo de permanência passam a coexistir e
  precisam ser apurados separadamente para que o agregado, que é a métrica de
  sucesso, continue interpretável.
- As seis questões restantes (Q-02 a Q-05, Q-07, Q-08) seguem abertas para o
  `/prd`. Q-03 e Q-08 têm reflexo direto no `/design`.

---

## Fora deste artefato — regras negativas

- **Qualquer tecnologia**: banco, fila, framework, biblioteca, protocolo,
  formato de serialização, nome de serviço → `/techspec`.
- **Requisito numerado ou critério de aceite** → `/prd`. Aqui há comportamento
  descrito, não requisito verificável.
- **Cenário Gherkin** → `/prd`. É lá que o cenário ganha ID e vira contrato.
- **Tela, layout, componente visual** → `/design`. Aqui o fluxo é agnóstico de
  interface; se só faz sentido com tela, está no artefato errado.
- **Escolha de direção de negócio** → `/shape`. Se a exploração revelar que a
  direção está errada, devolva ao `/shape`; não decida aqui.
- **Modelagem de dados, tabela, campo, tipo** → `/techspec`. Entidade aqui é
  vocabulário de negócio, não esquema.
- **Estimativa, task, sequenciamento de entrega** → `/tasks`.
