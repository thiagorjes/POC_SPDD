# Revisão técnica — TASK-01.7 (revisão parcial)
_Data: 2026-09-11 | Revisor: agente `/code-review` | Épico: EPIC-01 | PR: não aberto_
_Commits revisados: 76020f4..1c657e2_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

Revisão **parcial**: uma task, não o épico. O veredicto não fecha o EPIC-01 e não
substitui a revisão de fechamento, que é a única que pode medir os envelopes que
só existem com o épico inteiro em pé.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-01.7 |
| Cenários entregues | SCN-001.1, SCN-001.2 |
| Arquivos | 41 (34 de produção em `frontend/src`, 3 de suíte, 4 de infraestrutura) |
| Suíte | `e2e/entrada.spec.ts` 3/3 verdes; suíte `e2e` completa 18 testes, 4 verdes / 14 vermelhos (rotas de EPIC-02 em diante); Jest idêntico à linha de base |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

Fase 0 com linha de base real pela sexta vez seguida: `git diff 8346946..HEAD`
sobre `**/*.feature` volta vazio, e a árvore de teste do frontend recebeu apenas
acréscimo — dois arquivos novos em `e2e/verificacoes/`, que a task declara como
verificações **fora** da contagem de cenários.

---

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-001.1 | sim | sim | Entrada pelo provedor, sem campo de senha na tela do produto; medido contra a stack de pé |
| SCN-001.2 | sim | sim | Ausência de caminho alternativo de entrada; ADR-006 respeitado nos quatro estados de recusa |

Os nove critérios de aceite foram confrontados um a um com asserção executável.
Sete têm; o critério 4 não tem nenhuma (ACH-03) e o critério 5 é medido por teste
sem poder de falha (ACH-04). O critério 9 está declaradamente não medido, com
dono nomeado em TASK-02.2 — declaração correta, e não achado.

- **Escopo além do especificado:** dois itens. O tema escuro completo em
  `frontend/src/app/globals.css` não é pedido por critério, requisito ou
  protótipo desta task e não é alcançável por nenhum caminho do produto
  (ACH-11). E a tabela de arquivos da task prescreve cliente de dados e cliente
  de mensageria que não foram criados — ausência, não excesso, mas igualmente
  fora do declarado e ausente da lista de desvios (ACH-21).

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-004 | 100% das escritas reavaliadas no servidor | 100% da única escrita alcançável | `e2e/verificacoes/entrada-e-projetos.spec.ts` — requisição direta ao serviço com token sem alcance, recusada com `403` | dentro |
| RNF-005 | Funcional de 1280 px a 1024 px, sem perda de ação nem rolagem horizontal | — | nenhum instrumento existe | não medido |
| RNF-006 | Zero violação A/AA nas 11 telas, temas claro e escuro | 0 violação em 4 auditorias sobre 3 telas, tema claro | `@axe-core/playwright`, tags `wcag2a wcag2aa wcag21a wcag21aa` | não medido |
| RNF-001 | p95 ≤ 2 s de propagação | — | exige broadcast, que é EPIC-07 | não medido |
| RNF-002 | 3 instâncias, 300 sessões, sem divergência | — | exige o épico fechado | não medido |
| RNF-003 | Subida a partir da imagem, percurso TL-03 → TL-04 → TL-06 | — | as três telas nascem em EPIC-02 e EPIC-03 | não medido |
| RNF-007 | Quatro séries de métrica | — | EPIC-06 | não medido |
| RNF-008 | Histórico imutável | — | EPIC-03 em diante | não medido |
| RNF-009 | p95 ≤ 2 s das consultas de andamento | — | RF-015 e RF-016 são EPIC-06 | não medido |
| RNF-010 | 120 leituras e 30 escritas por minuto por sujeito | dentro, medido em TASK-01.6 | herdado; nada nesta task o altera | dentro |

