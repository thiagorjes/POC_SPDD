# PRD — kanban-tarefas

> D2 convergente. Fecha o escopo da entrega e registra **o que precisa ser
> verdade**. É aqui que o comportamento explorado vira requisito verificável.
> **Gates:** GATE-SPEC (aprovação humana), GATE-PROVENIENCIA

- **Shape Brief:** `docs/shape/kanban-tarefas-brief.md`
- **Solução:** `docs/solution/kanban-tarefas-solution.md`
- **Design:** `docs/design/kanban-tarefas/screen-map.md`
- **Data:** 2026-09-16
- **Versão:** 1.6 — emenda vinda da revisão técnica do board. O board, que é a
  leitura mais exercitada do produto e a que todo cliente refaz a cada reconexão,
  não tinha envelope de tempo de resposta em requisito não funcional nenhum:
  RNF-009 cobre as consultas de andamento e de tempo por etapa, e a semelhança de
  assunto escondia a lacuna. Entram RNF-011, com o envelope, e RN-039, com o
  recorte das tarefas terminais — o segundo é regra de negócio e não otimização,
  porque muda o que a pessoa vê no board. Cenário novo SCN-003.4; nenhum cenário
  preexistente foi alterado.
- **Versão:** 1.5 — correção de INC-22, o escopo órfão que a v1.4 criou: as duas
  sinalizações desenhadas para pagar o custo do fluxo não configurado entraram
  pelo protótipo sem requisito que as obrigasse. O demandante decidiu
  **instituí-las como comportamento requerido**. A regra do projeto sem fluxo
  passa a exigir as duas — nomear o passo seguinte no desfecho da criação, e
  marcar na relação de projetos aquele que ainda não tem etapa —, e a marca
  ganha cenário congelado próprio. Total: **70 cenários**. O gate de spec
  precisa ser **reconfirmado**.
- **Versão:** 1.4 — correção de INC-18, sem tocar em cenário nenhum: a v1.3
  declarava que criar projeto não tinha tela "por ser operação de instalação", e
  essa qualificação era inferência do agente e não decisão do demandante. O
  demandante decidiu **instituir a tela** (TL-11), e foram corrigidas a origem no
  protótipo do requisito de criação de projeto e o universo de medição dos dois
  requisitos não funcionais de interface — 10 → **11 telas**. Nenhum cenário congelado foi criado, alterado ou revogado, logo o
  gate de spec **não** precisa de nova reconfirmação por esta versão.
- **Versão:** 1.3 — terceira emenda, da lacuna de especificação encontrada pela
  etapa de verificação: não existia requisito de criação de projeto, e sem ele
  um sistema recém-instalado não sai do zero por meios próprios. A v1.2 emendou
  INC-11 e INC-12, e a v1.1 emendou INC-01 a INC-06, INC-08 e INC-10, todos da
  análise de consistência (`docs/analyze/kanban-tarefas-analysis.md`). Ver
  "Emendas de cenário" ao fim. O gate de spec precisa ser **reconfirmado** para
  que os cenários voltem a valer como congelados.
- **Parametrização:** 3 cenários por RF — caminho feliz, caminho alternativo e
  borda (decidido em 2026-09-09). Três requisitos passam a ter quatro cenários
  pelas emendas de 2026-09-09; os cenários acrescentados estão na tabela de
  emendas. Total após a v1.5: **70 cenários**

> **Procedência do documento.** Este PRD **não** é revisão do
> `requirements/prd/kanban-tarefas-prd.md` (v1.0, 2026-08-24). Aquele foi escrito
> sobre o enquadramento E-01 puro, antes de `/discovery`, `/shape`, `/solution` e
> `/design`, e sua numeração de requisitos não corresponde à deste documento.
> Ele permanece como insumo histórico e não é fonte de verdade. Coincidência de
> numeração entre os dois documentos não significa correspondência de conteúdo.

---

## Escopo da entrega

Derivado da fronteira do Shape Brief. **Nenhuma divergência** em relação ao
brief: tudo que estava em "Dentro" virou requisito, e nada que estava em "Fora"
entrou.

| Dentro | Fora | Por que fora |
| --- | --- | --- |
| Registro do andamento da tarefa pelo próprio desenvolvedor | Integração com canal externo de notificação — e-mail, mensageria, mensagem instantânea | Limite rígido da Intent. Consequência assumida em C-01: o aviso só alcança quem abre o sistema |
| Sinalização e resolução de impedimento como parte do fluxo | Controle de horas ou timesheet | Limite rígido da Intent |
| Handoff explícito entre etapas, com espera de tomada contada à parte | Escalonamento automático de impedimento por prazo (E-04) | Alternativa descartada no brief. Nada neste sistema expira por decurso de prazo (RN-026) |
| Fila própria do que aguarda tomada por mim, atravessando projetos | Múltiplas organizações ou clientes | Limite rígido da Intent — tenant único |
| Fluxo de etapas configurável por projeto, com raias | Dependência entre projetos | Limite rígido da Intent |
| Visão agregada do andamento, substituindo a consolidação manual | Importação em massa de tarefas, modelos de tarefa, duplicação | Limite declarado da Intent. Confirmado em 2026-09-09 que não há ferramenta anterior com dado a migrar (H-05) |
| Tempo por etapa, visível no fluxo e agregado por projeto | Anexos e arquivos em tarefas | Limite declarado da Intent |
| Acesso somente-leitura para gestor de outro time | Qualquer agregação de tempo por pessoa | Restrição **estrutural** do brief. Não é filtro ausente: é o que impede o produto de virar instrumento de cobrança individual |
| Acesso autenticado pelo provedor de identidade corporativo, com permissão por projeto | Agregação automática dos canais de comunicação atuais (E-03) | Alternativa descartada no brief |
| Uso simultâneo da mesma tarefa por participantes diferentes, com recusa explícita do perdedor | Fallback de autenticação local | ADR-006 |

---

## Regras de negócio

