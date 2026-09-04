---
name: guidelines
description: >
  Mantém a biblioteca compartilhada de guidelines — transversais em `_shared/` e
  coleções por stack em `<camada>/<stack>/` — e declara quais coleções governam
  cada sistema. Conduz a entrevista, materializa o transversal na stack concreta
  e nunca duplica o que já foi decidido. É setup, não etapa de feature.
camada: governance
satisfaz-gates: []
input-artifacts:
  - memory/constitution.md
  - guidelines/README.md
  - guidelines/_templates/stack-guidelines-template.md
output-artifacts:
  - guidelines/<camada>/<stack>/stack.md
  - guidelines/<camada>/<stack>/architecture.md
  - guidelines/<camada>/<stack>/coding-standards.md
  - guidelines/<camada>/<stack>/testing.md
  - guidelines/<camada>/<stack>/definition-of-done.md
  - guidelines.yaml
  - docs/decisions/{{TIPO}}-{{NNN}}-*.md
---

## Objetivo

Deixar registrado, antes da primeira feature, o que é regra e o que é escolha
livre — e em que nível cada coisa vale. Sem isso `/techspec` inventa arquitetura
a cada feature, `/implement` escreve no estilo do último exemplo que leu e
`/code-review` não tem contra o que revisar: vira opinião.

Esta é a única skill que escreve normas. Todas as outras as consomem.

## A biblioteca é compartilhada

As guidelines **não pertencem a um sistema**. São organizadas por stack, porque
`backend/java` vale para todo sistema Java:

```
guidelines/
  _shared/              transversais, agnósticas de stack
  _templates/           molde de coleção nova
  <camada>/
    _shared/            transversais da camada (opcional)
    <stack>/            a coleção concreta
```

`<camada>` é aberto — `backend`, `frontend`, `mobile`, `data`, `infra`, o que o
workspace tiver. `<stack>` é a linguagem ou o framework que decide o "como".
Nunca crie uma camada nova sem que exista pelo menos uma stack dentro dela.

Cada sistema declara em `guidelines.yaml`, na própria raiz, quais coleções o
governam. Um sistema com API Java e front Next.js declara as duas.

O molde de coleção nova vive em `_templates/`, dentro da biblioteca — não em
`.agents/templates/`. Por isso esta skill não declara `template:` no
frontmatter: o caminho da biblioteca é por sistema (`raiz` no
`guidelines.yaml`), e um caminho fixo aqui estaria errado no primeiro sistema
que a montasse em outro lugar.

## A regra de corte

**Se trocar a stack torna a frase falsa, ela pertence à coleção da stack. Se
continua verdadeira, pertence a `_shared/`.**

"Erro 4xx não é logado como `error`" é transversal. "Usar `@Slf4j` do Lombok" é
de `backend/java`. Esse teste resolve quase toda dúvida de onde escrever, e é a
pergunta a fazer antes de criar qualquer regra.

## O risco desta skill

São dois, opostos.

**Duplicar o transversal.** É o mais comum e o mais caro: a coleção da stack
repete o que `_shared/` já decidiu, as duas divergem com o tempo, e a versão
errada é sempre a que alguém leu. A coleção **materializa** o transversal —
linka e mostra o "como" —, nunca o reescreve.

**Produzir arquivos inaplicáveis.** Uma norma só serve se alguém consegue dizer,
olhando um diff, se ela foi cumprida. Por isso toda regra carrega verificador
declarado — comando de lint, teste, ou explicitamente "revisor humano". Regra
sem verificador vira pergunta na entrevista até virar verificável ou ser
descartada.

## Camada

`governance`. A biblioteca fica fora de `governance/`, que é declarativa e onde
agentes não escrevem (IDSD 4.1.1). Esta skill escreve normas técnicas, não
policy de gate; o catálogo de gates e as policies continuam intocáveis.

## Argumentos

- `--stack <camada>/<stack>` — cria ou revisa uma coleção específica
- `--shared` — trabalha só nos transversais
- `--vincular` — só atualiza o `guidelines.yaml` do sistema, sem entrevista
- `--ingestao` — extrai as normas de um codebase existente antes de perguntar
- `--revisar` — relê o que existe e entrevista apenas o que mudou

Sem argumento: identifica o que falta e propõe o escopo antes de começar.

## Pré-condições

- `memory/constitution.md` existe.
- `guidelines/README.md` e `guidelines/_templates/stack-guidelines-template.md`
  existem. São a fonte do que uma coleção deve conter — se faltarem, pare.
- Nenhuma feature em execução. Mudar norma no meio de um flow invalida revisões
  já feitas; se houver, avise e pergunte se deve seguir.

## Workflow

### Fase 0 — Inventário

Antes de perguntar qualquer coisa, leia o que já responde sozinho:

1. `guidelines/README.md` — quais coleções existem e qual o estado de cada uma.
2. O `_shared/` e o `<camada>/_shared/` aplicáveis — é o que **não** se pergunta
   de novo.
3. A coleção alvo, se já existe.
4. No sistema: manifesto de dependências, configuração de lint, formatter, build
   e CI.

