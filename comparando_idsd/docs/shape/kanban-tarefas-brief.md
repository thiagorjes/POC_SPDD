# Shape Brief — kanban-tarefas

> D1 convergente. Produz **decisão, não especificação**.
> Responde *o quê* e *até onde* — nunca *como se comporta* nem *com o quê*.
> **Gate:** GATE-DIRECAO (aprovação humana obrigatória)

- **Intent:** `docs/intent/kanban-tarefas-intent.md`
- **Discovery:** `docs/discovery/kanban-tarefas-discovery.md`
- **Data:** 2026-09-04
- **Modo:** entrevista
- **Artefato externo ingerido:** —

---

## Problema escolhido

O estado do trabalho não existe em lugar nenhum como estado — existe apenas como
mensagem, espalhada por chat, e-mail e WhatsApp, e reconstruída à mão pelo
Product Owner em uma planilha, a partir de uma coleta que já sai incompleta. A
consequência que mais custa não é a falta de um relatório: é que **o próximo
responsável não fica sabendo que a vez chegou** — review, validação e desbloqueio
começam tarde, e um impedimento pode ficar cinco dias sem ninguém agir. Foram
adotados em conjunto os enquadramentos **E-01** (falta um lugar único onde o
estado da tarefa viva) e **E-02** (o tempo se perde no handoff, não na execução).
E-01 sozinho — o enquadramento do discovery de 2026-08-24 e do PRD — não cobre a
dor que o próprio demandante apontou como principal em 2026-09-04, registrada
como contradição C-04 no discovery.

## Direção escolhida

O sistema passa a ser **o lugar onde o trabalho acontece**, não um relatório a
consultar: o desenvolvedor registra ali o andamento e o impedimento, e esse
registro é simultaneamente o estado da tarefa e o sinal para quem vem depois. A
transição de etapa deixa de ser um aviso que alguém precisa notar em um canal e
passa a ser um fato observável no fluxo. A aposta é que, quando o registro do
estado e o aviso ao próximo responsável são a mesma ação, a mensagem deixa de se
perder porque deixa de existir como mensagem. Disso decorrem, sem esforço
adicional de ninguém, a visão agregada que hoje é feita à mão e o tempo por
etapa que hoje não é medido.

A restrição de notificação apenas interna é mantida rígida, com a consequência
assumida: só alcança quem abre o sistema. Isso é sustentável apenas se a
premissa acima for verdadeira — o board como canal de trabalho diário, não como
painel. A adesão é, portanto, o risco central desta direção, e está registrada
como hipótese H-01, não como certeza.

## Alternativas descartadas

> Sem esta seção o brief não é convergência, é apenas uma proposta.
> Mínimo de duas alternativas reais — variações cosméticas não contam.

| Alternativa | Por que foi considerada | Por que foi descartada | Reabrir se |
| --- | --- | --- | --- |
| **E-01 puro** — só centralização do estado, como no PRD e no discovery de 2026-08-24 | É o enquadramento sobre o qual os 12 DRs e os 9 protótipos já foram construídos; descartá-lo custaria retrabalho | Deixa D-04 sem tratamento — o atraso de review e validação, que o demandante apontou como o efeito mais caro. Centralizar o estado sem tornar o handoff explícito reproduz o problema em um lugar novo | Nunca isoladamente: E-02 foi incorporado à direção, não adiado. Só voltaria se `/solution` demonstrar que tornar o handoff explícito exige mecanismo incompatível com o board configurável |
| **E-02 isolado** — fila de "é sua vez" para reviewer, validador e quem desbloqueia | Ataca diretamente o atraso relatado, com escopo bem menor | Não produz visão agregada nem tempo por etapa, e portanto não atende o gestor de outro time (D-06) nem substitui a planilha do PO (D-03). Invalidaria a maior parte dos DRs e do material de design | Se a medição do primeiro período mostrar que o problema é inteiramente de handoff e que a visão agregada não é usada por ninguém |
| **E-03** — automatizar a consolidação que o PO faz à mão, agregando os canais atuais | Tira trabalho recorrente de uma pessoa sem exigir mudança de hábito da equipe | Não ataca a causa: o que se perdeu no chat continua perdido. E sem registro estruturado não há tempo por etapa — ficaria uma planilha melhor, não um processo diferente | Se a planilha do PO não morrer após a entrega, ou se H-01 (adesão) for refutada |
| **E-04** — dar dono e prazo ao impedimento, com escalonamento automático | É o recorte que ataca literalmente o caso registrado dos 5 dias | Cobre só o impedimento e deixa de fora o andamento diário e a visibilidade. Como recorte isolado, é estreito demais para a intenção declarada | Se, já com o board em uso, a medição mostrar impedimentos parados apesar do registro — aí escalonamento vira necessidade demonstrada, não suposta |
| **E-05** — não construir; instituir ritual de sincronização | O discovery não encontrou nenhum rito hoje, e parte da dor pode ser ausência de processo, não de ferramenta | Não atende o gestor de outro time, que por definição não participa do rito, e não produz histórico de tempo por etapa. Também não sustenta o modo de uso assíncrono relatado | Se o board entrar em uso e o atraso de handoff não cair — indício de que o problema era de prática, não de registro |

