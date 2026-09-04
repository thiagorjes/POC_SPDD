# Discovery — kanban-tarefas

> D1 divergente. Explora contexto, dores e **enquadramentos alternativos** do
> problema. Não escolhe: abre.
> Sem gate próprio — alimenta o `/shape`, que converge e sai no GATE-DIRECAO.

- **Intent:** `docs/intent/kanban-tarefas-intent.md`
- **Contexto:** `docs/context/kanban-tarefas-context.md`
- **Data:** 2026-09-04

> **Procedência das respostas:** entrevista com Thiago Goncalves Cavalcante
> (Product Owner) em 2026-09-04. Ele confirmou S-01 — responde pelas três
> perspectivas (dev, liderança técnica, gestor de outro time), consultando o
> gestor e os integrantes do time diretamente. Não houve entrevista separada com
> dev nem com gestor. O discovery de 2026-08-24
> (`requirements/discovery/kanban-tarefas-discovery.md`) entra como **insumo**;
> ele traz uma hipótese de solução única e um só enquadramento, e por isso não
> substitui a divergência desta etapa.

---

## Situação atual

O acompanhamento das atividades acontece hoje em **três canais paralelos —
chat, e-mail e WhatsApp** — sem nenhum registro estruturado do estado da tarefa.
Não existe ferramenta de acompanhamento em uso (L-05 permanece aberta como
confirmação formal, mas nada no relato aponta para uma).

O dev comunica status e impedimento no canal que estiver à mão no momento. Não
há regra sobre qual canal usar para qual coisa, nem destinatário definido para o
aviso de impedimento — a mensagem vai para o canal, não para uma pessoa
responsável por agir sobre ela. É exatamente aí que a informação se perde: o
aviso concorre com o restante do tráfego do canal (o discovery de 2026-08-24
descreve isso como perder-se "em meio a outros comunicados (ex.: spam de
email)").

**A gambiarra que sustenta o processo:** o **Product Owner varre os três canais,
junta o que consegue recuperar e consolida tudo em uma planilha**. A planilha é
hoje a única visão agregada do andamento — e ela existe porque uma pessoa a
reconstrói manualmente, a partir de um material que já sai incompleto da coleta.
Duas consequências ficam registradas por ela: o estado do trabalho é o que o PO
conseguiu capturar, não o que de fato aconteceu; e a visão agregada tem uma
única fonte humana, com a latência e as falhas dessa pessoa.

Não foi relatada reunião diária nem outro ritual de sincronização que sirva de
rede de segurança para a comunicação perdida.

## Dores observadas

| # | Dor | Quem sente | Frequência | Evidência | Custo de conviver com ela |
| --- | --- | --- | --- | --- | --- |
| D-01 | Mensagem de status ou impedimento se perde entre chat, e-mail e WhatsApp | Devs (quem avisa e não é atendido) e liderança técnica | Recorrente — sem quantificação (L-02) | Relato do PO em 2026-09-04; discovery 2026-08-24 §1; caso registrado de demanda parada por 5 dias por aviso não visto a tempo | Tarefa fica parada sem ninguém saber; a espera só termina quando alguém cobra |
| D-02 | Não existe destinatário definido para o aviso de impedimento — a mensagem vai para o canal, não para quem pode resolver | Dev bloqueado | A cada impedimento | Relato do PO em 2026-09-04 (nenhuma fonte descreve rito de escalonamento) | Resolver depende de alguém notar por acaso; o tempo de parada é indeterminado |
| D-03 | A consolidação do andamento recai inteiramente sobre o PO, à mão, em planilha, a partir de coleta incompleta | Product Owner | Contínua | Relato do PO em 2026-09-04 | Trabalho manual recorrente de uma pessoa; a visão agregada é tão boa quanto a varredura dela |
| D-04 | As etapas seguintes começam tarde — review, validação e demais handoffs — porque o próximo responsável não sabe que a vez chegou | Reviewer, validador, e quem depende da entrega | Recorrente | Relato do PO em 2026-09-04: **"atraso de entregas, atraso em review, validação etc. tudo começa tarde por falha de comunicação"** | Atraso que se acumula ao longo da cadeia; o tempo perdido não está na execução, está entre as etapas |
| D-05 | Cobrança manual recorrente entre integrantes da equipe | Toda a equipe | Recorrente | Discovery 2026-08-24 §1 — "cobranças recorrentes entre a equipe" | Atrito interpessoal; a cobrança substitui o processo |
| D-06 | Gestor de outro time não tem como ver andamento sem perguntar a alguém | Gestores de outros times | A cada necessidade de acompanhar | PRD §1 e §2; discovery 2026-08-24 §2 — declaração do demandante, não pesquisa com os próprios gestores | Ou o gestor fica sem informação, ou interrompe quem executa para obtê-la |

