# Design Brief — kanban-tarefas

_Data: 2026-09-04 | Revisões: 2026-09-09 (INC-09, sobre o PRD v1.1) e 2026-09-09 (INC-13, sobre o PRD v1.2) e 2026-09-09 (INC-14, sobre RN-011 na v1.2) e 2026-09-10 (INC-18, tela de novo projeto, sobre o PRD v1.3) | Etapa: `/design`_

- **Shape Brief:** `docs/shape/kanban-tarefas-brief.md`
- **Exploração de solução:** `docs/solution/kanban-tarefas-solution.md`

> **Procedência.** Este brief substitui `requirements/design/kanban-tarefas-design-brief.md`
> (v1.0, 2026-08-25), construído sobre o enquadramento E-01 puro e sobre uma
> paleta que contraria a coleção de guidelines declarada no `guidelines.yaml`. O
> material de agosto — 11 protótipos e tokens — permanece em
> `requirements/design/` como insumo histórico e **não** é fonte de verdade.
> Isso resolve a suposição S-04: o material anterior descrevia uma versão
> anterior da intenção.

---

## 1. Contexto e Objetivo

A direção aprovada é que **registrar o estado e avisar o próximo responsável
sejam a mesma ação**. Para o design isso tem consequência dura: não existe tela
de notificação separada do fluxo. O aviso é propriedade visível do trabalho,
permanente enquanto durar, e não um alerta que passa.

A aposta do produto (H-01) é que o board se torne o lugar de trabalho diário do
desenvolvedor, e não um painel a consultar. Toda decisão de interface aqui é
julgada por esse critério: **se para saber que a vez chegou for preciso
procurar, o time volta para o chat e a direção falha.**

Duas restrições estruturais atravessam a interface e não são negociáveis em
nenhuma tela: nenhuma visão apresenta tempo agregado por pessoa, e o gestor de
outro time não tem, em lugar algum, ação de escrita disponível.

### 1.1 Inventário de telas

Reconstruído a partir dos seis fluxos da exploração de solução. **Não há
referência a RF** — numeração de requisito é do `/prd`, que ainda não rodou; o
inventário de agosto mapeava RF-001..RF-019 de um PRD que este flow não
produziu.

| ID | Nome | Fluxo(s) que atende | Persona | Rota sugerida |
| --- | --- | --- | --- | --- |
| TL-01 | Entrada autenticada | Todos | Todas | `/entrar` |
| TL-02 | Meus projetos | Todos | Todas | `/projetos` |
| TL-03 | Board do projeto — variação A, cartão compacto | Registrar avanço; Assumir tarefa; Sinalizar impedimento | Desenvolvedor, reviewer, liderança | `/projetos/:id/board` |
| ~~TL-03b~~ | Board do projeto — variação B, cartão expandido. **Variação descartada em 2026-09-09 (DM-02 do PRD).** Não é tela do produto: o arquivo permanece como registro da alternativa avaliada e não é insumo de implementação | — | — | — |
| TL-04 | Detalhe da tarefa, em painel lateral sobre o board | Todos os fluxos de tarefa | Desenvolvedor, reviewer, liderança | painel sobre `/projetos/:id/board` |
| TL-05 | Nova tarefa | Entrada de trabalho no fluxo | Desenvolvedor, Product Owner | painel sobre `/projetos/:id/board` |
| TL-06 | Minha fila — o que aguarda tomada por mim, em todos os projetos | Assumir tarefa | Reviewer, validador, desenvolvedor | `/minha-fila` |
| TL-07 | Andamento e tempo por etapa | Consultar andamento | Product Owner, gestor de outro time, liderança | `/projetos/:id/andamento` |
| TL-08 | Configuração do fluxo do projeto — etapas, ordem, terminais, raias | Configurar fluxo | Responsável pela configuração | `/projetos/:id/config/fluxo` |
| TL-09 | Participação e permissões do projeto | Configurar fluxo | Responsável pela configuração | `/projetos/:id/config/participacao` |
| TL-10 | Reabrir tarefa concluída | Decorrente de Q-06 | Product Owner, e somente ele | diálogo sobre TL-04 |
| TL-11 | Novo projeto — nome, descrição e a primeira administradora do projeto, nomeada na mesma operação | Abertura de projeto (RF-022) | Administração global, e somente ela | painel sobre `/projetos` |

