# PRD — {{FEATURE}}

> D2 convergente. Fecha o escopo da entrega e registra **o que precisa ser
> verdade**. É aqui que o comportamento explorado vira requisito verificável.
> **Gates:** GATE-SPEC (aprovação humana), GATE-PROVENIENCIA

- **Shape Brief:** `docs/shape/{{FEATURE}}-brief.md`
- **Solução:** `docs/solution/{{FEATURE}}-solution.md`
- **Design:** `docs/design/{{FEATURE}}/screen-map.md` <!-- se houver interface -->
- **Data:** {{DATA}}
- **Versão:** {{VERSAO}}

---

## Escopo da entrega

> Uma página. O que entra nesta entrega, derivado da fronteira do Shape Brief.
> Divergência em relação ao brief precisa ser justificada aqui.

| Dentro | Fora | Por que fora |
| --- | --- | --- |

## Regras de negócio

| ID | Regra | Procedência | Fonte |
| --- | --- | --- | --- |
| RN-001 | | | |

## Requisitos funcionais

### RF-001 — {{TÍTULO}}

- **Descrição:**
- **Prioridade:** deve | deveria | poderia
- **Procedência:**
- **Fonte:**
- **Regras aplicáveis:** RN-...
- **Origem no protótipo:** <!-- tela/estado, se houver -->

#### Critérios de aceite

| ID | Cenário | Tipo de teste |
| --- | --- | --- |
| SCN-001.1 | | unitário \| integração \| e2e |

```gherkin
Cenário: SCN-001.1 — {{TÍTULO}}
  Dado que ...
  Quando ...
  Então ...
```

## Requisitos não-funcionais

| ID | Requisito | Envelope | Condição de medição | Procedência |
| --- | --- | --- | --- | --- |
| RNF-001 | | | | |

> **Envelope** é o limite verificável (ex: p95 < 300 ms). RNF sem envelope e sem
> condição de medição não é verificável e reprova o GATE-NFR mais adiante.

## Procedência — resumo

| Tipo | Quantidade | IDs |
| --- | --- | --- |
| informada | | |
| derivada | | |
| extraída de legado | | |
| hipótese a validar | | |
| inferida pelo agente | | |

> **Toda** regra e requisito tem exatamente um tipo de procedência.
> `hipótese a validar` exige experimento e critério de descarte na tabela abaixo.
> `inferida pelo agente` é permitida, mas cada ocorrência precisa ser confirmada
> por humano antes do gate — é o tipo que a auditoria vai olhar primeiro.

### Hipóteses a validar

| ID | Experimento | Critério de descarte | Prazo |
| --- | --- | --- | --- |

### Inferências do agente pendentes de confirmação

| ID | O que foi inferido | Por que não havia fonte | Confirmado por |
| --- | --- | --- | --- |

## Dúvidas materiais em aberto

> Dúvida material bloqueia o GATE-SPEC. Registrada por `/clarify`.

| # | Dúvida | Afeta | Quem responde | Status |
| --- | --- | --- | --- | --- |

## Rastreabilidade de origem

| RF/RNF | Veio de | Referência |
| --- | --- | --- |

---

## Aprovação — gate de spec

- **Aprovado por:** {{NOME}}
- **Data:**
- **Ressalvas:**
- **Cenários congelados a partir desta aprovação:** sim

> A partir daqui os cenários são contrato. Alteração exige emenda registrada —
> mexer num `.feature` aprovado sem emenda reprova o GATE-GHERKIN-CONGELADO.

## Emendas de cenário

| Data | Cenário | O que mudou | Motivo | Aprovado por |
| --- | --- | --- | --- | --- |

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
