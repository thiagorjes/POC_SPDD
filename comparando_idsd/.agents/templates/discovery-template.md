# Discovery — {{FEATURE}}

> D1 divergente. Explora contexto, dores e **enquadramentos alternativos** do
> problema. Não escolhe: abre.
> Sem gate próprio — alimenta o `/shape`, que converge e sai no GATE-DIRECAO.

- **Intent:** `docs/intent/{{FEATURE}}-intent.md`
- **Contexto:** `docs/context/{{FEATURE}}-context.md`
- **Data:** {{DATA}}

---

## Situação atual

> Como o trabalho acontece hoje, sem a solução. Descreva o processo real,
> incluindo as gambiarras — é nelas que mora o requisito não dito.

## Dores observadas

| # | Dor | Quem sente | Frequência | Evidência | Custo de conviver com ela |
| --- | --- | --- | --- | --- | --- |

> **Evidência** é dado, reclamação registrada, ticket, observação de campo.
> "É sabido que" não é evidência — sem fonte, marque como suposição a validar.

## Personas

| Persona | Contexto de uso | Objetivo | O que a frustra hoje |
| --- | --- | --- | --- |

## Enquadramentos alternativos do problema

> O coração da divergência. O mesmo conjunto de dores admite mais de uma
> leitura, e cada leitura leva a uma solução diferente. Mínimo de dois.

| # | Enquadramento | Se este for o problema, resolve para quem | O que ficaria sem resposta |
| --- | --- | --- | --- |

## Restrições de contexto

| Restrição | Origem | Como afeta as opções |
| --- | --- | --- |

## Sinais contraditórios

> O que diferentes fontes dizem de forma incompatível. Não resolva aqui —
> registrar a contradição é mais valioso do que escolher um lado cedo demais.

| Tema | Fonte A diz | Fonte B diz | Quem decide |
| --- | --- | --- | --- |

## Perguntas em aberto

| # | Pergunta | Por que importa | Quem responde |
| --- | --- | --- | --- |

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