| ID | Regra | Procedência | Fonte |
| --- | --- | --- | --- |
| RN-001 | O fluxo de um projeto é um conjunto ordenado de etapas, com ao menos uma etapa terminal. Configuração que deixaria o projeto sem etapa terminal é recusada. A regra governa **toda configuração de fluxo**, e não a existência do projeto: pela exceção nomeada em RN-038, o projeto recém-criado ainda não tem fluxo e por isso não é configuração inválida — é fluxo ainda não configurado, estado em que o projeto recusa criação de tarefa até que a configuração das etapas seja feita | informada | Demandante — `/solution`, fluxo de configuração; exceção acrescentada em 2026-09-10 (INC-19) |
| RN-002 | A tarefa tem **três dimensões de estado simultâneas e independentes**: a **etapa**, definida por cada projeto; a **condição de trabalho**, transversal a todo o sistema; e a **marca de impedimento**, que é ou não está aberta. Impedimento não é etapa nem condição: é dimensão própria, e por isso convive com qualquer das outras duas sem substituí-las | derivada | RN-001, a exploração de solução ("Estados e transições") e a decisão do demandante em Q-01, 2026-09-09 |
| RN-003 | As condições de trabalho possíveis são: aguardando tomada, em curso, concluída, encerrada sem conclusão. Impedida **não** é uma delas — ver RN-032 | derivada | RN-002 |
| RN-004 | A tarefa nasce **aguardando tomada, na primeira etapa** do fluxo do projeto. Não existe rascunho: tarefa que existe é trabalho que existe | informada | Demandante — `/solution` |
| RN-005 | Transição permitida é: para a etapa imediatamente seguinte, para a imediatamente anterior, ou — como exceção nomeada — para a **primeira etapa do fluxo**, de qualquer etapa | informada | Demandante, 2026-09-09 (Q-02 + conflito com Q-01) |
| RN-006 | Tarefa aguardando tomada **não tem responsável individual**: fica no pool da etapa até alguém assumir | informada | Demandante, 2026-09-04 (Q-10) |
| RN-007 | A **espera de tomada** — da chegada à etapa até alguém assumir — é contada e exibida **separadamente** do tempo de trabalho na etapa | informada | Demandante, 2026-09-04 (Q-09) |
| RN-008 | O **tempo de impedimento** é série própria e pode **coexistir** com o tempo de permanência na etapa e com a espera de tomada. As três nunca são somadas entre si | informada | Demandante, 2026-09-09 (Q-01) |
| RN-009 | Tarefa com impedimento aberto **pode** mudar de etapa. O impedimento acompanha a tarefa; sua contagem não é encerrada nem reiniciada pela transição, e a marca não é apagada por ela | informada | Demandante, 2026-09-09 (Q-01) |
| RN-010 | Impedimento exige motivo informado, e há no máximo **um aberto por tarefa**. Nova sinalização sobre tarefa já impedida anexa informação ao registro existente | informada | Demandante — `/solution` (B-02) |
| RN-011 | Tarefa com impedimento aberto **não pode ser concluída nem encerrada sem conclusão** — o impedimento precisa ter desfecho registrado antes. São as duas únicas restrições que a marca impõe; nenhuma outra operação é bloqueada por ela | derivada | RN-008 e B-04 — sem desfecho registrado por alguém, o tempo de impedimento fica aberto para sempre e o agregado mente. A metade sobre o encerramento é emenda de 2026-09-09 (INC-11, INC-12) |
| RN-012 | Em ação concorrente sobre a mesma tarefa, **a primeira prevalece e a segunda é recusada**, sendo devolvido a quem perdeu o estado corrente. Vale para **toda** escrita, sem exceção | informada | Demandante, 2026-09-09 (Q-03, B-03) |
| RN-013 | Toda solicitação de transição declara de qual etapa e condição o solicitante acredita partir, e é reavaliada contra o estado corrente antes de ser aceita. Origem divergente é recusada | derivada | RN-012 e B-05 |
| RN-014 | Nenhuma visão, filtro, ordenação, exportação ou agregação do sistema apresenta tempo agregado **por pessoa** | informada | Demandante, 2026-09-04 — restrição estrutural do Shape Brief |
| RN-015 | Participação somente-leitura não executa nenhuma escrita, e as ações de escrita **não lhe são apresentadas**. A recusa também vale se a ação for solicitada por outro caminho | informada | Demandante, 2026-09-04 (C-02, B-07) |
| RN-016 | Encerrar tarefa sem conclusão é privativo de quem tem **permissão de configuração** no projeto | informada | Demandante, 2026-09-09 (Q-05) |
| RN-017 | Reabrir tarefa concluída é privativo do **Product Owner**. Nenhum outro papel reabre | informada | Demandante, 2026-09-04 (Q-06) |
| RN-018 | **Encerrada sem conclusão é terminal absoluto**: o que saiu do fluxo sem conclusão não retorna — abre-se outra tarefa | informada | Demandante — `/solution` |
| RN-019 | Tarefa reaberta acumula **episódios**: o tempo por etapa passa a ser a soma dos episódios, e cada um é identificável no histórico | derivada | RN-017 — sem isso o agregado mistura primeira passagem e retrabalho |
| RN-020 | Remoção de etapa que contenha tarefas é **recusada**. Quem configura precisa esvaziá-la antes | informada | Demandante, 2026-09-09 (Q-07, B-06) |
| RN-021 | Renomear etapa é permitido e não afeta o histórico: a série temporal segue a **identidade** da etapa, não o nome | derivada | RN-020 e B-06 |
| RN-022 | Alteração de configuração vale **a partir dali** e nunca reescreve histórico já acumulado | informada | Demandante — `/solution` |
| RN-023 | Raia é agrupamento livre definido por projeto, **sem semântica fixa**. Não restringe transição e não entra em nenhuma agregação | informada | Demandante, 2026-09-09 (Q-08) |
| RN-024 | Impedimento aberto destaca-se para quem tem o **papel de desbloqueio** no projeto. Se o projeto não configurou o papel, fica visível a **todos os participantes** | informada | Demandante, 2026-09-09 (Q-04) |
| RN-025 | Ausência de histórico é informada como **"ainda não medido"**, jamais como zero. As duas afirmações são opostas | informada | Demandante — `/solution` (B-01) |
| RN-026 | **Nada expira por decurso de prazo**: impedimento não vence, tarefa não caduca, espera de tomada não escala sozinha. O tempo é contado e exibido, nunca acionado | informada | Demandante — descarte de E-04 no Shape Brief (B-08, B-09) |
| RN-027 | Pessoa removida do projeto com tarefa assumida: a tarefa volta a **aguardar tomada** na etapa em que está, e o registro de quem a assumiu é preservado no histórico | informada | Demandante — `/solution` (B-10) |
| RN-028 | Participação é vínculo pessoa↔projeto, com permissão própria e acumulável entre projetos. A conta de acesso é única e vem do provedor de identidade corporativo | informada | Intent e ADR-003 / BDR-001 |
| RN-029 | Agregado apurado sobre massa pequena é exibido **com ressalva de baixa massa**, nunca omitido | derivada | RN-025 — omitir e exibir zero têm o mesmo defeito |
| RN-030 | Devolver tarefa assumida a coloca de volta em aguardando tomada, na mesma etapa, e **reinicia** a contagem de espera | informada | Demandante — `/solution` |
| RN-031 | Toda operação de escrita é idempotente quanto ao alvo: repetir a mesma solicitação a partir do mesmo estado de origem não duplica registro nem recontabiliza tempo | derivada | Semântica de contrato da `/solution` |
| RN-032 | A marca de impedimento **coexiste** com qualquer condição de trabalho não terminal e só é apagada pelo registro do desfecho do impedimento. Nenhuma outra operação — mover, devolver, assumir, renomear etapa, remover participação — a apaga, encerra sua contagem ou a reinicia. O encerramento sem conclusão **não é exceção**: em vez de apagar a marca, ele é recusado enquanto ela existir (RN-011) | informada | Demandante, emenda de 2026-09-09 (INC-01); segunda frase da emenda de 2026-09-09 (INC-11) |
| RN-033 | Tarefa com impedimento aberto **pode ser assumida**. A tomada encerra a espera de tomada, registra o responsável e leva a condição a em curso; a marca de impedimento permanece aberta, com a contagem correndo | informada | Demandante, emenda de 2026-09-09 (INC-02) |
| RN-034 | Tarefa reaberta retorna à **primeira etapa do fluxo**, aguardando tomada e sem responsável, iniciando novo episódio | informada | Demandante, emenda de 2026-09-09 (INC-04) |
| RN-035 | A administração global do sistema dispensa a participação no projeto para **ver e agir** em qualquer projeto. Esse alcance não a isenta de RN-014 nem da imutabilidade do histórico, e toda promoção a administrador global é única por pessoa e registrada em histórico auditável | informada | Demandante, emenda de 2026-09-09 (INC-03); BDR-001 |
| RN-036 | Criar projeto é capacidade exclusiva da administração global. Nenhum papel de projeto a possui, porque nenhum papel de projeto existe antes de o projeto existir | informada | Demandante, emenda de 2026-09-10 (pendência 09) |
| RN-037 | A criação do projeto **nomeia a primeira participação**, com papel `project_admin`, numa única operação. Quem cria não se torna participante por criar: o alcance global já lhe dá acesso, e torná-lo participante misturaria escopo com participação, contra RN-035 | informada | Demandante, emenda de 2026-09-10 (pendência 09) |
| RN-038 | O projeto nasce **sem fluxo**. Enquanto não houver etapa configurada, a criação de tarefa é recusada com razão explícita. A criação faz uma coisa só, e a configuração do fluxo continua tendo um único lugar. Como o caminho de partida passa a ter dois passos obrigatórios, o segundo é sinalizado em dois momentos, e ambos são obrigatórios: no desfecho bem-sucedido da criação, que **nomeia** a configuração do fluxo como passo seguinte e oferece a ida até ela; e na relação de projetos, em que projeto sem etapa alguma aparece **marcado como fluxo não configurado** para quem o vê ali. Sem as duas, o custo aceito nesta regra ficaria invisível para quem tem de pagá-lo | informada | Demandante, emenda de 2026-09-10 (pendência 09) — alternativa de fluxo padrão apresentada e recusada; sinalização acrescentada em 2026-09-10 (INC-22) |
| RN-039 | O board exibe as tarefas **terminais** — concluídas ou encerradas sem conclusão — apenas enquanto a conclusão tiver ocorrido nos **últimos 30 dias**. As mais antigas saem do board e continuam integralmente acessíveis pela ficha da tarefa e pelas consultas de andamento e de tempo por etapa: o recorte é da tela, nunca do registro, e nenhum evento é apagado (RN-022, RNF-008). A regra não alcança tarefa não terminal, que permanece no board pelo tempo que levar — o que cresce sem limite é o acúmulo de trabalho **encerrado**, e é só ele que o recorte contém | informada | Demandante, emenda de 2026-09-16 — decisão sobre o achado ACH-03 da revisão técnica do board; alternativas de teto por etapa e de teto combinado apresentadas e recusadas |

---

## Requisitos funcionais

### RF-001 — Autenticar pelo provedor de identidade corporativo

- **Descrição:** o acesso ao sistema se dá exclusivamente por autenticação
  federada no provedor de identidade corporativo. Não existe cadastro local nem
  caminho alternativo de entrada.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Intent — limite declarado; ADR-003 e ADR-006
- **Regras aplicáveis:** RN-028
- **Origem no protótipo:** TL-01, estados idle, loading e erro de autenticação

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-001.1 | Pessoa autenticada com sucesso chega à lista de projetos | e2e |
| SCN-001.2 | Pessoa sem participação em projeto algum entra e vê a lista vazia | e2e |
| SCN-001.3 | Provedor de identidade indisponível recusa a entrada, sem fallback | integração |

```gherkin
Cenário: SCN-001.1 — Entrada bem-sucedida
  Dado que sou uma pessoa cadastrada no provedor de identidade corporativo
  E que participo de ao menos um projeto
  Quando concluo a autenticação
  Então sou levado à lista dos projetos em que participo
```

```gherkin
Cenário: SCN-001.2 — Autenticado sem participação
  Dado que sou uma pessoa cadastrada no provedor de identidade corporativo
  E que não participo de nenhum projeto
  Quando concluo a autenticação
  Então entro no sistema
  E vejo que ainda não participo de nenhum projeto
```

```gherkin
Cenário: SCN-001.3 — Provedor de identidade indisponível
  Dado que o provedor de identidade corporativo está indisponível
  Quando tento entrar no sistema
  Então a entrada é recusada com a indisponibilidade informada
  E nenhuma forma alternativa de autenticação me é oferecida
```

### RF-002 — Listar os projetos em que participo

- **Descrição:** a pessoa vê a relação dos projetos a que tem acesso, com a
  permissão que possui em cada um. Projeto do qual não participa não aparece nem
  é acessível por endereço direto. Projeto que ainda não teve o fluxo
  configurado aparece marcado como tal, porque é ali que a sinalização obrigada
  por RN-038 alcança quem precisa agir depois que o desfecho da criação já saiu
  da tela.
- **Prioridade:** deve
- **Procedência:** derivada
- **Fonte:** RN-028 e RN-015; a marca de fluxo não configurado vem de RN-038
- **Regras aplicáveis:** RN-015, RN-028, RN-038
- **Origem no protótipo:** TL-02, estados preenchido e vazio, e o cartão marcado
  como fluxo não configurado

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-002.1 | Participante vê seus projetos com a permissão de cada um | integração |
| SCN-002.2 | Lista vazia quando não há participação | integração |
| SCN-002.3 | Acesso direto a projeto do qual não participo é recusado | integração |
| SCN-002.4 | Projeto sem fluxo configurado aparece marcado na relação | integração |

```gherkin
Cenário: SCN-002.1 — Projetos com a permissão de cada um
  Dado que participo de três projetos com permissões diferentes
  Quando consulto meus projetos
  Então vejo os três
  E em cada um vejo qual permissão eu tenho ali
```

```gherkin
Cenário: SCN-002.2 — Nenhum projeto
  Dado que não participo de nenhum projeto
  Quando consulto meus projetos
  Então vejo que não participo de nenhum projeto
  E nenhum projeto do sistema me é exibido
```

```gherkin
Cenário: SCN-002.3 — Projeto sem participação
  Dado que existe um projeto do qual não participo
  Quando tento acessá-lo diretamente
  Então o acesso é recusado
  E nenhum dado daquele projeto me é revelado
```

```gherkin
Cenário: SCN-002.4 — Projeto ainda sem fluxo aparece marcado
  Dado que participo de dois projetos
  E que um deles ainda não tem etapa alguma configurada
  Quando consulto meus projetos
  Então vejo os dois
  E o que ainda não tem etapa alguma vem marcado como fluxo não configurado
  E o outro não vem marcado
```

### RF-003 — Visualizar o board do projeto

- **Descrição:** o board apresenta as tarefas do projeto distribuídas pelas
  etapas do fluxo configurado e pelas raias, exibindo em cada tarefa a condição
  atual, quem a assumiu quando houver, a espera de tomada em curso e a marca de
  impedimento quando existir.
- **Prioridade:** deve
- **Procedência:** derivada
- **Fonte:** RN-002, RN-003, RN-007, RN-023
- **Regras aplicáveis:** RN-002, RN-003, RN-006, RN-007, RN-008, RN-023, RN-032,
  RN-039