RNF-005 e RNF-006 são os dois envelopes que **esta** task deveria começar a
medir, porque são os únicos cujo universo de medição é composto de telas e três
delas passaram a existir aqui. RNF-006 foi medido em parte e a parte medida está
limpa; a metade do tema escuro é hoje inauditável por não ser alcançável.
RNF-005 não tem instrumento em lugar nenhum da cadeia — nem task, nem critério,
nem linha no plano de verificação (ACH-16). O GATE-NFR reprova por isso e não
apenas por diferimento.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | segurança | `frontend/src/app/entrar/iniciar/route.ts:20` | Redirecionador aberto pós-autenticação. O filtro do destino aceita prefixo que a normalização WHATWG converte em autoridade externa, e o valor é consumido em `entrar/retorno/route.ts:42` na resolução contra a origem. Confirmado por execução com o Node do projeto: quatro payloads atravessam o filtro e resolvem para host de terceiro; `//` é corretamente barrado e é a única forma que o filtro cobre. O comentário das linhas 18-19 institui que o mecanismo impede o redirecionador aberto — quarta ocorrência no épico da classe "mecanismo documentado não é o mecanismo implementado", no arquivo que decide destino. Não vaza o código de autorização, porque a troca é server-side e o cabeçalho de referência padrão só envia a origem; o dano é phishing com barra de endereços confiável até o último salto | /implement |
| ACH-02 | bloqueante | segurança | `frontend/package.json:20` | Item de varredura de dependência do DoD nunca executado nesta task. Medido aqui: `npm audit --omit=dev` fecha em 1 crítica e 2 altas, todas em dependência de produção. A crítica é execução remota de código no protocolo flight do React, corrigida em versão posterior à que está em disco. Entre as altas, uma permite que origem nula burle a verificação de CSRF de Server Action — e a criação de projeto **é** Server Action, cuja única proteção é essa verificação embutida. Correção disponível sem mudança de major. Mesma classe de ACH-01 e ACH-03 da revisão de TASK-01.2 | /implement |
| ACH-03 | bloqueante | código | `frontend/e2e/` (ausência) | Critério de aceite 4 marcado cumprido sem verificação executável. A varredura da suíte `e2e` inteira não devolve nenhuma asserção sobre a marca de alcance por administração global. Não é só ausência de teste: a marca é estruturalmente inalcançável pela suíte, porque a única execução que carrega a lista de projetos entra com conta que participa dos projetos, e o emblema só é renderizado no caminho oposto. É a realização visual de RN-035 e SCN-021.2 — a distinção entre alcance e participação —, que a revisão de TASK-01.5 já reprovou uma vez pelo lado do serviço. Resolvível de dentro da task | /implement |
| ACH-04 | bloqueante | código | `frontend/e2e/verificacoes/acessibilidade.spec.ts:40` | Critério de aceite 5 medido por teste sem poder de falha. As duas auditorias que medem o produto navegam e analisam sem nenhuma asserção que fixe qual página foi auditada. Duas regressões plausíveis deixam o teste verde medindo outra coisa: sessão que não se forma faz auditar a tela do provedor, e regressão na promoção do administrador global faz auditar a página de não encontrado. O arquivo vizinho, no critério 2, faz exatamente a ancoragem que falta aqui — a assimetria mostra que a forma certa era conhecida. Terceira ocorrência da classe que bloqueou TASK-01.6 e TASK-01.8 | /implement |
| ACH-05 | bloqueante | segurança | `frontend/next.config.ts` | Nenhum dos cabeçalhos de segurança que `frontend/nextjs/security.md` §5 institui como obrigatórios está presente: política de conteúdo, proteção contra enquadramento, transporte estrito, ausência de adivinhação de tipo, política de referência, e a supressão do cabeçalho de identificação do framework. Não há desvio declarado. Pesa por ser a **primeira** task de frontend do produto: o arquivo é o que as próximas vão copiar, e a omissão se propaga sem que nada quebre | /implement |
| ACH-06 | bloqueante | segurança | `docker/keycloak/realm.json:29` | Curinga de caminho no endereço de retorno registrado do cliente da interface, quando o endereço real é um só. Curinga permite que o código de autorização seja entregue em qualquer rota da aplicação, e é o multiplicador clássico de um redirecionador aberto — que ACH-01 confirma existir. Fixar o endereço exato remove a classe inteira. Registrado junto, no mesmo arquivo: exigência de transporte seguro desativada no realm inteiro (linha 4) e concessão direta por senha habilitada no cliente de suporte da suíte (linha 41), que alcança inclusive a conta promovida à administração global — aceitável enquanto o arquivo for exclusivo de desenvolvimento, e nada no arquivo garante que seja | /implement |
| ACH-07 | relevante | código | `frontend/src/componentes/formulario-de-novo-projeto.tsx:76` | O estado de sucesso afirma ao usuário que a pendência de configuração do fluxo fica marcada no cartão do projeto na lista. A marca não chega: o campo que a condiciona não é emitido pelo serviço, e o critério 9 declara isso. Declarar o critério não medido está certo; o que não está é a tela prometer a segunda sinalização de RN-038, que a emenda de 2026-09-10 instituiu como obrigatória junto com a primeira. Quem escolhe adiar confia num lembrete que não existe | /implement |
| ACH-08 | relevante | código | `frontend/src/app/projetos/novo/acoes.ts:51` | O mapeamento de recusa por campo não cobre a recusa mais provável. Verificado no serviço: a recusa de conta inexistente sai sem a relação de campos, só com o detalhe, e o mesmo vale para nome em branco — apenas identificador não conversível produz a relação. Consequência: o caso que a tela mais vai produzir, já que o campo é texto livre por não haver rota que liste contas, cai no alerta genérico do topo em vez de ao lado do controle, contra o que o mapa de telas prescreve. Agravante de cobertura: nenhum teste, de nenhuma ferramenta, submete o formulário — a Server Action inteira está sem cobertura | /implement |
| ACH-09 | relevante | código | `frontend/src/app/projetos/novo/page.tsx:29` | O painel de novo projeto marca o conteúdo principal como inerte, mas a barra superior fica fora dele e continua focável, enquanto o diálogo se declara modal — tecnologia assistiva ignora o que o teclado ainda alcança, e a camada de escurecimento cobre visualmente o controle que recebe o foco. Some-se que o atalho de salto do leiaute aponta para a região inerte, virando atalho para lugar nenhum enquanto o painel está aberto, e que o diálogo não recebe foco na abertura nem fecha por tecla de escape. A auditoria automatizada não detecta nada disso, e é por isso que o critério 5 verde não cobre este achado | /implement |
| ACH-10 | relevante | código | `frontend/src/app/` (ausência) | Nenhum arquivo de estado de carregamento, de erro ou de não encontrado em toda a árvore de rotas, contra o mapa de telas, que marca carregamento como estado obrigatório de duas telas e erro como estado de uma delas. Efeitos verificáveis: a lista de projetos aguarda duas chamadas sem interface de pendência; qualquer falha do serviço — inclusive a recusa por limite de requisições que TASK-01.6 acabou de instituir — sobe sem limite de erro e produz a página genérica do framework; e a recusa de navegação do painel entrega a página padrão, em outro idioma, num documento declarado em português | /implement |
| ACH-11 | relevante | código | `frontend/src/app/globals.css:45` | Tema escuro completo em tokens, com o modo declarado na configuração, e nenhuma escrita do atributo que o alcança em lugar nenhum do código — é código morto. Duas consequências: RNF-006 exige contraste conforme nos dois temas, e essa metade é hoje inauditável; e os valores, se um dia forem alcançados, já reprovam, porque a cor primária permanece a do tema claro sobre fundo escuro, abaixo do mínimo exigido tanto para o botão quanto para o contorno de foco, que reusa a mesma variável | /implement |
| ACH-12 | relevante | segurança | `frontend/src/lib/auth/cookies.ts:21` | A marca de transporte seguro do cookie de sessão é derivada do esquema de uma variável pública de configuração, e não do esquema real da requisição. A justificativa para desenvolvimento local é legítima; o acoplamento falha na direção perigosa: atrás de terminação de TLS, variável deixada no esquema inseguro — que é o valor padrão do próprio arquivo de configuração e o literal do compose — faz o token trafegar sem a marca, em silêncio, sem que nada no arranque denuncie. Falha aberta. Rebaixado de bloqueante por não ser alcançável na topologia atual; **o rebaixamento aguarda aprovador humano nomeado** | /implement |
| ACH-13 | relevante | segurança | `frontend/src/app/entrar/retorno/route.ts:14` | Os cookies de transação sobrevivem a todo caminho de falha. A limpeza só roda no caminho de sucesso, e nenhuma das quatro saídas de recusa a invoca — verificador, estado e destino ficam gravados pelo tempo restante da janela. O estado deixa de ser efetivamente de uso único dentro dela, e o destino envenenado de ACH-01 persiste para uma tentativa seguinte. Isolado é pequeno; agrava ACH-01. **Rebaixamento aguarda aprovador humano nomeado** | /implement |
| ACH-14 | relevante | segurança | `frontend/src/app/sair/route.ts:15` | Encerramento de sessão por verbo de navegação, sem token de verificação, com cookies de política branda que são enviados em navegação de topo. Qualquer site consegue disparar o encerramento por link ou janela, e a rota não apenas apaga a sessão local: encerra a sessão no provedor com dica de identidade, de modo que o efeito é completo e não local. O dano é assédio e indisponibilidade, não perda de dado. **Rebaixamento aguarda aprovador humano nomeado** | /implement |
| ACH-15 | relevante | código | `frontend/e2e/verificacoes/entrada-e-projetos.spec.ts:42` | A metade de interface do critério 7 afirma a ausência da ação sem antes ancorar que a página é a lista: sessão que não se forma deixa a asserção verde contra a tela do provedor. É o ACH-04 da revisão de TASK-01.5 repetido no frontend — asserção negativa sobre coleção vazia é verde que não verifica nada. A outra metade, a recusa do serviço à requisição direta, tem poder de falha real e é o que salva o critério | /implement |
| ACH-16 | relevante | spec | `docs/tests/kanban-tarefas-verificacao.md` | RNF-005 não é nomeado em nenhum dos 43 arquivos de task, em nenhum critério de aceite e em nenhuma linha do plano de verificação — varredura devolve zero nas três fontes. O universo de medição do envelope é composto de telas, três delas nasceram aqui, e não há dono declarado para medi-lo em momento nenhum da cadeia. Sem isso o envelope reprova por construção no fechamento do épico, e a causa estará a 43 arquivos de distância | /tasks |
| ACH-17 | relevante | código | `frontend/package.json` | O verificador de lint declarado no DoD da coleção é inexecutável: o comando que o roteiro invoca foi removido na major do framework adotada aqui. O item do DoD não pode ser cumprido nem reprovado — some do sinal em vez de falhar. Ausentes também os dois verificadores que o mesmo DoD exige para formatação e para código não referenciado | /implement |
| ACH-18 | menor | código | `frontend/src/lib/api/cliente.ts:76` | Desserialização do corpo sem guarda: resposta que não é JSON — de proxy, de gateway, de erro de infraestrutura — levanta erro de sintaxe em vez da falha tipada que o restante do caminho trata, e a Server Action o repassa adiante. Sem o limite de erro de ACH-10, termina na página genérica | /implement |
| ACH-19 | menor | código | `frontend/src/app/entrar/page.tsx:45` | Consulta ao catálogo de razões por índice em objeto literal, sem restringir a propriedades próprias: parâmetro que nomeie membro herdado do protótipo escapa do valor padrão e renderiza um alerta sem título. Não é injeção — o framework escapa e o valor bruto não chega ao documento —, é robustez | /implement |
| ACH-20 | menor | código | `frontend/src/componentes/cartao-de-projeto.tsx:37` | RF-002 fala em qual permissão a pessoa tem no projeto, e o cartão exibe papéis; o campo de permissões da resposta não é consumido em ponto nenhum do código, só declarado no tipo. Divergência pequena com o protótipo, que rotula a participação pela capacidade. Registradas na mesma linha duas dependências declaradas e não usadas em arquivo nenhum | /implement |
| ACH-21 | menor | código | `docs/tasks/kanban-tarefas/TASK-01.7-frontend-entrada-e-projetos.md` | A tabela de arquivos da task prescreve cliente de dados e cliente de mensageria que não existem em disco. A ausência é provavelmente correta — a arquitetura escolhida não alcança o gateway do navegador, e nada nesta task consome mensageria —, mas ela não está entre os desvios declarados, que registram apenas a divergência de árvore imposta pela suíte. Desvio não declarado é indistinguível de esquecimento na próxima leitura | /implement |

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.