> **Nota sobre evidência:** nenhuma das dores tem medição. O discovery de
> 2026-08-24 declara em §1 que "não há dados quantitativos levantados até o
> momento", e o demandante confirmou em 2026-09-04 que os KPIs permanecem
> qualitativos (S-02). Tudo acima é relato e caso ilustrativo — suficiente para
> descrever o problema, insuficiente para constituir linha de base. L-02 segue
> aberta e chega ao `/shape` assim.

## Personas

| Persona | Contexto de uso | Objetivo | O que a frustra hoje |
| --- | --- | --- | --- |
| Desenvolvedor | Diário e recorrente; atualizações frequentes, e eventualmente simultâneas na mesma tarefa por participantes diferentes (discovery 2026-08-24 §2) | Registrar onde está e sinalizar que travou, uma vez só, e ser atendido | Avisa e não é atendido; precisa repetir o aviso em outro canal; é cobrado por algo que já comunicou (D-01, D-02, D-05) |
| Liderança técnica | Diário; acompanha e desbloqueia | Saber o que está travado agora e quem precisa agir | Só descobre o impedimento quando ele já custou dias (D-01) |
| Product Owner | Contínuo; hoje é o ponto de consolidação | Ter a visão agregada do andamento sem reconstruí-la à mão | Varre três canais e monta planilha a partir de material incompleto (D-03) |
| Gestor de outro time | Esporádico; só consome | Ver progresso, impedimentos e tempo por etapa sem participar da execução | Depende de perguntar; a resposta vem da planilha do PO, com a latência dela (D-06) |
| Reviewer / validador (papel, não pessoa) | A cada handoff | Saber que a vez chegou, sem monitorar canal | Descobre tarde que havia algo esperando por ele (D-04) |

> As duas primeiras personas vêm do discovery de 2026-08-24; PO, gestor e
> reviewer/validador emergiram da entrevista de 2026-09-04. Nenhuma foi levantada
> em entrevista direta com o usuário final — todas passam pelo relato do
> demandante (S-01, L-03).

## Enquadramentos alternativos do problema

> O coração da divergência. O mesmo conjunto de dores admite mais de uma
> leitura, e cada leitura leva a uma solução diferente. Mínimo de dois.