- **Origem no protótipo:** TL-03, estados preenchido e vazio

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-003.1 | Board exibe etapa, condição, responsável e contadores | e2e |
| SCN-003.2 | Etapa sem tarefa é exibida vazia, e não omitida | integração |
| SCN-003.3 | Tarefa com impedimento aberto e aguardando tomada exibe a condição de trabalho, a marca de impedimento e as duas contagens em curso | integração |
| SCN-003.4 | Tarefa concluída há mais de 30 dias sai do board e continua acessível pela ficha | integração |

```gherkin
Cenário: SCN-003.1 — Board com o trabalho distribuído
  Dado que sou participante de um projeto com fluxo configurado
  E que há tarefas em etapas diferentes
  Quando abro o board do projeto
  Então vejo cada tarefa na etapa em que está
  E vejo a condição de cada uma
  E vejo quem assumiu cada tarefa que está em curso
```

```gherkin
Cenário: SCN-003.2 — Etapa sem tarefa
  Dado que o fluxo do projeto tem uma etapa sem nenhuma tarefa
  Quando abro o board do projeto
  Então a etapa é exibida
  E é indicado que ela não tem tarefa alguma
```

```gherkin
Cenário: SCN-003.3 — Contagens que coexistem
  Dado que uma tarefa aguarda tomada há duas horas
  E que ela tem impedimento aberto há uma hora
  Quando abro o board do projeto
  Então vejo a espera de tomada e o tempo de impedimento como grandezas distintas
  E nenhuma delas é apresentada como soma da outra
  E a condição da tarefa continua sendo aguardando tomada, com a marca de impedimento exibida à parte
```

```gherkin
Cenário: SCN-003.4 — Conclusão antiga sai do board sem sair do registro
  Dado que uma tarefa foi concluída há mais de trinta dias
  E que outra tarefa foi concluída nesta semana
  Quando abro o board do projeto
  Então vejo na etapa terminal a tarefa concluída nesta semana
  E não vejo ali a tarefa concluída há mais de trinta dias
  E ao abrir a ficha da tarefa mais antiga vejo o histórico dela por inteiro
```

### RF-004 — Criar tarefa no projeto

- **Descrição:** participante com permissão de escrita cria uma tarefa, que
  nasce aguardando tomada na primeira etapa do fluxo do projeto.
- **Prioridade:** deve
- **Procedência:** derivada
- **Fonte:** RN-004
- **Regras aplicáveis:** RN-004, RN-006, RN-015
- **Origem no protótipo:** TL-05, estados preenchido, erro de validação e sucesso

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-004.1 | Tarefa criada nasce aguardando tomada na primeira etapa | integração |
| SCN-004.2 | Criação sem título é recusada | unitário |
| SCN-004.3 | Projeto sem fluxo configurado não aceita criação de tarefa | integração |

```gherkin
Cenário: SCN-004.1 — Tarefa nasce no início do fluxo
  Dado que participo de um projeto com fluxo configurado
  Quando crio uma tarefa informando seu título
  Então ela passa a existir na primeira etapa do fluxo
  E sua condição é aguardando tomada
  E ela não tem responsável individual
```

```gherkin
Cenário: SCN-004.2 — Criação sem título
  Dado que participo de um projeto com fluxo configurado
  Quando tento criar uma tarefa sem informar o título
  Então a criação é recusada
  E a razão da recusa me é informada
```

```gherkin
Cenário: SCN-004.3 — Projeto sem fluxo configurado
  Dado que participo de um projeto cujo fluxo ainda não foi configurado
  Quando tento criar uma tarefa
  Então a criação é recusada
  E sou informado de que o fluxo do projeto precisa ser configurado antes
```

### RF-005 — Avançar a tarefa para a etapa seguinte

- **Descrição:** o participante indica que a tarefa avança. A transição encerra
  a contagem de permanência na origem, inicia a do destino e recoloca a tarefa em
  aguardando tomada, sem responsável — é o registro que também é o aviso. A
  transição altera apenas etapa e condição de trabalho: a marca de impedimento,
  quando existe, atravessa a movimentação intacta.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante — Shape Brief, direção escolhida; `/solution`, fluxo
  "Registrar avanço da tarefa"
- **Regras aplicáveis:** RN-005, RN-006, RN-007, RN-009, RN-012, RN-013, RN-031,
  RN-032
- **Origem no protótipo:** TL-03 e TL-04, estados preenchido, transição recusada
  e ação concorrente recusada

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-005.1 | Avanço recoloca a tarefa aguardando tomada na etapa seguinte | integração |
| SCN-005.2 | Avanço para etapa não alcançável é recusado | integração |
| SCN-005.3 | Avanço a partir de origem divergente do estado corrente é recusado | integração |

```gherkin
Cenário: SCN-005.1 — Avanço para a etapa seguinte
  Dado que uma tarefa está em curso comigo na etapa Desenvolvimento
  E que a etapa seguinte do fluxo é Review
  Quando indico que ela avança para Review
  Então ela passa a estar na etapa Review
  E sua condição passa a ser aguardando tomada
  E ela deixa de ter responsável individual
  E a contagem de espera de tomada em Review começa
```

```gherkin
Cenário: SCN-005.2 — Etapa de destino não alcançável
  Dado que uma tarefa está na etapa Desenvolvimento
  E que Homologação não é adjacente a Desenvolvimento nem é a primeira etapa do fluxo
  Quando tento mover a tarefa para Homologação
  Então a transição é recusada
  E a tarefa permanece em Desenvolvimento
  E a razão da recusa me é informada
```

```gherkin
Cenário: SCN-005.3 — Origem declarada já não é a atual
  Dado que li a tarefa quando ela estava na etapa Desenvolvimento
  E que outra pessoa já a moveu para Review
  Quando peço para avançá-la a partir de Desenvolvimento
  Então a transição é recusada
  E me é informado que ela está agora em Review
  E nenhuma contagem é alterada
```

### RF-006 — Retroceder a tarefa e devolvê-la ao início do fluxo

- **Descrição:** a tarefa pode retroceder uma etapa, ou voltar à primeira etapa
  do fluxo a partir de qualquer etapa — a exceção nomeada de RN-005, criada para
  o caso da tarefa que precisa voltar ao início. Tarefa com impedimento aberto
  também pode retroceder, carregando a marca e a contagem.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante, 2026-09-09 (Q-01 e Q-02)
- **Regras aplicáveis:** RN-005, RN-008, RN-009, RN-013, RN-032
- **Origem no protótipo:** TL-03 e TL-04, menu de movimentação com as etapas
  alcançáveis

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-006.1 | Retrocesso de uma etapa recoloca a tarefa aguardando tomada | integração |
| SCN-006.2 | Retorno à primeira etapa é permitido de qualquer etapa | integração |
| SCN-006.3 | Tarefa com impedimento aberto retrocede sem que a marca seja apagada nem a contagem interrompida | integração |

```gherkin
Cenário: SCN-006.1 — Retrocesso de uma etapa
  Dado que uma tarefa está em curso na etapa Review
  E que a etapa anterior do fluxo é Desenvolvimento
  Quando indico que ela retorna para Desenvolvimento
  Então ela passa a estar em Desenvolvimento
  E sua condição passa a ser aguardando tomada
```

```gherkin
Cenário: SCN-006.2 — Retorno ao início do fluxo
  Dado que uma tarefa está na etapa Homologação
  E que a primeira etapa do fluxo é Backlog
  Quando indico que ela retorna para Backlog
  Então ela passa a estar em Backlog
  E a transição é aceita ainda que Backlog não seja adjacente a Homologação
```

```gherkin
Cenário: SCN-006.3 — Tarefa com impedimento aberto que retrocede
  Dado que uma tarefa está na etapa Review com impedimento aberto há três horas
  Quando ela é movida para Desenvolvimento
  Então ela passa a estar em Desenvolvimento
  E sua condição de trabalho passa a ser aguardando tomada
  E o impedimento continua aberto
  E o tempo de impedimento continua contando desde o início original
```

### RF-007 — Assumir tarefa que aguarda tomada

- **Descrição:** qualquer participante com permissão de escrita assume a tarefa
  que aguarda tomada na etapa. A tomada encerra a espera de handoff e registra
  quem assumiu e desde quando. É o fluxo que materializa E-02. Impedimento aberto
  não impede a tomada: quem assume uma tarefa impedida assume também o trabalho
  de destravá-la, e a contagem do impedimento segue correndo.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante — Shape Brief, jornada 2; Q-10
- **Regras aplicáveis:** RN-006, RN-007, RN-012, RN-031, RN-032, RN-033
- **Origem no protótipo:** TL-06 e TL-04, estados preenchido, sucesso e recusa
  concorrente

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-007.1 | Tomada registra o responsável e encerra a espera | integração |
| SCN-007.2 | Repetir a tomada por quem já assumiu não altera nada | unitário |
| SCN-007.3 | Segunda pessoa a assumir é recusada e informada de quem assumiu | integração |
| SCN-007.4 | Tomada de tarefa com impedimento aberto é aceita e preserva a marca | integração |

```gherkin
Cenário: SCN-007.1 — Tomada da tarefa
  Dado que uma tarefa aguarda tomada na etapa Review há duas horas
  E que participo do projeto com permissão de escrita
  Quando assumo a tarefa
  Então passo a constar como responsável por ela
  E sua condição passa a ser em curso
  E a espera de tomada é encerrada com duas horas registradas
```

```gherkin
Cenário: SCN-007.2 — Tomada repetida por quem já assumiu
  Dado que já assumi uma tarefa
  Quando peço para assumi-la novamente
  Então continuo constando como responsável
  E o momento em que assumi não é alterado
```

```gherkin
Cenário: SCN-007.3 — Duas pessoas assumem ao mesmo tempo
  Dado que uma tarefa aguarda tomada na etapa Review
  E que outra pessoa a assumiu um instante antes de mim
  Quando tento assumi-la
  Então minha ação é recusada
  E me é informado quem a assumiu
  E o responsável registrado não é alterado
```

```gherkin
Cenário: SCN-007.4 — Tomada de tarefa com impedimento aberto
  Dado que uma tarefa aguarda tomada na etapa Review há duas horas
  E que ela tem impedimento aberto há uma hora
  Quando assumo a tarefa
  Então passo a constar como responsável por ela
  E sua condição de trabalho passa a ser em curso
  E a espera de tomada é encerrada com duas horas registradas
  E o impedimento continua aberto, contando desde o início original
```

### RF-008 — Devolver tarefa assumida

- **Descrição:** quem assumiu pode devolver a tarefa ao pool da etapa. Ela volta
  a aguardar tomada na mesma etapa e a contagem de espera recomeça.
- **Prioridade:** deveria
- **Procedência:** informada
- **Fonte:** Demandante — `/solution`, caminho alternativo do fluxo de tomada
- **Regras aplicáveis:** RN-006, RN-030, RN-012, RN-032
- **Origem no protótipo:** TL-04, ação de devolver no painel de detalhe

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-008.1 | Devolução recoloca a tarefa no pool e reinicia a espera | integração |
| SCN-008.2 | Quem não assumiu não pode devolver | integração |
| SCN-008.3 | Devolução de tarefa com impedimento aberto preserva a marca, e a tarefa segue assumível | integração |

