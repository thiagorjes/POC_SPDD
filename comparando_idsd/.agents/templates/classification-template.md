# Classificação — {{FEATURE}}

> Artefato de Controle. Resolve o flow aplicável a partir do Intent.
> Conservadora por construção: na dúvida entre duas classes, escolha a de
> maior exigência de gates.
> IDSD 4.3 — **Gate:** GATE-CLASSIFY

- **Intent de origem:** `docs/intent/{{FEATURE}}-intent.md`
- **Data:** {{DATA}}
- **Classificado por:** agente | humano

---

## Classificação

| Eixo | Valor | Justificativa em uma linha |
| --- | --- | --- |
| Tipo | feature \| bug \| dependencia \| migracao \| emergencia | |
| Domínio | | |
| Risco | baixo \| medio \| alto | |
| Impacto | local \| sistema \| multi-sistema | |

## Flow resolvido

- **Flow:** `governance/flows/{{FLOW}}.yaml`
- **Gates obrigatórios herdados da classe:** <!-- copiados de gates-por-classe.yaml -->

## Override

> Preencher apenas se a classificação automática foi alterada por humano.
> Override sem autor e motivo é inválido e reprova o gate.

- **Classificação original:**
- **Classificação após override:**
- **Autor:**
- **Motivo:**
- **Data:**

## Reclassificação

> Se o `/discovery` ou qualquer etapa posterior revelar impacto maior que o
> classificado, a reclassificação é **obrigatória** e retroage: os gates da
> nova classe passam a valer, mesmo que a execução já esteja adiantada.

| Data | De | Para | O que revelou |
| --- | --- | --- | --- |

---

## Fora deste artefato — regras negativas

- **Descrição do problema ou da dor** → `/intent`.
- **Estimativa de esforço, prazo ou tamanho** → `/tasks`. Risco não é tamanho.
- **Decisão sobre como resolver** → `/shape`.
- **Lista de gates inventada pelo agente.** Os gates vêm da policy; este
  artefato apenas cita quais foram herdados.
- **Dispensa de gate.** Waiver é artefato próprio em `governance/waivers/`,
  assinado por humano, com validade.
