# Revisão técnica — TASK-01.1 (revisão parcial de task)

_Data: 2026-09-10 | Revisor: agente | Épico: EPIC-01 | PR: não aberto_
_Commits revisados: dcf6db8..fe491d6 (mais a árvore de trabalho não commitada)_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

Esta é uma **revisão parcial** (`/code-review TASK-01.1`), não o fechamento do
EPIC-01. O épico tem oito tasks e só a primeira está concluída: nenhum cenário
congelado fecha aqui, e nenhum envelope de RNF é mensurável ainda. O relatório
registra o que é verificável hoje e nomeia explicitamente o que fica para o
fechamento do épico.


## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-01.1 |
| Cenários entregues | SCN-001.1 (habilitado, não fechado) |
| Arquivos | 11 (1 `pom.xml`, 1 `application.yml`, 1 classe, 7 marcadores de pacote, 1 `.gitignore`) |
| Suíte | 49 arquivos de teste em disco, 0 executáveis — a compilação de teste falha |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

Ressalva material, registrada como ACH-01: as duas linhas acima foram apuradas
por leitura de disco, **não por git**. Nem os `.feature` nem os arquivos sob
`backend/src/test/` foram alguma vez commitados — não estão ignorados, estão
apenas fora do controle de versão. Não existe, portanto, ponto de congelamento
contra o qual comparar.

> Qualquer das duas linhas diferente de "nenhum" sem emenda registrada reprova
> o GATE-VERIFICACAO-INDEPENDENTE, e a revisão para aqui.


## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-001.1 | não | parcial | Não é executável nesta task por construção: fecha ao fim do EPIC-01 e depende de TASK-01.2 a TASK-01.7. A árvore de pacotes, o `pom.xml` e o `application.yml` correspondem ao guia técnico da task; o critério de aceite 1 (`mvn -q verify`) não é satisfeito nem satisfazível aqui — ver ACH-07 |

- **Escopo além do especificado:** nenhum. Duas adições não listadas na tabela de
  arquivos da task e justificadas: `backend/.gitignore` (`target/`) e a classe de
  arranque `br.com.idsd.kanban.Aplicacao`, cujo nome é imposto pela suíte
  congelada (`InstanciasEmParalelo:64`). `spring-boot-starter-security` entra como
  dependência explícita do que o Resource Server já traz transitivamente.

## Critérios de aceite da task

Verificações executadas nesta revisão, em contêiner `maven:3.9-eclipse-temurin-25`:

| Critério da task | Resultado |
| --- | --- |
| 1 — compila e o contexto sobe | `mvn -Dmaven.test.skip=true package` verde; `mvn verify` **falha** em `testCompile` (Red esperado, ver ACH-07) |
| 2 — `ddl-auto` é `validate` em todo perfil | ok — ocorrência única, no documento raiz |
| 3 — Flyway não roda na aplicação | ok — `spring.flyway.enabled: false` no documento raiz; nenhuma migração no arranque |
| 4 — liveness e readiness distintos | ok como endpoints; conteúdo do grupo de readiness é ACH-03 |
| 5 — quatro pacotes de domínio, `shared` e `config` | ok; o quinto pacote que a suíte pressupõe (`internal/impedimento`) está fora do escopo desta task |
| 6 — gate JaCoCo de 80% | ok por configuração (`check` na fase `verify`, BUNDLE/LINE 0.80); não exercitável enquanto a suíte não compilar |


## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| — | não aplicável nesta revisão parcial | — | — | — |

Nenhum RNF do PRD tem instrumentação nesta task e nenhum é mensurável antes de o
EPIC-01 fechar: RNF-002 exige três instâncias contra o mesmo banco, RNF-005 e
RNF-006 são de interface, RNF-009 exige massa de doze meses. A medição de todos
eles é obrigação do fechamento do épico, e a ausência dela **reprova o GATE-NFR**
neste momento — o gate não é atravessado por esta revisão.

> RNF não medido não passa por omissão. Se a instrumentação não existe, o
> GATE-NFR reprova — a ausência de medição é o achado.


## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | dados | `backend/src/test/`, `docs/prd/kanban-tarefas/` | A suíte congelada e os `.feature` nunca foram commitados. A Fase 0 desta skill exige conferir **no git** que nada mudou desde o congelamento, e isso é hoje impossível: não há linha de base. Enquanto durar, a restrição do `/implement` de não tocar na suíte é inverificável, e uma alteração acidental passaria sem deixar rastro | /implement |
| ACH-02 | bloqueante | segurança | `backend/src/main/resources/application.yml:8` | A senha do banco é lida de variável de ambiente (`${BANCO_SENHA}`). A coleção que governa este sistema decide o contrário: `infra/docker/architecture.md` §7 manda credencial em **arquivo montado** lido a partir do caminho, e nomeia `ENV` como proibido; `definition-of-done.md` item 6 verifica isso com `trivy --scanners secret`. A instrução da própria TASK-01.1 ("configurar por variável de ambiente: URL e credencial do banco") contradiz a norma, então o defeito nasce na task e é reproduzido pela implementação — a correção precisa dos dois lados | /implement |
| ACH-03 | relevante | código | `backend/src/main/resources/application.yml:38-45` | O grupo de readiness contém apenas `readinessState`: nenhum indicador de banco foi incluído. Com o healthcheck do compose apontando para `/actuator/health/readiness` (TASK-01.2), uma instância com o banco fora responde pronta e continua recebendo tráfego. O ponto de atenção da task diz que banco e conexão de escuta entram no readiness; só a metade que tira do liveness foi cumprida | /implement |
| ACH-04 | menor | segurança | `backend/src/main/resources/application.yml:65-69` | Usuário e senha literais (`kanban`/`kanban`) no perfil de teste, em arquivo versionado. Rebaixado a menor porque o valor é sobrescrito por `@DynamicPropertySource` e não credencia nada fora do contêiner efêmero da suíte — **justificativa aceita e aprovador a nomear no fechamento do épico**; a norma que ACH-02 cita não abre exceção por perfil, e um literal nesse lugar é o começo do hábito | /implement |
| ACH-05 | relevante | spec | `backend/pom.xml:28-102` | Não há dependência de OpenAPI/springdoc, e nenhuma das 43 tasks a menciona. O `definition-of-done.md` de `backend/java` exige `@Tag`, `@Operation`, `@ApiResponses` e `@Schema` em todo endpoint novo, e o ADR-009 desvia apenas em versão de Java, banco e plataforma — o que a coleção decide sobre desenho de código permanece valendo. Sem a dependência declarada, os 20+ endpoints do produto nascem todos fora do DoD | /techspec |
| ACH-06 | menor | código | `backend/pom.xml:147-175` | A execução `relatorio` do JaCoCo está declarada **depois** de `gate-de-cobertura` na mesma fase `verify`. Maven respeita a ordem de declaração: quando o gate reprovar, o build para antes de gerar o relatório, e quem for corrigir a cobertura fica sem o dado que diria onde ela falta | /implement |
| ACH-07 | relevante | spec | `docs/tasks/kanban-tarefas/TASK-01.1-esqueleto-backend.md:94` | A task está marcada `concluída` com o critério de aceite 1 (`mvn -q verify`) não cumprido — `verify` falha em `testCompile`, e não pode passar antes do fim do épico, porque a suíte referencia classes de produção das tasks seguintes. O critério é inatingível por task e foi escrito como se fosse por task; o histórico documenta o fato, mas a tabela de aceite continua afirmando um verificador que reprova. Reescrever o critério (ou marcá-lo como de fechamento de épico) é do `/tasks` | /tasks |
| ACH-08 | menor | dados | `backend/src/test/java/br/com/idsd/kanban/suporte/ProvedorSimulado.java` | Referencia o body file `identidade/descoberta-200.json`, e `backend/src/test/resources/` não existe. Fixture ausente na suíte congelada; afeta SCN-001.3, verificado em TASK-01.4. Território do `/tests` — o `/implement` não pode criá-la sem tocar na suíte | /tests |

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.


## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | n/a | Nenhum controller, DTO ou endpoint próprio nesta task; a superfície HTTP começa em TASK-01.4 |
| Autorização verificada por operação | n/a | Não há `SegurancaConfig` (arquivo declarado da TASK-01.4). O efeito hoje é conservador: o autoconfigure nega tudo, e os dois probes respondem `401` — quem os liberar em TASK-01.4 precisa liberar **só** eles |
| Segredo fora do código e do log | achado | ACH-02 (`application.yml:8`) e ACH-04 (`application.yml:65-69`). Nenhum `client secret` no backend, coerente com Resource Server puro; `show-details: never` no health impede vazamento de detalhe de infraestrutura pela porta aberta |
| Dado sensível fora de log e mensagem de erro | ok | Nenhum log escrito nesta task; nenhuma configuração de log que eleve nível de framework |
| Dependência nova sem vulnerabilidade conhecida | achado | 14 dependências declaradas e **nenhuma varredura SCA executada** — não há ferramenta configurada no projeto nem etapa que a rode, e o DoD de `backend/java` exige SCA sem CVE Critical/High. Registrado aqui e não como achado próprio porque a ausência de pipeline é dívida já nomeada na coleção `infra/docker` §4; passa a ser obrigação do fechamento do EPIC-01 |


## Guardrails extraídos

Regras que esta revisão descobriu e que devem valer para as próximas.

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Toda task cujo verificador só é executável ao fim do épico declara isso na coluna de verificação, em vez de nomear um comando que reprova | ACH-07 | `.agents/skills/tasks/SKILL.md` |
| Credencial de aplicação nunca chega por `ENV`, nem em perfil de teste versionado — `spring.config.import: configtree` sobre o caminho montado é a forma prevista | ACH-02, ACH-04 | `requirements/guidelines/backend/java/coding-standards.md` |
| Probe de readiness só é probe se contiver os indicadores das dependências de que o tráfego depende; grupo com `readinessState` sozinho é liveness com outro nome | ACH-03 | `requirements/guidelines/infra/docker/architecture.md` |
| Suíte congelada é commitada **antes** do primeiro `/implement`; congelamento sem linha de base em git não é congelamento | ACH-01 | `.agents/skills/tests/SKILL.md` |
| Execução de relatório de cobertura precede o gate na mesma fase, para que a reprovação venha com o dado que a explica | ACH-06 | `requirements/guidelines/backend/java/testing.md` |


## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 2 (ACH-01, ACH-02)
- **Revisor humano:** Thiago Goncalves Cavalcante — pendente de confirmação

Os dois gates são de fechamento de épico e não são atravessáveis por uma task
isolada: o veredicto acima reprova o **estado atual do EPIC-01**, não a qualidade
do que a TASK-01.1 entregou, que corresponde ao guia técnico salvo nos oito
achados registrados. O merge do épico depende de ACH-01 e ACH-02 fechados e da
medição dos envelopes de RNF.

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.


## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