```gherkin
Cenário: SCN-008.1 — Devolução ao pool da etapa
  Dado que assumi uma tarefa na etapa Review
  Quando a devolvo
  Então ela volta a aguardar tomada na etapa Review
  E deixa de ter responsável individual
  E uma nova contagem de espera de tomada começa
```

```gherkin
Cenário: SCN-008.2 — Devolução por quem não assumiu
  Dado que uma tarefa foi assumida por outra pessoa
  Quando tento devolvê-la
  Então a devolução é recusada
  E o responsável registrado não é alterado
```

```gherkin
Cenário: SCN-008.3 — Devolução com impedimento aberto
  Dado que assumi uma tarefa e sinalizei um impedimento nela
  Quando a devolvo
  Então ela volta a aguardar tomada na mesma etapa
  E o impedimento continua aberto com sua contagem em curso
  E ela continua disponível para ser assumida por qualquer participante
```

### RF-009 — Sinalizar impedimento

- **Descrição:** o participante declara que a tarefa não pode prosseguir por
  causa externa, informando o motivo. A tarefa passa a constar com impedimento
  aberto, preservando a etapa **e a condição de trabalho**, e passa a se destacar
  para quem responde pelo desbloqueio.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante — Intent e Shape Brief, jornada 1
- **Regras aplicáveis:** RN-008, RN-010, RN-024, RN-026, RN-031, RN-032
- **Origem no protótipo:** TL-04, bloco de impedimento; TL-03, marca no cartão

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-009.1 | Impedimento sinalizado preserva a etapa e a condição de trabalho, e inicia contagem própria | integração |
| SCN-009.2 | Sinalização sem motivo é recusada | unitário |
| SCN-009.3 | Segunda sinalização anexa informação ao impedimento aberto | integração |

```gherkin
Cenário: SCN-009.1 — Impedimento aberto
  Dado que uma tarefa está em curso na etapa Desenvolvimento
  Quando declaro que ela está impedida informando o motivo
  Então ela passa a constar com impedimento aberto
  E sua condição de trabalho continua sendo em curso
  E ela permanece na etapa Desenvolvimento
  E a contagem do tempo de impedimento começa
  E ela passa a se destacar para quem responde pelo desbloqueio no projeto
```

```gherkin
Cenário: SCN-009.2 — Impedimento sem motivo
  Dado que uma tarefa está em curso
  Quando tento declará-la impedida sem informar o motivo
  Então a sinalização é recusada
  E nenhum impedimento é aberto na tarefa
```

```gherkin
Cenário: SCN-009.3 — Sinalização sobre tarefa já impedida
  Dado que uma tarefa já tem impedimento aberto há duas horas
  Quando sinalizo um novo impedimento nela informando outro motivo
  Então nenhum segundo impedimento é criado
  E a informação é anexada ao impedimento existente
  E a contagem em curso não é reiniciada
```

### RF-010 — Resolver impedimento

- **Descrição:** quem responde pelo desbloqueio — ou o próprio participante que
  sinalizou — declara o impedimento resolvido, registrando o desfecho. A marca de
  impedimento é apagada e sua contagem encerrada; etapa e condição de trabalho
  ficam como estavam, porque a resolução não é transição.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante — `/solution`, fluxo "Desbloquear tarefa impedida"
- **Regras aplicáveis:** RN-008, RN-024, RN-031, RN-032
- **Origem no protótipo:** TL-04 e TL-06, seção de impedimentos que esperam
  desbloqueio

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-010.1 | Resolução encerra a contagem e apaga a marca, sem alterar etapa nem condição de trabalho | integração |
| SCN-010.2 | Quem sinalizou também pode resolver | integração |
| SCN-010.3 | Resolver impedimento já resolvido não altera nada | unitário |

```gherkin
Cenário: SCN-010.1 — Impedimento resolvido
  Dado que uma tarefa em curso tem impedimento aberto há três horas
  Quando declaro o impedimento resolvido registrando o desfecho
  Então a contagem de impedimento é encerrada com três horas registradas
  E a tarefa deixa de constar com impedimento aberto
  E sua condição de trabalho continua sendo em curso
  E permanece na etapa em que estava
```

```gherkin
Cenário: SCN-010.2 — Resolução por quem sinalizou
  Dado que eu mesmo sinalizei o impedimento de uma tarefa
  E que não tenho o papel de desbloqueio no projeto
  Quando declaro o impedimento resolvido registrando o desfecho
  Então a resolução é aceita
```

```gherkin
Cenário: SCN-010.3 — Resolução repetida
  Dado que um impedimento já foi resolvido
  Quando peço novamente sua resolução
  Então nada é alterado
  E o tempo de impedimento registrado permanece o mesmo
```

### RF-011 — Concluir tarefa em etapa terminal

- **Descrição:** ao chegar a uma etapa terminal de sucesso, a tarefa é
  concluída: todas as contagens cessam em definitivo e ela deixa de figurar como
  trabalho em curso. Tarefa com impedimento aberto não pode ser concluída.
- **Prioridade:** deve
- **Procedência:** derivada
- **Fonte:** RN-001 e RN-011
- **Regras aplicáveis:** RN-001, RN-011, RN-031
- **Origem no protótipo:** TL-04, ação Concluir indisponível por impedimento
  aberto

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-011.1 | Chegada à etapa terminal conclui a tarefa e cessa as contagens | integração |
| SCN-011.2 | Conclusão fora de etapa terminal é recusada | integração |
| SCN-011.3 | Conclusão com impedimento aberto é recusada | integração |

```gherkin
Cenário: SCN-011.1 — Conclusão em etapa terminal
  Dado que uma tarefa está em curso na última etapa antes da terminal
  E que a etapa Concluído é terminal no fluxo do projeto
  Quando a movo para Concluído
  Então sua condição passa a ser concluída
  E todas as contagens de tempo cessam
  E ela deixa de figurar como trabalho em curso
```

```gherkin
Cenário: SCN-011.2 — Conclusão sem passar por etapa terminal
  Dado que uma tarefa está na etapa Review, que não é terminal
  Quando tento marcá-la como concluída
  Então a operação é recusada
  E sou informado de que a conclusão se dá ao alcançar uma etapa terminal
```

```gherkin
Cenário: SCN-011.3 — Conclusão com impedimento aberto
  Dado que uma tarefa tem impedimento aberto
  Quando tento movê-la para a etapa terminal Concluído
  Então a operação é recusada
  E sou informado de que o impedimento precisa ter desfecho registrado antes
  E o impedimento permanece aberto
```

### RF-012 — Encerrar tarefa sem conclusão

- **Descrição:** quem tem permissão de configuração no projeto retira do fluxo
  uma tarefa que não será concluída — cancelada, obsoleta ou com impedimento
  permanente. O histórico é preservado; a tarefa não é apagada.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante, 2026-09-09 (Q-05)
- **Regras aplicáveis:** RN-011, RN-016, RN-018, RN-031, RN-032
- **Origem no protótipo:** TL-04, ação de encerramento sem conclusão

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-012.1 | Encerramento por quem configura retira a tarefa do fluxo | integração |
| SCN-012.2 | Participante sem permissão de configuração é recusado | integração |
| SCN-012.3 | Tarefa encerrada sem conclusão não retorna ao fluxo | integração |
| SCN-012.4 | Encerramento é recusado enquanto o impedimento não tem desfecho | integração |

```gherkin
Cenário: SCN-012.1 — Encerramento sem conclusão
  Dado que tenho permissão de configuração no projeto
  E que uma tarefa está em curso e não será mais realizada
  E que ela não tem impedimento aberto
  Quando a encerro sem conclusão registrando o motivo
  Então sua condição passa a ser encerrada sem conclusão
  E as contagens de permanência na etapa e de espera de tomada cessam
  E o histórico dela é preservado
```

```gherkin
Cenário: SCN-012.2 — Encerramento por participante comum
  Dado que participo do projeto sem permissão de configuração
  Quando tento encerrar uma tarefa sem conclusão
  Então a operação é recusada
  E a condição da tarefa não é alterada
```

```gherkin
Cenário: SCN-012.3 — Retorno de tarefa encerrada sem conclusão
  Dado que uma tarefa está encerrada sem conclusão
  Quando tento movê-la para qualquer etapa
  Então a operação é recusada
  E sou informado de que o caminho é abrir outra tarefa
```

```gherkin
Cenário: SCN-012.4 — Encerramento de tarefa com impedimento aberto
  Dado que tenho permissão de configuração no projeto
  E que uma tarefa tem impedimento aberto sem desfecho registrado
  Quando tento encerrá-la sem conclusão
  Então a operação é recusada
  E sou informado de que o desfecho do impedimento precisa ser registrado antes
  E a condição da tarefa não é alterada
  E o impedimento permanece aberto
```

### RF-013 — Reabrir tarefa concluída

- **Descrição:** o Product Owner, e somente ele, reabre uma tarefa concluída. A
  reabertura é registrada como retorno no histórico e inicia um novo episódio de
  contagem, que o agregado precisa distinguir da primeira passagem. A tarefa
  reaberta retorna à primeira etapa do fluxo: retrabalho refaz o percurso, e
  devolvê-la à etapa terminal a deixaria concluída de novo no instante seguinte.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante, 2026-09-04 (Q-06); etapa de retorno decidida na emenda
  de 2026-09-09 (INC-04)
- **Regras aplicáveis:** RN-017, RN-019, RN-034
- **Origem no protótipo:** TL-10, com a variante "sem permissão" renderizada

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-013.1 | Product Owner reabre e a tarefa volta à primeira etapa do fluxo | integração |
| SCN-013.2 | Quem não é Product Owner não reabre | integração |
| SCN-013.3 | O tempo por etapa da tarefa reaberta passa a somar episódios | integração |

```gherkin
Cenário: SCN-013.1 — Reabertura pelo Product Owner
  Dado que sou o Product Owner do projeto
  E que uma tarefa está concluída
  Quando a reabro informando o motivo
  Então ela volta a aguardar tomada na primeira etapa do fluxo
  E fica sem responsável individual
  E o retorno fica registrado no histórico dela como novo episódio
```

```gherkin
Cenário: SCN-013.2 — Reabertura por outro papel
  Dado que participo do projeto e não sou o Product Owner
  E que uma tarefa está concluída
  Quando tento reabri-la
  Então a operação é recusada
  E a tarefa permanece concluída
```

