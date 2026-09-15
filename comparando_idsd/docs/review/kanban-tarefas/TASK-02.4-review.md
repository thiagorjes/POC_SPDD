# Revisão técnica — TASK-02.4

_Data: 2026-09-15 | Revisor: agente `/code-review` | Épico: EPIC-02 | PR: —_
_Commits revisados: 9e1491d..e24a667_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-02.4 |
| Cenários entregues | nenhum — task de infraestrutura |
| Arquivos | 10 de produção (6 criados, 4 alterados) |
| Suíte | 189 testes, 93 falhando (base inalterada); perna de integração não executada |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

O commit desta task não toca em `.feature` nem em arquivo sob `src/test`.
GATE-VERIFICACAO-INDEPENDENTE preservado, e a revisão prossegue.

---

## Conformidade com a especificação

A task não declara cenário congelado, e está certa em não declarar: ela cria o
caminho que os épicos seguintes percorrem. A conformidade é medida contra os
sete critérios de aceite dela.

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| Critério 1 — sequências distintas e contíguas sob concorrência | não | não | ACH-01 — o bloqueio consultivo exclusivo recusa a segunda escrita concorrente |
| Critério 2 — transação revertida não consome sequência | não | sim | não medido; o contador é coluna e o incremento reverte com a transação; correto por leitura |
| Critério 3 — log e projeção commitam juntos | não | sim | não medido; o registrador recusa rodar fora de transação e não abre uma própria |
| Critério 4 — publicação só após o commit | não | sim | não medido; `afterCommit` e não `afterCompletion` |
| Critério 5 — nenhum caminho exposto altera ou apaga evento | sim | sim | nenhum repositório publica escrita sobre o log; a entidade não tem acessor de escrita |
| Critério 6 — reconstrução reproduz o estado capturado | não | parcial | inalcançável: ACH-03 e ACH-04 na suíte, ACH-02 na spec |
| Critério 7 — escrita na janela de reconstrução recebe `409` | não | parcial | não medido; mecanismo presente, mas ACH-01 o faz disparar quando não deve |

- **Escopo além do especificado:** `TipoDeEvento` e `TipoDeIntervalo` não constam
da tabela de arquivos da task. Não é escopo a mais em sentido próprio — os dois
enums são exigidos pelo mapeamento `@Enumerated(STRING)` que a task pede —, mas
a tabela está incompleta. Já registrado pelo `/implement`; ver ACH-11.

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-002 | detecção de lacuna por `seq` sem duplicata nem buraco | — | — | não medido |
| RNF-008 | log imutável, garantido por ausência de método e por concessão restrita | — | — | não medido |

Docker está ausente da máquina e nenhum teste de integração subiu. Pela regra da
etapa, RNF não medido é achado e não "não avaliado": ver ACH-05.

