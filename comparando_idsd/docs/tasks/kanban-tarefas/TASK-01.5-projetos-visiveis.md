# TASK-01.5 — Listagem e detalhe de projeto com alcance global

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.4
- **Cenários cobertos:** SCN-002.1, SCN-002.2, SCN-002.3, SCN-002.4, SCN-021.2, SCN-021.3
- **Origem:** RF-002, RF-021, RN-015, RN-035, RN-038, BDR-001

#### Contexto

A visibilidade de projeto é o primeiro ponto em que a autorização por
participação aparece na resposta, e é também onde o alcance de administração
global fica visível. O alcance é **de escopo, não de imunidade**: ele amplia o
que a pessoa enxerga, e não dispensa nenhuma das garantias estruturais.

#### O que deve ser feito

- [x] Implementar `GET /v1/projetos` devolvendo somente projetos com
      participação da pessoa.
- [x] Derivar `permissoes` dos papéis **no servidor**.
- [x] Devolver lista vazia com `200` quando não há participação alguma.
- [x] Implementar `GET /v1/projetos/{projetoId}` devolvendo `404` quando não há
      participação.
- [x] Fazer o administrador global receber todos os projetos, cada um com
      `acessoPorAdministracaoGlobal: true`.
- [x] Propagar a mesma marca no detalhe do projeto.
- [ ] Devolver `fluxoConfigurado` em cada item da lista, `false` enquanto o
      projeto não tiver etapa alguma. — **não executável nesta task:** a tabela
      `etapa` não existe no banco. Ver ACH-01 no histórico.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/projeto/ProjetoController.java` | criar | duas rotas de leitura |
| `backend/src/main/java/<pkg>/internal/projeto/ProjetoConsulta.java` | criar | projeção para registro de consulta, com join explícito |
| `backend/src/main/java/<pkg>/internal/projeto/ProjetoResumo.java` | criar | registro de saída da lista |
| `backend/src/main/java/<pkg>/internal/projeto/ProjetoDetalhe.java` | criar | registro de saída do detalhe |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`GET /v1/projetos` — saída `200`:

```
{
  "conteudo": [ { "id", "nome", "descricao", "papeis": [], "permissoes": [], "fluxoConfigurado": true } ],
  "totalElements": 0,
  "totalPages": 0
}
```

- Ausência de participação devolve `200` com `conteudo: []` — **nunca** `404`, e
  nunca a relação completa do sistema.
- `permissoes` existe para que o cliente não apresente ação que não pode
  executar. A recusa real acontece no serviço.
- Administrador global recebe todos os projetos, cada item acrescido de
  `acessoPorAdministracaoGlobal: true`. O alcance é visível na resposta, não
  implícito.
- `fluxoConfigurado` é `false` enquanto não existir etapa alguma no projeto.
  Derive por existência no banco, dentro da mesma consulta — **não** devolva a
  coleção de etapas para o cliente concluir sozinho, e **não** faça uma segunda
  ida ao banco por item.

`GET /v1/projetos/{projetoId}`:

- `404` quando não há participação. **Não** `403`: revelar a existência do
  projeto já é vazamento.
- Para administrador global, o detalhe vem acompanhado da mesma marca de acesso.

#### Guia técnico — pontos de atenção

- **`404`, não `403`, quando revelar a existência já é vazamento.** Trocar o
  código por `403` reprova cenário congelado.
- **O alcance global não é imunidade.** Ele não contorna a proibição de agregar
  tempo por pessoa — que não tem o que contornar, porque a coluna de pessoa não
  existe no esquema; não contorna a imutabilidade do log, garantida na role de
  banco; e não existe rota de alteração de tempo já contado para ele nem para
  qualquer outro perfil. As três garantias são estruturais de propósito: se
  dependessem de verificação por papel, o administrador global seria justamente
  o papel que as dispensaria. **Não acrescente exceção por papel em lugar
  nenhum.**
- **Consulta única, não navegação de associação.** Montar cada item navegando
  associação produz o problema de consultas em cascata. Declare a leitura como
  projeção para registro de consulta, com join explícito.
- Permissão é sempre resolvida sobre a participação real, nunca sobre papel
  vindo do cliente.
- **A marca de fluxo não configurado é requisito, não enfeite.** Ela existe
  porque o projeto nasce sem fluxo por decisão registrada, e o caminho de
  partida tem dois passos obrigatórios sem que nada no primeiro lembre o
  segundo. Sem essa marca, quem tem de configurar o fluxo não descobre que
  falta — descobre quando a criação de tarefa é recusada.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | A lista traz apenas projetos com participação, com papéis e permissões | pessoa participante de dois de três projetos recebe dois itens |
| 2 | Sem participação, a resposta é `200` com lista vazia | requisição de pessoa recém-provisionada |
| 3 | Projeto de terceiro devolve `404` e nenhum dado do projeto | corpo de erro sem nome, descrição ou qualquer atributo do projeto |
| 4 | Administrador global vê projeto em que não participa, com a marca de alcance | campo `acessoPorAdministracaoGlobal` verdadeiro na lista e no detalhe |
| 5 | Nenhuma rota desta task aceita recorte por pessoa | inspeção das assinaturas: não há parâmetro de pessoa |
| 6 | Nenhuma consulta desta task emite mais de uma ida ao banco por requisição | contagem de comandos executados no teste de integração |
| 7 | Projeto sem etapa alguma vem com `fluxoConfigurado` falso, e projeto com etapa vem verdadeiro | lista com os dois projetos numa mesma resposta |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-10 | emenda | SCN-002.4 e `fluxoConfigurado` acrescentados pela emenda do PRD v1.5, que fechou INC-22 instituindo a sinalização do fluxo não configurado como comportamento requerido |
| 2026-09-10 | tentativa 1 | Quatro arquivos criados conforme a tabela. A consulta sai em **uma** ida ao banco para os dois sujeitos, com os dois `left join` explícitos e a condição do usuário dentro do `ON` — levá-la ao `WHERE` transformaria o join em interno e faria a administração global enxergar apenas o que ela participa, que é o defeito silencioso desta rota. Os papéis abrem em linhas e o agrupamento é em memória: navegar a associação por item produziria a consulta por projeto que o critério 6 proíbe. Compilação verde; suíte executada por `javac` mais o console do JUnit, porque a suíte inteira segue sem compilar pelos mesmos cinco arquivos de tasks posteriores |
| 2026-09-10 | decisão | O `403` do participante sem papel e o `404` de quem não participa saem de `Acesso.participa()` e **nunca** de conjunto de permissões vazio — os dois casos produzem vazio, e derivar a distinção daí é exatamente o erro que a marca existe para evitar (TechSpec v1.8). A regra ficou escrita no cabeçalho do controlador, e o `detail` da recusa é genérico: repetir nome ou descrição na própria recusa entregaria o que ela protege (SCN-002.3) |
| 2026-09-10 | decisão | O `404` e o `403` são devolvidos como `ProblemDetail` montado no controlador, e não delegados ao tratamento padrão. `spring.mvc.problemdetails.enabled` não está ligado, e ligá-lo seria escrever em `application.yml`, que não é arquivo desta task; além disso o corpo do `/error` não é o `application/problem+json` que o contrato promete. Montar aqui mantém a resposta dentro do escopo declarado e não antecipa o tratamento global, que é de task posterior |
| 2026-09-10 | desvio de escopo | `ProjetoRepository.java` e `ResolvedorDePermissao.java` alterados fora da tabela de arquivos, deliberadamente. No repositório entraram as duas consultas — pô-las em `ProjetoConsulta` faria do registro de projeção um componente de acesso a dados, contra o que a própria tabela o nomeia. No resolvedor entrou `acessoDerivado(...)`, restrito ao pacote: a relação resolve N acessos numa requisição só, e chamar `acessoAoProjeto` por linha seria o N+1 do critério 6 — mas montar o acesso fora do resolvedor criaria a **segunda fonte** da regra dos dois sujeitos, que é o defeito que ADR-010 existe para eliminar. A javadoc que ainda dizia estar em aberto a escolha entre `403` e `404` foi conformada à TechSpec v1.8 |
| 2026-09-10 | achado | **ACH-01 — `fluxoConfigurado` não é implementável nesta task.** O campo exige existência sobre `etapa`, e a única migration em disco cria apenas `usuario`, `projeto`, `participacao` e `participacao_papel`; a tabela `etapa` nasce na migration do EPIC-02. A task declara depender só de TASK-01.4. O campo ficou **fora** da resposta em vez de fixado em `false`: constante falsa pareceria implementada e atravessaria a revisão em silêncio, enquanto o campo ausente falha alto na task de frontend que o consome. Bloqueia o critério 7. Dono `/tasks` |
| 2026-09-10 | achado | **ACH-02 — SCN-002.4 não tem teste.** `docs/tests/kanban-tarefas-verificacao.md:144` declara o cenário coberto em `internal/acesso/SessaoEProjetosIT.java`, e a varredura da árvore de teste por `SCN-002.4` devolve zero. A emenda v1.5 propagou para a tabela de cobertura sem que o teste fosse escrito. Dono `/tests` |
| 2026-09-10 | achado | **ACH-03 — a asserção de `permissoes` em SCN-002.1 é insatisfazível por qualquer resposta.** O caminho `$.conteudo[?(@.nome=='Alfa')].permissoes` é indefinido: o filtro devolve uma coleção cujo único elemento é o **array** de permissões, e `hasItem("ESCREVER_TAREFA")` compara contra esse array, não contra seus itens. Medido: `mismatches were: [was <["LER","ESCREVER_TAREFA"]>]` — a permissão exigida está lá, e a asserção reprova assim mesmo. As três asserções de `papeis[0]` passam porque `.value()` desembrulha lista de um elemento; a variante com `Matcher` não desembrulha. Nenhuma forma de saída satisfaz as três asserções ao mesmo tempo, então não é caso de ajustar a resposta. Corrigir exige tocar a suíte congelada, que a restrição desta skill proíbe. Dono `/tests` |
| 2026-09-10 | medição | 14/14 verdes em `ResolvedorDePermissaoTest`, sem regressão. Nos dois testes de integração alcançáveis: 12 testes, **7 verdes e 5 vermelhos**. Passaram a valer nesta task SCN-002.2 e SCN-002.3 (detalhe), além dos quatro já verdes de TASK-01.4. Dos 5 vermelhos, 4 param em `Cenario.fluxoPadrao`, que semeia etapas por rota de EPIC-02 — Red legítimo de task posterior — e 1 é ACH-03. O `test-compile` completo continua falhando exatamente nos mesmos cinco arquivos de tasks posteriores |
| 2026-09-10 | ressalva | Critério 6 cumprido para a consulta de projetos, que é uma ida ao banco em qualquer dos dois sujeitos. Há uma segunda consulta por requisição, constante e não por item: a resolução do usuário pelo `sub` do token, sem a qual não há `usuarioId` com que consultar. Ela é a mesma para lista e detalhe e não cresce com o número de projetos |
