# Plano de verificação — {{FEATURE_NAME}}
_Data: {{DATE}} | Autor: {{AUTHOR}}_
_PRD: docs/prd/{{FEATURE_NAME}}-prd.md (cenários congelados)_
_Tasks: docs/tasks/{{FEATURE_NAME}}-tasks.md_

> Escrito **antes** da implementação e **sem acesso a ela**. Este documento é a
> evidência de que a verificação foi projetada a partir do contrato, e não
> ajustada ao que o código acabou fazendo.

---

## Declaração de independência

- **Implementação existente no momento da escrita:** não \| sim (justificar)
- **Arquivos de produção lidos:** nenhum
- **Commit da suíte:** {{SHA}}
- **Arquivos de produção alterados nesse commit:** nenhum

> Se qualquer linha acima não for "nenhum"/"não", o GATE-VERIFICACAO-INDEPENDENTE
> reprova. Suíte escrita olhando a implementação não verifica o contrato —
> verifica o que foi feito.

---

## Cenários congelados

- **Origem:** `docs/prd/{{FEATURE_NAME}}/*.feature`
- **Alterados desde o gate de spec:** nenhum

| Cenário | RF | Épico | Tipo | Arquivo de teste | Situação |
| --- | --- | --- | --- | --- | --- |
| SCN-001.1 | RF-001 | EPIC-01 | {{TIPO}} | `{{CAMINHO}}` | coberto \| pendente |

> Todo cenário congelado tem ao menos um teste. Cenário sem teste reprova o
> gate; teste sem cenário é escopo que ninguém aprovou.

---

## Estratégia

- **Framework:** {{FRAMEWORK}} <!-- conforme testing.md da coleção declarada -->
- **Runner:** {{COMANDO}}
- **Cobertura mínima exigida:** {{PERCENTUAL}} <!-- da TechSpec -->

### Dependências simuladas

| Dependência | Motivo | Forma |
| --- | --- | --- |
| {{DEPENDENCIA}} | {{MOTIVO}} | mock \| stub \| fake \| contrato |

---

## Testes além dos cenários

Cobertura que o Gherkin não expressa — propriedade, carga, fronteira numérica.

| Teste | Por que existe | Origem |
| --- | --- | --- |
| {{TESTE}} | {{MOTIVO}} | RNF-001 \| regra de borda \| —  |

---

## Execução inicial (Red)

- **Data:** {{DATE}}
- **Resultado esperado:** todos os testes de cenário falham por ausência de
  implementação, e nenhum falha por erro na própria suíte.

| Arquivo | Testes | Falhando | Motivo da falha |
| --- | --- | --- | --- |
| `{{CAMINHO}}` | {{N}} | {{N}} | ausência de implementação |

> Teste que passa antes de existir implementação está verificando outra coisa.
> Investigue antes de seguir.

---

## Congelamento da suíte

- **A partir daqui a suíte não é alterada pela implementação.**
- Alteração legítima exige emenda de cenário no PRD (ID preservado) ou registro
  de desvio aprovado por revisor humano.

| Data | Arquivo | O que mudou | Motivo | Aprovador |
| --- | --- | --- | --- | --- |

---

## Fora deste artefato — regras negativas

- **Código de produção** — pertence ao `/implement`. Escrever a implementação
  aqui destrói a independência que este artefato existe para provar.
- **Cenário Gherkin novo ou alterado** — pertence ao `/prd`, por emenda.
- **Requisito ou regra de negócio** — pertence ao `/prd`. Teste que precisa de
  uma regra que não está no PRD revela lacuna de spec, não licença para inventar.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Ajuste de teste para acomodar comportamento observado** — isso é o defeito
  que o congelamento impede.
