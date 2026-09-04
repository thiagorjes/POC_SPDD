# Tasks — {{FEATURE_NAME}}
_Data: {{DATE}} | Autor: {{AUTHOR}}_
_PRD: docs/prd/{{FEATURE_NAME}}-prd.md_
_TechSpec: docs/techspec/{{FEATURE_NAME}}-techspec.md_
_Cenários congelados: docs/prd/{{FEATURE_NAME}}/*.feature_

> Este documento é o plano de execução. Ele não redefine requisito, não decide
> arquitetura e não escreve teste — só distribui, ordena e instrui.
> A camada de User Story não existe: o cenário Gherkin já é a narrativa e já
> está congelado como contrato.

---

## Princípios para tasks prontas para IA

Uma task está pronta quando quem a executa **não precisa abrir o PRD nem a
TechSpec para saber o que fazer**. Isso implica cópia integral, não referência:

- **Auto-contida.** Todo dado necessário está no arquivo da task — assinatura de
  método, nome de campo, mensagem de erro literal, modo de arredondamento.
  "Conforme a TechSpec §4.2" não é instrução, é adiamento.
- **Um resultado verificável.** Se a task tem dois resultados independentes, são
  duas tasks.
- **Sem decisão em aberto.** Task que começa com uma escolha a fazer é uma
  lacuna do `/techspec` que escapou do `/analyze` — devolva em vez de executar.
- **Escopo de arquivo declarado.** O que pode ser criado, o que pode ser
  alterado, e o que é proibido tocar.

---

## Sumário de épicos

| ID | Épico | Sistema | Cenários entregues | Tasks | Executor | Pode iniciar |
| --- | --- | --- | --- | --- | --- | --- |
| EPIC-01 | {{NOME_EPICO}} | {{SISTEMA}} | SCN-001.1, SCN-001.2 | {{N}} | agente \| humano \| misto | imediatamente |

**Invariante de rastreabilidade:** todo cenário do PRD é entregue por
**exatamente um** épico; todo épico entrega **pelo menos um** cenário. Cenário
órfão ou duplicado reprova o GATE-RASTREABILIDADE.

### Cobertura de cenários

| Cenário | RF | Épico | Tipo de teste |
| --- | --- | --- | --- |
| SCN-001.1 | RF-001 | EPIC-01 | {{TIPO}} |

### Dimensionamento

| Épico | Tasks | Situação |
| --- | --- | --- |
| EPIC-01 | {{N}} | ok (≤ 8) \| aceito (9–12) \| revisar (13–20) \| bloqueado (> 20) |

---

## Grafo de dependências

```
EPIC-01
  ├── TASK-01.1
  └── TASK-01.2 (depende de TASK-01.1)
```

Épicos que podem correr em paralelo: {{LISTA}}.
A ordem dentro do épico é serial — as tasks compartilham o mesmo PR.

---

## EPIC-01 — {{NOME_EPICO}}

- **Sistema:** {{SISTEMA}}
- **Entrega:** SCN-001.1, SCN-001.2
- **Depende de:** nenhum
- **Fatia vertical:** {{O_QUE_FICA_IMPLANTAVEL_E_TESTAVEL_AO_FINAL}}
- **Gate do épico:** PR aberto e cenários entregues passando.

### TASK-01.1 — {{TITULO}}

- **Status:** pendente \| em execução \| concluída \| bloqueada
- **Sistema:** {{SISTEMA}}
- **Executor:** agente \| humano \| misto
- **Tentativas:** {{N}} <!-- orçamento do laço agêntico; herdado do flow -->
- **Esforço:** P \| M \| G <!-- obrigatório quando executor = humano -->
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-001.1
- **Origem:** RF-001, RN-002, TechSpec §{{SECAO}}

#### Contexto

{{POR_QUE_ESTA_TASK_EXISTE_EM_UM_PARAGRAFO}}

#### O que deve ser feito

- [ ] {{ACAO_1}}
- [ ] {{ACAO_2}}

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `{{CAMINHO}}` | criar \| alterar | {{O_QUE_MUDA}} |

**Proibido tocar:** `{{CAMINHOS}}` <!-- inclui sempre os .feature e step definitions -->

#### Guia técnico — padrão a seguir

Referência viva no repositório: `{{ARQUIVO_ANALOGO_JA_EXISTENTE}}`.

{{ASSINATURAS_NOMES_DE_CAMPO_MENSAGENS_LITERAIS}}

#### Guia técnico — pontos de atenção

- {{ARMADILHA_CONHECIDA_1}}

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | {{CRITERIO}} | {{COMO_SE_COMPROVA}} |

> Step definitions **não** entram aqui. Eles já existem — foram escritos pelo
> `/tests`, que rodou antes e sem acesso à implementação. A task depende deles;
> não os produz nem os altera.

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| {{DATA}} | {{tentativa \| bloqueio \| decisão \| desvio}} | {{DETALHE}} |

> Registro incremental, escrito no momento em que o fato ocorre. É daqui que o
> `/evidence` recolhe tentativas, custo e intervenção humana — reconstruir
> depois não vale como evidência.

---

## Fora do escopo desta entrega

- {{ITEM}}

---

## Fora deste artefato — regras negativas

O que aparecer aqui é sinal de etapa anterior mal fechada, e o validador reprova:

- **Requisito novo ou reescrito** — pertence ao `/prd`. Task não cria requisito;
  se algo necessário não tem RF, o PRD está incompleto.
- **Decisão arquitetural** — pertence ao `/techspec`. Escolha de biblioteca,
  padrão ou modelo de dados feita aqui não passou por revisão técnica.
- **Cenário Gherkin novo ou alterado** — os cenários estão congelados no gate de
  spec; mudança exige emenda registrada no PRD, com o ID preservado.
- **Código de teste e step definitions** — pertencem ao `/tests`.
- **Implementação** — trechos de código de produção pertencem ao `/implement`.
  Guia técnico é assinatura e contrato, não corpo de função.
- **User Story** — a camada foi eliminada; o cenário é a narrativa.
