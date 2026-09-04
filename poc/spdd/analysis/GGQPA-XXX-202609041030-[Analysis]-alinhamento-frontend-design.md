# SPDD Analysis: Alinhamento do Frontend aos Requisitos de Design (Kanban de Tarefas)

## Original Business Requirement

> corrigir o frontend seguindo os requisitos de design presentes em 'D:\DEV\Projects\SPDD_puro\poc\requirements\design', pois o resultado final não ficou igual.

Fontes de design consolidadas (lidas integralmente):

- `requirements/design/kanban-tarefas-design-brief.md` v1.0 — identidade visual, paleta, tipografia/grid, navegação/layout, inventário de telas TL-01..TL-10, fluxos, matriz de estados por tela, acessibilidade (WCAG AA, pt_BR, desktop 1280px+), decisões em aberto (densidade de card), escopo do protótipo.
- `requirements/design/kanban-tarefas/design-tokens.json` — tokens de cor, tipografia, espaçamento, raio, breakpoints, elevação e acessibilidade.
- `requirements/design/kanban-tarefas/screen-map.md` — rotas, componentes principais por tela, transições de navegação e mapa de arquivos do protótipo.
- `requirements/design/kanban-tarefas/prototypes/_shared.css` + 11 arquivos HTML (`tl-01` a `tl-10`, incluindo `tl-03b`) — a materialização normativa do design: vocabulário de classes, estrutura de layout, composição de cada tela e os estados obrigatórios.

## Domain Concept Identification

O domínio aqui **não é o negócio de kanban** (esse já está implementado e não está em questão) — é o **sistema de design** e sua fidelidade de aplicação. Os conceitos abaixo são conceitos de design/UI, identificados por comparação entre o protótipo normativo e o código React/Next.js existente.

### Existing Concepts (from codebase)

- **Design tokens gerados**: `frontend/design-tokens.json` → `scripts/gen-tokens.mjs` → `src/styles/tokens.css`. Pipeline correto e alinhado ao DDR-001; os **valores** de token conferem com `design/kanban-tarefas/design-tokens.json`. Este é o único eixo do design hoje plenamente respeitado.
- **Folha de estilo global** (`src/styles/globals.css`): define um vocabulário de classes **próprio, em português** (`.layout`, `.cartao`, `.conteudo`, `.coluna`, `.card`, `.toasts`, `.vazio`, `.tabela`, `.barra`, `.abas`, `.lista-cartoes`, `.raia-titulo`, `.badge`), consumindo os tokens corretos.
- **Chrome de aplicação** (`src/components/Chrome.tsx`): sidebar + topbar presentes conceitualmente, montados como grid de 2 colunas com a topbar *dentro* da coluna de conteúdo.
- **Telas implementadas**: TL-01 (`app/login`), TL-02 (`app/projetos`), TL-03/03b (`components/Board.tsx`), TL-04 (`TarefaDrawer`), TL-05 (`NovaTarefaModal`), TL-06 (`ConfirmarExclusaoModal`), TL-07 (`Dashboard`), TL-08/09/10 (`app/projetos/[id]/admin/*`). **Cobertura funcional de telas está completa** — o problema é de forma, não de ausência.
- **Estados de tela** (`components/Estados.tsx`, `components/Feedback.tsx`): abstrações `Skeleton`, `Vazio`, `Erro`, `SemPermissao`, `AvisoSomenteLeitura`, toasts e modal bloqueante. Existem, mas com uma taxonomia própria (seis estados: loading/vazio/erro/sem permissão/somente leitura/ok) que **não coincide** com a taxonomia do brief (seção 6: idle/loading/preenchido/erro/sucesso/vazio).

### New Concepts Required