Cada item encontrado vira afirmação a confirmar, não pergunta aberta. Perguntar
o que está escrito no repositório queima a paciência do entrevistado antes das
perguntas que importam.

No modo `--ingestao`, amplie: amostre o código e extraia o padrão praticado.
Registre com procedência `extraída de legado` — o que o código faz não é
necessariamente o que o time quer manter, e essa diferença é o assunto da
entrevista.

### Fase 1 — Escopo

Apresente e confirme antes de entrevistar:

- quais coleções serão criadas ou revisadas;
- quais transversais elas materializam;
- quais arquivos condicionais se aplicam (`database.md` só se houver banco
  relacional, `design-system.md` só em frontend com UI, e assim por diante);
- o que fica de fora e por quê.

Coleção que o sistema usa mas que ninguém vai escrever agora **não é omitida**:
vira stub — diretório com `README.md` declarando `Status: não elaborada` e
apontando o template. Dívida declarada é diferente de ausência silenciosa, e o
stub é o que impede outra pessoa de recriar a coleção do zero em paralelo.

### Fase 2 — Entrevista

Uma pergunta por vez. Ordem: **stack → arquitetura → testes → definition of done
→ condicionais**. A ordem não é estética: cada resposta restringe as seguintes.
Decidir padrão de nomenclatura antes de saber se o sistema é um monólito ou um
conjunto de serviços produz retrabalho garantido.

`stack.md` vem primeiro e sempre, porque é ele que decide quais condicionais
existem. Versões são fixas — "latest" não é resposta.

Para cada arquivo, três perguntas bastam na maioria dos casos:

1. O que é obrigatório aqui?
2. O que é proibido, e por quê?
3. Quando a regra pode ser quebrada, e quem autoriza?

A terceira é a mais negligenciada e a que evita a maior parte das discussões
futuras em revisão.

Antes de registrar qualquer resposta, aplique a regra de corte. Se a resposta
vale para qualquer stack, ela não vai para a coleção: vai para `_shared/`, e
isso muda quem precisa aprovar — norma transversal afeta todos os sistemas.

Ao receber resposta que implica trade-off relevante — banco, protocolo de
integração, estratégia de deploy, meta de cobertura, design system — não
registre direto: pergunte pela alternativa descartada e pelo motivo. Esse par
vira Decision Record na Fase 4.

### Fase 3 — Escrita

Um arquivo por vez, salvando assim que o assunto fecha na entrevista. Não
acumule a coleção inteira para o final.

- Imperativo, uma regra por linha. Sem "recomenda-se", sem "sempre que
  possível". Se admite exceção, a exceção vai na tabela de exceções com quem
  autoriza.
- Verificador obrigatório em toda regra.
- Todo arquivo cross-linka o `_shared/` que materializa — isso o validador
  reprova — e termina com um bloco acionável de revisão. A forma é livre:
  lista de tarefas ou lista numerada imperativa servem igualmente, e o
  validador não julga a sintaxe.
- Exemplos de código mínimos e idiomáticos, só onde a regra é ambígua sem eles.
- Idioma pt_BR.

Frontend com UI: `design-system.md` traz tokens concretos, mecânica de
composição e API dos primitivos; `design-tokens.json` é a cópia legível por
máquina; `components/` guarda o código reutilizável. **Inventário de telas e
componentes de rota não entram** — são artefatos do sistema, não da biblioteca.
Essa fronteira é a mesma que separa `/design` (que consome) de `/guidelines`
(que estabelece).

### Fase 4 — Decision Records

Para cada trade-off da Fase 2, crie um DR a partir de
`.agents/templates/decision-record-template.md`, com o tipo apropriado (ADR,
BDR, SDR, DDR) e o próximo número livre daquele tipo. Registre a alternativa
descartada e o motivo — DR sem alternativa descartada é ata de reunião, não
decisão.

Acrescente a linha na tabela do tipo em `memory/constitution.md`, com o ID como
link relativo.

### Fase 5 — Índice e vínculo

- Atualize a tabela de coleções em `guidelines/README.md`.
- Atualize ou crie `guidelines.yaml` na raiz do sistema, declarando a raiz da
  biblioteca e as coleções que o governam.
- Rode `.agents/skills/guidelines/scripts/check_guidelines.py` e corrija o que
  apontar.

### Fase 6 — Confirmação

Apresente, por coleção: quantas regras, quantas com verificação automatizada,
quantas dependentes de revisor humano, quantas pendências em aberto, e quais
condicionais foram dispensados com o motivo.

Regras que **você** inferiu e que ainda não foram confirmadas bloqueiam o
encerramento. Inferir é permitido; deixar a inferência passar por decisão do
time, não.

Feche atualizando `memory/state.md` e informando que a dimensão N do canvas
passa a resolver — `derive_canvas.py` lê as coleções declaradas no
`guidelines.yaml`, sem cópia.

## Saída no chat

Caminho de cada arquivo escrito e uma linha de resumo por coleção. Nunca o
conteúdo das normas nem os exemplos de código.

## Handoff

Aprovado: o sistema está pronto para receber flows de execução — `/intent`.
Reprovado: permanece aqui. Pendência declarada não bloqueia; regra sem
verificador e coleção declarada no `guidelines.yaml` que não existe, sim.