Nota de método: o `qa` afirmou que não há redirecionador aberto, tendo testado
os mesmos prefixos. A verificação foi refeita por execução e o `security` está
certo — a afirmação do `qa` foi descartada em vez de conciliada. Foi também por
execução que se descartou a primeira medição própria, que falhava por o
interpretador de comandos consumir a barra invertida antes de o Node a ver: o
achado só é confiável porque a construção do payload passou a ser por código de
caractere.

---

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | achado | ACH-01 — filtro do destino não cobre a normalização de autoridade; ACH-19. A validação do formulário está corretamente declarada como usabilidade e não autorização |
| Autorização verificada por operação | ok | RNF-004 medido: requisição forjada ao serviço recusada com `403`; a ocultação da ação na tela está declarada como conveniência de navegação, e a recusa de navegação do painel também |
| Segredo fora do código e do log | ok | Nenhum segredo de cliente embarcado — cliente público com desafio de código; endereços internos deliberadamente sem o prefixo público; nenhuma chamada de console em todo o código de produção; o segredo versionado do banco é decisão documentada, com valor descartável |
| Dado sensível fora de log e mensagem de erro | ok | Token exclusivamente em cookie inacessível ao script; nenhuma mensagem de erro carrega dado do recurso além do detalhe e da relação de campos |
| Dependência nova sem vulnerabilidade conhecida | achado | ACH-02 — 1 crítica e 2 altas em dependência de produção, varredura executada nesta revisão |

