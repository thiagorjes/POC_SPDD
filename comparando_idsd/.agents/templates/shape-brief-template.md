# Shape Brief — {{FEATURE}}

> D1 convergente. Produz **decisão, não especificação**.
> Responde *o quê* e *até onde* — nunca *como se comporta* nem *com o quê*.
> **Gate:** GATE-DIRECAO (aprovação humana obrigatória)

- **Intent:** `docs/intent/{{FEATURE}}-intent.md`
- **Discovery:** `docs/discovery/{{FEATURE}}-discovery.md`
- **Data:** {{DATA}}
- **Modo:** entrevista | ingestão <!-- ingestão: Lean Inception, RFP, spec herdada -->
- **Artefato externo ingerido:** <!-- caminho, se modo ingestão -->

---

## Problema escolhido

> Um parágrafo. Qual dos enquadramentos levantados no discovery foi adotado.

## Direção escolhida

> Um parágrafo. A aposta, em nível de negócio.

## Alternativas descartadas

> Sem esta seção o brief não é convergência, é apenas uma proposta.
> Mínimo de duas alternativas reais — variações cosméticas não contam.

| Alternativa | Por que foi considerada | Por que foi descartada | Reabrir se |
| --- | --- | --- | --- |

## Fronteira de escopo

| Dentro | Fora | Adiado para depois |
| --- | --- | --- |

> "Adiado" exige gatilho: o que precisa acontecer para voltar à mesa.

## Personas e jornadas priorizadas

| Persona | Jornada | Prioridade | Por que esta primeiro |
| --- | --- | --- | --- |

## Restrições de negócio

| Restrição | Origem | Rígida ou negociável |
| --- | --- | --- |

## Métrica de sucesso

> Como saberemos, depois de entregue, se a aposta estava certa.

| Métrica | Linha de base hoje | Alvo | Prazo de leitura | Instrumentação existe |
| --- | --- | --- | --- | --- |

## Hipóteses a validar

> Obrigatório em modo ingestão. Saída de workshop é hipótese, não requisito.

| # | Hipótese | Experimento | Critério de descarte |
| --- | --- | --- | --- |

## Restrições técnicas herdadas

> Só em modo ingestão, quando o ritual externo teve participação técnica.
> Entram como **restrição conhecida**, nunca como decisão arquitetural — o
> `/techspec` pode contrariá-las com justificativa.

| Restrição | Quem trouxe | Passou por gate técnico |
| --- | --- | --- |

---

## Aprovação — gate de direção

- **Aprovado por:** {{NOME}}
- **Data:**
- **Ressalvas:**

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