Duas telas não existiam no material de agosto e nascem da direção aprovada:

- **TL-06 Minha fila** materializa E-02. Sem ela, descobrir que a vez chegou
  exige abrir cada board — e a promessa de que o registro é o próprio aviso não
  se cumpre. É a tela de maior risco para H-01.
- **TL-10 Reabrir tarefa concluída** existe por causa de Q-06: é a única ação do
  produto visível para uma só pessoa. O design precisa deixar claro que é
  exceção, não caminho normal.

Uma tela de agosto **saiu**: a confirmação de exclusão de card. Excluir foi
substituído por *encerrar sem conclusão*, que é transição de estado e acontece
em TL-04 — a tarefa sai do fluxo, mas não do histórico. A distinção é o que
protege o tempo por etapa.

**TL-11 entrou em 2026-09-10**, e não pela direção: pelo achado INC-18. A
emenda v1.3 do PRD instituiu a criação de projeto sem tela, com a justificativa
de que seria operação de instalação. A justificativa não se sustentava contra as
regras da própria emenda — abrir projeto é operação recorrente, para sempre —, e
o efeito era um produto em que a única pessoa autorizada a criar projeto não
tinha por onde. O demandante decidiu instituir a tela. Ela é a segunda ação do
produto visível para um só perfil, ao lado de TL-10, e a única cuja ausência
deixaria o sistema recém-instalado sem saída por dentro.

### 1.2 Escopo do protótipo

- **Telas:** TL-01 a TL-11 — **11 telas**. Há um 12º arquivo, TL-03b, que é
  registro da variação de densidade descartada em DM-02, não tela do produto.
- **Variações:** nenhuma em aberto. A única que existiu — densidade do cartão
  (QD-02) — foi resolvida no PRD em favor do compacto.
- **Temas:** claro e escuro, ambos demonstráveis.
- **Estados obrigatórios:** conforme a matriz da seção 6.3.
- **Fora do protótipo:** qualquer visão que agregue tempo por pessoa, e qualquer
  ação de escrita exposta a quem tem acesso somente de leitura.

## 2. Tokens de Cor

**Fonte de verdade:** `requirements/guidelines/frontend/nextjs/design-tokens.json`
e o `design-system.md` da mesma coleção, declarada em `guidelines.yaml`. Decisão
de 2026-09-04, registrada em **DDR-004**, que supersede o DDR-001.

| Aspecto | Valor | Origem |
| --- | --- | --- |
| Ação primária | `#004B8D`, consumida sempre como token semântico | Coleção Next.js |
| Papéis semânticos | `background`/`foreground`, `primary`, `secondary`, `ghost`, `destructive`, `ring`, cada um com par claro/escuro | Coleção Next.js |
| Tema | Claro **e** escuro, alternados por classe na raiz | Decisão de 2026-09-04 (DDR-004) |
| Impedimento | **Papel semântico novo**, par claro/escuro, distinto do destrutivo | Decisão de 2026-09-04 (QD-01) |

**Regra que governa todo o produto:** superfície e texto apenas com token
semântico. Nenhum hexadecimal cru, nenhuma rampa de marca aplicada direto — é o
que faz o tema escuro funcionar sem retrabalho.

**Impedimento (QD-01, decidida).** A coleção não tem papel para essa condição.
Foi criado um par próprio em vez de reutilizar o destrutivo: impedimento é
condição legítima do trabalho, não falha de quem o registra, e aparece o dia
inteiro. Consequência assumida: altera a biblioteca compartilhada, que governa
outros sistemas — vira trabalho no `/tasks`. A distinção não se apoia só em
matiz: o cartão impedido carrega ícone, rótulo textual e contador travado.

