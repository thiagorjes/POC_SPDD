# Classificação — kanban-tarefas

> Artefato de Controle. Resolve o flow aplicável a partir do Intent.
> Conservadora por construção: na dúvida entre duas classes, escolha a de
> maior exigência de gates.
> IDSD 4.3 — **Gate:** GATE-CLASSIFY

- **Intent de origem:** `docs/intent/kanban-tarefas-intent.md`
- **Data:** 2026-09-04
- **Classificado por:** agente

---

## Classificação

| Eixo | Valor | Justificativa em uma linha |
| --- | --- | --- |
| Tipo | feature | Comportamento inteiramente novo em sistema greenfield; não há comportamento existente divergindo do especificado nem paridade a preservar. |
| Domínio | gestão de fluxo de trabalho, com subdomínio de identidade e controle de acesso | O sistema não tem mapa de domínios registrado — ver nota abaixo; o domínio foi derivado da Intent. |
| Risco | alto | A Intent declara controle de acesso por papéis configuráveis por projeto e autenticação federada: dois dos sinais que a skill lista como determinantes de risco alto, independentemente do tamanho. |
| Impacto | sistema | Sistema único, single-tenant, sem dependência entre projetos e sem integração externa de notificação; o provedor de identidade é consumido, não alterado. |

### Nota sobre o eixo Domínio

O template pede o domínio "conforme o mapa de domínios do sistema", e esse mapa
não existe em `governance/` nem em `memory/`. O valor acima foi derivado da
Intent e é provisório: quando o mapa for estabelecido, este eixo deve ser
reconciliado. Registrar a derivação é preferível a deixar o campo vazio, porque
o eixo alimenta a resolução de flow e a leitura do gate.

### Por que risco alto, e não médio

Dois sinais da lista da skill estão presentes de forma explícita na Intent, e
qualquer um deles bastaria:

- **Altera permissão.** O controle de acesso é por papéis escopados por projeto,
  com toggles que ajustam permissão por projeto — quer dizer que a autorização é
  dado configurável em runtime, não constante de código. Erro aqui não aparece
  como falha, aparece como acesso concedido a quem não deveria tê-lo.
- **Toca autenticação.** A Intent declara SSO contra provedor de identidade
  corporativo, sem senha local.

Some-se que a Intent declara explicitamente que nenhuma escrita pode depender
apenas de validação no cliente — um limite que só faz sentido enunciar quando a
consequência de violá-lo é séria.

### Por que impacto sistema, e não multi-sistema

Foi considerado. O sistema depende de um provedor de identidade externo e de uma
plataforma de contêineres, mas **consome** ambos sem alterar o comportamento de
nenhum deles; a Intent exclui integração com canais externos de notificação e
descarta multi-tenant e dependência entre projetos. A regra do conservadorismo
se aplica à dúvida, e aqui não há dúvida quanto ao alcance da mudança — elevar o
eixo acionaria o gate E2E de épico multi-sistema, hoje sem ambiente integrado
(pendência 01 do `state.md`), sem que exista um segundo sistema a verificar.

Se a `/techspec` ou o `/design` revelarem alteração em sistema vizinho — por
exemplo, mudança de configuração no provedor de identidade que outros sistemas
compartilham —, a reclassificação é obrigatória e retroage.

## Flow resolvido

- **Flow:** `governance/flows/feature.yaml`
- **Gates obrigatórios herdados da classe:** <!-- copiados de gates-por-classe.yaml -->

Núcleo inegociável, que nenhuma classe dispensa:

- GATE-CLASSIFY
- GATE-CONTEXT
- GATE-EVIDENCIA
- GATE-DADOS

Da classe `feature`:

- GATE-INTENT
- GATE-DIRECAO
- GATE-SPEC
- GATE-PROVENIENCIA
- GATE-CONSISTENCIA
- GATE-RASTREABILIDADE
- GATE-VERIFICACAO-INDEPENDENTE
- GATE-GHERKIN-CONGELADO
- GATE-NFR
- GATE-REVISAO-TECNICA

Total: 14 gates. GATE-INTENT já foi atravessado em 2026-09-04.

A policy admite waiver apenas para GATE-NFR e GATE-CONSISTENCIA, e o proíbe para
GATE-EVIDENCIA, GATE-DADOS e GATE-VERIFICACAO-INDEPENDENTE. Nenhum waiver foi
solicitado nesta classificação.

## Override

> Preencher apenas se a classificação automática foi alterada por humano.
> Override sem autor e motivo é inválido e reprova o gate.

- **Classificação original:** não houve override — a classificação vigente é a automática.
- **Classificação após override:** não se aplica.
- **Autor:** não se aplica.
- **Motivo:** não se aplica.
- **Data:** não se aplica.

## Reclassificação

> Se o `/discovery` ou qualquer etapa posterior revelar impacto maior que o
> classificado, a reclassificação é **obrigatória** e retroage: os gates da
> nova classe passam a valer, mesmo que a execução já esteja adiantada.

| Data | De | Para | O que revelou |
| --- | --- | --- | --- |
| — | — | — | Nenhuma reclassificação até o momento. |

Gatilhos a vigiar nesta demanda, dado o que a Intent declara:

- Alteração de configuração compartilhada no provedor de identidade → impacto
  passa a multi-sistema.
- Necessidade de migrar dado de alguma ferramenta de acompanhamento em uso hoje
  → o tipo deixa de ser apenas `feature`.

---

## Fora deste artefato — regras negativas

- **Descrição do problema ou da dor** → `/intent`.
- **Estimativa de esforço, prazo ou tamanho** → `/tasks`. Risco não é tamanho.
- **Decisão sobre como resolver** → `/shape`.
- **Lista de gates inventada pelo agente.** Os gates vêm da policy; este
  artefato apenas cita quais foram herdados.
- **Dispensa de gate.** Waiver é artefato próprio em `governance/waivers/`,
  assinado por humano, com validade.
