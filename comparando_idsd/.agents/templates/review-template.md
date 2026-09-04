# Revisão técnica — {{ESCOPO}}
_Data: {{DATE}} | Revisor: {{REVISOR}} | Épico: {{EPIC}} | PR: {{PR}}_
_Commits revisados: {{SHA_INICIAL}}..{{SHA_FINAL}}_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-01.1, TASK-01.2 |
| Cenários entregues | SCN-001.1 |
| Arquivos | {{N}} |
| Suíte | {{N}} testes, {{N}} falhando |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

> Qualquer das duas linhas diferente de "nenhum" sem emenda registrada reprova
> o GATE-VERIFICACAO-INDEPENDENTE, e a revisão para aqui.

---

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-001.1 | sim \| não | sim \| não \| parcial | {{OBS}} |

- **Escopo além do especificado:** nenhum
  <!-- código que não corresponde a nenhum requisito é escopo que ninguém aprovou -->

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-001 | {{ENVELOPE}} | {{VALOR}} | {{INSTRUMENTO}} | dentro \| fora \| não medido |

> RNF não medido não passa por omissão. Se a instrumentação não existe, o
> GATE-NFR reprova — a ausência de medição é o achado.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante \| relevante \| menor | código \| spec \| segurança \| dados | `arquivo:linha` | {{DESCRICAO}} | /implement \| /prd \| /techspec |

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.

---

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | ok \| achado | {{REF}} |
| Autorização verificada por operação | ok \| achado | {{REF}} |
| Segredo fora do código e do log | ok \| achado | {{REF}} |
| Dado sensível fora de log e mensagem de erro | ok \| achado | {{REF}} |
| Dependência nova sem vulnerabilidade conhecida | ok \| achado \| n/a | {{REF}} |

---

## Guardrails extraídos

Regras que esta revisão descobriu e que devem valer para as próximas.

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| {{REGRA}} | ACH-01 | guidelines/{{CAMADA}}/{{STACK}}/{{ARQUIVO}}.md |

---

## Veredicto

- **GATE-REVISAO-TECNICA:** aprovado \| reprovado
- **GATE-NFR:** aprovado \| reprovado
- **Bloqueantes em aberto:** {{N}}
- **Revisor humano:** {{NOME}} — {{DATA}}

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