**Dívida herdada.** A coleção reconhece cores ainda não tokenizadas para
*informação*, *sucesso* e borda de campo. O `design-tokens.json` deste produto
as promove a papéis semânticos com rastreio da origem, em vez de reproduzir a
dívida.

## 3. Tipografia

Herdada integralmente da coleção (DDR-004). Nada é decidido aqui.

| Aspecto | Valor |
| --- | --- |
| Família | Inter Variable, fallback de sistema |
| Pesos | 400, 500 e 600 — apenas estes |
| Escala | 8 degraus fechados, em px: 12, 14, 16, 18, 20, 24, 30, 36 |
| Corpo de texto | 16px como base do documento |

O board é a tela mais densa do produto e tende a puxar para tamanhos abaixo da
escala. A escala é fechada: hierarquia no cartão se resolve com peso e cor, não
com tamanho fora dos 8 degraus.

## 4. Espaçamento e Grid

| Aspecto | Valor | Origem |
| --- | --- | --- |
| Base de espaçamento | 16px, com a escala custom da coleção | Coleção Next.js |
| Container de página | Espaçamento `4xl` (32px) | Coleção Next.js |
| Raio | Base 16px; botão `lg` (8px); cartão e badge `xl` (12px) | Coleção Next.js |
| Alturas de controle | Botão 44px (md) e 36px (sm); campo 44px | Coleção Next.js |
| Alvo de toque | Mínimo 36px; ação primária 44px | Coleção Next.js |
| Breakpoints | Desktop-first. Base para ≥1280px, adaptação para baixo até 1024px | Coleção + Intent |
| Abaixo de 1024px | Não é alvo | Intent |

**Layout.** Sidebar colapsável mais topbar fixa de 80px. A sidebar reúne
projetos acessíveis, minha fila, andamento e configuração do projeto ativo, com
destaque do projeto corrente; a topbar traz identificação do usuário e os avisos
internos pendentes.

O board recebe tratamento particular por ser a tela mais espremida: a sidebar
colapsa por padrão quando ele está aberto, e a rolagem horizontal das etapas
nunca esconde a primeira coluna sem indicação.

## 5. Componentes

**Mecânica herdada da coleção:** comportamento por primitivo Radix, aparência
por CVA, composição por utilitário de mesclagem de classes. Toda API pública
aceita classe externa. Os primitivos vêm de `components/ui/` da coleção e não
são recriados.

Reutilizados sem alteração: botão, campo de texto, seleção, caixa de marcação,
interruptor, cartão, alerta, badge, abas, diálogo, painel lateral, menu suspenso,
dica de contexto, tabela e avisos efêmeros.

Componentes novos, específicos deste produto:

| Componente | Papel | Observação |
| --- | --- | --- |
| Cartão de tarefa | Unidade de trabalho no board | Precisa acomodar espera de tomada com contador, quem assumiu e a marca de impedimento **somada às demais informações, não no lugar delas**. Densidade decidida em 2026-09-09: **compacta** (QD-02/DM-02) |
| Coluna de etapa | Agrupa tarefas por posição no fluxo | Realce durante o arrasto quando alcançável; esmaecida quando não |
| Raia | Agrupamento livre dentro das etapas | Não restringe transição (Q-08) |
| Marcador de espera de tomada | Contador contínuo de espera | Sempre visível (QD-03) |
| Marcador de impedimento | Dimensão própria, com tempo correndo em série separada | Usa o papel semântico novo, nunca o destrutivo. Convive com qualquer condição e desabilita exatamente duas ações: concluir e encerrar sem conclusão (RN-011) |
| Barra de tempo por etapa | Distribuição do tempo de permanência | Agregação apenas por etapa e por projeto |

