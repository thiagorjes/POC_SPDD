# AGENTS.md — IDSD

<!-- GERADO por .agents/scripts/init.py — nao editar. -->

> Referencia canonica cross-vendor de skills e agents deste sistema.
> `CLAUDE.md` referencia este arquivo via `@`.

---

## Camadas

- **Governanca** — `governance/`: constitution, policies e o catalogo de gates.
  Declarativa. Agentes nao tem permissao de escrita (IDSD 4.1.1).
- **Controle** — `/intent`, `/classify`, `/context` e a orquestracao.
  Resolve o flow aplicavel e confronta cada transicao com as policies.
- **Execucao** — os flows registrados em `governance/flows/`.
  O SSPDD e um deles, nao o unico.

Nenhum flow e obrigado a rodar o SSPDD. Todo flow e obrigado a satisfazer
os gates que a policy exige para a classe do intent que o originou —
verificado no registro por `validate_flow.py`.

---

## Skills disponiveis

- **/analyze** — Confronta os artefatos da cadeia — intent, shape, solução, design, PRD e TechSpec — em busca de contradição, lacuna, ambiguidade, duplicação e escopo órfão. Localiza e devolve; não corrige. Satisfaz o gate de consistência. Use após /techspec, antes de /tasks.
- **/classify** — Classifica a demanda por tipo, domínio, risco e impacto, e resolve qual flow se aplica. Conservadora por construção. Segunda etapa da camada de Controle. Use logo após /intent, e novamente sempre que uma etapa posterior revelar impacto maior que o classificado.
- **/code-review** — Revisa o épico implementado contra a task, a especificação e os envelopes de RNF, com análise de segurança obrigatória. O revisor não escreve código: achado é devolvido a quem implementa. Satisfaz a revisão técnica e o gate de NFR. Use ao fechar o épico, antes do merge.
- **/context** — Monta o contexto mínimo necessário para a etapa seguinte e registra toda lacuna como evidência em vez de inferir. Terceira etapa da camada de Controle, reexecutada antes de cada etapa de Execução. Use após /classify e sempre que uma etapa precisar de insumo que ainda não está em disco.
- **/design** — Conduz entrevista de descoberta de design (após /solution, antes de /prd) para features com interface visual — mapeia telas, fluxos, estados e requisitos de acessibilidade, gera o Design Brief e aciona o agente prototipador autônomo para gerar tokens e protótipos navegáveis. Pular para features puramente backend/API.
- **/discovery** — Explora contexto, dores, personas e enquadramentos alternativos do problema. É a divergência do primeiro diamante — abre o leque, não escolhe. Recebe a Intent já validada em vez de elicitá-la. Use após /context, antes de /shape.
- **/evidence** — Sela o pacote de evidências no release com hash — versões, gates, classificação e overrides, rastreabilidade, ordem de verificação, envelopes de NFR, achados, tentativas, custo e intervenções humanas. É cartório e não narrador; registro ausente reprova o pacote e nunca é reconstruído. Última etapa de qualquer flow.
- **/guidelines** — Mantém a biblioteca compartilhada de guidelines — transversais em `_shared/` e coleções por stack em `<camada>/<stack>/` — e declara quais coleções governam cada sistema. Conduz a entrevista, materializa o transversal na stack concreta e nunca duplica o que já foi decidido. É setup, não etapa de feature.
- **/implement** — Executa uma task até a suíte congelada passar, sem tocar nos cenários nem nos step definitions. Trabalha dentro do escopo de arquivo declarado na task, registra cada tentativa no histórico e para quando o orçamento acaba. Use após /tests, uma task por vez.
- **/intent** — Captura a intenção do demandante nas palavras dele — resultado esperado, motivação, limites e fora de escopo — e valida com ele antes de qualquer descoberta. Primeira etapa da camada de Controle. Use na abertura de qualquer demanda, antes de /classify.
- **/prd** — Converge a exploração de solução e o protótipo em escopo de entrega — regras de negócio, requisitos funcionais e não-funcionais, critérios de aceite em Gherkin com ID estável e procedência declarada por regra. Sai no gate de spec, a partir do qual os cenários viram contrato congelado. Use após /solution e /design.
- **/shape** — Converge o discovery em uma direção decidida — problema escolhido, alternativas descartadas com motivo, fronteira de escopo, personas priorizadas e métrica de sucesso. Produz decisão, não especificação. Suporta modo ingestão para Lean Inception, RFP e specs herdadas. Use após /discovery, antes de /solution.
- **/solution** — Explora o comportamento da solução sem nenhuma tecnologia — fluxos de operação, estados e transições, semântica de contrato e regras de borda. É a divergência do segundo diamante e funciona para fluxos com e sem tela. Use após /shape, antes de /design e /prd.
- **/spdd-sync** — Confronta o código entregue com a especificação que o originou e decide, com o humano, qual dos dois lados está errado — corrigir a spec ou corrigir o código. Registra cada desvio e nunca resolve sozinha. Use após /code-review, quando o comportamento implementado divergiu do que estava especificado.
- **/tasks** — Distribui os cenários congelados do PRD em épicos verticais e tasks auto-contidas, com dependências explícitas, escopo de arquivo declarado e instrução precisa o bastante para ser executada sem reabrir PRD ou TechSpec. Satisfaz o gate de rastreabilidade pela invariante cenário↔épico. Use após /techspec e /analyze.
- **/techspec** — Traduz os cenários congelados do PRD em decisões técnicas — arquitetura, modelo de dados, contratos, testes, segurança e rastreabilidade. Decide como, nunca o quê: escopo novo descoberto aqui volta para o /prd. Não satisfaz gate próprio; alimenta o /analyze. Use após o /prd, antes do /analyze.
- **/tests** — Escreve a suíte de verificação a partir dos cenários congelados, antes da implementação e sem acesso a ela. Produz step definitions, testes e o plano de verificação, e congela a suíte. Satisfaz a verificação independente e o congelamento do Gherkin. Use após /tasks e antes de /implement.

