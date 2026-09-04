---
description: "Traduz os cenários congelados do PRD em decisões técnicas — arquitetura, modelo de dados, contratos, testes, segurança e rastreabilidade. Decide como, nunca o quê: escopo novo descoberto aqui volta para o /prd. Não satisfaz gate próprio; alimenta o /analyze. Use após o /prd, antes do /analyze."
applyTo: "**"
---

<!-- GERADO por .agents/scripts/generate_platform.py — nao editar. -->
<!-- Fonte: .agents/skills/techspec/SKILL.md -->

# /techspec

## Objetivo

Tomar as decisões técnicas antes de existir código, para que `/tasks` distribua
trabalho e `/implement` execute sem reabrir a discussão. Você recebe cenários
congelados e devolve o desenho que os realiza.

Você não satisfaz gate. O que você produz é insumo do `/analyze`, que satisfaz o
de consistência — e é lá que a contradição entre PRD e TechSpec aparece. Escrever
pensando nisso muda o padrão: cada decisão precisa ser confrontável com o PRD por
um terceiro que não participou da conversa.

## O limite: como, nunca o quê

O PRD saiu do gate de spec com os cenários **congelados**. A partir dali eles são
contrato, e esta skill não os toca.

Quase toda TechSpec encontra escopo que o PRD não previu — um estado sem
transição definida, um caso de erro sem comportamento, uma regra que só faz
sentido com um campo que ninguém pediu. A tentação é resolver aqui, porque é mais
rápido. **Devolva ao `/prd`.** Requisito que nasce na TechSpec não passou pelo
gate de spec, não tem cenário, não tem procedência declarada, e some da matriz de
rastreabilidade sem que nada acuse.

Registre o achado, devolva, consuma uma devolução do orçamento. O orçamento
existe para tornar visível o custo de uma spec mal fechada — não para evitar a
devolução.

## Os títulos de seção são contrato

`derive_canvas.py` projeta quatro dimensões do REASONS Canvas lendo âncoras
nomeadas neste documento:

| Seção | Alimenta |
| --- | --- |
| `## 2. Decisões Arquiteturais` | A — Approach |
| `## 3. Modelo de Dados` | E — Entities |
| `## 5. Arquitetura e Fluxo` | S — Structure |
| `## 6. Dependências Inter-Sistemas` | S — Structure |

Renomear ou renumerar uma dessas seções não quebra nada na hora: o canvas sai com
âncora não resolvida e parece defeito do gerador. Use o template e mantenha os
títulos literais.

**O canvas não é escrito aqui.** Ele é derivado das fontes e verificado pelo check
`canvas-drift`. Se você escrever nele à mão, reintroduz exatamente a divergência
que a derivação existe para eliminar.

## Argumentos

- (sem argumento) — feature ativa
- `"nome-da-feature"` — feature específica
- `--ci` — não interativo: sem perguntas, decisões pelo default do flow, falha em
  vez de perguntar
- `--revisar` — relê a TechSpec existente e reabre apenas o que mudou no PRD

## Pré-condições

- `docs/prd/[feature]-prd.md` aprovado no gate de spec, com os `.feature`
  congelados em `docs/prd/[feature]/`.
- `guidelines.yaml` existe na raiz do sistema. Se não existir, o sistema não está
  vinculado à biblioteca de normas: pare e instrua o flow de `setup`
  (`/guidelines`). Não invente convenção técnica no lugar dela.
- Se a Intent marca interface visual, `docs/design/[feature]-design-brief.md`
  existe. O `/design` roda antes do `/prd`; se chegou aqui sem brief, a ausência
  é anterior a você — registre como achado e devolva, não compense.

## Workflow

### Fase 0 — Leitura

Leia, nesta ordem, antes de perguntar qualquer coisa:

1. PRD e os `.feature`. Monte a lista de RFs — é o denominador da matriz de
   rastreabilidade da Fase 3.
