# Revisão técnica — TASK-02.3

_Data: 2026-09-15 | Revisor: agente | Épico: EPIC-02 | PR: —_
_Commits revisados: be09e10..a5b2f79_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-02.3 |
| Cenários entregues | SCN-004.1 (declarado; ver ACH-04) |
| Arquivos | 13 — 4 migrations, 4 entidades, 1 enumeração, 4 repositórios |
| Suíte | 189 testes, 93 falhando |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

O commit `a5b2f79` toca seis arquivos de teste — `LimitesDaConfiguracaoDoFluxoIT`,
`SubstituicaoDeFluxoConcorrenteIT`, `TetoDeCorpoIT`, `SessaoEProjetosIT`,
`TradutorDeIntegridadeTest` e `suporte/Cenario.java`. **Nenhum deles é desta
task:** são os 18 testes que o `/tests` acrescentou em 2026-09-14 ao fechar os
achados de TASK-02.2, registrados no histórico daquela mão, e o commit os
recolheu junto por ter sido feito uma vez só. O único commit que já tocou um
`.feature` é `8346946`, o do próprio congelamento. A integridade da verificação
está preservada e a revisão prossegue.

---

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-004.1 | não | parcial | Verificado por `internal/tarefa/CriacaoDeTarefaIT.java`, que está 0/6. Falha em `404` de `POST /v1/projetos/{id}/tarefas` — rota que nasce em TASK-02.5. O esquema que o cenário pressupõe existe e foi medido; o cenário não pode passar a partir desta task. Ver ACH-04 |

As dez condições de aceite da task foram medidas e as dez estão satisfeitas: as
migrations aplicam em ordem em banco limpo; o contexto sobe com
`ddl-auto=validate`; `condicao` recusa `IMPEDIDA`; os dois únicos parciais
recusam o segundo impedimento aberto e o segundo intervalo aberto do mesmo tipo,
e os três tipos abertos convivem; `intervalo_tarefa` não tem coluna de pessoa
nem de total; não há gatilho; e a concessão restrita recusa `UPDATE`, `DELETE` e
`TRUNCATE` no log e `DELETE` em `etapa` e `raia`. A ressalva das duas últimas é
o objeto de ACH-01.

A lista obrigatória de índices de `data-model.md` §6 está implementada por
inteiro. Os cinco índices de chave estrangeira criados além dela —
`tarefa_etapa`, `tarefa_raia`, `impedimento_intervalo`,
`impedimento_aberto_por_pessoa`, `impedimento_resolvido_por` — não são escopo a
mais: `database.md` §4 manda indexar toda chave estrangeira, e a própria §6
abre dizendo isso.

- **Escopo além do especificado:** nenhum de comportamento. Dois de arquivo —
  os quatro repositórios e a enumeração `Condicao`, criados fora da tabela de
  arquivos da task. Ver ACH-05.

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-008 | Nenhuma operação do produto altera ou remove evento já registrado | Recusa por todos os caminhos do produto; recusa no banco **apenas** com role fabricada para a medição | Inspeção dos caminhos expostos (repositório sem método, entidade sem acessor de escrita) e tentativa real de `UPDATE`/`DELETE`/`TRUNCATE` por role de login membro de `aplicacao_kanban` em `postgres:16-alpine` | ~~fora (ACH-01)~~ → **dentro**, desde TASK-02.9 |

