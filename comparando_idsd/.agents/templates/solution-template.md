# Exploração de Solução — {{FEATURE}}

> D2 divergente, universal — vale para fluxos com e sem tela.
> Responde *como se comporta*, sem nenhuma tecnologia.
> Sem gate próprio: alimenta o `/prd`, que fecha no GATE-SPEC.

- **Shape Brief:** `docs/shape/{{FEATURE}}-brief.md`
- **Data:** {{DATA}}

> **Teste de fronteira com o `/techspec`:** se trocar a tecnologia torna a frase
> falsa, a frase é de techspec e não pertence aqui.
> Pertence: "a operação é idempotente e rejeita duplicata em 24h devolvendo o
> resultado anterior". Não pertence: "chave de idempotência em Redis com TTL".

---

## Fluxos de operação

> Um bloco por fluxo. Passos em linguagem de negócio, sem componente técnico.

### Fluxo: {{NOME}}

- **Ator:**
- **Gatilho:**
- **Resultado esperado:**

| # | Passo | Quem faz | O que precisa ser verdade antes |
| --- | --- | --- | --- |

**Caminhos alternativos:**

| Condição | O que acontece |
| --- | --- |

## Estados e transições

| Estado | Significado para o negócio | Transições que saem | Quem pode disparar |
| --- | --- | --- | --- |

- **Estado inicial:**
- **Estados terminais:**
- **Transições proibidas:** <!-- o que nunca pode acontecer -->

## Semântica de contrato

> Comportamento observável de cada operação, independente de protocolo.

| Operação | Entrada (significado) | Saída (significado) | Idempotente | Efeito colateral |
| --- | --- | --- | --- | --- |

## Regras de borda

| # | Situação | Comportamento esperado | Por que não é o óbvio |
| --- | --- | --- | --- |

> Cobrir no mínimo: vazio, duplicado, concorrente, fora de ordem, parcial,
> expirado, sem permissão. "Não se aplica" é resposta válida, mas explícita.

## Opções de comportamento consideradas

> Isto é a divergência de D2. Sem alternativas, não houve exploração.

| Questão em aberto | Opção A | Opção B | Recomendação | Decidir em |
| --- | --- | --- | --- | --- |

## Entidades e vocabulário

> Nomes que o negócio usa. Vira insumo direto da dimensão E do canvas.

| Termo | Definição | Não confundir com |
| --- | --- | --- |

---

## Fora deste artefato — regras negativas

- **Qualquer tecnologia**: banco, fila, framework, biblioteca, protocolo,
  formato de serialização, nome de serviço → `/techspec`.
- **Requisito numerado ou critério de aceite** → `/prd`. Aqui há comportamento
  descrito, não requisito verificável.
- **Cenário Gherkin** → `/prd`. É lá que o cenário ganha ID e vira contrato.
- **Tela, layout, componente visual** → `/design`. Aqui o fluxo é agnóstico de
  interface; se só faz sentido com tela, está no artefato errado.
- **Escolha de direção de negócio** → `/shape`. Se a exploração revelar que a
  direção está errada, devolva ao `/shape`; não decida aqui.
- **Modelagem de dados, tabela, campo, tipo** → `/techspec`. Entidade aqui é
  vocabulário de negócio, não esquema.
- **Estimativa, task, sequenciamento de entrega** → `/tasks`.