2. `docs/solution/[feature]-solution.md` — os fluxos e estados já decididos sem
   tecnologia. A TechSpec escolhe a tecnologia que os realiza; ela não redecide o
   comportamento.
3. O design brief, se houver.
4. `guidelines.yaml` → as coleções declaradas em `colecoes` (os cinco arquivos
   obrigatórios de cada) e os transversais em `_shared/`. A coleção **materializa**
   o transversal: onde as duas falam do mesmo assunto, vale a coleção. Se
   `colecoes` estiver vazio com `motivo` declarado, siga pelos transversais e
   registre a limitação na Seção 1.

5. Confirme que o PRD não está `stale` em relação ao que já foi lido:
   ```
   python .agents/scripts/validate.py --mode input \
     --rules .agents/skills/techspec/validate-rules.json \
     --artifact docs/prd/[feature]-prd.md
   ```
   Se acusar stale, alerte e aguarde confirmação explícita antes de seguir.
   Especificar sobre um PRD que mudou depois do congelamento produz decisão
   tecnicamente correta para um escopo que já não existe.

O que os guidelines já decidiram **não vira pergunta**. Perguntar padrão de
nomenclatura a quem acabou de escrevê-lo em `coding-standards.md` queima a
paciência antes das perguntas que importam.

### Fase 1 — Dependências inter-sistemas

Para cada sistema integrado citado no PRD, verifique se
`systems/[outro-sistema]/guidelines.yaml` existe localmente.

| Situação | Ação |
| --- | --- |
| Sistema próprio, ausente | instruir `git clone <repo> systems/[sistema]` e aguardar |
| Sistema de terceiro | solicitar documentação da API ou swagger |
| Indisponível | oferecer mock contract |

Mock aceito vira `docs/contracts/[X]-mock-contract.md`, marcado **PENDENTE DE
VALIDAÇÃO**, documentado na Seção 6 e com task de substituição prevista. Mock não
declarado como mock é a origem mais comum de integração que só falha em produção.

### Fase 2 — Incertezas técnicas (condicional)

Só executa se houver incerteza real. Se os guidelines e o PRD já resolvem tudo,
informe "Nenhuma incerteza técnica identificada" e siga.

Conta como incerteza o que, decidido errado agora, custa retrabalho depois:
integração sem documentação, biblioteca com trade-off não óbvio, padrão de
modelagem fora dos guidelines, estratégia de auth para um caso específico do PRD,
comportamento sob concorrência ou consistência eventual.

Para cada uma: registre o que é desconhecido, pesquise, decida, justifique. Com
**duas ou mais**, gere `docs/techspec/[feature]-research.md` — por incerteza:
requisito do PRD que a origina, opções com prós e contras, decisão, justificativa,
impacto na TechSpec; e fecha com as não resolvidas.

Incerteza bloqueante para o usuário e aguarda. Não bloqueante vai para a Seção 10.

### Fase 3 — Decisões e escrita

Perguntas técnicas **uma de cada vez**, em três blocos. Pule o que os guidelines
ou o `/solution` já resolveram.

**Bloco A — Abordagem técnica:**
- "Qual abordagem arquitetural para esta feature? (ex: REST API, event-driven, batch)"
- "Há alguma decisão técnica específica desta feature que difere do padrão das coleções declaradas?"

**Bloco B — Modelo de dados:**
- "Quais entidades novas esta feature introduz?"
- "Quais entidades existentes serão modificadas?"
- "Há migrações de banco de dados necessárias?"

**Bloco C — Integrações:**
- "Quais sistemas ou serviços externos esta feature consome ou expõe?"
- Para cada integração: "Qual o contrato esperado? Há documentação disponível?"

Toda decisão com trade-off vira Decision Record a partir de
`.agents/templates/decision-record-template.md`, com o tipo correto (ADR, SDR,
DDR) e o próximo número livre daquele tipo — contadores independentes. DR sem
alternativa descartada e motivo é ata de reunião; registre os dois. Acrescente a
linha na tabela do tipo em `memory/constitution.md`.