Os demais envelopes do PRD não são desta task e só são mensuráveis no
fechamento do épico, que continua aberto. O GATE-NFR permanece reprovado pelas
razões já registradas, mas **não mais por RNF-008**: ele foi medido fora nesta
revisão, e TASK-02.9 o trouxe para dentro no mesmo dia. A role de login
`kanban_app` deixou de ser fabricada para a medição e passou a ser a credencial
com que a aplicação conecta — `usesuper = f`, sem posse de objeto —, de modo que
as três recusas do log valem para o produto e não só para o experimento.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | segurança | `backend/src/main/resources/db/migration/V2026091414__evento_tarefa.sql:104` | RNF-008 é garantido de um lado só. A concessão restrita está correta e foi medida — com uma role de login membro de `aplicacao_kanban`, `UPDATE`, `DELETE` e `TRUNCATE` do log e `DELETE` de `etapa`/`raia` saem em *permission denied* —, mas essa role não existe no arranjo real: a aplicação conecta com `BANCO_USUARIO`, que no compose e no Testcontainers é o `POSTGRES_USER` da imagem, superusuário e dono do schema. Superusuário ignora privilégio e dono reconcede a si mesmo, de modo que toda revogação é inerte. `contracts/sessao-e-projetos.md:118` afirma que RNF-008 é "garantido na role de banco" — a garantia que o contrato invoca é exatamente a que não existe. Criar a role de login, montar seu segredo e apontar `BANCO_USUARIO` para ela é mudança de `docker/compose.yaml` e do arranjo de segredos, **fora do escopo de arquivo desta task**: o achado não é corrigível de dentro dela. Corresponde à pendência 21, aberta pela própria execução | /tasks |
| ACH-02 | relevante | código | `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/EventoTarefaRepositorio.java:20` | A garantia é descrita no presente em três lugares onde uma das pernas não vale. O javadoc do repositório diz "a garantia é dupla ... a role de aplicação recebe apenas `SELECT, INSERT`"; o de `EventoTarefa.java:18` diz "a garantia é tripla"; e o `COMMENT ON TABLE evento_tarefa` da migration repete a afirmação. O comentário final da migration **é** honesto e registra que as revogações ainda não surtem efeito — mas ele é comentário de arquivo `.sql` e não viaja para o catálogo: quem lê a tabela por `\d+` recebe só a versão que afirma a garantia. Enquanto ACH-01 não fechar, os três textos induzem o próximo leitor a confiar numa proteção que não está ligada | /implement |
| ACH-03 | relevante | spec | `docs/techspec/kanban-tarefas/data-model.md:§5 impedimento` | `impedimento` não tem coluna de bloqueio otimista, e a escrita que RN-010 define é leitura-modificação-escrita sobre documento JSON: a segunda sinalização anexa em `anotacoes` sem criar impedimento novo (SCN-009.3). Duas sinalizações concorrentes sobre a mesma tarefa leem o mesmo array e a última grava por cima — uma anotação some sem erro e sem rastro, que é precisamente o dano que SDR-002 existe para impedir e que `Tarefa` evita com `@Version`. O índice único parcial protege contra dois impedimentos **abertos**, não contra duas escritas no mesmo. A implementação segue a spec: a tabela de campos de §5 não declara a coluna, então o defeito é da decisão e não de quem a cumpriu | /techspec |
| ACH-04 | relevante | spec | `docs/tasks/kanban-tarefas/TASK-02.3-migrations-verdade-e-projecao.md:8` | A task declara "Cenários cobertos: SCN-004.1", e SCN-004.1 é verificado por `CriacaoDeTarefaIT`, que precisa de `POST /v1/projetos/{id}/tarefas` — rota de TASK-02.5. A classe está 0/6. Uma task de esquema não entrega cenário: ela remove o impedimento para que outra o entregue. Enquanto a coluna existir como está, a invariante cenário↔épico atribui a SCN-004.1 uma task que estruturalmente não pode fazê-lo passar, e o `/code-review` de EPIC-02 vai reencontrar a mesma linha | /tasks |
| ACH-05 | menor | spec | `docs/tasks/kanban-tarefas/TASK-02.3-migrations-verdade-e-projecao.md:38` | A tabela de arquivos declara as quatro entidades e nada mais, enquanto o item de execução pede "as entidades JPA e os repositórios correspondentes". Os quatro repositórios e a enumeração `Condicao` foram criados sem estar declarados, e `check_escopo.py` os acusa. A divergência é da task consigo mesma — o texto pede o que a tabela não lista —, e o efeito é que o validador de escopo perde poder de sinal justamente onde ele seria útil | /tasks |
| ACH-06 | menor | spec | `docs/techspec/kanban-tarefas/data-model.md:§5 intervalo_tarefa` | Dentro do mesmo anel de projeção, três referências são chave estrangeira e quatro não: `impedimento.intervalo_id`, `aberto_por` e `resolvido_por` são FK; `impedimento.tarefa_id` e os três `tarefa_id`/`projeto_id`/`etapa_id` de `intervalo_tarefa` não são. A implementação reproduz a spec campo a campo, e a spec nunca justifica a assimetria — ao contrário de `evento_tarefa`, cuja ausência de FK é explicada pelo log sobreviver à projeção, o que não vale para a projeção referenciando a si mesma. O efeito é que linha de série de tempo e impedimento podem sobreviver órfãos à tarefa, e a rotina de reconstrução de §9 fica sem garantia de ordem no nível do banco | /techspec |
| ACH-07 | menor | dados | `docs/techspec/kanban-tarefas/data-model.md:§6` | O índice da fila de RF-014 é prescrito como `(condicao, projeto_id)` parcial em `condicao = 'AGUARDANDO_TOMADA'`. Dentro de um índice parcial cujo predicado fixa `condicao`, a coluna-chave `condicao` é constante em toda tupla: ela nunca discrimina, ocupa espaço em cada entrada e empurra `projeto_id` para a segunda posição. `(projeto_id)` parcial serviria a mesma consulta com índice menor. A implementação seguiu a lista obrigatória, então o ajuste é na lista | /techspec |
| ACH-08 | menor | segurança | `backend/src/main/resources/db/migration/V2026091414__evento_tarefa.sql:38` | `evento_tarefa.dados` é documentado como "**Nunca** dado de cliente (IDSD 4.10.1)" na migration e no javadoc de `EventoTarefa`, e nada o impõe: a coluna é `jsonb` livre e o campo é `String` livre. Hoje não há defeito, porque nenhum caminho grava evento; o risco nasce com o primeiro escritor, em EPIC-03, e é lá que a regra precisa virar mecanismo em vez de continuar comentário. Registrado também como guardrail. **Rebaixado de bloqueante a menor** — achado de segurança nasce bloqueante — pela justificativa de que não há escritor de `evento_tarefa` em nenhum caminho do sistema hoje, de modo que não existe superfície por onde dado de cliente entre na coluna; o risco nasce com o primeiro escritor, em EPIC-03. **Aprovador humano: Thiago Goncalves Cavalcante, 2026-09-15** | /implement |

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.