---

## Agents disponiveis

- **architect** — Atua como Software Architect no Comitê de Análise Assíncrono. Revisa TechSpec e Guidelines em busca de falhas arquiteturais, gargalos de escalabilidade e anti-patterns.
- **database** — Atua como Database Administrator (DBA) no Comitê de Análise Assíncrono. Revisa modelos de dados, endpoints e estratégias de armazenamento em busca de gargalos (N+1), falhas de normalização e falta de indexação.
- **designer** — Atua como Desenvolvedor Frontend Prototipador Autônomo. Lê PRD e design-brief.md e materializa protótipos navegáveis reais (HTML/JSON), sem interagir com o usuário — apenas executa e entrega os artefatos.
- **devops** — Atua como Engenheiro de DevOps/Plataforma no Comitê de Análise Assíncrono. Revisa pipelines de CI/CD, estratégias de deploy, infraestrutura, observabilidade e práticas de git workflow.
- **qa** — Atua como Quality Engineer no Comitê de Análise Assíncrono e em revisões pós-implementação. Modo requisitos: revisa PRD/TechSpec em busca de critérios de aceite vagos e RNFs sem meta mensurável. Modo código: revisa arquivos implementados em busca de falhas de correção, segurança e cobertura de testes.
- **security** — Atua como Security Engineer (AppSec) no Comitê de Análise Assíncrono. Revisa arquivos em busca de vulnerabilidades, falhas de autenticação, vazamento de dados e quebras de compliance.

---

## Convencoes

- Toda skill tem `SKILL.md` em `.agents/skills/[skill]/` e `validate-rules.json`.
- Todo agent tem definicao em `.agents/agents/[agent].md`.
- `memory/constitution.md` — principios estaveis e DRs.
  `memory/state.md` — estado operacional.
- Arquivos em `.claude/`, `.cursor/`, `.opencode/` e `.github/` sao gerados.
- O REASONS Canvas e derivado das fontes, nao mantido a mao:
  `.agents/scripts/derive_canvas.py --feature <nome>`.
- Sincronia dos derivados: `.agents/scripts/check_drift.py`
  (`canvas-drift` e `skill-drift`).