- **Vocabulário de design system canônico**: o conjunto de classes de `_shared.css` (`.app-shell`, `.page-header`, `.btn`/`.btn-primary`/`.btn-secondary`/`.btn-outline`/`.btn-danger`/`.btn-text`, `.card`, `.badge-tipo`/`-warning`/`-error`/`-success`, `.task-card`/`.task-card__impedido`, `.column`/`.column__header`/`.column--drop-valid`/`.column--drop-invalid`, `.badge-transicao`, `.toast-error`/`.toast-success`, `.form-field`/`.form-error`, `.empty-state`, `.kpi-grid`/`.kpi-card`, `.bar-chart`/`.bar`/`.bar-wrap`, `.avatar`, `.sidebar__brand`/`.sidebar__project-active`/`.topbar__notif`/`.topbar__notif-badge`/`.topbar__user`, `.text-secondary`, `.skeleton`, `.modal-overlay`/`.modal`). Precisa existir como camada única e nomeada de forma estável no app, substituindo o vocabulário paralelo atual — hoje **não há mapa 1:1** entre os dois.
- **Shell de aplicação em grid de áreas**: `app-shell` com `grid-template-areas: "sidebar topbar" / "sidebar main"` e linha de topbar de 56px. Conceito estrutural ausente: o app usa duas colunas com a topbar aninhada, o que muda alinhamento, altura e comportamento de scroll.
- **Componentes visuais atômicos ausentes**: `Avatar` (círculo com iniciais), `NotificationBell` (ícone + badge de contagem), `Badge` com variantes semânticas, `Button` com variantes, `PageHeader` (título + ação à direita), `EmptyState`, `KpiCard`, `BarChart` vertical, `TransitionHint` (rótulo "Permitido"/"Sem transição" no header da coluna durante o drag).
- **Fonte Inter como recurso carregado**: o token declara `'Inter', system-ui, …`, mas nenhuma fonte é carregada (sem `next/font`, sem `<link>`). Hoje **toda a tipografia cai no fallback** — é a divergência visual de maior alcance, pois afeta cada pixel de texto de todas as telas.
- **Agrupamento visual raia→colunas**: no protótipo a **raia é o container externo** (`.swimlane` com título) e as colunas do workflow vivem dentro dela; no código a hierarquia está **invertida** (coluna externa, raias empilhadas dentro). Conceitualmente é a mesma informação, visualmente é outro board.

### Key Business Rules

- **Token é fonte única de verdade** (DDR-001): nenhuma cor, espaçamento, raio ou elevação literal em componente. Governa: todos os estilos. Hoje violado pontualmente (`#fff3cd`/`#8a6d3b` em `.badge.impedida`, `#eceff1`/`#f5f7f8` no skeleton, `rgba(33,37,41,0.4)` no backdrop, dezenas de `style={{ gap: 8, marginBottom: 24 }}` inline em vez de `var(--espaco-*)`).
- **Destino de drop é por coluna, nunca por raia** (RF-002/RN-003, nota da seção 5 do brief): a **semântica** está correta no código (`destinosPermitidos` por etapa), mas a **expressão visual** diverge — o protótipo especifica outline verde tracejado + fundo `#eafaf1` (`--color-success`) e um rótulo textual no header da coluna; o código usa borda/inset azul primário e nenhum rótulo. Governa: `Board`, `.column--drop-valid/--drop-invalid`.
- **Impedimento é sinalizado por borda esquerda 4px warning e badge "Impedido"** (RF-004): no código **todo** card recebe borda esquerda 3px primária e o impedido apenas troca a cor — o design reserva a borda esquerda exclusivamente para o estado de impedimento. Governa: `.task-card`.
- **Seis estados obrigatórios por tela, conforme a matriz da seção 6**: idle, loading, preenchido, erro, sucesso, vazio — por tela, não genericamente. Governa: todas as TL. A taxonomia atual do app não é rastreável contra essa matriz.
- **WCAG AA, foco visível, `aria-live="polite"` para notificações em tempo real** (seção 7, DDR-003): parcialmente atendido — foco visível ok, `aria-live` presente no dropdown de notificações e nos toasts; falta `role="alert"`/`aria-live="assertive"` no erro de transição bloqueada, que o protótipo marca explicitamente como assertivo.
- **Desktop-first, prioritário 1280px+, secundário 1024px, sem suporte mobile obrigatório** (RNF-005): o app **esconde a sidebar abaixo de 1024px**, retirando a navegação exatamente no breakpoint que o brief declara como suportado.
- **Densidade de card é decisão em aberto, resolvida por revisão do protótipo** (seção 8 do brief): o app resolveu unilateralmente entregando um **toggle em runtime** entre compacto e expandido. Isso não é o que TL-03 vs TL-03b pede.
- **pt_BR sem multi-idioma**: atendido, porém todos os textos da UI estão **sem acentuação** ("Notificacoes", "Usuarios", "Descricao", "Voce nao tem permissao"), enquanto o protótipo é integralmente acentuado. Divergência visível e trivialmente corrigível.

## Strategic Approach

### Solution Direction

Tratar `_shared.css` + os 11 HTMLs do protótipo como **especificação normativa de UI**, e fazer o frontend convergir para ela em três camadas, de baixo para cima:

1. **Camada de fundação** — carregar Inter via `next/font`, manter o pipeline de tokens como está (já correto) e **reescrever `globals.css` para o vocabulário de classes do protótipo**, portando `_shared.css` como base e estendendo apenas onde o app real exige (drawer, toasts empilhados, animação de skeleton, estados de permissão que o protótipo não cobre).
2. **Camada de shell** — substituir `.layout` por `.app-shell` com áreas de grid, e reconstruir `Chrome` com `sidebar__brand`, nav com `aria-current`, `sidebar__project-active`, e topbar com sino + badge de contagem, avatar e menu de usuário.
3. **Camada de tela** — percorrer TL-01..TL-10 em ordem, alinhando cada tela ao seu HTML de referência: estrutura, composição, hierarquia tipográfica e a linha de estados da matriz da seção 6.

A direção geral é **CSS-first**: a maior parte da divergência é de folha de estilo e de marcação, não de lógica. A lógica de dados, permissões, streaming e chamadas de API não deve ser tocada — é a parte que funciona.

### Key Design Decisions

- **Portar `_shared.css` vs. reescrever `globals.css` do zero**: portar preserva a paridade exata com o protótipo e torna a divergência auditável por diff; reescrever do zero arrisca reintroduzir vocabulário próprio. → **Portar `_shared.css` como base do `globals.css`**, mantendo as regras extras que o app precisa em um bloco claramente demarcado após o bloco portado. Consequência aceita: renomeação em massa de classes nos componentes (`.cartao`→`.card`, `.coluna`→`.column`, `.vazio`→`.empty-state`, `.tabela`→`table`, etc.).
- **Classes utilitárias do protótipo vs. CSS Modules / componentes tipados**: o app hoje mistura classes globais com `style={{}}` inline abundante. Componentizar tudo é o ideal de longo prazo, mas maximiza o diff e o risco em uma correção de fidelidade. → **Manter classes globais (como o protótipo), extrair apenas os átomos que se repetem em ≥3 telas** (`Avatar`, `Badge`, `Button`, `PageHeader`, `EmptyState`), e **eliminar `style={{}}` inline** que carregue valores de token.
- **Redefinir a taxonomia de estados**: alinhar `Estados.tsx` à matriz da seção 6 significa mexer em componentes usados por todas as telas. → **Manter as abstrações atuais e mapeá-las** para os nomes do brief (loading→`loading`, vazio→`vazio`, erro→`erro`), **acrescentando** o estado `sucesso` onde a matriz o exige (TL-04, TL-05, TL-06, TL-08, TL-09) e preservando `SemPermissao`/`AvisoSomenteLeitura` como extensões legítimas (o protótipo não cobre RBAC, mas o PRD exige).
- **Toggle de densidade do board**: era uma decisão de produto em aberto (seção 8 do brief). → **DECIDIDO pelo usuário (2026-09-04): manter o toggle de runtime entre compacto (TL-03) e expandido (TL-03b)**, com compacto como padrão. O toggle deve usar `.btn`/`.btn-outline` e `aria-pressed` do design system, e o card expandido deve seguir TL-03b (título, descrição curta, badges tipo/prioridade, responsável, indicador de impedimento, lead-time da etapa).
- **Sidebar abaixo de 1024px**: → **remover o `display:none`**; em 1024px a sidebar permanece (breakpoint secundário suportado). Nenhum comportamento mobile é requisito.
- **Dashboard: tabela vs. protótipo**: o código entrega uma tabela rica (amostras, médias, barras horizontais) — informacionalmente **superior** ao protótipo, mas visualmente divergente. → **Adotar a estrutura do protótipo (`kpi-grid` com 3 KPIs + `bar-chart` vertical) e manter a tabela como detalhamento abaixo**, preservando a regra correta de "etapa sem amostras não vira barra".
- **Filtro de período do Dashboard**: → **DECIDIDO pelo usuário (2026-09-04): seguir os presets do protótipo** — `<select aria-label="Filtro de período">` com "Últimos 30 dias" / "Últimos 90 dias", substituindo os dois campos `datetime-local`. O preset é convertido no par início/fim já aceito pela API.
- **Acentuação de textos**: → corrigir para pt_BR acentuado em toda a UI, alinhado ao protótipo.

### Alternatives Considered

- **Adotar uma biblioteca de UI (Bootstrap/Tailwind/shadcn)**: a paleta é literalmente Bootstrap 5, o que torna tentador. Rejeitado — introduz dependência e um segundo sistema de tokens concorrendo com `design-tokens.json`, contrariando DDR-001, e não aproxima do protótipo, que é CSS puro.
- **Corrigir apenas cores e espaçamentos, deixando estrutura como está**: rejeitado — os tokens já estão corretos; a divergência percebida ("não ficou igual") é dominada por fonte não carregada, estrutura de shell, ausência de avatar/sino/KPI/gráfico e vocabulário de componentes. Um ajuste só de cor não moveria a agulha.
- **Reescrever o frontend a partir dos HTMLs**: rejeitado — descartaria lógica de auth, RBAC, streaming e integração de API que está correta e é a parte cara.
- **Renderizar os HTMLs do protótipo como páginas**: rejeitado — protótipos contêm seções de estado empilhadas para revisão, não são a UI de produção.