## 6. Padrões de Interação

### 6.1 Caminhos principais

**Handoff — a razão de E-02 estar na direção.** TL-01 → TL-02 → TL-03, onde o
desenvolvedor localiza a tarefa que assumiu → TL-04 → indica avanço de etapa →
o cartão reaparece na etapa de destino marcado como aguardando tomada, com o
tempo de espera correndo à vista → o reviewer vê o item surgir em TL-06 sem ter
aberto o board → assume → TL-04 passa a exibi-lo como responsável, e a contagem
de espera encerra.

**Impedimento.** TL-03 → TL-04 → abre impedimento com motivo → o cartão passa a
exibir a marca e o contador próprio, permanentemente, sem depender de alguém ter
notado um alerta → a tarefa **continua se movendo entre etapas e podendo ser
assumida**, com a marca acompanhando → quem responde pelo desbloqueio vê o
destaque em TL-03 e em TL-06 → resolve com desfecho registrado em TL-04 → a marca
some e nem a condição nem a etapa são alteradas, porque a abertura nunca as
alterou (RN-032).

### 6.2 Erros e bordas com tratamento visual

**Transição não permitida (Q-02).** Ao arrastar um cartão, apenas as etapas
alcançáveis a partir da atual ficam realçadas; as demais esmaecem. Soltar fora
recusa a transição, o cartão retorna à origem e a razão é informada sem
interromper o trabalho. Raia **não** restringe transição — é agrupamento,
conforme Q-08.

**Impedimento é a terceira dimensão (Q-01 e emenda do PRD v1.1).** Etapa,
condição de trabalho e marca de impedimento são independentes: nenhuma tela
exibe "impedida" como condição, e a marca coexiste com qualquer condição não
terminal (RN-002, RN-003, RN-032). A tarefa com impedimento aberto **pode** mudar
de etapa, inclusive retroceder ao backlog, e **pode ser assumida** (RN-009,
RN-033) — a única coisa que a marca impede é concluir (RN-011). O contador de
impedimento atravessa a transição sem ser reiniciado nem encerrado, e corre em
paralelo aos de permanência e de espera, que nunca lhe são somados (RN-008).

O protótipo TL-04 havia sido gerado antes da decisão de Q-01, desabilitando
"mover" em cartão impedido — era o achado **INC-09**, e a divergência foi
corrigida em 2026-09-09: a ação está habilitada, com a nota explicando por quê,
e a ficha exibe as três dimensões em campos separados. TL-03, TL-03b e TL-06
tiveram o rótulo e a legenda realinhados, e TL-06 ganhou um item de fila
impedido e assumível, que é o que SCN-007.4 congela.

**Ação concorrente (B-03).** Duas pessoas agem sobre a mesma tarefa. A ação que
perdeu é **recusada e informada com o estado atual**, nunca sobrescrita em
silêncio. É a interação mais sensível do produto: o discovery registra uso
simultâneo como rotina, e resolver com "o último vence" reintroduziria dentro do
sistema a perda silenciosa de informação que motivou o projeto inteiro. Como a
tarefa aguardando tomada não tem responsável nomeado (Q-10), ela aparece na fila
de várias pessoas ao mesmo tempo — a recusa é ocorrência esperada, não exceção
rara.

**Escrita por quem só lê (B-07).** Ao gestor de outro time nenhuma ação de
escrita é oferecida em nenhuma tela — os elementos não são renderizados, e não
apenas desabilitados. Não é ocultação cosmética: é a fronteira entre visibilidade
e interferência, que o demandante recusou explicitamente.

**Vazio que não é zero (B-01).** Em TL-07, projeto sem histórico exibe "ainda
não medido", jamais o número zero. Zero afirmaria que as tarefas atravessam as
etapas instantaneamente, e é justamente o primeiro período de uso que estabelece
a linha de base da métrica de sucesso.

### 6.3 Estados por tela