Escreva a partir do template, **salvando a cada seção concluída**.

**Fonte única de verdade:** modelo de dados e contratos são volumosos demais para
o documento principal. Eles vivem em artefatos granulares; a TechSpec resume e
referencia. Nunca gere o mesmo conteúdo em dois lugares.

| Seção | Conteúdo |
| --- | --- |
| 1. Visão Geral Técnica | e a limitação de guidelines, se houver |
| 2. Decisões Arquiteturais | com referência aos DRs criados |
| 3. Modelo de Dados | resumo + link; a fonte é `[feature]/data-model.md` (ER, campos, índices, ciclo de vida, migrations) |
| 4. Contratos de API | índice + link; um arquivo por recurso em `[feature]/contracts/`. Sem API: registre "Nenhuma interface de API identificada" |
| 5. Arquitetura e Fluxo | |
| 6. Dependências Inter-Sistemas | inclui os mocks da Fase 1 |
| 7. Estratégia de Testes | conforme o `testing.md` da coleção declarada |
| 8. Segurança e Observabilidade | |
| 9. Matriz de Rastreabilidade | cada RF do PRD → decisão, entidade, contrato |
| 10. Questões em Aberto | |

Salve ao concluir **cada** linha da tabela, não ao final do conjunto — a sessão
pode acabar no meio e o que não está em disco não existe.

Ao fechar a Seção 9, rode o check antes de seguir, em vez de descobrir o gap só
na Fase 5:

```
python .agents/skills/techspec/scripts/check_rf_coverage.py \
  --prd docs/prd/[feature]-prd.md \
  --techspec docs/techspec/[feature]-techspec.md
```

Gere também, **obrigatoriamente**, `docs/techspec/[feature]/quickstart.md`: stack,
estrutura de pastas, setup mínimo, cenários principais por RF com exemplo
executável, pontos de atenção. É o que `/implement` lê antes de codificar, e é o
que evita que ele reabra a TechSpec inteira para achar uma assinatura.

### Fase 4 — Comitê de análise (opcional)

Com tudo salvo em disco, ofereça revisão cruzada pelos agents especialistas
(architect, security, database, devops, qa). Eles leem os arquivos do disco — não
cole conteúdo no prompt. Consolide 1 a 3 pontos por agent, pergunte se aceita
aplicar, aplique e revalide.

Em `--ci`, pule.

### Fase 5 — Validação e handoff

```
python .agents/scripts/validate.py --mode output \
  --rules .agents/skills/techspec/validate-rules.json \
  --artifact docs/techspec/[feature]-techspec.md
```

A validação roda `check_rf_coverage.py` e reprova RF do PRD ausente da matriz.
Complete a matriz — não relaxe o check.

Atualize `memory/state.md`: a linha da demanda em **Demandas ativas** passa a
`/techspec` concluída, e toda devolução ao `/prd` entra em **Devoluções**. Não há
Artifact Registry nesta stack: o estado da demanda é a linha, não uma tabela de
versões paralela.

## Fora deste artefato — regras negativas

- **Requisito funcional novo** — pertence ao `/prd`. Aqui ele não tem cenário nem
  procedência.
- **Cenário Gherkin** — pertence ao `/prd`, e está congelado.
- **Task, estimativa, sequenciamento** — pertencem ao `/tasks`.
- **Escrita no canvas** — ele é derivado.
- **Norma técnica de aplicação geral** — pertence ao `/guidelines`. Se a decisão
  vale para além desta feature, ela é guideline, e escrevê-la aqui a esconde de
  todos os outros sistemas.

## Handoff

Próximo comando: `/analyze --pre-tasks`.

Bloqueiam o encerramento: RF sem linha na matriz, mock sem task de substituição
prevista, e incerteza bloqueante em aberto. Achado devolvido ao `/prd` não
bloqueia o encerramento — bloqueia o `/analyze`, e é lá que ele será cobrado.
