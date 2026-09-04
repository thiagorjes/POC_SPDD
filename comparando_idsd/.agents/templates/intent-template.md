# Intent — {{FEATURE}}

> Artefato de Controle. Registra a intenção **nas palavras do demandante**.
> IDSD 4.2 — a intenção pertence a quem demanda. O agente transcreve e organiza;
> não traduz para linguagem técnica, não completa, não melhora.

- **Demandante:** {{NOME}} — {{PAPEL}}
- **Data de captura:** {{DATA}}
- **Validado pelo demandante em:** {{DATA}} <!-- obrigatório antes do gate -->
- **Gate:** GATE-INTENT

---

## Resultado esperado

> O que precisa ser diferente no mundo depois que isto existir.
> Nas palavras do demandante, entre aspas quando for citação direta.

## Motivação

> Por que agora. O que dói hoje ou que oportunidade se perde.

## Limites declarados

> Restrições que o demandante já impõe: prazo, orçamento, sistema que não pode
> parar, decisão já tomada em outra instância.

| Limite | Origem | Negociável |
| --- | --- | --- |

## Fora de escopo

> O que o demandante diz explicitamente que **não** quer. Registrar mesmo quando
> parecer óbvio — é o que evita ampliação silenciosa mais tarde.

## Interface

- **Tem interface visual:** sim | não | a definir
  <!-- Alimenta `intent.tem_interface`, que aciona /design no flow. -->

## Perguntas do agente ao demandante

| # | Pergunta | Resposta |
| --- | --- | --- |

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