---

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | ok | A task não expõe fronteira. O domínio de `condicao` é fechado nas duas camadas que RN-003 pede — enumeração `Condicao` sem `IMPEDIDA` e restrição `tarefa_condicao_valida` —, e a recusa foi medida por `UPDATE` direto |
| Autorização verificada por operação | n/a | Nenhuma rota nesta task. A autorização de banco é o objeto de ACH-01 |
| Segredo fora do código e do log | ok | Nenhum segredo nos treze arquivos. A role `aplicacao_kanban` é `NOLOGIN` e sem senha por decisão registrada na própria migration; o segredo da role de login pertence à mudança que ACH-01 devolve a `/tasks` |
| Dado sensível fora de log e mensagem de erro | achado | ACH-08 — a regra que mantém dado de cliente fora de `evento_tarefa.dados` existe como comentário e não como mecanismo |
| Dependência nova sem vulnerabilidade conhecida | n/a | Nenhuma dependência acrescentada |

Ponto estrutural, além da tabela: a imutabilidade do log é o ativo de segurança
desta task, e ela está garantida por uma perna só. Está em ACH-01.

---

## Guardrails extraídos

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Privilégio de banco revogado só é garantia se a aplicação conectar como role **sem** `SUPERUSER` e que **não** seja dona do objeto. Migration que escreve `REVOKE` deve declarar essa pré-condição, e a task que a escreve deve verificar se ela vale no arranjo de execução — senão entrega proteção inerte com aparência de proteção | ACH-01 | `guidelines/backend/java/database.md` |
| Garantia que depende de configuração externa ao arquivo não se afirma no presente dentro dele. Ou o texto nomeia a pré-condição, ou afirma o que o arquivo sozinho garante. Vale em especial para `COMMENT ON`, que é lido pelo catálogo sem o comentário de código ao redor | ACH-02 | `guidelines/_shared/documentacao-de-codigo.md` |
| Campo `jsonb` sobre o qual se afirma uma restrição de conteúdo nasce com o mecanismo que a impõe, ou a afirmação não é escrita. Comentário não é validação | ACH-08 | `guidelines/backend/java/database.md` |
| Coluna-chave fixada pelo predicado de um índice parcial é peso morto: ela não discrimina e não deve entrar na lista de chaves | ACH-07 | `guidelines/backend/java/database.md` |
| Task que só cria esquema não declara cenário coberto. O cenário pertence à task que entrega o caminho que o exercita | ACH-04 | `.agents/skills/tasks/SKILL.md` |