```gherkin
Cenário: SCN-013.3 — Tempo por etapa de tarefa reaberta
  Dado que uma tarefa permaneceu quatro horas em Review antes de ser concluída
  E que ela foi reaberta e passou mais duas horas em Review
  Quando consulto o tempo por etapa dessa tarefa
  Então vejo seis horas em Review
  E vejo que esse total corresponde a dois episódios distintos
```

### RF-014 — Minha fila do que aguarda tomada por mim

- **Descrição:** visão que atravessa todos os projetos em que a pessoa
  participa, reunindo o que aguarda tomada por ela e os impedimentos pelos quais
  responde. Existe para que descobrir que a vez chegou não exija abrir board
  algum — é o suporte mais direto de H-01.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante — DDR-006; QD-04 decidida em 2026-09-09
- **Regras aplicáveis:** RN-006, RN-007, RN-014, RN-024, RN-026, RN-028
- **Origem no protótipo:** TL-06, estados preenchido, vazio e recusa concorrente

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-014.1 | A fila reúne itens de todos os projetos, o mais parado no topo | integração |
| SCN-014.2 | A ordenação pode ser invertida pela pessoa | e2e |
| SCN-014.3 | Nada aguardando tomada exibe fila vazia, sem contagem alguma | integração |

```gherkin
Cenário: SCN-014.1 — Fila atravessando projetos
  Dado que participo de dois projetos
  E que há uma tarefa aguardando tomada há cinco horas em um deles
  E que há outra aguardando tomada há uma hora no outro
  Quando abro minha fila
  Então vejo as duas tarefas
  E a que aguarda há cinco horas aparece antes da que aguarda há uma hora
```

```gherkin
Cenário: SCN-014.2 — Inverter a ordenação da fila
  Dado que minha fila está ordenada pela maior espera primeiro
  Quando inverto a ordenação
  Então a tarefa que aguarda há menos tempo passa a aparecer primeiro
```

```gherkin
Cenário: SCN-014.3 — Fila vazia
  Dado que nenhuma tarefa aguarda tomada nos projetos em que participo
  Quando abro minha fila
  Então vejo que nada aguarda tomada por mim
  E nenhum tempo de espera me é apresentado
```

### RF-015 — Consultar o andamento do projeto

- **Descrição:** visão do trabalho distribuído pelas etapas, com o que está
  impedido e há quanto tempo. Substitui a consolidação manual do Product Owner. É
  a visão que o gestor de outro time acessa em modo somente-leitura.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante — Shape Brief, jornadas 4 e 5
- **Regras aplicáveis:** RN-014, RN-015, RN-026
- **Origem no protótipo:** TL-07, nas duas variantes de permissão — escrita e
  somente-leitura. O seletor de papel na topbar do protótipo é recurso de
  demonstração para alternar entre elas, e **não** é função do produto: nenhum
  requisito institui troca de papel pelo usuário

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-015.1 | Andamento exibe a distribuição por etapa e os impedimentos abertos | integração |
| SCN-015.2 | Gestor somente-leitura não recebe nenhuma ação de escrita | e2e |
| SCN-015.3 | Impedimento antigo permanece visível sem que nada seja acionado | integração |

```gherkin
Cenário: SCN-015.1 — Andamento do projeto
  Dado que participo de um projeto com tarefas em várias etapas
  E que duas delas estão impedidas
  Quando consulto o andamento do projeto
  Então vejo quantas tarefas há em cada etapa
  E vejo as duas tarefas impedidas e há quanto tempo cada uma está assim
```

```gherkin
Cenário: SCN-015.2 — Consulta por quem só tem leitura
  Dado que tenho acesso somente-leitura ao projeto
  Quando consulto o andamento do projeto
  Então vejo o andamento
  E nenhuma ação de escrita me é apresentada
  E qualquer escrita que eu solicite por outro caminho é recusada
```

```gherkin
Cenário: SCN-015.3 — Impedimento parado há muito tempo
  Dado que uma tarefa está impedida há cinco dias
  Quando consulto o andamento do projeto
  Então vejo a tarefa impedida com os cinco dias acumulados à mostra
  E nenhuma ação automática foi disparada por causa do tempo decorrido
```

### RF-016 — Consultar o tempo por etapa

- **Descrição:** agregado do tempo de permanência, da espera de tomada e do
  tempo de impedimento, apurados como séries distintas, por etapa e por projeto.
  Nunca por pessoa. É o requisito que produz a linha de base que hoje não existe.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante — Intent e Shape Brief, métrica de sucesso; Q-09 e Q-01
- **Regras aplicáveis:** RN-007, RN-008, RN-014, RN-019, RN-025, RN-029
- **Origem no protótipo:** TL-07, estado "ainda não medido" distinto de zero

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-016.1 | As três séries são apresentadas separadamente, por etapa | integração |
| SCN-016.2 | Nenhuma agregação por pessoa é possível | integração |
| SCN-016.3 | Projeto sem histórico informa "ainda não medido", nunca zero | integração |

```gherkin
Cenário: SCN-016.1 — Séries de tempo separadas
  Dado que um projeto acumulou histórico em três etapas
  Quando consulto o tempo por etapa
  Então vejo, para cada etapa, o tempo de permanência agregado
  E vejo a espera de tomada como série distinta
  E vejo o tempo de impedimento como série distinta
  E nenhuma das três é apresentada como soma das outras
```

```gherkin
Cenário: SCN-016.2 — Tentativa de agregar por pessoa
  Dado que sou o Product Owner do projeto
  Quando consulto o tempo por etapa
  Então nenhum recorte, filtro, ordenação ou exportação por pessoa me é oferecido
  E qualquer agregação por pessoa solicitada por outro caminho é recusada
```

```gherkin
Cenário: SCN-016.3 — Projeto sem histórico
  Dado que um projeto foi criado e nenhuma tarefa concluiu etapa alguma
  Quando consulto o tempo por etapa
  Então sou informado de que ainda não há medição
  E nenhum valor zero me é apresentado como tempo
```

### RF-017 — Configurar as etapas do projeto

- **Descrição:** quem tem permissão de configuração define as etapas do projeto,
  a ordem entre elas e quais são terminais. A configuração vale dali em diante e
  não reescreve histórico. Etapa com tarefas não pode ser removida.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante — Intent; Q-07 decidida em 2026-09-09
- **Regras aplicáveis:** RN-001, RN-020, RN-021, RN-022
- **Origem no protótipo:** TL-08, erros "sem etapa terminal" e "remover etapa com
  tarefas"

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-017.1 | Fluxo configurado passa a valer para as transições seguintes | integração |
| SCN-017.2 | Configuração sem etapa terminal é recusada | unitário |
| SCN-017.3 | Remoção de etapa que contém tarefas é recusada | integração |

```gherkin
Cenário: SCN-017.1 — Fluxo configurado
  Dado que tenho permissão de configuração no projeto
  Quando defino as etapas do projeto, sua ordem e quais são terminais
  Então o fluxo passa a valer para as transições seguintes
  E o histórico já acumulado permanece inalterado
```

```gherkin
Cenário: SCN-017.2 — Fluxo sem etapa terminal
  Dado que tenho permissão de configuração no projeto
  Quando tento salvar um fluxo em que nenhuma etapa é terminal
  Então a configuração é recusada
  E o fluxo vigente não é alterado
```

```gherkin
Cenário: SCN-017.3 — Remoção de etapa ocupada
  Dado que a etapa Review tem três tarefas
  Quando tento removê-la do fluxo
  Então a remoção é recusada
  E sou informado de que a etapa precisa ser esvaziada antes
  E as três tarefas permanecem em Review
```

### RF-018 — Configurar as raias do projeto

- **Descrição:** quem tem permissão de configuração define as raias que agrupam
  o trabalho dentro das etapas. Raia é agrupamento livre: não restringe transição
  e não entra em nenhuma agregação.
- **Prioridade:** deveria
- **Procedência:** informada
- **Fonte:** Demandante, 2026-09-09 (Q-08)
- **Regras aplicáveis:** RN-022, RN-023
- **Origem no protótipo:** TL-08 e TL-03, raia como faixa na etapa e chip no
  cartão

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-018.1 | Raias definidas agrupam o trabalho dentro de cada etapa | integração |
| SCN-018.2 | Raia não restringe transição entre etapas | integração |
| SCN-018.3 | Nenhum agregado é apurado por raia | integração |

```gherkin
Cenário: SCN-018.1 — Raias no board
  Dado que tenho permissão de configuração no projeto
  Quando defino duas raias para o projeto
  Então o board passa a agrupar as tarefas por essas raias dentro de cada etapa
```

```gherkin
Cenário: SCN-018.2 — Transição entre tarefas de raias diferentes
  Dado que uma tarefa pertence à raia Sustentação
  E que a etapa de destino contém apenas tarefas da raia Projeto
  Quando movo a tarefa para a etapa seguinte
  Então a transição é aceita
  E a tarefa mantém a raia Sustentação
```

```gherkin
Cenário: SCN-018.3 — Agregação por raia
  Dado que o projeto tem raias definidas e histórico acumulado
  Quando consulto o tempo por etapa
  Então nenhum recorte por raia me é oferecido
```

### RF-019 — Gerir participação e permissões do projeto

- **Descrição:** quem tem permissão de configuração define quem participa do
  projeto e com que permissão, incluindo o papel de desbloqueio e o acesso
  somente-leitura. A permissão é por projeto e acumulável entre projetos.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante — Intent; ADR-003 e BDR-001
- **Regras aplicáveis:** RN-015, RN-024, RN-027, RN-028
- **Origem no protótipo:** TL-09, incluindo a remoção de quem tem tarefa assumida

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-019.1 | Permissão concedida passa a valer imediatamente | integração |
| SCN-019.2 | Remover quem tem tarefa assumida devolve a tarefa ao pool | integração |
| SCN-019.3 | Sem papel de desbloqueio configurado, o impedimento vai a todos | integração |
| SCN-019.4 | Remoção de participação interrompe o acompanhamento em tempo real de quem já estava com o projeto aberto | e2e |

```gherkin
Cenário: SCN-019.1 — Concessão de participação
  Dado que tenho permissão de configuração no projeto
  Quando concedo a uma pessoa participação com permissão de escrita
  Então ela passa a ver o projeto entre os seus
  E passa a poder agir sobre as tarefas dali em diante
```

```gherkin
Cenário: SCN-019.2 — Remoção de quem tem tarefa assumida
  Dado que uma pessoa assumiu uma tarefa na etapa Review
  Quando removo sua participação do projeto
  Então a tarefa volta a aguardar tomada na etapa Review
  E o registro de que ela havia assumido é preservado no histórico
```

```gherkin
Cenário: SCN-019.3 — Projeto sem papel de desbloqueio
  Dado que o projeto não tem ninguém com o papel de desbloqueio
  Quando uma tarefa é declarada impedida
  Então o impedimento é registrado
  E ele se destaca para todos os participantes do projeto
```