| Tela | idle | loading | preenchido | erro | sucesso | vazio |
| --- | --- | --- | --- | --- | --- | --- |
| TL-01 Entrada autenticada | Sim | Sim | Não se aplica | Falha na autenticação | Sim | Não se aplica |
| TL-02 Meus projetos | Sim | Sim | Sim | Sim | Não se aplica | **Dois vazios distintos:** nenhuma participação (sem saída — pedir acesso) e, para a administração global, nenhum projeto no sistema (com saída — criar o primeiro) |
| TL-03 Board | Sim | Sim | Sim | Transição recusada; ação concorrente recusada | Não se aplica | Etapa sem tarefa |
| TL-04 Detalhe da tarefa | Sim | Sim | Sim | Ação recusada por estado ou por permissão; conclusão e encerramento sem conclusão recusados por impedimento aberto, com mover disponível | Sim | Não se aplica |
| TL-05 Nova tarefa | Sim | Sim | Sim | Validação | Sim | Não se aplica |
| TL-06 Minha fila | Sim | Sim | Sim | Sim | Ao assumir | Nada aguardando por mim |
| TL-07 Andamento e tempo | Sim | Sim | Sim | Sim | Não se aplica | Ainda não medido, distinto de zero |
| TL-08 Configuração do fluxo | Sim | Sim | Sim | Sem etapa terminal; etapa a remover com tarefas | Sim | Fluxo não configurado |
| TL-09 Participação e permissões | Sim | Sim | Sim | Sim | Sim | Só o configurador participa |
| TL-10 Reabrir tarefa concluída | Sim | Sim | Não se aplica | Sem permissão | Sim | Não se aplica |
| TL-11 Novo projeto | Sim | Sim | Sim | Validação de nome e de administradora ausentes; recusa do servidor quando a pessoa nomeada não existe | Sim — e o sucesso **nomeia o segundo passo obrigatório**, oferecendo ir configurar o fluxo | Não se aplica |

Os quatro estados que mais importam, por serem os que o material de agosto não
tinha como revelar: **aguardando tomada com tempo correndo** (TL-03, TL-06),
**ação concorrente recusada** (TL-03, TL-04), **ainda não medido**
(TL-07) e **impedimento como dimensão à parte** (TL-03, TL-04, TL-06) —
este último acrescentado na revisão de 2026-09-09.

Um quinto entrou em 2026-09-10 com TL-11: **projeto criado e ainda inutilizável**.
Ele aparece em três lugares de propósito — no sucesso de TL-11, no cartão do
projeto em TL-02 e no vazio de TL-08 — porque o custo que o demandante assumiu ao
recusar o fluxo padrão é justamente o de um segundo passo obrigatório que nada
lembra. Mitigar isso é trabalho de interface, e é a razão de a tela existir com
esse desenho e não como um formulário que apenas confirma.

### 6.4 Decisões de design em aberto

| # | Questão | Situação | Impacto |
| --- | --- | --- | --- |
| QD-01 | Papel semântico para impedimento | **Decidida em 2026-09-04:** criar papel próprio, par claro/escuro, adicionado à coleção antes do uso | Impedimento é condição legítima do trabalho. Altera a biblioteca compartilhada — vira trabalho no `/tasks` |
| QD-03 | Como o tempo de espera aparece no cartão | **Decidida em 2026-09-04:** contador contínuo, sempre visível | Escondê-lo até um limiar seria decidir que até lá a espera não importa — e o produto deliberadamente não tem prazo nem escalonamento (B-08) |
| QD-02 | Densidade do cartão: compacto ou expandido | **Decidida em 2026-09-09 pelo demandante, na DM-02 do PRD:** o cartão compacto é o padrão e TL-03b fica como variação descartada | A decisão saiu do PRD, não desta etapa, e este brief apenas se conforma a ela — era o achado INC-13. Nenhum cenário congelado depende da escolha; o que dependia era a leitura de quem toma o design como insumo |
| QD-04 | Ordenação de TL-06: por tempo de espera ou por projeto | **Aberta** | Por tempo põe o mais parado no topo, o que serve à dor; por projeto é mais previsível para quem atua em um só |

