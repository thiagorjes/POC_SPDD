# TASK-01.7 — Frontend: entrada autenticada, lista de projetos e novo projeto

- **Status:** concluída — os seis bloqueantes da revisão fechados e medidos, e com eles ACH-12, ACH-13 e ACH-14, **corrigidos em vez de rebaixados**: corrigir dispensa o aprovador nomeado que o rebaixamento de achado de segurança exige; critério 9 declaradamente não medido, dono TASK-02.2; ACH-16 (RNF-005 sem dono em nenhuma task) fora do alcance desta, dono `/tasks`
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 5
- **Esforço:** M
- **Depende de:** TASK-01.5, TASK-01.6, TASK-01.8
- **Cenários cobertos:** SCN-001.1, SCN-001.2
- **Origem:** RF-001, RF-002, RF-022, RN-036, RN-037, RN-038, DDR-004, DDR-005, telas TL-01, TL-02 e TL-11

#### Contexto

As duas primeiras telas do produto e o esqueleto do frontend: App Router,
cliente REST tipado, autenticação por authorization code com PKCE em client
público. Os dois cenários desta task são de ponta a ponta, então é aqui que a
fatia vertical do épico fecha.

#### O que deve ser feito

- [x] Criar o projeto Next.js com App Router e a árvore `app/`, `components/`,
      `lib/api/`. — a árvore é `src/`, e os componentes estão em
      `src/componentes/`: a suíte congelada importa daí, e conformar a tabela
      exigiria tocar teste congelado.
- [x] Adotar o design system da coleção `frontend/nextjs` como fonte de verdade
      visual — tokens de cor, tipografia e espaçamento vêm dela.
- [x] Implementar a entrada autenticada em `/entrar`, com authorization code e
      PKCE, client público, sem segredo no cliente.
- [x] Implementar o cliente REST tipado com anexo do token e tratamento de
      `problem+json`.
- [x] Implementar `/projetos` consumindo `GET /v1/projetos`, exibindo nome,
      descrição e as permissões de cada projeto. — **sem `descricao`**: ela foi
      removida da relação por ACH-08 da revisão de TASK-01.5 (TechSpec v1.9), e
      exibi-la exigiria campo que a resposta não emite.
- [x] Exibir o bloco "nenhum projeto" quando a lista vem vazia — e não uma tela
      de erro.
- [x] Exibir a marca de acesso por administração global no item cujo
      `acessoPorAdministracaoGlobal` é verdadeiro.
- [x] Tratar a falha de autenticação e a indisponibilidade do provedor sem
      oferecer caminho alternativo de entrada.
- [x] Implementar o painel de novo projeto (TL-11) sobre `/projetos`, chamando
      `POST /v1/projetos` com nome, descrição e a conta que será a primeira
      `project_admin` — os três num envio só.
- [x] Exibir a ação **Novo projeto** apenas para quem tem administração global,
      e o estado vazio "ainda não existe projeto neste sistema" com a saída para
      o painel.
- [x] No sucesso, nomear o segundo passo obrigatório e oferecer a ida à
      configuração do fluxo; ~~marcar no cartão do projeto que ele não aceita
      tarefa enquanto não houver etapa~~ — a marca **não** foi implementada:
      `fluxoConfigurado` não existe na resposta até TASK-02.2 criar a tabela
      `etapa`, e derivá-la no cliente é o que o critério 9 proíbe.
- [x] Garantir conformidade WCAG 2.1 AA nas três telas.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/package.json` | criar | Next.js App Router, React Query, cliente de mensageria para uso posterior |
| `frontend/app/entrar/page.tsx` | criar | tela TL-01 |
| `frontend/app/projetos/page.tsx` | criar | tela TL-02 |
| `frontend/lib/api/cliente.ts` | criar | cliente REST tipado, anexo do token, erro em problem+json |
| `frontend/lib/api/sessao.ts` | criar | chamada de `GET /v1/sessao` |
| `frontend/lib/api/projetos.ts` | criar | chamadas de `GET /v1/projetos` e `POST /v1/projetos` |
| `frontend/app/projetos/novo/page.tsx` | criar | tela TL-11, painel sobre `/projetos` |
| `frontend/components/` | criar | componentes compartilhados das telas |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/` (protótipo é insumo,
não destino), e todo arquivo de verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Protótipos de referência, a serem materializados em componentes reais:
`docs/design/kanban-tarefas/prototypes/TL-01-entrada-autenticada.html` e
`TL-02-meus-projetos.html` e `TL-11-novo-projeto.html`. Eles trazem os estados
que as telas precisam ter: carregando, vazio, erro de autenticação, validação e
sucesso.