Vale distinguir. RNF-008 foi medido em TASK-02.9 contra o esquema, e a perna de
banco está em vigor desde o commit anterior. O que esta task acrescenta e não
mediu é a perna de **aplicação** no caminho de escrita que ela mesma cria — que
por leitura está correta e é o critério 5.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | código | `RegistradorDeEvento.java:113` | O bloqueio consultivo tomado pelo escritor é **exclusivo**, e escritores disputam-no entre si: a segunda escrita concorrente no mesmo projeto recebe `409 reconstrucao-em-curso` sem que reconstrução alguma esteja em curso | /implement |
| ACH-02 | bloqueante | spec | `kanban-tarefas-techspec.md` §9 | A reconstrução prometida — `tarefa`, `intervalo_tarefa` e `impedimento` do zero — não é alcançável com o log como está especificado | /techspec |
| ACH-03 | bloqueante | código de teste | `suporte/Cenario.java` — `recuarInicioDoIntervalo` | Desloca só a projeção e nunca o log, de modo que o critério 6 não é satisfazível por implementação correta nenhuma | /tests |
| ACH-04 | bloqueante | código de teste | `alem/ReconstrucaoDaProjecaoIT.java:124` | `TRUNCATE intervalo_tarefa` com a FK de `impedimento` apontando para a tabela: o PostgreSQL recusa | /tests |
| ACH-05 | bloqueante | código | `TASK-02.4-nucleo-de-escrita.md` — critérios 1 a 7 | Os sete critérios de aceite sem medição, e com eles RNF-002 e a perna de aplicação de RNF-008. **Medido em 2026-09-15: a causa não é ambiente e sim a rota de TASK-02.5 — ver emenda no Veredicto.** Destino reatribuído | /tasks |
| ACH-06 | relevante | código | `RegistradorDeEvento.java:92` | `UPDATE ... RETURNING` por `createNativeQuery(...).getSingleResult()` não é uso garantido no Hibernate 6, e é o ponto de que todo o caminho de escrita depende | /implement |
| ACH-07 | relevante | código | `EventoTarefa.java:22`, `EventoTarefaRepositorio.java:26` | Afirmam que a perna de banco de RNF-008 "está escrita e não está em vigor". TASK-02.9 a pôs em vigor no commit imediatamente anterior | /implement |
| ACH-08 | relevante | código | `ReconstrutorDeProjecao.java:137` | A reescrita in-place pressupõe que a projeção existente esteja bem-formada — precisamente a hipótese que a rotina existe para dispensar | /implement |
| ACH-09 | menor | código | `RegistradorDeEvento.java:65` | O comentário justifica o `flush` por uma razão que não se sustenta: o aplicador não lê o identificador do evento | /implement |
| ACH-10 | menor | código | `EventoTarefa.java:101` | `condicaoOrigem`/`condicaoDestino` seguem `String` enquanto `tipo` virou enum na mesma task, sem razão declarada para a assimetria | /implement |
| ACH-11 | menor | spec | `TASK-02.4` — tabela de arquivos | Diz que o registrador "delega intervalos", e omite os dois enums. A suíte congelada prova que quem chama o aplicador é o serviço de domínio | /tasks |
| ACH-12 | bloqueante | segurança | `RegistradorDeEvento.java:56`, `EventoTarefa.java:164` | O caminho único de escrita não confere coerência entre `tarefaId`, `projetoId` e `atorId`: um `Novo` montado pelo construtor cheio grava no log de outro projeto e consome a sequência dele, e a autoria do único registro de auditoria é parâmetro livre nunca confrontado com o principal autenticado | /implement |
| ACH-13 | bloqueante | segurança | `RegistradorDeEvento.java:135` | Exceção no gancho `afterCommit` transforma escrita já comitada em `500`, induzindo o cliente a repetir o que já ocorreu. A proteção pertence a quem registra a sincronização, não a cada implementação da porta | /implement |
| ACH-14 | relevante | código | `ReconstrutorDeProjecao.java:186`, `:215` | Log e projeção do projeto carregados integralmente em memória, sem paginação nem streaming, dentro da transação que segura o bloqueio exclusivo: em projeto de vida longa é exaustão de heap durante a janela sem escrita | /implement |
| ACH-15 | bloqueante | segurança | `EventoTarefa.java:119`, `EventoTarefa.java:161` | `dados` é `String` livre, sem validação de JSON, sem teto e sem sanitização, num log imutável sem caminho de retificação. O rebaixamento de ACH-08 de TASK-02.3 valia por não haver escritor; **esta task é o primeiro escritor** e o argumento caducou | /implement |
| ACH-16 | bloqueante | segurança | `ReconstrutorDeProjecao.java:79` | A chave do bloqueio é o `xor` das metades do UUID, que é construtível: se algum caminho permitir influenciar o identificador de projeto, colidir com a chave de um projeto alheio é aritmética de um passo — e, somado a ACH-01, bloqueia escritas de quem o atacante não alcança | /implement |
| ACH-17 | bloqueante | segurança | `ReconstrutorDeProjecao.java:90`, `:114` | Métodos públicos que apagam e reescrevem a projeção de um projeto arbitrário, sem verificação de autorização. A defesa documentada — "quem executa tem acesso ao processo" — é a ausência de rota, que é circunstancial e não controle | /implement |
| ACH-18 | menor | código | `IntervaloTarefaRepositorio.java:70` | A coleção de identificadores vai inteira para um `in :ids`, sem lote: acima do limite de parâmetros do protocolo a remoção falha no meio de uma reconstrução que já reescreveu linhas | /implement |
| ACH-19 | menor | código | `RegistradorDeEvento.java:97` | Projeto inexistente faz `getSingleResult` lançar e sair como `500 erro-interno`, confundindo defeito com pedido sobre recurso ausente | /implement |