| # | Enquadramento | Se este for o problema, resolve para quem | O que ficaria sem resposta |
| --- | --- | --- | --- |
| E-01 | **Falta um lugar único onde o estado da tarefa viva.** A informação existe, mas está espalhada por três canais e nunca é registrada como estado — só como mensagem. | Devs e liderança técnica, que passam a ler o estado em vez de reconstituí-lo; PO, que deixa de consolidar | Por que a comunicação acontece hoje em três canais, e o que faz alguém manter o registro atualizado quando o canal continua existindo e sendo mais cômodo. Se o board não for alimentado, a planilha volta. É o enquadramento do discovery de 2026-08-24 e do PRD. |
| E-02 | **O problema é o handoff: o próximo responsável não sabe que a vez chegou.** O tempo não se perde na execução, se perde entre etapas (D-04) — e o impedimento é um caso particular disso, o handoff para quem desbloqueia (D-02). | Reviewer, validador e quem desbloqueia; e o dev bloqueado, que passa a ter destinatário | O acompanhamento agregado e o lead-time do gestor. Uma solução de fila/sinalização de "é sua vez" resolve o atraso sem produzir visão de andamento nem histórico por etapa. |
| E-03 | **O problema é que o custo de consolidar recai sobre uma pessoa.** O trabalho de coleta existe e é feito — manualmente, pelo PO, em planilha (D-03). | Product Owner, e o gestor que consome a planilha dele | A causa da perda de mensagem. Automatizar a agregação dos canais existentes tira o trabalho do PO, mas o que se perdeu no chat continua perdido — e sem registro estruturado não há lead-time por etapa. |
| E-04 | **O problema é que o impedimento não tem dono nem prazo.** Ninguém é responsável por agir sobre um aviso, e nada acontece quando ele fica sem resposta — por isso "5 dias" é possível. | Dev bloqueado, e a liderança técnica que responde pelo desbloqueio | Todo o fluxo que não é impedimento: andamento normal, visibilidade contínua, lead-time. Uma solução de escalonamento com dono e SLA ataca o caso registrado sem tocar no acompanhamento diário. |
| E-05 | **O problema é que ninguém está olhando — falta o ritual, não a ferramenta.** Não há reunião diária nem qualquer ponto de sincronização; os canais são só onde a ausência de rito aparece. | Toda a equipe, sem construir software | Visibilidade do gestor de outro time (que por definição não participa do rito) e histórico de lead-time. Também não resolve nada em regime assíncrono ou distribuído. Enquadramento cuja conclusão pode ser "não construir" — está aqui porque o primeiro diamante precisa admiti-lo, não porque seja o mais provável. |

Os cinco não são exclusivos entre si, mas **priorizam usuários diferentes e
dores diferentes**, e cada um leva a uma solução de forma distinta. E-01 é o que
o PRD e o material de design já assumem — registrá-lo ao lado dos outros é o que
permite ao `/shape` escolhê-lo por decisão, e não por inércia.

## Restrições de contexto

| Restrição | Origem | Como afeta as opções |
| --- | --- | --- |
| Notificação apenas interna ao sistema, sem e-mail, Slack ou canal externo | Intent — restrição declarada, não negociável | Restringe E-01, E-02 e E-04: o aviso só alcança quem abre o sistema. Ver a contradição C-01. |
| Sem controle de horas/timesheet | Intent — não negociável | Exclui qualquer leitura de "o problema é medir esforço" |
| Single-tenant; sem dependência entre projetos | Intent — não negociável | Limita E-02: handoff entre projetos distintos fica fora |
| Lead-time mede o processo, não a pessoa; o sistema não pode virar instrumento de cobrança individual do dev | Intent — efeito recusado pelo demandante | Condiciona E-01 e E-03: toda agregação por responsável tende a produzir exatamente esse uso |
| Gestor não deve passar a interferir na execução | Intent — efeito recusado pelo demandante | Tensiona E-01 e E-03: dar visibilidade e impedir interferência puxam em direções opostas. Ver C-02 |
| Doze DRs já aceitos (stack, RBAC, broadcast, schema, empacotamento, tokens, mecânica do board) | `docs/decisions/` | Materializam E-01. Um `/shape` que escolha outro enquadramento invalida parte deles |
| Design brief, mapa de 8 telas e 9 protótipos navegáveis já produzidos | `requirements/design/` | Mesmo efeito: material construído sobre E-01 (S-04) |
| Sem prazo e sem orçamento; execução por IA, com o custo como métrica observada | Intent | Nenhuma opção é descartada por esforço; o critério de escolha não é custo de construção |
| Equipe já opera com noção mínima de fluxo kanban | Intent — premissa declarada | Favorece E-01; é premissa do demandante, não observação verificada |

## Sinais contraditórios

> O que diferentes fontes dizem de forma incompatível. Não resolva aqui —
> registrar a contradição é mais valioso do que escolher um lado cedo demais.