Nada foi adiado com gatilho de retomada: as alternativas descartadas carregam
suas próprias condições de reabertura, e o demandante optou em 2026-09-04 por
não manter uma lista separada de adiados.

## Fronteira de escopo

| Dentro | Fora | Adiado para depois |
| --- | --- | --- |
| Registro do andamento da tarefa pelo próprio desenvolvedor | Integração com canal externo de notificação — e-mail, Slack, WhatsApp | — |
| Sinalização de impedimento como parte do fluxo, não como mensagem | Controle de horas ou timesheet | — |
| Handoff explícito entre etapas: o próximo responsável sabe que a vez chegou | Múltiplas organizações ou clientes (multi-tenant) | — |
| Fluxo de etapas configurável por projeto, incluindo raias, acompanhando o processo real de cada time | Dependência entre projetos | — |
| Visão agregada do andamento, substituindo a consolidação manual do PO | Importação em massa de cards, templates de card, duplicar card | — |
| Tempo por etapa, visível no próprio fluxo e agregado em painel | Anexos e arquivos em cards | — |
| Acesso somente-leitura para gestor de outro time | Escalonamento automático de impedimento com prazo (E-04) | — |
| Acesso autenticado pelo provedor de identidade corporativo, com permissão por projeto | Agregação automática dos canais de comunicação atuais (E-03) | — |
| Uso simultâneo da mesma tarefa por participantes diferentes | Qualquer agregação de tempo por pessoa | — |

**Envelope de qualidade — atravessa inteiro para o `/prd`.** A Intent declarou
limites que não são fronteira de escopo e sim exigência de qualidade sobre tudo
que está na coluna "Dentro": propagação das alterações aos demais usuários em até
2 segundos sem refresh manual; operação de 1 a N pods sem divergência de estado,
de dezenas a centenas de usuários simultâneos; empacotamento e execução em
contêineres na plataforma corporativa; nenhuma escrita dependendo exclusivamente
de validação client-side; interface responsiva para desktop nos principais
navegadores usados pela equipe. Nenhum deles foi reduzido nesta etapa. Eles não
aparecem numerados aqui porque requisito numerado é do `/prd` — este ponteiro
existe para que a passagem não os perca.

## Personas e jornadas priorizadas

