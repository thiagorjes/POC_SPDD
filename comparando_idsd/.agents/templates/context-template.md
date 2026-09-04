# Contexto — {{FEATURE}}

> Artefato de Controle. Monta o contexto mínimo de cada etapa e **registra
> lacuna como evidência em vez de inferir**.
> IDSD 4.6 — **Gate:** GATE-CONTEXT

- **Data:** {{DATA}}
- **Etapa alvo:** {{ETAPA}}

---

## Contexto resolvido

| Item | Fonte | Trecho relevante | Confiança |
| --- | --- | --- | --- |

> **Fonte** é caminho de arquivo, URL ou pessoa. "Conhecimento do modelo" não é
> fonte válida — se não há fonte, é lacuna.

## Lacunas

> A parte mais importante deste artefato. Lacuna registrada é evidência;
> lacuna preenchida por inferência é violação.

| # | O que falta | Quem sabe | Bloqueia qual etapa | Status |
| --- | --- | --- | --- | --- |

- **Lacunas bloqueantes em aberto:** {{N}}
  <!-- Se > 0, a etapa alvo não pode iniciar. -->

## Suposições operacionais

> Só para lacunas **não bloqueantes** que a etapa pode atravessar assumindo algo.
> Cada suposição precisa de um dono e de um momento de confirmação.

| # | Suposição | Se estiver errada | Dono | Confirmar até |
| --- | --- | --- | --- | --- |

## Sistemas e artefatos em jogo

| Sistema | Papel nesta execução | Guidelines |
| --- | --- | --- |

---

## Fora deste artefato — regras negativas

- **Resposta inventada para uma lacuna.** Se o agente não tem fonte, o campo é
  lacuna. Preencher por plausibilidade é a violação exata que a 4.6 proíbe.
- **Decisão de qualquer tipo.** Este artefato reúne insumo; quem decide é
  `/shape`, `/prd` ou `/techspec`.
- **Requisito, regra de negócio ou cenário** → `/prd`.
- **Resumo do domínio escrito pelo agente sem citar fonte.** Se não dá para
  apontar de onde veio, não entra.
- **Dado real de cliente.** Nunca, em nenhum campo (IDSD 4.10.1). Use
  referência ao registro, não o conteúdo.