| Tema | Fonte A diz | Fonte B diz | Quem decide |
| --- | --- | --- | --- |
| C-01 — Alcance da notificação | Intent: notificações são **internas ao sistema**, sem canal externo, e isso é não negociável | Entrevista 2026-09-04 e discovery 2026-08-24 §1: a equipe vive em chat, e-mail e WhatsApp, e é justamente aí que o aviso se perde. Notificação interna só alcança quem abre o sistema — o mesmo modo de falha, em um canal novo | Thiago Goncalves Cavalcante (demandante) — `/shape` |
| C-02 — Visibilidade sem interferência | Intent: gestor obtém **"visibilidade sem participar da execução"** | Intent, na mesma fonte: **"não quero que gestor passe a interferir na execução"**. Dar ao gestor andamento e lead-time por etapa em tempo quase real é o insumo típico da interferência que se quer evitar | Thiago Goncalves Cavalcante (demandante) — `/shape` |
| C-03 — Objeto da medição | Intent: o lead-time **"é apenas forma de medir a eficiencia do processo, não serve para cobrar pessoas"** | O board associa tarefa a responsável, e lead-time por etapa é agregável por pessoa. A restrição é de uso, não de estrutura — nada no sistema a sustenta | Thiago Goncalves Cavalcante (demandante) — `/shape`, e depois `/prd` |
| C-04 — Onde está o tempo perdido | Discovery 2026-08-24 e PRD: o problema é **falta de centralização** do estado | Entrevista 2026-09-04: **"tudo começa tarde por falha de comunicação"** — o atraso está nos handoffs (review, validação), não na ausência de um registro central | `/shape` |
| C-05 — Quem foi ouvido | Discovery 2026-08-24 apresenta duas personas com frustrações e modo de uso, como resultado de descoberta | O artefato não registra participantes, método nem data de entrevista (L-03), e o demandante confirmou responder pelas três perspectivas (S-01). Não se distingue persona levantada com usuário de persona redigida pelo PO | Thiago Goncalves Cavalcante (demandante) |

## Perguntas em aberto

| # | Pergunta | Por que importa | Quem responde |
| --- | --- | --- | --- |
| P-01 | Se a notificação é só interna, o que faz alguém abrir o sistema a tempo de ver o impedimento? | É C-01. Sem resposta, o enquadramento E-01 reproduz a dor D-01 dentro do produto novo | Demandante — `/shape` |
| P-02 | Quantos impedimentos por período, quanto tempo em média se perde, quantas tarefas em curso, quantos projetos? | L-02. Sem linha de base, `/shape` fixa métrica de sucesso no vazio e `/evidence` registra entrega, não efeito | Devs e liderança técnica; possivelmente extraível dos canais em uso |
| P-03 | Os três canais continuam existindo depois do sistema, ou algum deles é desativado por decisão? | Se continuam, o board compete com eles e a adesão vira o risco principal de E-01 | Demandante |
| P-04 | Quem é o dono de um impedimento aberto, e o que acontece se ele ficar sem resposta por N tempo? | Define se E-04 é subproblema de E-01 ou um enquadramento com solução própria | Demandante / liderança técnica |
| P-05 | A planilha do PO deixa de existir, ou continua como visão paralela? | Se continua, D-03 não foi resolvida e o sistema virou mais uma fonte a consolidar | Product Owner |
| P-06 | Existe ferramenta de acompanhamento anterior com dado a migrar? | L-05 / S-03. Resposta positiva reclassifica a demanda e aciona gates de migração retroativamente | Demandante — antes de `/prd` |
| P-07 | Dev e gestor podem ser entrevistados diretamente antes de `/shape` fechar a direção? | L-03 / S-01. Todo o material desta etapa passa por um único relator, que já formulou uma solução | Demandante |

---

## Fora deste artefato — regras negativas

- **Escolher um enquadramento.** É a função do `/shape`. Um discovery que
  apresenta uma única leitura do problema não divergiu.
- **Requisito numerado, critério de aceite, cenário Gherkin** → `/prd`.
- **Solução, fluxo de operação, estado, contrato** → `/solution`.
- **Tela, componente, layout** → `/design`.
- **Tecnologia** → `/techspec`.
- **Métrica com alvo definido** → `/shape`. Aqui se registra a linha de base
  observada, não a meta.
- **Reescrever a intenção do demandante** → `/intent` é a fonte. Se o discovery
  contradiz a Intent, isso é um achado a levar ao `/shape`, não uma correção a
  fazer aqui.
- **Dado real de cliente** (IDSD 4.10.1).