| Persona | Jornada | Prioridade | Por que esta primeiro |
| --- | --- | --- | --- |
| Desenvolvedor | Registrar onde a tarefa está e sinalizar que travou | 1 | É a jornada da qual todas as outras dependem. Se o dev não alimentar o board, não há handoff, não há visão agregada e não há tempo por etapa — e a direção inteira cai junto com a hipótese H-01 |
| Reviewer / validador (papel, não pessoa) | Saber que a vez chegou e assumir a tarefa | 2 | É o par imediato da jornada 1 e o que materializa E-02. Sem ela, o registro do dev vira relatório e o atraso permanece |
| Liderança técnica | Ver o que está travado agora e agir sobre o impedimento | 3 | Ataca o caso registrado dos 5 dias. Depende de a jornada 1 já estar em uso |
| Product Owner | Ter a visão agregada sem reconstruí-la à mão | 4 | Substitui a planilha. É consequência das anteriores, não pré-requisito delas |
| Gestor de outro time | Consultar andamento e tempo por etapa, sem participar da execução | 5 | Somente-leitura e consumo esporádico. É a única jornada que pode ficar por último sem travar as outras |

## Restrições de negócio

| Restrição | Origem | Rígida ou negociável |
| --- | --- | --- |
| Notificação apenas interna ao sistema, sem canal externo | Intent — limite declarado | Rígida. Consequência assumida nesta direção: o aviso só alcança quem abre o sistema (C-01) |
| Tempo por etapa é agregado por etapa e por projeto, **nunca por pessoa** | Decisão do demandante em 2026-09-04, resolvendo C-03 | Rígida — e **estrutural**, não apenas acordo de uso. `/prd` e `/techspec` precisam honrá-la no modelo, não na disciplina |
| Gestor de outro time tem papel somente-leitura | Decisão do demandante em 2026-09-04, resolvendo C-02 | Rígida. É o que separa "visibilidade" de "interferência na execução", os dois lados da contradição registrada na própria Intent |
| Sem controle de horas ou timesheet | Intent — limite declarado | Rígida |
| Single-tenant; sem dependência entre projetos | Intent — limite declarado | Rígida |
| Fluxo de etapas configurável por projeto | Intent — proposta do demandante | Negociável quanto à forma; a necessidade de acompanhar o processo real de cada time é rígida |
| Nenhuma escrita pode depender exclusivamente de validação client-side | Intent — limite declarado | Rígida |
| Sem prazo e sem orçamento; execução por IA, com o custo como métrica observada do processo | Intent | — |
| Doze DRs aceitos e material de design já produzido assumem E-01 | `docs/decisions/`, `requirements/design/` | A incorporação de E-02 os preserva; a extensão do handoff sobre eles é assunto de `/solution` e `/design` (S-04 segue aberta) |

## Métrica de sucesso

> Como saberemos, depois de entregue, se a aposta estava certa.

Não existe linha de base: o discovery de 2026-08-24 declara que não há dado
quantitativo, e o demandante confirmou em 2026-09-04 que os KPIs permanecem
qualitativos (S-02). A decisão desta etapa é **não bloquear o fluxo para
garimpar os canais atuais, e fazer o próprio sistema produzir a medição**: o
primeiro período de uso estabelece a linha de base, e o alvo é declarado contra
ela depois. A consequência está registrada abaixo em cada linha — nenhuma
métrica tem alvo hoje, e nenhuma instrumentação existe.

| Métrica | Linha de base hoje | Alvo | Prazo de leitura | Instrumentação existe |
| --- | --- | --- | --- | --- |
| Tempo entre a sinalização de um impedimento e a primeira ação sobre ele | Inexistente. Único ponto conhecido: um caso de 5 dias | A declarar após o primeiro período de uso | Primeiro período de uso estabelece a base; alvo lido no período seguinte | Não — vira trabalho no `/tasks` |
| Tempo de permanência da tarefa em cada etapa, com atenção à espera entre etapas | Inexistente | A declarar após o primeiro período de uso | Idem | Não — vira trabalho no `/tasks` |
| A planilha de consolidação do PO deixou de ser mantida | Existe e é mantida à mão hoje | Deixar de existir | Ao final do primeiro período de uso | Não se aplica — verificação por observação, não por instrumento |
| Proporção do trabalho em curso que está registrada no sistema (adesão) | Zero — nada é registrado hoje | A declarar; é a leitura direta de H-01 | Primeiro período de uso | Não — vira trabalho no `/tasks` |

