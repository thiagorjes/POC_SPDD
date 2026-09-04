# Testes — Exemplo

## Escopo

Governa a suíte automatizada. Não governa teste exploratório manual.

## Regras

| # | Regra | Verificação | Origem |
| --- | --- | --- | --- |
| 01 | Todo cenário do PRD tem um `.feature` correspondente. | `check_congelamento.py` | ADR-001 |
| 02 | Step definition não importa módulo de produção fora da fronteira pública. | revisor humano | entrevista 2026-09-03 |

## Exceções permitidas

Nenhuma.

## Decisões que sustentam estas regras

| DR | Título | Regras afetadas |
| --- | --- | --- |
| ADR-001 | Camadas | 01 |

## Fora deste artefato — regras negativas

- **Não** descreve requisito de produto.
