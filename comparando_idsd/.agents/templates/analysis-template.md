# Análise de consistência — {{FEATURE_NAME}}
_Data: {{DATE}} | Modo: --pre-tasks \| --pre-implement_
_Artefatos analisados: {{LISTA}}_

> Esta análise não corrige nada. Ela localiza a divergência e diz qual artefato
> é o dono da correção. Emendar aqui esconderia o defeito no lugar errado.

---

## Artefatos e versões

| Artefato | Caminho | Última alteração | Considerado |
| --- | --- | --- | --- |
| Intent | docs/intent/{{FEATURE_NAME}}-intent.md | {{SHA}} | sim \| ausente |
| Shape Brief | docs/shape/{{FEATURE_NAME}}-brief.md | {{SHA}} | sim \| ausente |
| Solução | docs/solution/{{FEATURE_NAME}}-solution.md | {{SHA}} | sim \| ausente |
| Design | docs/design/{{FEATURE_NAME}}/screen-map.md | {{SHA}} | sim \| ausente \| n/a |
| PRD | docs/prd/{{FEATURE_NAME}}-prd.md | {{SHA}} | sim \| ausente |
| TechSpec | docs/techspec/{{FEATURE_NAME}}-techspec.md | {{SHA}} | sim \| ausente |

- **Artefato desatualizado em relação a montante:** nenhum

---

## Cobertura da cadeia

Cada elo precisa ser total nas duas direções. Elemento sem origem é escopo que
ninguém pediu; elemento sem destino é requisito que ninguém vai entregar.

| Elo | Órfãos a montante | Órfãos a jusante | Situação |
| --- | --- | --- | --- |
| direção → requisito | {{N}} | {{N}} | ok \| achado |
| regra de borda → cenário | {{N}} | {{N}} | ok \| achado |
| tela/estado → requisito | {{N}} | {{N}} | ok \| achado \| n/a |
| requisito → decisão técnica | {{N}} | {{N}} | ok \| achado |
| RNF → estratégia de verificação | {{N}} | {{N}} | ok \| achado |

---

## Achados

| ID | Tipo | Severidade | Onde | Descrição | Dono da correção |
| --- | --- | --- | --- | --- | --- |
| INC-01 | contradição \| lacuna \| ambiguidade \| duplicação \| escopo órfão | bloqueante \| relevante \| menor | {{ARTEFATO_E_TRECHO}} | {{DESCRICAO}} | /prd \| /techspec \| /solution \| /shape \| /design |

**Tipos:**

- **contradição** — dois artefatos afirmam coisas incompatíveis. O mais grave,
  porque cada um parece coerente lido isoladamente.
- **lacuna** — algo exigido a montante não aparece a jusante.
- **ambiguidade** — texto que duas pessoas competentes leem de formas
  diferentes. Vira defeito na implementação, não aqui.
- **duplicação** — a mesma decisão escrita em dois lugares, livre para divergir.
- **escopo órfão** — existe a jusante sem origem a montante.

---

## Procedência e hipóteses

| Verificação | Resultado |
| --- | --- |
| Inferências do agente pendentes de confirmação | {{N}} |
| Hipóteses sem experimento, critério ou prazo | {{N}} |
| Suposições operacionais vencidas | {{N}} |

> Qualquer contagem diferente de zero é achado bloqueante: a especificação está
> apoiada em algo que ninguém confirmou.

---

## Veredicto

- **GATE-CONSISTENCIA:** aprovado \| reprovado
- **Bloqueantes em aberto:** {{N}}
- **Aprovado por:** {{NOME}} — {{DATA}}

> Achado bloqueante se resolve no artefato dono, e a análise é reexecutada.
> Compensar depois — no plano de tasks ou no teste — é o que este gate existe
> para impedir.

---

## Fora deste artefato — regras negativas

- **Correção de qualquer artefato** — pertence ao dono declarado no achado.
- **Requisito, regra ou cenário novo** — pertence ao `/prd`.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Task ou estimativa** — pertence ao `/tasks`.
- **Reabertura de direção** — pertence ao `/shape`; achado que questiona a
  direção escolhida é devolução, não decisão tomada aqui.