Enquanto os alvos não forem declarados, `/evidence` registra entrega e não
efeito. Isso é risco conhecido e aceito pelo demandante, não omissão.

## Hipóteses a validar

> Obrigatório em modo ingestão. Saída de workshop é hipótese, não requisito.

Este brief não é de ingestão, mas a direção escolhida repousa sobre suposições
que não foram verificadas com usuário. Registrá-las aqui é o que impede que
cheguem ao `/prd` com peso de requisito.

| # | Hipótese | Experimento | Critério de descarte |
| --- | --- | --- | --- |
| H-01 | O board se torna o canal de trabalho diário do dev, e não mais um painel a consultar — sem isso, notificação interna não alcança ninguém e a direção inteira cai | Medir a adesão no primeiro período de uso: proporção do trabalho em curso registrada no sistema, e se o registro precede ou sucede a comunicação no chat | Se o trabalho continuar sendo comunicado primeiro nos canais e replicado no board depois, ou se a maior parte do trabalho em curso não estiver registrada. Nesse caso, E-03 volta à mesa e C-01 é reaberta |
| H-02 | Tornar o handoff explícito faz review e validação começarem antes — a premissa de E-02 | Comparar a espera entre etapas ao longo dos primeiros períodos de uso | Se a espera entre etapas não cair, o problema era de prática e não de registro: E-05 volta à mesa |
| H-03 | As duas personas do discovery de 2026-08-24 descrevem usuários reais, e não a leitura do Product Owner sobre eles (L-03, S-01, contradição C-05) | Entrevista direta com ao menos um dev e um gestor de outro time antes de `/prd` fechar o escopo | Se a entrevista revelar objetivo ou frustração que muda a ordem de prioridade das jornadas acima |
| H-04 | A equipe já opera com noção mínima de fluxo kanban — premissa declarada na Intent, nunca verificada | Observação no primeiro uso | Se o fluxo configurável por projeto não for compreendido sem treinamento |
| H-05 | Não há ferramenta anterior com dado a migrar (L-05, S-03) | Confirmação do demandante antes de `/prd` | Resposta positiva reclassifica a demanda: deixa de ser apenas `feature` e aciona gates de migração retroativamente |

## Restrições técnicas herdadas

> Só em modo ingestão, quando o ritual externo teve participação técnica.
> Entram como **restrição conhecida**, nunca como decisão arquitetural — o
> `/techspec` pode contrariá-las com justificativa.

Não se aplica — modo entrevista. As decisões técnicas já existentes estão em
`docs/decisions/` como DRs aceitos, com gate próprio; não são herança de
workshop.

---

## Aprovação — gate de direção

- **Aprovado por:** Thiago Cavalcante — Product Owner / Aprovador
- **Data:** 2026-09-04
- **Ressalvas:** nenhuma registrada pelo aprovador. Permanecem em aberto, por
  decisão da própria etapa e não como ressalva ao gate: H-03 (entrevista direta
  com dev e gestor antes de `/prd` fechar escopo) e H-05 / S-03 (confirmação
  formal de que não há ferramenta anterior com dado a migrar — resposta positiva
  reclassifica a demanda e aciona gates de migração retroativamente).

---

## Fora deste artefato — regras negativas

- **Requisito numerado (RF-nn, RNF-nn, RN-nn).** Zero. Se aparecer numeração de
  requisito, o brief virou PRD prematuro → `/prd`.
- **Critério de aceite ou cenário Gherkin** → `/prd`.
- **Fluxo passo a passo, máquina de estados, semântica de contrato** →
  `/solution`. Aqui se decide *que* jornada é prioritária, não *como* ela corre.
- **Tela, wireframe, componente** → `/design`.
- **Tecnologia, biblioteca, banco, protocolo** → `/techspec`.
- **Task, épico, estimativa** → `/tasks`.
- **Levantamento aberto sem conclusão.** Se a seção "Direção escolhida" tem mais
  de uma direção, a convergência não aconteceu — o brief não passa no gate.