QD-04 é a única que segue aberta, e é exatamente o que o protótipo existe para
responder com o time. QD-01, QD-02 e QD-03 estão decididas.

## 7. Acessibilidade

**Nível: WCAG 2.1 AA, obrigatório.** Decisão de 2026-09-04, registrada em
**DDR-005**, que supersede o DDR-003 — este dispensava acessibilidade por se
tratar de sistema interno. O custo marginal caiu porque a coleção já entrega
foco visível, rótulo acessível obrigatório em ícone sem texto, estados de
carregamento anunciados e navegação por teclado nos primitivos.

- **Teclado é requisito de primeira classe no board.** Mover a tarefa entre
  etapas tem equivalente por teclado. O caminho alternativo ao arrasto previsto
  no DDR-002 deixa de ser conveniência e passa a ser obrigação: sem ele, a
  operação central do produto fica inacessível. As etapas não alcançáveis
  aparecem nesse caminho com a razão da recusa, em vez de simplesmente sumirem.
- **Mudança de estado é anunciada sem interromper a leitura em andamento** —
  tarefa que chega aguardando tomada, impedimento aberto e recusa por ação
  concorrente.
- **Contraste** verificado inclusive nas cores que a coleção ainda não
  tokenizou, e no papel novo de impedimento, nos dois temas.
- **Idioma:** apenas pt-BR. Sem suporte multilíngue, o que simplifica o layout.

## 8. Decision Records de Design (DDR)

| DDR | Título | Efeito |
| --- | --- | --- |
| [DDR-004](../decisions/DDR-004-adocao-design-system-nextjs.md) | Adoção do design system da coleção Next.js como fonte de verdade visual | Supersede DDR-001 |
| [DDR-005](../decisions/DDR-005-acessibilidade-wcag-aa-obrigatoria.md) | Acessibilidade WCAG 2.1 AA obrigatória e feedback realinhado ao design system | Supersede DDR-003 |
| [DDR-006](../decisions/DDR-006-espera-de-tomada-primeira-classe.md) | Espera de tomada como estado visível de primeira classe, com fila própria | Novo nesta etapa |
| [DDR-007](../decisions/DDR-007-impedimento-como-dimensao-visual-ortogonal.md) | Impedimento como dimensão visual ortogonal, que só desabilita o que o contrato recusa | Novo na revisão de 2026-09-09 (INC-09); emendado na de INC-14 |
| [DDR-002](../decisions/DDR-002-drag-and-drop-board.md) | Interação do board: arrasto com destaque de etapas válidas e caminho alternativo | Mantido; o caminho alternativo passa a obrigatório por DDR-005 |

## 9. Referências

**Artefatos desta etapa:**

- `docs/design/kanban-tarefas/screen-map.md`
- `docs/design/kanban-tarefas/design-tokens.json`
- `docs/design/kanban-tarefas/prototypes/` — 11 arquivos HTML, mais folha de
  estilo e script compartilhados

**Fontes que governam este brief:**

- `requirements/guidelines/frontend/nextjs/design-system.md` e
  `design-tokens.json` — fonte de verdade visual (DDR-004)
- `requirements/guidelines/frontend/_shared/design-principles.md`
- `docs/shape/kanban-tarefas-brief.md` — direção aprovada no GATE-DIRECAO
- `docs/solution/kanban-tarefas-solution.md` — fluxos, estados, bordas e as
  questões Q-01 a Q-10

**Insumo histórico, sem valor normativo:**

- `requirements/design/kanban-tarefas-design-brief.md` (v1.0, 2026-08-25) e
  `requirements/design/kanban-tarefas/` — superados por este brief e por DDR-004