## Risk & Gap Analysis

### Requirement Ambiguities

- **"não ficou igual" não é quantificado**: não há critério de aceite de fidelidade. Assumido: paridade estrutural e visual com os HTMLs de `prototypes/` em 1280px, tela a tela, sem exigir pixel-perfect.
- ~~**Densidade do card (seção 8 do brief) permanece em aberto**: TL-03 vs TL-03b nunca foi decidido; o app criou um toggle.~~ **RESOLVIDO (2026-09-04): toggle mantido, compacto como padrão.**
- **Protótipo não cobre RBAC nem estados de permissão**: `SemPermissao` e `AvisoSomenteLeitura` existem no código por exigência do PRD, sem referência visual. Precisam de tratamento visual derivado (provavelmente `empty-state` e `toast`/faixa informativa).
- ~~**Filtro de período do Dashboard**: o protótipo mostra um `<select>` com presets ("Últimos 30/90 dias"); o código usa dois campos `datetime-local`.~~ **RESOLVIDO (2026-09-04): presets do protótipo são normativos.**
- **Sidebar colapsável** (brief seção 3): declarada como requisito, mas o protótipo não implementa o colapso e o código não tem o controle. Escopo indefinido.
- **Notificações**: o protótipo mostra um sino com badge; o código faz polling a cada 30s com dropdown. Estrutura de interação não especificada no design.
- **`_shared.css` não cobre 100% dos tokens**: não define `--elevacao-*` nem usa `box-shadow` nos cards, enquanto `design-tokens.json` especifica elevações e o código as aplica. Conflito entre as duas fontes de design — o JSON deve prevalecer.

### Edge Cases

- **Board com muitas raias × muitas colunas**: com a hierarquia correta (raia externa, board interno), cada raia gera seu próprio scroll horizontal. Alinhamento entre colunas de raias diferentes não é garantido pelo protótipo e pode ficar visualmente quebrado.
- **Coluna vazia dentro de raia preenchida**: o protótipo mostra `empty-state` compacto dentro da coluna; o código hoje retorna `null` para a combinação raia×coluna sem cards, e só mostra vazio quando o board inteiro está vazio.
- **Nomes longos** (projeto, etapa, tarefa, usuário): o protótipo usa textos curtos; sem `text-overflow`/`min-width:0` os grids e colunas de largura fixa quebram.
- **Contagem de notificações ≥ 10 / 100**: o badge do sino tem padding fixo de 1px 5px — precisa de truncamento ("9+").
- **Avatar sem nome ou com nome de uma palavra**: derivação de iniciais precisa de regra.
- **Drag com projeto finalizado / sem permissão**: o card não é arrastável — o design não especifica affordance visual para isso (`cursor: grab` deve virar `default`).
- **Toast de transição bloqueada durante scroll horizontal do board**: o protótipo o mostra inline; o app o mostra fixo no canto inferior direito. Fora do campo de visão do usuário que arrastou.

### Technical Risks

- **Renomeação em massa de classes**: risco alto de regressão silenciosa (classe órfã não estiliza nada, e não há teste visual). Mitigação: fazer a troca tela a tela com verificação visual de cada uma, e varrer por classes antigas remanescentes ao final.
- **Ausência de qualquer teste de frontend** (`package.json` não tem runner de teste): não há rede de segurança. Mitigação: verificação manual guiada pela matriz de estados da seção 6; considerar snapshots visuais fora de escopo desta correção.
- **Inversão da hierarquia raia/coluna no `Board`**: é a única mudança estrutural com impacto em lógica (o agrupamento e o cálculo de alvos de drop são reorganizados). Risco de quebrar a regra RF-002/RN-003, que hoje está correta. Mitigação: preservar `destinosPermitidos` como única fonte de verdade do drop e não derivar nada da raia.
- **`next/font` com Inter**: adiciona download de fonte; em ambiente Docker sem rede na build (ADR-008) o `next/font/google` falha no build. Mitigação: avaliar self-host da fonte em `public/`.
- **`style={{}}` inline espalhado**: a remoção toca praticamente todos os componentes, ampliando o diff além do estritamente visual.
- **`position: absolute` do dropdown de notificações** sem ancestral posicionado: já é um bug de layout latente; será resolvido pela reestruturação da topbar.

