# TASK-01.4 — Sessão autenticada, autoprovisionamento e admin global

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.3
- **Cenários cobertos:** SCN-001.3, SCN-021.1
- **Origem:** RF-001, RF-021, RN-035, ADR-003, ADR-006, ADR-010

#### Contexto

A entrada no sistema autoprovisiona a pessoa a partir do token, porque não há
cadastro local. No mesmo caminho vive a promoção do primeiro administrador
global — o mecanismo de autorização mais poderoso do sistema, e por isso o que
mais precisa de forma estrita: promoção por identificador verificado, uma única
vez, com registro auditável.

#### O que deve ser feito

- [x] Implementar `GET /v1/sessao` como Resource Server, exigindo
      `Authorization: Bearer <jwt>` emitido pelo realm confiável.
- [x] Autoprovisionar `usuario` na primeira entrada a partir do identificador de
      sujeito, do nome e do e-mail do token; espelhar nome e e-mail a cada
      entrada.
- [x] Implementar a promoção a administrador global casando o **identificador
      de sujeito** do token com a property configurada.
- [x] Recusar a promoção se já existir qualquer administrador global.
- [x] Registrar toda promoção em nível `WARN`, com quem foi promovido e quando.
- [x] Devolver `401` para token ausente, inválido ou expirado.
- [x] Devolver `503` com cabeçalho `Retry-After` quando o provedor de identidade
      está indisponível e o conjunto de chaves não pode ser validado.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/acesso/SessaoController.java` | criar | rota `GET /v1/sessao` |
| `backend/src/main/java/br/com/idsd/kanban/internal/acesso/SessaoService.java` | criar | autoprovisionamento e promoção |
| `backend/src/main/java/br/com/idsd/kanban/internal/acesso/SessaoResposta.java` | criar | registro de saída |
| `backend/src/main/java/br/com/idsd/kanban/config/SegurancaConfig.java` | criar | Resource Server, negação por padrão |
| `backend/src/main/resources/application.yml` | alterar | property do sujeito a promover |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Contrato literal da rota:

- **Entrada:** apenas o token.
- **Saída `200`:** `{ id, nome, email, adminGlobal }`.
- **`401`** token ausente, inválido ou expirado.
- **`503`** com `Retry-After` quando o provedor está indisponível. O código
  distingue indisponibilidade de credencial ruim, e **nenhum caminho
  alternativo de autenticação é oferecido** — não existe fallback local.

Erro sempre em `application/problem+json`. Rota não mapeada nega por padrão.

Promoção do administrador global:

- A property de configuração guarda o **identificador de sujeito** do token,
  nunca o e-mail.
- A promoção só ocorre se ainda não existir nenhum administrador global. A
  segunda tentativa pelo mesmo caminho não promove ninguém e não altera nada.
- O registro em `WARN` é o histórico auditável exigido, e precisa nomear quem
  foi promovido e o instante.

#### Guia técnico — pontos de atenção

- **Nunca promova por e-mail.** O e-mail é mutável no provedor e, em realm com
  autocadastro ou federação, atribuível por quem se registra: usá-lo como chave
  de promoção poria o bypass universal de autorização a uma requisição de
  distância. Foi exatamente o defeito que a decisão anterior de bootstrap
  carregava.
- **A promoção é única.** "Só ocorre se não existir nenhum administrador global"
  precisa ser verificado dentro da mesma transação da promoção, ou duas entradas
  simultâneas promovem duas pessoas.
- **Indisponibilidade do provedor não abre porta.** Devolver `200` com sessão
  degradada, ou aceitar token sem validar assinatura, contraria a decisão de não
  haver fallback.
- O autoconfigure do OAuth2 resolve o issuer de forma *eager* na subida do
  contexto: sem a ordem de subida do compose a aplicação falha no arranque com
  mensagem que não aponta para a causa.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Primeira entrada cria o usuário a partir do token | duas chamadas seguidas produzem um único registro em `usuario` |
| 2 | Nome e e-mail são espelhados a cada entrada | alterar o nome no token e reentrar atualiza a coluna |
| 3 | Provedor indisponível devolve `503` com `Retry-After` e nenhum caminho alternativo | derrubar o provedor e chamar a rota |
| 4 | A promoção casa o identificador de sujeito, e não o e-mail | token com o e-mail configurado mas sujeito diferente **não** promove |
| 5 | A promoção é única | segunda tentativa pelo mesmo caminho não promove ninguém |
| 6 | Toda promoção fica registrada em `WARN` com quem e quando | inspeção do log da execução que promoveu |
| 7 | Token ausente, inválido ou expirado devolve `401` em problem+json | três requisições, uma por caso |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-10 | Red medido | `mvn test-compile` reprova pelos mesmos cinco arquivos de tasks posteriores (tarefa, etapa, impedimento, projeção): a suíte **não compila**, e nenhum teste dos dois cenários desta task chega a executar. Terceira task seguida em que o Red é de compilação e não de asserção |
| 2026-09-10 | tentativa 1 | Cinco arquivos da tabela criados/alterados, mais três declarados fora dela (ver o achado de escopo). Quatro decisões que a task deixava à implementação: **(a)** a unicidade da promoção **não** é consulta seguida de escrita, é um `UPDATE ... WHERE NOT EXISTS (admin global)` no repositório — o ponto de atenção manda a verificação estar na mesma transação, e duas transações concorrentes que consultam antes de escrever veem as duas "nenhum admin global" e promovem duas pessoas; com o `NOT EXISTS` avaliado pelo banco no próprio comando, a segunda atualiza zero linhas, e `0` é a recusa. **(b)** A property de bootstrap nasce **vazia**, e vazia significa que ninguém é promovido — ausência de configuração jamais pode ser lida como "qualquer um serve". **(c)** A colisão da restrição única de `subject_id` em duas primeiras entradas simultâneas é capturada e resolvida relendo a linha: a colisão é o resultado correto (ela é o que garante uma linha só), o que não pode é virar erro para quem entrou. **(d)** `SessaoResposta` não tem papel nem permissão, de propósito: papel existe por projeto (BDR-001), e um campo desses aqui seria a porta pela qual o cliente montaria autorização global a partir da sessão |
| 2026-09-10 | achado — o `503` não chega pelo entry point | Medido, e não previsto: com o JWKS inalcançável o Spring levanta `AuthenticationServiceException`, que **escapa da cadeia de segurança** em vez de chegar ao `AuthenticationEntryPoint`. O contêiner despacha para o tratamento de erro e a segunda passagem chega ao entry point já anônima, com um `InsufficientAuthenticationException` que não distingue coisa alguma — a primeira versão respondia `401` para provedor fora do ar, que é exatamente o que SCN-001.3 recusa. A tradução passou a ser um filtro registrado **antes** do filtro do bearer token, que é o único ponto que o envolve. A captura é estreita por escolha: token malformado ou expirado levanta `InvalidBearerTokenException` e continua saindo em `401`, e alargar aqui transformaria credencial ruim em `503` — o erro oposto, e o mais silencioso dos dois, porque o cliente ficaria tentando de novo uma coisa que nunca vai funcionar |
| 2026-09-10 | verificação | Os sete critérios medidos por execução contra a stack real, com token de verdade por direct grant. **1:** duas chamadas seguidas de `ana` devolvem o mesmo `id` e a tabela tem uma linha. **2:** nome alterado à mão no banco volta a `Ana Ribeiro` na entrada seguinte — o espelho é reescrito, e quem está desatualizado é o banco, nunca o token. **3:** com o JWKS apontado para host inexistente, `503` com `Retry-After: 30` e `problem+json` sem campo algum de caminho alternativo. **4:** `ana` e `bruno`, com `sub` diferente do designado, não são promovidos; só o `sub` exato promove — a chave é o sujeito, e o e-mail não participa da decisão. **5:** com a property reapontada para o `sub` de `bruno` e já existindo administração global, ele entra com `adminGlobal: false` e a tabela segue com um único `t`. **6:** o `WARN` da promoção traz `usuarioId`, `sub` e `promovidoEm`. **7:** os três casos de `401` — ausente, malformado e expirado. Os probes agora respondem `200` sem token, e com isso o backend fica **`healthy`** pela primeira vez: era o critério 1 da TASK-01.2, que ficou bloqueado esperando esta task. `trivy --scanners secret` e `trivy --severity HIGH,CRITICAL` saem em exit 0 sobre a imagem reconstruída (0 e 0), que é a dupla varredura que o guardrail da revisão de TASK-01.2 instituiu. Suíte reconferida: os mesmos cinco arquivos de tasks posteriores, nenhuma regressão |
| 2026-09-10 | achado — SCN-001.3 é inalcançável pela suíte congelada | Não é questão de implementação: `ProvedorSimulado` **nunca é ligado ao contexto**. Nada registra o `issuerUri()` dele em propriedade alguma, e o perfil de teste aponta `jwk-set-uri` para `http://localhost:1/jwks` de propósito, para evitar a descoberta OIDC na subida. Pior, o cenário autentica com o pós-processador `jwt()` do `spring-security-test`, que injeta a autenticação pronta e **nunca chama o decodificador** — então derrubar o WireMock não muda nada, e a mesma classe de teste afirma `200` e `503` sem mecanismo que os separe. O comportamento existe e foi medido contra a stack real; o que não existe é o arnês que o exercite. Dono `/tests` |
| 2026-09-10 | achado — SCN-021.1 exige rota que a especificação nega | `AdminGlobalIT` consulta `GET /v1/administracao/promocoes` e afirma `subjectId` e `promovidoEm` no corpo. Essa rota **não existe em lugar nenhum da cadeia**: não está no contrato, na TechSpec, no PRD nem em nenhuma das 43 tasks, e o PRD diz literalmente que a promoção *não tem interface no produto*, com o contrato definindo o registro em `WARN` como o histórico auditável que RN-035 exige. Implementá-la seria criar produto que ninguém especificou, e por isso não foi feito: o critério 6 da task pede inspeção de log, e é o log que existe. Dono `/tests` |
| 2026-09-10 | achado — `src/test/resources` não existe | O `ProvedorSimulado` referencia o body file `identidade/descoberta-200.json` sob um diretório de recursos de teste que nunca foi criado. É o mesmo achado que a TASK-01.1 registrou e continua aberto, agora com um segundo cenário dependendo dele. Dono `/tests` |
| 2026-09-10 | achado — token válido sem `name` ou `email` | As colunas são `NOT NULL` no modelo de dados, e o contrato não diz o que fazer com um token que o realm emitiu sem essas claims. O realm importado as garante por mapper, então o caso não é alcançável no ambiente atual, e inventar aqui um valor de reserva seria decidir semântica de identidade dentro da implementação. Fica registrado sem tratativa. Dono `/techspec` |
| 2026-09-10 | achado — três arquivos fora da tabela declarada | `Usuario.java` ganhou o método que reflete a promoção já gravada no objeto em memória — não é setter livre, porque quem promove é o comando condicional do repositório. `UsuarioRepository.java` ganhou esse comando. Os dois são da TASK-01.3, do mesmo épico, e não havia como promover sem tocá-los: a tabela de arquivos desta task não previu o lado da persistência da promoção. `docker/compose.yaml` recebeu `IDSD_ADMIN_GLOBAL_SUBJECT_ID` apontando para o `sub` da conta `admin` do realm — sem ela o ambiente de desenvolvimento nasceria sem administração global, que é um estado do qual ele não sai por dentro do produto, já que criar projeto é capacidade exclusiva dela (RN-036). Dono `/tasks` |
| 2026-09-10 | `check_escopo.py` | Quinze erros, **três desta task** (`Usuario.java`, `UsuarioRepository.java`, `compose.yaml`), todos declarados no achado acima. Os doze restantes são os arquivos da TASK-01.3 e do fechamento do bloqueante dela, ainda não commitados, que o script vê como alteração fora do escopo por comparar com o commit anterior |