Rotas do produto: `/entrar` (TL-01), `/projetos` (TL-02) e o painel de novo
projeto sobre `/projetos` (TL-11).

Corpo enviado na criação:

```
{ "nome", "descricao", "primeiroAdministradorId" }
```

Respostas: `201` com o projeto criado, `403` para quem não tem administração
global, `422` para nome ausente ou conta inexistente.

Forma da resposta consumida:

```
{ "conteudo": [ { "id", "nome", "descricao", "papeis": [], "permissoes": [] } ],
  "totalElements": 0, "totalPages": 0 }
```

`permissoes` serve **apenas** para não apresentar ação que a pessoa não pode
executar. A recusa real acontece no serviço.

#### Guia técnico — pontos de atenção

- **Lista vazia é sucesso, não erro.** Ela tem bloco próprio na tela; tratar
  `conteudo: []` como falha reprova cenário congelado.
- **Não há segredo de cliente no frontend.** Client público com PKCE.
- **Sem fallback de autenticação.** Provedor fora do ar mostra a recusa e o
  convite a tentar de novo, e não uma segunda forma de entrar.
- **Acessibilidade AA é obrigatória**, não recomendação: contraste, foco
  visível, navegação por teclado e rótulo acessível em todo controle. Nenhuma
  informação pode ser transmitida **só** por cor.
- Esconder ação na interface não é autorização; a permissão real é a do
  serviço.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Pessoa autenticada com sucesso chega à lista de projetos | percurso de ponta a ponta em navegador |