```gherkin
Cenário: SCN-019.4 — Acompanhamento em tempo real após remoção da participação
  Dado que uma pessoa está com o board do projeto aberto acompanhando as alterações
  Quando removo sua participação do projeto
  Então ela deixa de receber as alterações do projeto sem precisar fechar nem recarregar a página
  E uma nova consulta ao projeto lhe é recusada
```

### RF-020 — Refletir no board as alterações feitas por outras pessoas

- **Descrição:** quem está com o board aberto passa a ver as alterações feitas
  por outros participantes sem precisar recarregar a página. É o que sustenta a
  premissa de que o registro é o próprio aviso — e o que torna a recusa
  concorrente compreensível para quem perdeu.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Intent — limite declarado; ADR-004
- **Regras aplicáveis:** RN-012, RN-013
- **Origem no protótipo:** TL-03, região de anúncio de mudança de estado
- **Envelope de tempo:** ver RNF-001

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-020.1 | Tarefa movida por outra pessoa aparece na nova etapa sem recarregar | e2e |
| SCN-020.2 | Item que chega à minha fila aparece sem recarregar | e2e |
| SCN-020.3 | Quem perde a ação concorrente recebe o estado atual, não um erro genérico | e2e |

```gherkin
Cenário: SCN-020.1 — Movimentação feita por outra pessoa
  Dado que estou com o board do projeto aberto
  Quando outra pessoa move uma tarefa de Desenvolvimento para Review
  Então a tarefa passa a aparecer em Review no meu board
  E eu não precisei recarregar a página
```

```gherkin
Cenário: SCN-020.2 — Chegada à minha fila
  Dado que estou com minha fila aberta
  E que nada aguardava tomada por mim
  Quando uma tarefa é movida para uma etapa em que sou participante
  Então ela passa a aparecer na minha fila com a espera correndo
  E eu não precisei recarregar a página
```

```gherkin
Cenário: SCN-020.3 — Perda em ação concorrente
  Dado que estou com o board aberto e vejo uma tarefa aguardando tomada
  E que outra pessoa a assumiu um instante antes de mim
  Quando tento assumi-la
  Então minha ação é recusada
  E vejo quem assumiu e desde quando
  E o board passa a exibir a tarefa em curso com o responsável correto
```

### RF-021 — Administrar o sistema como administrador global

- **Descrição:** existe um alcance de administração global que dispensa a
  participação no projeto para ver e agir em qualquer projeto do sistema. É a
  única capacidade que atravessa a fronteira entre visibilidade e execução, e por
  isso é nomeada, restringida e auditada: a promoção é única por pessoa, feita
  por identificador verificado da conta corporativa, e todo acesso exercido por
  esse alcance é identificado como tal na resposta. O alcance é de escopo, não de
  imunidade — não dispensa RN-014 nem torna o histórico alterável.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante, emenda de 2026-09-09 (INC-03); ADR-003, ADR-010,
  BDR-001
- **Regras aplicáveis:** RN-014, RN-028, RN-035
- **Origem no protótipo:** sem tela própria. O alcance se exerce sobre as telas
  já existentes; a promoção não tem interface no produto

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-021.1 | Promoção a administrador global é única, por identificador verificado, e fica registrada | integração |
| SCN-021.2 | Administrador global acessa projeto em que não participa, e o acesso é identificado como global | integração |
| SCN-021.3 | O alcance global não dispensa a proibição de agregar por pessoa nem torna o histórico alterável | integração |

```gherkin
Cenário: SCN-021.1 — Promoção única e auditada
  Dado que ainda não existe administrador global no sistema
  E que há uma conta corporativa com identificador verificado designada para isso
  Quando essa pessoa é promovida a administradora global
  Então a promoção fica registrada com quem foi promovido e quando
  E uma segunda promoção pelo mesmo caminho é recusada
```

```gherkin
Cenário: SCN-021.2 — Acesso a projeto sem participação
  Dado que sou administrador global
  E que existe um projeto do qual não participo
  Quando abro esse projeto
  Então vejo o board e posso agir sobre as tarefas
  E me é indicado que estou agindo pelo alcance de administração global
```

```gherkin
Cenário: SCN-021.3 — Limites que o alcance global não ultrapassa
  Dado que sou administrador global
  Quando consulto o tempo por etapa de qualquer projeto e tento alterar tempo já contado
  Então nenhuma visão me oferece tempo agregado por pessoa
  E a alteração de tempo já contado é recusada como para qualquer outro perfil
```

---

### RF-022 — Criar projeto

- **Descrição:** o administrador global cria um projeto informando o nome e a
  pessoa que passa a ser sua primeira `project_admin`. As duas coisas acontecem
  na mesma operação, porque um projeto sem participante nenhum é inalcançável:
  ninguém dentro dele pode conceder a primeira participação, e quem cria não se
  torna participante — o alcance global já lhe dá acesso, e virar participante
  confundiria escopo com participação. O projeto nasce **sem fluxo**, e por isso
  ainda não aceita tarefa: a configuração das etapas continua sendo RF-017, e a
  recusa até lá é explícita, não silenciosa. Por isso o desfecho bem-sucedido da
  criação nomeia a configuração do fluxo como passo seguinte obrigatório e
  oferece a ida até ela, conforme RN-038 passou a exigir.
- **Prioridade:** deve
- **Procedência:** informada
- **Fonte:** Demandante, emenda de 2026-09-10 — pendência 09 do `state.md`,
  lacuna encontrada pela `/tests`; ADR-010, BDR-001
- **Regras aplicáveis:** RN-001, RN-028, RN-035, RN-036, RN-037, RN-038
- **Origem no protótipo:** TL-11, painel sobre `/projetos` visível apenas para a
  administração global, com validação, envio e sucesso; e TL-02, no vazio "ainda
  não existe projeto neste sistema" e no cartão "fluxo não configurado". Tela
  instituída em 2026-09-10 por decisão do demandante (INC-18)

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-022.1 | Administrador global cria o projeto e nomeia a primeira project_admin | integração |
| SCN-022.2 | Quem não é administrador global não cria projeto | integração |
| SCN-022.3 | Projeto recém-criado não aceita tarefa antes de o fluxo ser configurado | integração |

```gherkin
Cenário: SCN-022.1 — Criação com a primeira participação nomeada
  Dado que sou administrador global
  E que existe uma conta corporativa que ainda não participa de projeto nenhum
  Quando crio um projeto informando o nome e essa pessoa como primeira administradora do projeto
  Então o projeto passa a existir com esse nome
  E essa pessoa consta como participante com o papel de administradora do projeto
  E eu não consto como participante do projeto que criei
```

```gherkin
Cenário: SCN-022.2 — Criação recusada a quem não tem o alcance global
  Dado que participo de projetos como administrador de projeto
  E que não sou administrador global
  Quando tento criar um projeto
  Então a criação é recusada com razão explícita
  E nenhum projeto é criado
```

```gherkin
Cenário: SCN-022.3 — Projeto nasce sem fluxo e recusa tarefa até ser configurado
  Dado que um projeto acabou de ser criado
  E que seu fluxo ainda não tem etapa nenhuma
  Quando a administradora do projeto tenta criar uma tarefa
  Então a criação é recusada, indicando que o fluxo precisa ser configurado antes
  E depois que ela configura o fluxo a criação da tarefa é aceita
```

---

## Requisitos não-funcionais

| ID | Requisito | Envelope | Condição de medição | Procedência |
| --- | --- | --- | --- | --- |
| RNF-001 | Propagação de alteração de estado aos demais usuários com sessão aberta, sem recarregamento manual | p95 ≤ 2 s entre a aceitação da escrita e a atualização visível nas demais sessões | Board com 100 tarefas e 50 sessões abertas no mesmo projeto, medido do aceite da escrita ao repintar da sessão observadora | informada (Intent — limite declarado) |
| RNF-002 | Operação de 1 a N instâncias sem divergência de estado entre elas | Nenhuma divergência observável entre instâncias; suporte de dezenas a centenas de usuários simultâneos, com alvo de 300 sessões ativas | Executar com 3 instâncias e 300 sessões, aplicando escritas concorrentes na mesma tarefa; comparar o estado devolvido por cada instância | informada (Intent — limite declarado) |
| RNF-003 | Empacotamento e execução em contêineres na plataforma corporativa | Subida completa a partir da imagem, sem passo manual; parada e nova subida sem perda de estado persistido | Subir a partir da imagem publicada em ambiente limpo e executar o percurso TL-03 → TL-04 → TL-06 | informada (Intent — limite declarado; ADR-008) |
| RNF-004 | Nenhuma escrita se completa com verificação feita apenas do lado do cliente | 100% das operações de escrita reavaliadas contra o estado corrente no servidor antes do aceite | Para cada operação de escrita, submeter solicitação forjada que burle a verificação de interface e verificar a recusa | informada (Intent — limite declarado) |
| RNF-005 | Interface responsiva para desktop nos navegadores usados pela equipe | Funcional de 1280 px a 1024 px de largura, sem perda de ação nem rolagem horizontal não indicada. Abaixo de 1024 px não é alvo | Percorrer as 11 telas nas duas larguras extremas, nos navegadores em uso pela equipe | informada (Intent — limite declarado) |
| RNF-006 | Acessibilidade WCAG 2.1 AA | Zero violação de nível A ou AA nas 11 telas; toda operação de board executável por teclado; contraste conforme nos temas claro e escuro | Verificação automatizada de acessibilidade nas 11 telas, mais percurso completo do handoff usando apenas teclado | informada (DDR-005) |
| RNF-007 | Instrumentação das quatro métricas de sucesso, que hoje não existe | Espera de tomada, tempo de permanência, tempo de impedimento e proporção do trabalho registrado, apurados por etapa e por projeto e exportáveis para leitura ao fim de cada período | Ao fim do primeiro período de uso, as quatro séries existem e são consultáveis; nenhuma delas é apurável por pessoa | informada (Shape Brief — métrica de sucesso; instrumentação declarada inexistente) |
| RNF-008 | Histórico de transições e de impedimentos é registro imutável | Nenhuma operação do produto altera ou remove evento já registrado; retenção por todo o tempo de vida do projeto | Tentar alterar tempo já contado por cada caminho exposto pelo produto e verificar a recusa | derivada (RN-022 e a transição proibida "alterar retroativamente tempo já contado") |
| RNF-009 | Tempo de resposta das consultas de andamento e de tempo por etapa | p95 ≤ 2 s com 12 meses de histórico e 5.000 tarefas no projeto | Carga sintética com 12 meses de histórico, medindo as consultas de RF-015 e RF-016 | inferida pelo agente |
| RNF-011 | Tempo de resposta da leitura do board (RF-003) | p95 ≤ 2 s com 5.000 tarefas no projeto, incluídas as terminais fora da janela de RN-039. O envelope vale para a resposta completa, e não para a primeira consulta: o board é montado em memória a partir de um número fixo de consultas, e o custo que cresce é o de materializar e serializar o resultado | Carga sintética de 5.000 tarefas num único projeto, com o mesmo arnês de RNF-009, medindo `GET` do board. A medição é feita **com** o recorte de RN-039 em vigor, porque é ele que separa o que o board carrega do que o projeto acumula | informada (Demandante, emenda de 2026-09-16 — ACH-03 da revisão técnica do board) |
| RNF-010 | Limite de requisições por sujeito autenticado, para que uso anômalo de uma conta não degrade o sistema para as demais | 120 solicitações de leitura por minuto e 30 de escrita por minuto, por sujeito. Excedente é **recusado com razão explícita e com a indicação de quando repetir**, nunca descartado em silêncio. O envelope é ponto de partida sem base empírica e é revisto ao fim do primeiro período de uso | Submeter rajada acima e abaixo de cada limite, verificando o aceite abaixo, a recusa informada acima e que o consumo de um sujeito não afeta a resposta de outro | informada (Demandante, emenda de 2026-09-09 — INC-05) |