Verificados e corretos, registrados porque foram conferidos e não presumidos: o
desafio de código com verificador que nunca sai do servidor; a comparação do
estado na volta do provedor; a ausência deliberada de token de renovação com
justificativa; a marca de inacessibilidade ao script nos cinco cookies e a
limpeza cobrindo os cinco nomes; a fronteira arquitetural de servidor no cliente
REST, que torna a violação erro de compilação e não convenção; e a ausência
total de escrita direta em HTML, avaliação dinâmica ou reescrita de conteúdo.

---

## Guardrails extraídos

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Destino de redirecionamento vindo do cliente é validado por **resolução** contra a origem e comparação de origem, nunca por inspeção de prefixo da string — a normalização de URL converte separadores que o prefixo não prevê | ACH-01 | `requirements/guidelines/frontend/nextjs/security.md` |
| Fechar task que declara dependência nova exige a varredura de vulnerabilidade do DoD executada e a saída registrada — a mesma regra já extraída para imagem em TASK-01.2, agora para pacote de aplicação | ACH-02 | `requirements/guidelines/frontend/nextjs/definition-of-done.md` |
| Critério de aceite que afirma marca, emblema ou estado visual exige asserção que **produza** o estado, e não apenas exercite a tela onde ele apareceria | ACH-03 | `requirements/guidelines/frontend/nextjs/testing.md` |
| Auditoria automatizada de acessibilidade é precedida de asserção de conteúdo que fixa qual página está sendo auditada; sem âncora, ela mede o que estiver na tela | ACH-04 | `requirements/guidelines/frontend/nextjs/testing.md` |
| Asserção negativa sobre elemento de interface é precedida de asserção positiva na mesma página — vale para o frontend a regra que TASK-01.5 extraiu para coleção vazia | ACH-15 | `requirements/guidelines/_shared/testing.md` (dívida nomeada: o arquivo não existe) |
| Texto voltado ao usuário que promete comportamento de outra tela só entra quando o comportamento existe; critério declarado não medido não autoriza a promessa | ACH-07 | `requirements/guidelines/frontend/nextjs/coding-standards.md` |
| Marca de transporte seguro de cookie deriva do esquema da requisição, não de variável de configuração — derivação de variável falha aberta e em silêncio | ACH-12 | `requirements/guidelines/frontend/nextjs/security.md` |
| Envelope de RNF cujo universo de medição é composto de telas tem dono declarado em task antes de a primeira tela nascer | ACH-16 | `requirements/guidelines/_shared/definition-of-done.md` (dívida nomeada: o arquivo não existe) |

Duas promoções para `_shared/` seguem impedidas pela mesma razão das três
revisões anteriores: os arquivos transversais de teste e de definição de pronto
não existem na biblioteca, e criar arquivo de coleção é decisão de
`/guidelines`, não de `/code-review`. Registrado como dívida nomeada, com dono.

---

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 6
- **Revisor humano:** pendente — 2026-09-11

O GATE-NFR reprova por razão própria e não apenas por diferimento: RNF-005 não
tem instrumento em nenhum ponto da cadeia e não tem dono, e RNF-006 é medido só
na metade alcançável, com a outra metade inauditável por código morto. RNF-004 é
o único que sai **dentro** aqui, e RNF-010 é herdado de TASK-01.6.

Três dos seis bloqueantes são resolvíveis de dentro da task (ACH-03, ACH-04 e
ACH-05); dois são de dependência e de configuração de realm (ACH-02, ACH-06); e
ACH-01 é de código, estreito e com correção de poucas linhas.

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
