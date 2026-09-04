# Intent — kanban-tarefas

> Artefato de Controle. Registra a intenção **nas palavras do demandante**.
> IDSD 4.2 — a intenção pertence a quem demanda. O agente transcreve e organiza;
> não traduz para linguagem técnica, não completa, não melhora.

- **Demandante:** Thiago Goncalves Cavalcante — Product Owner / Aprovador
- **Data de captura:** 2026-09-04
- **Validado pelo demandante em:** 2026-09-04 <!-- obrigatório antes do gate -->
- **Gate:** GATE-INTENT

> **Procedência:** capturada por ingestão, a pedido do demandante, a partir de
> `requirements/prd/kanban-tarefas-prd.md` (v1.0, 2026-08-24) e
> `requirements/design/kanban-tarefas-design-brief.md`. Não houve entrevista.
> Toda linha abaixo vem literalmente dessas fontes; nada foi inferido. Onde a
> fonte não respondia à pergunta da captura, está registrado como lacuna na
> tabela de perguntas.

---

## Resultado esperado

A equipe de desenvolvimento passa a ter **"uma forma centralizada de controlar o
andamento das atividades"**, em que **"os próprios desenvolvedores atualizam
status e sinalizam impedimentos"**.

O que precisa estar diferente, nas palavras da fonte:

- Impedimentos deixam de depender de **"comunicação dispersa"** e passam a ser
  sinalizados no próprio fluxo, com **"notificações internas de impedimento"**.
- Quem observa uma tarefa consegue **"agir rapidamente sem depender de cruzamento
  manual de reports"**.
- O tempo gasto em cada etapa fica visível — **"lead-time visível por etapa no
  board e agregado em dashboard"** — para **"identificar gargalos no fluxo de
  trabalho"**.
- Gestores de outros times obtêm **"visibilidade sem participar da execução"** e
  **"sem precisar acompanhar a execução diretamente"**.
- O fluxo do board acompanha o processo real de cada time, por ser
  **"configurável por projeto"**.

## Motivação

**"A equipe de desenvolvimento não tem uma forma centralizada de controlar o
andamento das atividades. O acompanhamento hoje depende de reports manuais via
email e chat, que precisam ser cruzados manualmente para identificar
impedimentos e direcioná-los ao responsável — gerando perda de mensagens
críticas."**

Evidência que o demandante registrou: **"há caso registrado de demanda parada por
5 dias por aviso de impedimento não visto a tempo"**.

## Limites declarados

| Limite | Origem | Negociável |
| --- | --- | --- |
| **"Notificações são internas ao sistema — sem integração com email, Slack ou outros canais externos."** | restrição declarada | Não |
| **"Sistema não suporta múltiplas organizações/clientes (single-tenant)."** | restrição declarada | Não |
| **"Sistema não suporta dependência entre projetos."** | restrição declarada | Não |
| **"Sistema não controla horas/timesheet do desenvolvedor."** | restrição declarada | Não |
| Autenticação via SSO — **"acessar o sistema sem precisar cadastrar ou lembrar uma senha local"** | proposta do demandante | A definir |
| **"Controle de acesso por papéis escopados por projeto"**, com ajuste por projeto | proposta do demandante | A definir |
| **"Workflows e colunas configuráveis por projeto"**, com **"raias (swimlanes)"** | proposta do demandante | A definir |
| **"Lead-time visível por etapa no board e agregado em dashboard"** | proposta do demandante | A definir |
| Alterações do board refletidas aos demais usuários **"em até 2 segundos, sem necessidade de refresh manual"** | limite declarado | A definir |
| Operar **"com 1 pod e escalar para 2 ou mais pods"**, **"de dezenas a centenas de usuários simultâneos"**, **"sem divergência de estado"** | limite declarado | Não |
| **"Empacotado e executável em containers"**, orquestrável na plataforma de contêineres corporativa | limite declarado | Não |
| **"Nenhuma escrita pode depender exclusivamente de validação client-side"** | limite declarado | Não |
| Interface **"responsiva para uso em desktop"**, nos **"principais navegadores desktop utilizados pela equipe"** | limite declarado | Não |
| Premissa: o provedor de identidade corporativo **"já está disponível"** para o SSO | premissa declarada | — |
| Premissa: **"Ambiente de destino suporta execução containerizada"** e orquestrada | premissa declarada | — |
| Premissa: **"Times de dev e gestores já operam com noção mínima de fluxo kanban"** | premissa declarada | — |