### ACH-01 — o detalhe, porque é o que reprova

`pg_try_advisory_xact_lock` toma o bloqueio em **modo exclusivo**. Dois backends
não o obtêm ao mesmo tempo, e é por isso que ele serve para excluir a
reconstrução. O que o arquivo não considera é que ele exclui também **um
escritor do outro**: a chave é a mesma, derivada só do projeto.

O efeito é direto em `SeqSobConcorrenciaIT.escritasSimultaneasProduzemSequenciaIntegra`
— 40 escritas em 8 threads no mesmo projeto, exigindo 40 eventos com `seq`
contígua de 1 a 40. Com este desenho, as escritas que chegarem enquanto outra
estiver em transação saem em `409` e não gravam nada. O teste falha de forma
determinística, e o critério 1 da task com ele.

A intenção do arquivo está certa e a semântica escolhida é que não a expressa: o
que o desenho pede é bloqueio **compartilhado** no escritor e exclusivo no
reconstrutor — muitos escritores convivem, e a reconstrução espera todos e os
exclui enquanto dura. Não classifico o remédio; ele pertence ao `/implement`.

Um efeito colateral vale nota: o `409` mente sobre a causa. Um operador que o
receba irá procurar uma reconstrução que não está acontecendo.

---

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | achado | ACH-12 e ACH-15 — o caminho único de escrita não valida a coerência do que recebe, nem o documento `dados` |
| Autorização verificada por operação | achado | ACH-17 — a reconstrução não verifica papel algum; a barreira é a ausência de rota |
| Segredo fora do código e do log | ok | nenhum segredo; o log da reconstrução registra identificador e contagem |
| Dado sensível fora de log e mensagem de erro | ok | a mensagem do `409` não carrega estado nem identificador |
| Consulta nativa sem injeção | ok | as três consultas nativas usam parâmetro nomeado; nenhuma concatena entrada |
| Dependência nova sem vulnerabilidade conhecida | n/a | nenhuma dependência nova |

A análise foi conduzida também pelo agente `security` sobre os arquivos em
disco, e o resultado convergiu no bloqueante por caminho independente: ele
descreve ACH-01 como negação de serviço auto-infligida no caminho quente do
board, acrescentando que qualquer participante do projeto passa a poder impedir
os demais de escrever apenas mantendo transações abertas, sem privilégio nenhum
além do que já tem, com `idle_in_transaction_session_timeout` de 30 s como único
teto. Isso agrava o achado: ele não é só quebra de critério, é superfície de
abuso por usuário legítimo.

Nenhuma injeção: as três consultas nativas usam parâmetro nomeado ligado e
nenhuma concatena entrada. O `TratadorDeErro` não devolve mensagem de exceção
imprevista ao cliente, e o broadcast carrega só identificadores, tipo, `seq` e
instante — o documento `dados` fica de fora, o que é correto tanto pelo limite
do canal quanto por não vazar conteúdo a assinantes cuja filtragem por projeto
ainda não existe.

Sobre ACH-15, o ponto merece ser dito por inteiro: o rebaixamento de ACH-08 na
revisão de TASK-02.3 apoiou-se em não haver escritor de `evento_tarefa` em
caminho nenhum, com o mecanismo previsto para nascer com o primeiro. **Esta
task é o primeiro escritor.** O prazo venceu aqui, não em EPIC-03, e o que está
em jogo não é só JSON malformado: é dado pessoal entrando num log declaradamente
imutável, sem caminho de retificação nem de eliminação.

---

## Guardrails extraídos

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Bloqueio consultivo que exclui uma classe de operação de outra exige modo compartilhado no lado numeroso; exclusivo dos dois lados serializa quem deveria conviver, e o sintoma é recusa legítima de tráfego normal | ACH-01 | `guidelines/backend/java/concorrencia.md` |
| Afirmação sobre garantia em vigor tem validade: quando outra task muda a premissa, o texto que a cita é parte do escopo dela | ACH-07 | `guidelines/_shared/documentacao-no-codigo.md` |
| Rotina de reparo não pode pressupor que o dado a reparar satisfaça a invariante que ela restaura | ACH-08 | `guidelines/_shared/rotinas-administrativas.md` |
| Comentário que declara a razão de uma chamada é verificável: se a razão citada não se sustenta, a chamada está certa por acidente | ACH-09 | `guidelines/_shared/documentacao-no-codigo.md` |