### Acceptance Criteria Coverage

O requisito do usuário não traz ACs formais. A tabela abaixo deriva critérios verificáveis do brief, do screen-map e dos protótipos — cada linha é um AC proposto para esta correção.

| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| AC-1 | Tipografia Inter efetivamente carregada e aplicada em toda a UI | Sim | Hoje nenhuma fonte é carregada; risco de build offline (ADR-008) — avaliar self-host |
| AC-2 | Todos os valores de cor/espaço/raio/elevação vêm de `tokens.css`, sem literais | Parcial | Pipeline correto; restam literais em `.badge.impedida`, `.skeleton`, `.backdrop` e em `style={{}}` inline |
| AC-3 | Shell usa `app-shell` com grid de áreas (sidebar 240px, topbar 56px) | Sim | Requer reestruturar `Chrome.tsx` |
| AC-4 | Sidebar com marca, nav com `aria-current`, e bloco de projeto ativo | Sim | Bloco de projeto ativo inexistente hoje |
| AC-5 | Topbar com sino + badge de contagem, avatar de iniciais e menu de usuário | Sim | Avatar e badge inexistentes; dropdown com posicionamento quebrado |
| AC-6 | Vocabulário de classes do app corresponde ao de `_shared.css` | Sim | Renomeação em massa; principal fonte de risco de regressão |
| AC-7 | TL-01 fiel: card 380px, subtítulo, botão primário full-width | Sim | Hoje 420px, sem subtítulo, botão sem variante |
| AC-8 | TL-02 fiel: `page-header` + grid de cards, card inteiro clicável, meta secundária, badge de status | Sim | Meta "N tarefas ativas · N impedidas" pode não existir na API — verificar no Canvas |
| AC-9 | TL-03/03b fiéis: raia como container externo, colunas dentro, card com badge-topo/título/avatar, contador na coluna | Sim | Inverte a hierarquia atual; contador por coluna a computar |
| AC-10 | Feedback de drag: outline verde tracejado no destino válido, esmaecimento no inválido, rótulo "Permitido"/"Sem transição" no header | Sim | Hoje azul primário e sem rótulo; semântica já correta |
| AC-11 | Card impedido: borda esquerda 4px warning + badge "Impedido"; cards normais sem borda esquerda | Sim | Hoje todo card tem borda esquerda primária |
| AC-12 | Erro de transição bloqueada visível no contexto do board com `role="alert" aria-live="assertive"` | Parcial | Toast fixo no canto pode ficar fora de vista durante scroll horizontal |
| AC-13 | TL-04/05/06 fiéis: drawer, modal com `form-field`/`form-error`, modal de confirmação com mensagem de impacto e bloqueio por permissão | Parcial | Não inspecionados em profundidade nesta fase — contexto de fronteira |
| AC-14 | TL-07 fiel: `kpi-grid` com 3 KPIs + gráfico de barras vertical + filtro de período por presets ("Últimos 30/90 dias") | Sim | Hoje 2 cartões soltos + tabela com barras horizontais e dois `datetime-local`; 3º KPI ("concluídas no período") pode não existir na API |
| AC-15 | TL-08/09/10 fiéis: abas, tabelas no padrão do protótipo, toggles, estados vazios | Parcial | Não inspecionados em profundidade — contexto de fronteira, ~500 linhas em `admin/page.tsx` |
| AC-16 | Estados por tela conforme matriz da seção 6 (idle/loading/preenchido/erro/sucesso/vazio) | Parcial | Taxonomia atual não é rastreável contra a matriz; estado `sucesso` ausente em várias telas |
| AC-17 | Textos da UI em pt_BR acentuado | Sim | Toda a UI está sem acentuação |
| AC-18 | Sidebar visível em 1024px e 1280px+ | Sim | Hoje ocultada abaixo de 1024px |
| AC-19 | WCAG AA: foco visível, labels ARIA em modais/formulários, `aria-live="polite"` nas notificações | Parcial | Foco e `aria-live` ok; auditoria de labels de formulário pendente |
| AC-20 | Toggle de densidade TL-03/TL-03b usa `.btn`/`aria-pressed` do design system; card expandido segue a composição de TL-03b | Sim | Decisão do usuário (2026-09-04): toggle mantido, compacto como padrão |

**Cobertura**: 13 de 20 endereçáveis integralmente, 7 parciais (3 por inspeção pendente de fronteira, 4 por lacuna de dado ou de auditoria), 0 bloqueadas.