> **Envelope** é o limite verificável (ex: p95 < 300 ms). RNF sem envelope e sem
> condição de medição não é verificável e reprova o GATE-NFR mais adiante.

---

## Procedência — resumo

| Tipo | Quantidade | IDs |
| --- | --- | --- |
| informada | 54 | RN-039, RNF-011, RN-001, RN-004, RN-005, RN-006, RN-007, RN-008, RN-009, RN-010, RN-012, RN-014, RN-015, RN-016, RN-017, RN-018, RN-020, RN-022, RN-023, RN-024, RN-025, RN-026, RN-027, RN-028, RN-030, RN-032, RN-033, RN-034, RN-035, RF-001, RF-005, RF-006, RF-007, RF-008, RF-009, RF-010, RF-012, RF-013, RF-014, RF-015, RF-016, RF-017, RF-018, RF-019, RF-020, RF-021, RNF-001, RNF-002, RNF-003, RNF-004, RNF-005, RNF-006, RNF-007, RNF-010 |
| derivada | 13 | RN-002, RN-003, RN-011, RN-013, RN-019, RN-021, RN-029, RN-031, RF-002, RF-003, RF-004, RF-011, RNF-008 |
| extraída de legado | 0 | nenhum |
| hipótese a validar | 0 | nenhum — as hipóteses do Shape Brief permanecem lá, e nenhuma regra ou requisito deste PRD repousa sobre hipótese não decidida |
| inferida pelo agente | 1 | RNF-009 |

Total: 36 regras de negócio, 21 requisitos funcionais e 11 não-funcionais,
com 66 cenários de aceite.

> **Toda** regra e requisito tem exatamente um tipo de procedência.
> `hipótese a validar` exige experimento e critério de descarte na tabela abaixo.
> `inferida pelo agente` é permitida, mas cada ocorrência precisa ser confirmada
> por humano antes do gate — é o tipo que a auditoria vai olhar primeiro.

### Hipóteses a validar

| ID | Experimento | Critério de descarte | Prazo |
| --- | --- | --- | --- |
| H-01 | Herdada do Shape Brief, não é regra deste PRD. Medir a adesão no primeiro período de uso: proporção do trabalho em curso registrada no sistema, e se o registro precede ou sucede a comunicação nos canais atuais | Se o trabalho continuar sendo comunicado primeiro nos canais e replicado no board depois. Nesse caso E-03 volta à mesa e a restrição de notificação interna é reaberta | Fim do primeiro período de uso |
| H-03 | Herdada do Shape Brief e mantida aberta por decisão de 2026-09-09: entrevista direta com ao menos um desenvolvedor e um gestor de outro time | Se a entrevista revelar objetivo ou frustração que mude a ordem de prioridade das jornadas. Consequência assumida: os cenários já estarão congelados e a correção exigirá emenda | Antes do início da implementação |

### Inferências do agente pendentes de confirmação

| ID | O que foi inferido | Por que não havia fonte | Confirmado por |
| --- | --- | --- | --- |
| RNF-009 | Envelope de p95 ≤ 2 s para as consultas de andamento e de tempo por etapa, com 12 meses de histórico e 5.000 tarefas | A Intent declara envelope para propagação de eventos, mas nada sobre tempo de resposta de consulta. Sem envelope, RF-015 e RF-016 reprovariam o GATE-NFR por não serem verificáveis. O valor foi espelhado no de RNF-001, e o volume estimado a partir do porte descrito no discovery | Thiago Cavalcante, 2026-09-09 — confirmado sem alteração |

---

## Dúvidas materiais em aberto

> Dúvida material bloqueia o GATE-SPEC.

| # | Dúvida | Afeta | Quem responde | Status |
| --- | --- | --- | --- | --- |
| DM-01 | Envelope de RNF-009 inferido pelo agente, sem fonte declarada | RF-015, RF-016 | Demandante | Resolvida em 2026-09-09: envelope confirmado como estava — p95 ≤ 2 s com 12 meses de histórico e 5.000 tarefas |
| DM-02 | QD-02, densidade do cartão: compacto ou expandido | Apenas apresentação de TL-03. Nenhum cenário deste PRD depende da escolha | Demandante | Resolvida em 2026-09-09: cartão compacto é o padrão; TL-03b fica como variação descartada |

Nenhuma dúvida material em aberto.

Todas as demais questões que chegaram abertas do `/solution` e do `/design` —
Q-01 a Q-10, QD-01, QD-03 e QD-04 — foram decididas pelo demandante e estão
incorporadas às regras de negócio acima.

---

## Rastreabilidade de origem

| RF/RNF | Veio de | Referência |
| --- | --- | --- |
| RF-001, RF-002 | Intent — acesso autenticado com permissão por projeto | `docs/intent/kanban-tarefas-intent.md`; ADR-003, ADR-006, BDR-001 |
| RF-003, RF-018 | `/solution` — board como representação das dimensões de estado; Q-08 | `docs/solution/kanban-tarefas-solution.md`; TL-03 |
| RF-004 | `/solution` — estado inicial da tarefa | TL-05 |
| RF-005, RF-006 | Shape Brief — direção escolhida; Q-01 e Q-02 | `docs/shape/kanban-tarefas-brief.md`; TL-03, TL-04 |
| RF-007, RF-008, RF-014 | Enquadramento E-02 e DDR-006 — handoff explícito e fila própria | `docs/decisions/DDR-006-espera-de-tomada-primeira-classe.md`; TL-06 |
| RF-009, RF-010 | Intent — sinalização de impedimento como parte do fluxo; Q-04 | TL-04 |
| RF-011, RF-012, RF-013 | `/solution` — estados terminais; Q-05 e Q-06 | TL-04, TL-10 |
| RF-015, RF-016 | Shape Brief — jornadas 4 e 5 e métrica de sucesso; Q-09 | TL-07 |
| RF-017, RF-019 | Intent — fluxo configurável por projeto e permissão por projeto; Q-07 | TL-08, TL-09 |
| RF-020 | Intent — propagação sem refresh manual | ADR-004; TL-03 |
| RNF-001 a RNF-005 | Envelope de qualidade do Shape Brief, herdado dos limites declarados da Intent | `docs/shape/kanban-tarefas-brief.md`, bloco "Envelope de qualidade" |
| RNF-006 | DDR-005 — acessibilidade WCAG 2.1 AA obrigatória | `docs/decisions/DDR-005-acessibilidade-wcag-aa-obrigatoria.md` |
| RNF-007 | Shape Brief — métrica de sucesso, com instrumentação declarada inexistente | `docs/shape/kanban-tarefas-brief.md` |
| RNF-008 | `/solution` — transição proibida "alterar retroativamente tempo já contado" | `docs/solution/kanban-tarefas-solution.md` |
| RNF-009 | Inferência do agente — sem fonte. Ver DM-01 | este documento |
| RF-021 | Emenda de 2026-09-09 (INC-03) — capacidade que o desenho técnico instituiu sem requisito de origem | `docs/analyze/kanban-tarefas-analysis.md`; ADR-010 |
| RNF-010 | Emenda de 2026-09-09 (INC-05) — limite de requisições instituído no desenho técnico sem requisito que o sustentasse | `docs/analyze/kanban-tarefas-analysis.md` |
| RF-022 | Emenda de 2026-09-10 — lacuna encontrada pela `/tests`: nenhum contrato criava projeto e nenhuma operação concedia a primeira participação | `docs/tests/kanban-tarefas-verificacao.md`, seção da lacuna; ADR-010 |

### Cobertura das regras de borda do `/solution`

| Borda | Cenário que a cobre |
| --- | --- |
| B-01 — vazio distinto de "ainda não medido" | SCN-016.3, com apoio de SCN-003.2 e SCN-014.3 |
| B-02 — impedimento duplicado | SCN-009.3 |
| B-03 — ação concorrente | SCN-007.3, SCN-020.3 |
| B-04 — encerramento com impedimento aberto | SCN-012.4, com apoio de SCN-011.3 para a conclusão |
| B-05 — pedido baseado em estado que já mudou | SCN-005.3 |
| B-06 — reconfiguração com trabalho em curso | SCN-017.3 |
| B-07 — sem permissão | SCN-015.2, com apoio de SCN-002.3 e SCN-012.2 |
| B-08 — expirado: não se aplica | SCN-015.3 verifica que nada é acionado por decurso de prazo |
| B-09 — impedido há muito tempo | SCN-015.3 |
| B-10 — pessoa removida com tarefa assumida | SCN-019.2 |

---

## Aprovação — gate de spec

- **Aprovado por:** Thiago Goncalves Cavalcante — Product Owner / Aprovador
- **Data:** 2026-09-09
- **Ressalvas:** nenhuma registrada pelo aprovador. Permanece aberta, por decisão
  desta etapa e não como ressalva ao gate, a hipótese H-03 — entrevista direta
  com ao menos um desenvolvedor e um gestor de outro time. Os cenários congelam
  sem ela; se a entrevista alterar a ordem de prioridade das jornadas, a correção
  sairá por emenda registrada.
- **Cenários congelados a partir desta aprovação:** sim

**Reconfirmação do gate — emenda v1.1**

- **Aprovado por:** Thiago Goncalves Cavalcante — Product Owner / Aprovador
- **Data:** 2026-09-09
- **Alcance:** as 10 linhas da tabela de emendas abaixo, mais as alterações fora
  de cenário que elas exigiram — RN-002, RN-003, RN-009, RN-011, as novas RN-032
  a RN-035, o RF-021, o RNF-010 e os ajustes de origem no protótipo de RF-003 e
  RF-015.