| 2 | Pessoa sem participação vê a lista vazia com o bloco próprio | percurso de ponta a ponta com usuário recém-provisionado |
| 3 | Falha de autenticação não oferece caminho alternativo | inspeção da tela de recusa |
| 4 | O item acessado por administração global é identificado como tal | marca visível no item, não só no console |
| 5 | As duas telas passam em auditoria de acessibilidade AA | verificação automatizada de acessibilidade sem violação de nível AA |
| 6 | Nenhuma informação é transmitida apenas por cor | inspeção dos estados em modo monocromático |
| 7 | A ação de criar projeto não aparece para quem não tem administração global, e a recusa continua sendo do serviço | inspeção da lista com conta comum, mais requisição direta ao serviço |
| 8 | O sucesso da criação nomeia a configuração do fluxo como segundo passo obrigatório e oferece a ida a ela | inspeção do estado de sucesso |
| 9 | O projeto sem fluxo é marcado como tal na lista, a partir de `fluxoConfigurado` da resposta e nunca de inferência do cliente | inspeção do cartão do projeto recém-criado, mais um projeto já configurado sem marca na mesma lista |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-10 | revisão | TL-11 incorporada ao escopo desta task ao fechar INC-18. Não virou task própria porque o épico já está no limite de 8 e porque a tela é painel sobre `/projetos`, que é justamente o que esta task entrega. Passa a depender de TASK-01.8, que expõe a rota |
| 2026-09-11 | Red medido | Suíte `e2e` completa: 18 testes, **18 vermelhos** — não existe frontend em disco. Jest: 2 suítes que não compilam (`CartaoDeTarefa`, `TempoPorEtapa`), ambas de EPIC-03. Os três cenários de `e2e/entrada.spec.ts` são o contrato desta task |
| 2026-09-11 | tentativa 1 | Esqueleto (Next 16 / React 19 / Tailwind), entrada por authorization code + PKCE S256 em client público, sessão em cookie `httpOnly`, cliente REST com `server-only`, TL-01, TL-02 e TL-11. **Decisão de árvore:** os componentes ficam em `src/componentes/` e não em `frontend/components/`, porque `CartaoDeTarefa.test.tsx` e `TempoPorEtapa.test.tsx`, congelados, importam daí — conformar a tabela exigiria tocar a suíte. **Decisão de arquitetura:** nenhum componente de tela alcança o gateway; toda leitura é Server Component e toda escrita é Server Action, o que também contorna o fato de o backend não declarar CORS em lugar nenhum. Resultado: SCN-001.1 verde, SCN-001.2 vermelho — a volta ao destino ia para `/entrar/iniciar?destino=%2Fv1%2Fsessao` |
| 2026-09-11 | tentativa 2 | `src/proxy.ts` carimba o caminho pedido num cabeçalho de requisição e `destinoDeRetorno()` o usa; o nome é `proxy` porque o Next 16 renomeou a convenção `middleware`. Duas falhas de ambiente apareceram e foram tratadas fora do produto: colisão de portas com outra stack chamada `idsd` na máquina (subida em 8081/8181/5433, sem parar a stack alheia, que seria ação destrutiva não autorizada) e imagem do backend anterior a TASK-01.5/01.8, reconstruída. Restava a violação de modo estrito no provedor: `getByLabel(/senha\|password/i)` casava o campo **e** o botão "Exibir senha" |
| 2026-09-11 | tentativa 3 | Tema de login `idsd` no provedor, sobrescrevendo só `showPassword`/`hidePassword` — o rótulo do botão deixa de conter "senha" e o seletor congelado passa a casar um elemento só. `loginTheme: keycloak` (v1) foi tentado antes e não resolve. **Verde: `e2e/entrada.spec.ts` 3/3.** Suíte `e2e` completa: 18 testes, 4 verdes / 14 vermelhos, e os 14 são rotas de EPIC-02 em diante — nenhuma regressão contra o Red de 18. Jest idêntico à linha de base |
| 2026-09-11 | verificação | Critérios 5, 2 e 7 medidos por execução, em `frontend/e2e/verificacoes/` — **fora** da contagem de cenários, porque não realizam cenário congelado nenhum. `acessibilidade.spec.ts` audita TL-01, a recusa de autenticação, TL-02 e TL-11 com axe nas etiquetas `wcag2a/2aa/21a/21aa`: 4/4 sem violação, depois de TL-11 trocar `opacity-40` no conteúdo de fundo por `inert` mais um escurecimento em camada separada — baixar a opacidade do texto baixa junto o contraste dele, e a auditoria reprovava com razão (1,8:1 contra os 4,5:1 do nível AA). `entrada-e-projetos.spec.ts` mede a lista vazia de quem não participa de nada e a ausência da ação de criar projeto para conta comum, **com a requisição direta ao serviço na mesma verificação**: esconder a ação não recusa nada, e é o `403` que prova a recusa. 6/6 verdes |
| 2026-09-11 | achado | **Critério 9 não é cumprível nesta task.** `GET /v1/projetos` devolve `id`, `nome`, `papeis`, `permissoes` e `acessoPorAdministracaoGlobal`; `fluxoConfigurado` nasce em TASK-02.2, junto da tabela `etapa` (ACH-03 da revisão de TASK-01.5). A marca ficou **fora** da lista em vez de derivada no cliente, que é exatamente o que o critério proíbe — e o critério 9 de TASK-02.2 já é o dono declarado |
| 2026-09-11 | achado | A instrução manda exibir `descricao` de cada projeto, e o campo **não existe** na relação: ACH-08 da revisão de TASK-01.5 o removeu de propósito, porque a relação é visível a quem o detalhe recusa e tudo o que ela carrega é, por definição, o que alguém sem `LER` pode ver. A task não foi atualizada; dono `/tasks`. O mesmo vale para o corpo de criação, que a task descreve com `descricao` |
| 2026-09-11 | achado | TL-11 pede a conta que será a primeira `project_admin` e **não há rota que liste contas** — o campo é o identificador em texto livre, e a recusa por conta inexistente sai como `422` com `errors[].campo`, que é o que o contrato congela. Rota de listagem de contas não existe em PRD, contrato nem em nenhuma das 43 tasks; dono `/prd` |
| 2026-09-11 | achado | O contêiner `e2e` do compose não alcança o provedor, então a suíte foi executada a partir do host contra a stack em pé. Dono `/tasks`/infra; não afeta o produto |
| 2026-09-11 | correção ACH-02 | Varredura de dependência do DoD executada — o item nunca tinha rodado. `npm audit --omit=dev` fechava em 1 crítica e 2 altas em produção, uma delas o desvio da verificação de CSRF de Server Action, que é o único mecanismo protegendo a criação de projeto. `next` a 16.3.4 e `postcss` a 8.5.28, ambos `--save-exact`. Medido depois: `npm audit --omit=dev` em **0 vulnerabilidades**, suíte `e2e` idêntica à linha de base (10 verdes / 14 vermelhos em 24), `tsc --noEmit` com as duas falhas pré-existentes de EPIC-03 e nada mais |
| 2026-09-11 | correção ACH-01 | Redirecionador aberto fechado, e **não** por lista de prefixos: `src/lib/auth/destino.ts` resolve o pedido contra `config.aplicacao` com `new URL` e compara `origin`, devolvendo `/projetos` a qualquer coisa que resolva para fora. O filtro anterior cobria só `//`, e `/\evil.com` começa com **uma** barra — passa em qualquer teste de prefixo e a normalização WHATWG o resolve para `http://evil.com/`. O ponto único substituiu as **três** cópias divergentes (`entrar/iniciar`, `entrar/retorno`, `cliente.ts`, esta última com só `startsWith('/')`), e a revalidação no retorno é deliberada: é ali que o redirecionamento é emitido e o valor chega por cookie, que a pessoa controla. Medido nos dois sentidos, com os payloads construídos por código de caractere para o interpretador de comandos não consumir a barra invertida: os seis hostis gravam `%2Fprojetos`, e `/projetos/abc?x=1` chega intacto |
| 2026-09-11 | correção ACH-05 | Os cabeçalhos de `frontend/nextjs` §5 em `next.config.ts`, mais `poweredByHeader: false` e `productionBrowserSourceMaps: false`. CSP em report-only como a coleção prescreve, **com `frame-ancestors 'none'` emitido também no cabeçalho aplicado** — anti-clickjacking não pode depender de um cabeçalho que por definição não bloqueia nada. Desvio declarado: sem `report-uri`/`report-to`, porque não há coletor neste sistema e apontar para um que não existe é silêncio com aparência de instrumentação. Medido por `curl -I`: os sete presentes, `X-Powered-By` ausente |
| 2026-09-11 | correção ACH-06 | Curingas do realm substituídos por caminhos exatos — `redirectUris` para `/entrar/retorno` e o pós-logout para `/entrar`. É o multiplicador clássico do redirecionador aberto de ACH-01. **Aplicado em disco e ainda não reimportado**: recriar o contêiner do provedor é ação destrutiva não autorizada, e a troca passa a valer na próxima subida limpa da stack |
| 2026-09-11 | correção ACH-04 | Toda auditoria de acessibilidade passou a ser precedida de âncora de página. Sem elas, sessão que não se forma deixava as quatro verdes medindo a tela do provedor — o arquivo vizinho já fazia a ancoragem certa no critério 2, e a assimetria mostrava que a forma era conhecida. A âncora da recusa é restrita ao `main`: o Next monta um anunciador de rota com `role="alert"` fora do conteúdo, que só existe depois de navegação client-side, de modo que a asserção solta passava isolada e falhava dentro da suíte, conforme a ordem — **defeito encontrado por execução**, não por inspeção |
| 2026-09-11 | correção ACH-03 | Critério 4 deixa de ser cumprido sem asserção alguma. Teste novo em `entrada-e-projetos.spec.ts`: `admin` cria o projeto por RN-036 e não vira participante por RN-037, então ela é a única conta que produz o estado que os cenários congelados não alcançam. Duas sessões em **contextos separados**, porque o cookie de sessão é `httpOnly` e sobrevive à navegação; nome de projeto único por execução, porque a suíte não limpa o banco e nome fixo faria a asserção medir quantas vezes ela já rodou. A contraparte — `ana`, que participa, vendo o mesmo cartão **sem** a marca — é o que dá poder de falha, e o poder de falha foi **provado**: com o emblema fixado em falso numa cópia descartável o teste fica vermelho, e verde com ele. ACH-15 fechou junto, com âncora positiva antes do `toHaveCount(0)` do critério 7 |
| 2026-09-11 | correção — medição | Suíte `e2e`: **25 testes, 11 verdes / 14 vermelhos**, contra a linha de base de 24 (10/14). O teste novo é o único delta e os 14 vermelhos são idênticos, todos rotas de EPIC-02 em diante — nenhuma regressão. Jest e `tsc --noEmit` idênticos à linha de base. Duas regressões próprias apareceram no caminho e foram fechadas, e a segunda é achado de produto: HSTS emitido em desenvolvimento derruba a aplicação inteira no navegador, porque o Chromium trata `localhost` como origem confiável e passa a forçar `https://localhost:3000`, que não existe — o cabeçalho ficou restrito a produção; e `entrar/retorno` montava a saída a partir de `nextUrl.origin`, que segue o endereço de bind (`-H 0.0.0.0` produzia `http://0.0.0.0:3000`, `ERR_ADDRESS_INVALID`) e deriva do `Host`, que quem chama controla — todas as saídas passaram a ser ancoradas em `config.aplicacao`. Lição de método: a primeira correção foi aplicada a uma causa plausível e **não verificada**, e só um script descartável de depuração revelou a verdadeira |
| 2026-09-11 | correção ACH-13 | Toda recusa de `entrar/retorno` passou a apagar a transação, e não só o caminho de sucesso. O verificador PKCE e o estado são de uso único, e deixá-los de pé por dez minutos depois de uma tentativa que falhou mantém viva exatamente a coisa que um código de autorização plantado por link precisaria encontrar — a recusa passava a ser a porta que o mecanismo existe para fechar |
| 2026-09-11 | correção ACH-12 | `secure` deixou de ser derivado de `NEXT_PUBLIC_APP_URL` — variável de configuração pública — e passa a vir da requisição servida, **falhando fechado**. A leitura anterior falhava aberta e em silêncio: por trás de terminação TLS, com a variável desajustada, o token de acesso viajava sem a marca e nada quebrava. Agora `x-forwarded-proto` decide quando existe, e na sua ausência o esquema inseguro só é aceito com host local (`localhost`, `127.0.0.1`, `[::1]`); host desconhecido em `http` é tratado como produção mal configurada. Só a porta é descascada do `Host`, por regex, porque partir em `:` quebraria `[::1]` |
| 2026-09-11 | correção ACH-14 | `/sair` passou de `GET` a `POST` com verificação de `Origin` contra `config.aplicacao`. Enquanto era navegação, qualquer site derrubava a sessão por link, porque os cookies são `sameSite: 'lax'` — e o efeito não era local, já que a saída leva o `id_token_hint` e encerra também a sessão do provedor. A verificação é do `Origin` e não do `Host`, que quem chama envia e compararia consigo mesmo; requisição sem `Origin` é recusada. O item do menu virou botão de formulário e continua `menuitem`, de modo que o teste congelado não precisou ser tocado. Um defeito de integração só apareceu por execução: o Radix fecha o menu ao selecionar, desmontando o portal — e com ele o formulário — no mesmo clique, então o envio nativo nunca saía e a sessão continuava de pé; `preventDefault` no `onSelect` resolve. Provado nos quatro sentidos por requisição direta: origem correta `303`, origem de terceiro `403`, sem origem `403`, `GET` `405` |
| 2026-09-11 | correção — medição | Suíte `e2e`: **25 testes, 11 verdes / 14 vermelhos**, idêntica à linha de base pós-ACH-06 — os 14 vermelhos são os mesmos, todos de EPIC-02 em diante. `entrada.spec.ts` isolado 3/3, o que importa porque o teste congelado de saída é o único que a correção de ACH-14 podia quebrar. Jest e `tsc --noEmit` idênticos. As auditorias de acessibilidade seguem sem violação, o que cobre a troca do link por botão no menu |
| 2026-09-11 | desvio | Arquivos fora da tabela declarada, todos deliberados: `src/proxy.ts` (destino de retorno), `src/componentes/` no lugar de `frontend/components/` (imposto pela suíte congelada), `docker/keycloak/realm.json` e `docker/keycloak/tema/idsd/**` (locale pt-BR e desambiguação do rótulo do provedor), `docker/compose.yaml` (endereços internos, `NEXT_PUBLIC_APP_URL`, tema montado, healthcheck em `/entrar` porque a raiz redireciona ao provedor), `frontend/e2e/verificacoes/**` e a dependência `@axe-core/playwright`. A correção acrescentou `src/lib/auth/destino.ts` — ponto único de validação de destino, criado porque manter a regra em três lugares foi o que produziu ACH-01 — e `next.config.ts`, que a tabela não declara e sem o qual ACH-05 não fecha. Nenhum `.feature` nem step definition tocado |
