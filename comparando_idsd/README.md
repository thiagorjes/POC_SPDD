# IDSD — Intent-Driven Software Delivery

Modelo de governança para um ciclo de desenvolvimento conduzido por agentes. O
SSPDD é um dos flows registrados aqui, não o único caminho.

---

## As três camadas

| Camada | Onde vive | O que faz |
| --- | --- | --- |
| **Governança** | `governance/` | Constitution, policies e o catálogo de gates. Declarativa. Agentes não têm permissão de escrita. |
| **Controle** | `/intent`, `/classify`, `/context` | Captura a intenção, classifica a demanda, resolve qual flow se aplica e monta o contexto de cada etapa. |
| **Execução** | `governance/flows/` | Os flows registrados. Cada um declara suas etapas e qual gate cada etapa satisfaz. |

Nenhum flow é obrigado a rodar o SSPDD. Todo flow é obrigado a satisfazer os
gates que a policy exige para a classe do intent que o originou — e isso é
verificado **no registro**, não em runtime: um flow que não cobre os gates da
classe que declara atender nunca chega a executar.

---

## Estrutura

```
.agents/            fonte única — skills, agents, templates, scripts
governance/         gates.yaml, policies/, flows/
docs/               artefatos produzidos pelos flows
guidelines.yaml     vinculo com a biblioteca compartilhada de guidelines
memory/             constitution.md (estável) e state.md (operacional)
.claude/ .cursor/
.opencode/ .github/ GERADOS a partir de .agents/ — não editar
```

Os diretórios de plataforma referenciam `.agents/` por include, sem duplicar
corpo de skill (exceto Copilot, que não suporta include e recebe o corpo
embutido, marcado como gerado).

---

## Comandos

| O quê | Comando |
| --- | --- |
| Regenerar tudo a partir de `.agents/` | `python .agents/scripts/init.py` |
| Verificar se algum derivado foi editado à mão | `python .agents/scripts/check_drift.py` |
| Validar os manifestos de flow contra a policy | `python .agents/scripts/validate_flow.py` |
| Validar a estrutura das skills | `python .agents/scripts/validate_skills.py .agents/skills` |
| Derivar o REASONS Canvas de uma feature | `python .agents/scripts/derive_canvas.py --feature <nome>` |

`init.py` é idempotente e roda os dois validadores antes de gerar qualquer
coisa. Se um flow ou uma skill estiver inconsistente, ele para sem escrever.

---

## Primeira execução

Abra a sessão **com o diretório de trabalho neste diretório**, não na raiz do
workspace. As duas stacks têm `.claude/skills/` próprio; a partir da raiz você
carrega a stack anterior.

1. `python .agents/scripts/init.py` — cria a árvore e os arquivos de contexto.
2. `/guidelines` — flow de setup, roda uma vez por sistema. Mantém as coleções
   da biblioteca compartilhada (`../guidelines/`, organizada por stack) e
   escreve o `guidelines.yaml` que diz quais delas governam este sistema. Sem
   ele a dimensão N do canvas não resolve e `/code-review` não tem contra o que
   revisar.
3. `/intent` — a partir daqui a demanda entra pela camada de Controle, que
   decide o flow.

---

## Dois artefatos são derivados

Nunca edite à mão:

- **`.claude/`, `.cursor/`, `.opencode/`, `.github/`** — derivados de
  `.agents/`. Corrija a fonte e rode `init.py`.
- **`docs/spdd/<feature>-canvas.md`** — derivado do PRD, da solução, da
  TechSpec, das tasks e das guidelines. Corrija a fonte e rode
  `derive_canvas.py`.

`check_drift.py` reprova nos dois casos. Editar um derivado não quebra nada na
hora — quebra depois, quando alguém confia nele e ele já não corresponde à
fonte.

---

## Estado da implantação

Ver [memory/state.md](memory/state.md).