- **Ressalvas:** nenhuma. H-03 permanece aberta nos mesmos termos da aprovação
  original.
- **Cenários recongelados a partir desta reconfirmação:** sim

**Reconfirmação do gate — emenda v1.2**

- **Aprovado por:** Thiago Goncalves Cavalcante — Product Owner / Aprovador
- **Data:** 2026-09-09
- **Alcance:** as 2 últimas linhas da tabela de emendas abaixo — SCN-012.4 novo e
  SCN-012.1 emendado —, mais as alterações fora de cenário que elas exigiram: a
  extensão de RN-011 às duas restrições que a marca impõe, a frase acrescentada
  a RN-032 declarando que o encerramento não é exceção, e as regras aplicáveis
  de RF-012.
- **Decisão de negócio registrada:** o encerramento sem conclusão é **recusado**
  enquanto houver impedimento sem desfecho — B-04 realizado como escrito. As
  alternativas de fechar em cascata e de deixar o intervalo aberto foram
  apresentadas e recusadas pelo aprovador.
- **Ressalvas:** nenhuma. H-03 permanece aberta nos mesmos termos da aprovação
  original.
- **Cenários recongelados a partir desta reconfirmação:** sim

**Reconfirmação do gate — emenda v1.3**

- **Aprovado por:** Thiago Goncalves Cavalcante — Product Owner / Aprovador
- **Data:** 2026-09-10
- **Alcance:** o RF-022 e seus três cenários novos, mais as regras RN-036,
  RN-037 e RN-038 que o sustentam. Nenhum cenário preexistente foi tocado.
- **Decisões de negócio registradas:** (1) criar projeto é capacidade exclusiva
  da administração global, e a operação **nomeia** a primeira `project_admin`,
  sem que quem cria vire participante — o custo assumido é o afunilamento da
  criação numa única pessoa, cuja promoção é única e auditada; (2) o projeto
  nasce **sem fluxo**, contra a recomendação de fluxo padrão. O custo assumido e
  registrado é que o caminho de instalação passa a ter dois passos obrigatórios,
  e o segundo não tem nada que o lembre: até RF-017 rodar, a criação de tarefa é
  recusada.
- **Ressalvas:** nenhuma. H-03 permanece aberta nos mesmos termos da aprovação
  original.
- **Cenários recongelados a partir desta reconfirmação:** sim

**Reconfirmação do gate — emenda v1.6**

- **Aprovado por:** Thiago Goncalves Cavalcante — Product Owner / Aprovador
- **Data:** 2026-09-16
- **Alcance:** o cenário novo SCN-003.4, a regra nova RN-039, o requisito não
  funcional novo RNF-011 e a linha de regras aplicáveis de RF-003. Nenhum cenário
  preexistente foi tocado.
- **Decisão de negócio registrada:** o recorte das tarefas terminais é **regra de
  negócio e não otimização**, porque muda o que a pessoa vê no board — e por isso
  entrou como RN, com cenário, e não como decisão técnica. A janela é de **30
  dias**; as alternativas de teto por etapa terminal e de teto combinado com a
  janela foram apresentadas e recusadas, pelo argumento de que o teto duro faz o
  time rápido perder de vista a semana corrente. O recorte é **da tela e nunca do
  registro**: nada é apagado, e a tarefa fora da janela continua inteira na ficha
  e nas consultas de andamento — é a distinção que mantém RN-022 e RNF-008
  intactos.
- **Por que o RNF não existia:** RNF-009 cobre as consultas de RF-015 e RF-016, e
  a semelhança de assunto escondeu que a leitura mais exercitada do produto não
  tinha envelope algum. O envelope de RNF-011 espelha o de RNF-009 e reusa o mesmo
  arnês de carga, de propósito — dois envelopes distintos para a mesma classe de
  consulta exigiriam justificar a diferença, e não há uma.
- **Cenários recongelados a partir desta reconfirmação:** sim

**Reconfirmação do gate — emenda v1.5**

- **Aprovado por:** Thiago Goncalves Cavalcante — Product Owner / Aprovador
- **Data:** 2026-09-10
- **Alcance:** o cenário novo SCN-002.4, mais as alterações fora de cenário que
  ele exigiu — a extensão de RN-038 às duas sinalizações obrigatórias, a
  descrição, a fonte, as regras aplicáveis e a origem no protótipo de RF-002, e
  a frase acrescentada à descrição de RF-022. Nenhum cenário preexistente foi
  tocado.
- **Decisão de negócio registrada:** as duas sinalizações do fluxo não
  configurado passam a ser **comportamento requerido**, e não conveniência de
  desenho. A alternativa — retirá-las do protótipo e deixar o custo de RN-038
  exatamente como estava escrito — foi apresentada e recusada. Só a marca na
  relação de projetos ganhou cenário congelado: ela é a sinalização durável, a
  única que ainda alcança quem precisa agir depois que o desfecho da criação
  saiu da tela.
- **Ressalvas:** nenhuma. H-03 permanece aberta nos mesmos termos da aprovação
  original.
- **Cenários recongelados a partir desta reconfirmação:** sim

> A partir daqui os cenários são contrato. Alteração exige emenda registrada —
> mexer num `.feature` aprovado sem emenda reprova o GATE-GHERKIN-CONGELADO.

## Emendas de cenário

| Data | Cenário | O que mudou | Motivo | Aprovado por |
| --- | --- | --- | --- | --- |
| 2026-09-09 | SCN-003.3 | Passa a exigir que a condição de trabalho continue sendo aguardando tomada e que a marca de impedimento seja exibida à parte | INC-01 — impedimento deixa de ser valor da condição e vira dimensão própria | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-006.3 | Passa a exigir que a condição de trabalho vá a aguardando tomada **sem** que a marca de impedimento seja apagada; título ajustado | INC-01 — o contrato de movimentação apagava a marca em silêncio, contra RN-009 | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-007.4 | Cenário **novo**: tomada de tarefa com impedimento aberto é aceita, encerra a espera, registra responsável e preserva a marca | INC-02 — nada dizia se tarefa impedida podia ser assumida, e SCN-008.3 produz esse estado de propósito | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-008.3 | Acrescenta que a tarefa devolvida com impedimento aberto continua assumível | INC-02 — fecha o estado que o próprio cenário fabrica | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-009.1 | "sua condição passa a ser impedida" vira "passa a constar com impedimento aberto", com a condição de trabalho preservada | INC-01 | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-009.2 | Desfecho passa a ser "nenhum impedimento é aberto", no lugar de "a condição não é alterada" | INC-01 — a sinalização nunca mexeu na condição | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-010.1 | Resolução passa a apagar a marca e encerrar a contagem, sem alterar etapa nem condição de trabalho; some a "condição anterior" | INC-01 — não havia condição anterior a restaurar | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-013.1 | Passa a exigir retorno à **primeira etapa do fluxo**, sem responsável, com novo episódio registrado | INC-04 — a etapa de retorno é decisão de negócio e estava sendo tomada no desenho técnico | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-019.4 | Cenário **novo**: remoção de participação interrompe o acompanhamento em tempo real com a sessão ainda aberta | INC-06 — sem ele, quem perde o acesso continua recebendo o estado do projeto | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-021.1, SCN-021.2, SCN-021.3 | Cenários **novos** do RF-021, requisito novo | INC-03 — o alcance de administração global existia no desenho técnico sem requisito, regra nem cenário | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-012.4 | Cenário **novo**: o encerramento sem conclusão é recusado enquanto o impedimento não tem desfecho registrado | INC-12 — B-04 prescrevia essa recusa e nenhum requisito, regra ou cenário a realizava; a tabela de cobertura apontava para a recusa de **conclusão**, que é outra operação | Thiago Goncalves Cavalcante |
| 2026-09-10 | SCN-022.1, SCN-022.2, SCN-022.3 | Cenários **novos** do RF-022, requisito novo | Lacuna encontrada pela `/tests`: não existia rota de criação de projeto em contrato nenhum, e a primeira participação não podia ser concedida por ninguém — a suíte contornava semeando por JDBC | Thiago Goncalves Cavalcante |
| 2026-09-10 | SCN-002.4 | Cenário **novo**: projeto sem etapa alguma aparece marcado na relação de projetos, e projeto com fluxo configurado não | INC-22 — a marca existia no protótipo sem regra que a obrigasse; sem cenário, ela seria a primeira coisa a desaparecer numa refatoração de tela, e com ela o custo aceito em RN-038 voltaria a ficar invisível | Thiago Goncalves Cavalcante |
| 2026-09-16 | SCN-003.4 | Cenário **novo**: tarefa concluída há mais de trinta dias sai do board e continua acessível pela ficha | ACH-03 da revisão técnica do board — ele devolvia toda tarefa já criada no projeto, sem recorte, janela ou paginação, e não havia envelope de tempo de resposta em RNF nenhum. Sem cenário, RN-039 seria regra invisível à verificação, e a primeira refatoração da consulta a desfaria sem que nada acusasse | Thiago Goncalves Cavalcante |
| 2026-09-09 | SCN-012.1 | Ganha a pré-condição de não haver impedimento aberto, e "todas as contagens cessam" passa a nomear as contagens de permanência e de espera de tomada | INC-11 — "todas as contagens" contradizia RN-032, segundo a qual só o desfecho encerra a contagem de impedimento | Thiago Goncalves Cavalcante |

> Na emenda v1.3 nenhum cenário preexistente foi alterado: ela apenas acrescenta
> o RF-022, seus três cenários e as regras RN-036 a RN-038.
>
> Nenhum cenário foi revogado em nenhuma das duas emendas anteriores. Os IDs preexistentes
> foram mantidos, e as mudanças fora de cenário — RN-002, RN-003, RN-009,
> RN-011, as novas RN-032 a RN-035, o RNF-010, os ajustes de origem no protótipo
> de RF-003 e RF-015, e na segunda emenda a extensão de RN-011, a frase
> acrescentada a RN-032 e as regras aplicáveis de RF-012 — estão registradas no
> histórico do `state.md` e nos achados da análise de consistência.

---

## Fora deste artefato — regras negativas

- **Tecnologia, biblioteca, banco, protocolo, esquema** → `/techspec`.
  O PRD diz o que precisa ser verdade; o techspec diz com o quê.
- **Decisão de direção de negócio** → `/shape`. Se o PRD precisa decidir
  direção, o gate de direção falhou — devolva.
- **Exploração de comportamento sem conclusão** → `/solution`. Aqui tudo é
  decidido; questão em aberto vira dúvida material, não alternativa.
- **Task, épico, estimativa, sequenciamento** → `/tasks`.
- **Step definition, código de teste** → `/tests`. Aqui há o cenário, não a
  implementação dele.
- **Requisito sem procedência.** Não existe. Campo vazio reprova o gate.
- **Dado real de cliente** (IDSD 4.10.1).