---

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 0 — o único, ACH-01, foi fechado por TASK-02.9
- **Revisor humano:** Thiago Goncalves Cavalcante — 2026-09-15 (aprovou o rebaixamento de ACH-08; decidiu ACH-01 pela role de aplicação com credencial separada para o arnês)

O trabalho de esquema desta task está correto e, pela primeira vez no épico,
medido: dez critérios de aceite medidos e dez satisfeitos, `ddl-auto=validate`
verde sobre os dois `jsonb`, o `IDENTITY` e a coluna gerada, e nenhuma
regressão — 189 testes, 96/93 contra a base 189/91/98.

O que reprovou não foi a implementação. Foi que o único envelope de RNF que esta
task tornava mensurável saiu **fora**, e o achado que o produz não era corrigível
de dentro do escopo de arquivo dela: ACH-01 teve destino `/tasks` e virou
TASK-02.9, executada e medida no mesmo dia. Com ela o envelope entrou, e a
contradição da suíte congelada (pendência 22) foi desatada pela separação de
papéis — o arnês limpa como dono, a aplicação exerce o produto pela role
restrita —, sem nenhuma classe vermelha no `@BeforeEach`.

**Os dois gates seguem reprovados, e agora por um motivo só:** o épico está
aberto. Nenhum achado desta revisão permanece.

GATE-NFR também reprova pela razão que já o reprovava antes: os envelopes só
são mensuráveis no fechamento do épico, e EPIC-02 continua aberto.

---

## Fechamento — 2026-09-15

Sete dos oito achados fechados no mesmo dia, pelos donos que a tabela de
Achados indicou. Medição no contêiner de ADR-012: **189 testes, 96 verdes / 93
vermelhos**, idêntico à medição de antes das correções — zero regressão, e o
contexto Spring sobe com `ddl-auto=validate` sobre o esquema já corrigido, o
que é a única prova de que a coluna `versao` e as duas chaves estrangeiras
novas batem com o mapeamento.

- **ACH-02 — fechado, `/implement`.** Os três textos deixam de afirmar no
  presente a perna que não está em vigor, e a ressalva passa a viajar para o
  catálogo: `COMMENT ON TABLE evento_tarefa` é reescrito em migration nova.
- **ACH-03 — fechado, `/techspec` + `/implement`.** TechSpec v1.15 decide a
  coluna; `impedimento.versao` e `@Version` entram. O que faltava não era
  disciplina de serviço: era o mecanismo que torna a perda de anotação
  impossível.
- **ACH-04 — fechado, `/tasks`.** A task deixa de declarar cenário coberto. A
  invariante cenário↔épico continua fechada: SCN-004.1 já era declarado por
  TASK-02.4, 02.5 e 02.8.
- **ACH-05 — fechado, `/tasks`.** Os quatro repositórios e `Condicao` entram na
  tabela de arquivos, e `check_escopo.py` volta a ter poder de sinal.
- **ACH-06 — fechado, `/techspec` + `/implement`.** Regra única e escrita:
  referência dentro do anel de projeção é FK, referência para fora não é,
  porque é instantâneo histórico.
- **ACH-07 — fechado, `/techspec` + `/implement`.** O índice da fila perde a
  coluna-chave que o próprio predicado já fixava.
- **ACH-08 — rebaixado a menor e aceito**, com aprovador humano nomeado. Sem
  mecanismo hoje; o guardrail fica, e o mecanismo nasce com o primeiro escritor
  de `evento_tarefa`, em EPIC-03.
- **ACH-01 — fechado por TASK-02.9**, fora desta task, como o destino previa. O
  demandante escolheu criar a role de aplicação com credencial separada para o
  arnês de teste, o que fecha as pendências 21 e 22 no mesmo passo e dispensa
  emendar `contracts/sessao-e-projetos.md:118` — a promessa que o contrato fazia
  passa a ser verdadeira em vez de ser retirada. A aplicação conecta como
  `kanban_app`, que não é superusuária nem dona do schema; o arnês limpa o banco
  com a credencial do dono, porque preparar o ambiente e exercer o produto são
  papéis diferentes. A senha nunca entra em SQL versionado: é atribuída por
  callback `afterMigrate` com placeholder do Flyway, alimentado pelo segredo
  montado em produção e pela constante do arnês no teste.

Arquivo novo: `db/migration/V2026091517__correcoes_do_anel_de_projecao.sql` —
as migrations de ordem 3, 4 e 5 já estavam aplicadas e não se altera migration
aplicada.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