**Prazo e orçamento:** o demandante declarou que **"não há prazo nem orçamento"**.
Acrescentou: **"será feito por IA e o custo é parte da avaliação do processo"** —
ou seja, o custo não é um teto a respeitar, é uma das coisas que estão sendo
medidas.

## Fora de escopo

Efeitos que o demandante declarou que **não** quer que aconteçam junto:

- **"Não quero que vire instrumento de cobrança individual do dev."**
  Esclarecido pelo demandante na validação: **"o lead-time é apenas forma de
  medir a eficiencia do processo, não serve para cobrar pessoas"**. O objeto da
  medição é o processo, não a pessoa.
- **"Não quero que gestor passe a interferir na execução."**

Escopo que o demandante declarou explicitamente que não será construído:

- **"Integração com sistemas externos de notificação (email, Slack etc.)"** —
  notificações são apenas internas.
- **"Controle de horas/timesheet do desenvolvedor."**
- **"Suporte a múltiplas organizações/clientes (multi-tenant)."**
- **"Dependência entre projetos."**
- **"Importação em massa de cards."**
- **"Templates de card."**
- **"Duplicar card."**
- **"Anexos/arquivos em cards."**

## Interface

- **Tem interface visual:** sim
  <!-- Alimenta `intent.tem_interface`, que aciona /design no flow. -->

O demandante descreve board, dashboard e telas de configuração, e declarou
requisito de responsividade desktop. Existe design brief e mapa de telas em
`requirements/design/kanban-tarefas/`.

## Perguntas do agente ao demandante

| # | Pergunta | Resposta |
| --- | --- | --- |
| 1 | O que precisa estar diferente depois que isto existir? | Respondida pela fonte — ver *Resultado esperado*. |
| 2 | Por que agora? O que dói hoje? | Respondida pela fonte — ver *Motivação*. |
| 3 | Que restrições já existem? Prazo, orçamento, decisão já tomada, sistema que não pode parar. | Respondida pelo demandante em 2026-09-04: **"não há prazo nem orçamento. será feito por IA e o custo é parte da avaliação do processo"**. Ver *Limites declarados*. |
| 4 | O que você **não** quer que aconteça junto com isso? | Respondida pelo demandante em 2026-09-04 com dois efeitos indesejados, além do escopo não construído que já vinha das fontes. Ver *Fora de escopo*. |
| 5 | Isso tem tela, ou é comportamento sem interface? | Respondida pela fonte — tem tela. |
| 6 | Os três KPIs estão como **"Qualitativa — sem meta numérica definida (ver discovery)"**. Isso permanece assim, ou há meta a declarar? | Respondida pelo demandante em 2026-09-04: **"fica assim"**. Sem meta numérica. |
| 7 | O PRD está em `Status: Draft`. A Intent deve refletir esse rascunho como está, ou houve mudança de intenção desde 2026-08-24? | Respondida pelo demandante em 2026-09-04: **"ignore o status draft e foque no conteúdo"**. O conteúdo do PRD v1.0 vale como intenção vigente. |

---

## Fora deste artefato — regras negativas

Se você está prestes a escrever qualquer coisa abaixo, o conteúdo pertence a
outra etapa. Escreva lá, não aqui.

- **Requisito numerado (RF/RNF/RN)** → `/prd`. Aqui não há requisitos.
- **Solução, mecanismo ou tela** → `/solution` ou `/design`. Se o demandante
  propôs uma solução, registre como *limite declarado* com origem "proposta do
  demandante", nunca como decisão.
- **Tecnologia** → `/techspec`.
- **Diagnóstico da causa do problema** → `/discovery`. Aqui está a dor relatada,
  não a dor investigada.
- **Alternativas consideradas ou descartadas** → `/shape`.
- **Classificação de risco, tipo ou domínio** → `/classify`.
- **Reformulação do problema pelo agente.** Se o agente discorda do
  enquadramento, isso é assunto de `/discovery` — a Intent registra o que foi
  dito, não o que deveria ter sido dito.