---

## Veredicto

`check_veredicto.py` sai com **8 erros, e os oito são sinal correto** e não a
pendência 20: seis cenários que não passam e dois RNF não medidos. O validador
está repetindo, em forma de erro, exatamente o veredicto que este relatório
alcança — mesmo padrão registrado na revisão de TASK-02.3.

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 10 — ACH-01 (`/implement`), ACH-02 (`/techspec`),
  ACH-03 e ACH-04 (`/tests`), ACH-05 (`/implement`, condicionado a ambiente) e
  os cinco de segurança ACH-12, ACH-13, ACH-15, ACH-16 e ACH-17 (`/implement`)

Os cinco últimos entram como bloqueantes **por regra, não por gravidade
aferida**: achado de segurança é bloqueante por padrão, e rebaixar exige
justificativa registrada e aprovador humano nomeado. O revisor não tem essa
autoridade — pela leitura técnica, ACH-12, ACH-13 e ACH-15 são relevantes e
ACH-16 e ACH-17 são menores, e é essa a proposta de rebaixamento que aguarda
decisão do demandante, no mesmo formato de ACH-08 em TASK-02.3.
- **Revisor humano:** pendente

Dos cinco bloqueantes, **um é defeito de produção**: ACH-01. Dois são da suíte
congelada e estão fora do alcance de quem implementa — foram abertos pelo
próprio `/implement` e esta revisão os confirma. Um é da spec. O quinto é
ausência de medição, e depende de o Docker voltar à máquina.

### Emenda de 2026-09-15 — a medição foi feita, e ACH-05 muda de razão

O Docker voltou (29.7.2) e a suíte rodou inteira no contêiner de ADR-012:
**190 testes, 96 verdes / 94 vermelhos**, contra a base 189 / 96 / 93. O teste a
mais é o arquivo a mais que passou a compilar; ele sai vermelho pela mesma
dependência das demais. **Zero regressão.**

**Os sete critérios seguem sem medição, e agora se sabe por quê.** As três
classes que os exercitariam — `SeqSobConcorrenciaIT`, `ReconstrucaoDaProjecaoIT`
e `ImutabilidadeDoLogIT` — morrem antes de qualquer asserção, em `404` de
`POST /v1/projetos/{id}/tarefas`, rota que nasce em TASK-02.5. A falha
`Expected size: 40 but was: 0` do critério 1 é consequência disso e não do
mecanismo: nenhuma escrita chegou a ocorrer. **ACH-05 permanece aberto, mas
deixa de ser pendência de ambiente e passa a ser dependência de task** — ele só
fecha quando a rota de criação existir, o que o põe fora do alcance de quem
implementa esta task.

**O mecanismo de ACH-01 foi medido direto contra `postgres:16-alpine`**, pelo
mesmo recurso que TASK-02.3 usou quando a suíte não alcançava o critério: duas
sessões tomam `pg_try_advisory_xact_lock_shared` na mesma chave e **ambas
recebem `true`**; a exclusiva sobre a mesma chave, com um compartilhado vivo,
recebe `false`. É exatamente a assimetria que o desenho exige — escritores
convivem entre si, nenhum convive com a reconstrução. A correção está
verificada no mecanismo; o que falta é o critério de ponta a ponta.

A hipótese de que `exigirAutorizacaoAdministrativa` (ACH-17) teria quebrado
`ReconstrucaoDaProjecaoIT` foi testada e é falsa: **zero `AccessDeniedException`
na suíte inteira**.

O trabalho de estrutura está correto e bem fundamentado: o caminho único de
escrita existe, a sequência vem do banco dentro da transação, o log não tem
caminho de alteração, a publicação sai depois do commit, e o catálogo
evento→intervalo está transcrito uma vez só e confere linha a linha com a task,
inclusive a linha que ela adverte ser a mais fácil de errar. A armadilha de
auto-invocação do proxy na rotina de reconstrução foi vista e evitada.

O que reprova é um único ponto de desenho — e é o ponto em que a task diz
"a contenção é o preço da contiguidade, e é aceita", aceitando a contenção do
`UPDATE`, não uma segunda contenção que recusa em vez de esperar.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`. O remédio de ACH-01 é
  deliberadamente não prescrito aqui.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
