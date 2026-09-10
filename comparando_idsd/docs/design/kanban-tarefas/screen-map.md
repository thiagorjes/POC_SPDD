# Screen Map — kanban-tarefas

_Gerado em 2026-09-09 pelo prototipador, na etapa `/design`._

- **Design Brief (fonte de verdade desta etapa):** [`../kanban-tarefas-design-brief.md`](../kanban-tarefas-design-brief.md)
- **Shape Brief:** [`../../shape/kanban-tarefas-brief.md`](../../shape/kanban-tarefas-brief.md)
- **Exploração de solução:** [`../../solution/kanban-tarefas-solution.md`](../../solution/kanban-tarefas-solution.md)
- **Fonte de verdade visual:** `requirements/guidelines/frontend/nextjs/design-system.md` +
  `design-tokens.json` da mesma coleção (**DDR-004**)
- **Tokens consumidos por este protótipo:** [`design-tokens.json`](design-tokens.json)

> `requirements/design/kanban-tarefas/` (2026-08-25) **não** é fonte de verdade.
> Foi consultado apenas como referência de layout e densidade; paleta e tokens de
> lá não valem.

---

## 1. Como abrir

Abra qualquer arquivo de `prototypes/` direto no navegador — não há build. Os
protótipos compartilham `proto.css` (tokens e componentes) e `proto.js`
(tema, contadores, arrasto, teclado, recusa concorrente).

**Tema:** o botão na topbar alterna claro/escuro e persiste a escolha. As duas
variantes usam exclusivamente variáveis semânticas; nenhum hexadecimal cru
existe fora dos blocos `:root` e `.dark` de `proto.css`.

**Ponto de entrada sugerido para revisão com o time:**
`TL-03` → `TL-04` → `TL-06` → `TL-07`. É o percurso que carrega as três
decisões que o protótipo existe para testar.

---

## 2. Inventário

| ID | Tela | Arquivo | Rota | Persona principal |
| --- | --- | --- | --- | --- |
| TL-01 | Entrada autenticada | [`prototypes/TL-01-entrada-autenticada.html`](prototypes/TL-01-entrada-autenticada.html) | `/entrar` | Todas |
| TL-02 | Meus projetos | [`prototypes/TL-02-meus-projetos.html`](prototypes/TL-02-meus-projetos.html) | `/projetos` | Todas |
| TL-03 | Board — cartão compacto | [`prototypes/TL-03-board-cartao-compacto.html`](prototypes/TL-03-board-cartao-compacto.html) | `/projetos/:id/board` | Desenvolvedor, reviewer, liderança |
| ~~TL-03b~~ | Board — cartão expandido. **Variação descartada em DM-02 (2026-09-09).** O arquivo [`prototypes/TL-03b-board-cartao-expandido.html`](prototypes/TL-03b-board-cartao-expandido.html) permanece como registro da alternativa, e não deve ser tomado como referência de implementação | — | — |
| TL-04 | Detalhe da tarefa (painel lateral) | [`prototypes/TL-04-detalhe-da-tarefa.html`](prototypes/TL-04-detalhe-da-tarefa.html) | painel sobre o board | Desenvolvedor, reviewer, liderança |
| TL-05 | Nova tarefa | [`prototypes/TL-05-nova-tarefa.html`](prototypes/TL-05-nova-tarefa.html) | painel sobre o board | Desenvolvedor, PO |
| TL-06 | Minha fila | [`prototypes/TL-06-minha-fila.html`](prototypes/TL-06-minha-fila.html) | `/minha-fila` | Reviewer, validador, desenvolvedor |
| TL-07 | Andamento e tempo por etapa | [`prototypes/TL-07-andamento-tempo-por-etapa.html`](prototypes/TL-07-andamento-tempo-por-etapa.html) | `/projetos/:id/andamento` | PO, gestor de outro time |
| TL-08 | Configuração do fluxo | [`prototypes/TL-08-configuracao-do-fluxo.html`](prototypes/TL-08-configuracao-do-fluxo.html) | `/projetos/:id/config/fluxo` | Responsável pela configuração |
| TL-09 | Participação e permissões | [`prototypes/TL-09-participacao-e-permissoes.html`](prototypes/TL-09-participacao-e-permissoes.html) | `/projetos/:id/config/participacao` | Responsável pela configuração |
| TL-10 | Reabrir tarefa concluída | [`prototypes/TL-10-reabrir-tarefa-concluida.html`](prototypes/TL-10-reabrir-tarefa-concluida.html) | diálogo sobre TL-04 | Product Owner, e somente ele |
| TL-11 | Novo projeto | [`prototypes/TL-11-novo-projeto.html`](prototypes/TL-11-novo-projeto.html) | painel sobre `/projetos` | Administração global, e somente ela |

Compartilhados: `prototypes/proto.css`, `prototypes/proto.js`.

---

## 3. Fluxos de navegação materializados

**Handoff — a razão de E-02 estar na direção**

```
TL-01 → TL-02 → TL-03 (dev localiza a tarefa que assumiu)
      → TL-04 → indica avanço de etapa
      → o cartão reaparece na etapa de destino, aguardando tomada,
        com o contador de espera correndo à vista
      → o reviewer vê o item surgir em TL-06 sem ter aberto o board
      → TL-06 → assume → TL-04 com ele como responsável,
        e a contagem de espera encerrada
```

No protótipo, o trecho "indica avanço" é executável em TL-03: arraste o cartão
ou dê foco nele e tecle <kbd>M</kbd>. O cartão muda de coluna e a região de
anúncio informa que a tarefa passou a aguardar tomada.

**Impedimento — permanente, não é alerta que passa**

```
TL-03 (cartão PC-297 com impedimento aberto, contador próprio correndo)
      → TL-04 (bloco de impedimento aberto, motivo, desfecho pendente)
      → a tarefa continua movendo de etapa e podendo ser assumida
        (RN-009, RN-033) — só concluir é recusado (RN-011)
      → quem desbloqueia vê o mesmo destaque em TL-06,
        na seção "Impedimentos que esperam desbloqueio por você"
      → resolve com desfecho registrado
      → a marca some; condição e etapa ficam como estavam (RN-032)
```

A marca de impedimento é a **terceira dimensão** de RN-002, ortogonal à etapa e à
condição de trabalho. Nenhuma tela a exibe como condição, e nenhuma ação além do
registro do desfecho a apaga. Foi a emenda do PRD v1.1 que a promoveu, e é ela
que corrige o achado INC-09.

**Exceção — reabertura**

```
TL-03 (cartão concluído) → TL-10 (diálogo, visível só para o PO)
```

**Partida — o sistema recém-instalado sai do zero por dentro do produto**

```
TL-01 → TL-02 no estado vazio da administração global
        ("ainda não existe projeto neste sistema")
      → TL-11 (painel): nome, descrição e a primeira administradora do projeto,
        nomeada na mesma operação (RN-037)
      → sucesso: o projeto existe e não aceita tarefa, porque nasce sem fluxo
        (RN-038)
      → TL-08 pelo botão "Configurar o fluxo agora"
```

Os dois passos são obrigatórios e nada, fora do estado de sucesso de TL-11 e da
marca no cartão em TL-02, lembraria do segundo. Desde a emenda de 2026-09-10 que
fechou INC-22 as duas sinalizações **são requeridas por RN-038**, e não escolha
de desenho: a marca no cartão tem cenário congelado próprio (SCN-002.4) e chega
à tela pelo campo `fluxoConfigurado` da resposta, nunca por inferência do
cliente. É o custo que RN-038 declara, e
essas duas superfícies existem para pagá-lo. Quem cria não vira participante
(RN-037), então o projeto criado não aparece em "Meus projetos" para ela: o
alcance é global e a lista o marca como tal, para que escopo não se confunda com
participação.

---

## 4. Estados por tela, e onde vê-los

| Tela | idle | loading | preenchido | erro | sucesso | vazio |
| --- | --- | --- | --- | --- | --- | --- |
| TL-01 | ✅ coluna 1 | ✅ coluna 2 | — | ✅ coluna 3, falha de autenticação | ✅ redireciona a TL-02 | — |
| TL-02 | ✅ | ✅ | ✅ 3 projetos + cartão de projeto sem fluxo configurado | ✅ | — | ✅ dois vazios distintos: "nenhum projeto" (sem saída) e "ainda não existe projeto" da administração global (com saída para TL-11) |
| TL-03 | ✅ | ✅ | ✅ board completo | ✅ transição recusada (arrasto/menu) · ✅ ação concorrente recusada | — | ✅ etapa Validação sem tarefa |
| TL-04 | ✅ | ✅ | ✅ três dimensões em campos separados | ✅ recusa por ação concorrente · ✅ Concluir e Encerrar sem conclusão indisponíveis por impedimento aberto (RN-011, SCN-012.4) · **Mover permanece disponível** (RN-009) | ✅ | — |
| TL-05 | ✅ | ✅ bloco "enviando" | ✅ | ✅ validação de título | ✅ bloco de sucesso | — |
| TL-06 | ✅ | ✅ | ✅ 6 itens + impedimentos, um deles assumível com impedimento aberto (SCN-007.4) | ✅ recusa concorrente no botão Assumir | ✅ bloco "ao assumir" | ✅ "nada aguarda tomada por você" |
| TL-07 | ✅ | ✅ | ✅ | ✅ | — | ✅ **ainda não medido**, distinto de zero |
| TL-08 | ✅ | ✅ | ✅ | ✅ sem etapa terminal · ✅ remover etapa com tarefas | ✅ fluxo salvo | ✅ fluxo não configurado |
| TL-09 | ✅ | ✅ | ✅ 6 participações | ✅ remover quem tem tarefa assumida (B-10) | ✅ permissão alterada | ✅ só o configurador participa |
| TL-10 | ✅ | ✅ | — | ✅ sem permissão | ✅ reaberta | — |
| TL-11 | ✅ | ✅ bloco "criando o projeto" | ✅ | ✅ nome ausente · ✅ administradora não escolhida, com a recusa do servidor declarada ao lado | ✅ criado, nomeando o segundo passo obrigatório e oferecendo TL-08 | — |

Os três estados que o material de agosto não tinha como revelar:

| Estado | Onde | O que provar |
| --- | --- | --- |
| Aguardando tomada com contador contínuo | TL-03, TL-06 | Que a espera é legível sem clique e sem limiar (QD-03) |
| Ação concorrente recusada | TL-03, TL-04, TL-06 | Que quem perdeu vê o estado atual e nada é sobrescrito (B-03) |
| Ainda não medido | TL-07 | Que ausência de linha de base não vira zero (B-01) |
| Impedimento como dimensão à parte | TL-03, TL-04, TL-06 | Que a marca coexiste com qualquer condição, atravessa a movimentação e não impede a tomada (RN-002, RN-032, RN-033) |

---

## 5. Como as decisões aparecem na tela

| Decisão | Materialização |
| --- | --- |
| **DDR-004** — design system Next.js como fonte visual | Todo valor de `proto.css` sai de `design-tokens.json` da coleção. Primária `#004B8D` como token, Inter, escala de 8 degraus, pesos 400/500/600, raio base 16, alturas 44/36, espaçamento base 16 |
| **DDR-005** — WCAG 2.1 AA obrigatório | Foco visível `ring-2 ring-offset-2` nunca suprimido; ícone sem texto sempre com `aria-label`; `role="status"`/`aria-live="polite"` para anunciar chegada, impedimento e recusa; skip link; rótulo em todo campo; `aria-busy` em carregamento |
| **DDR-005** — teclado no board | `Tab` até o cartão, <kbd>M</kbd> abre o menu de movimentação. Só as etapas alcançáveis são acionáveis; as demais trazem a razão da recusa. A lista é a mesma que o arrasto usa — uma fonte, duas rotas |
| **DDR-002** — arrasto com destaque | Ao iniciar o arraste, etapas alcançáveis ganham borda tracejada e realce; as demais esmaecem. Soltar fora recusa, o cartão volta e a razão é informada sem interromper |
| **DDR-006** — espera de tomada de primeira classe | Contador no cartão (TL-03) **e** tela própria que atravessa projetos (TL-06) |
| **QD-01** — papel semântico de impedimento | Par claro/escuro novo, distinto do destrutivo, usado em badge, cartão, faixa permanente, contador, barra de TL-07 e faixa de exceção de TL-10 |
| **QD-03** — contador contínuo | Sempre visível, sem limiar, atualizando enquanto a página estiver aberta. Ruído mitigado por hierarquia visual, nunca por ocultação |
| **Q-08** — raia é agrupamento | Raia aparece como faixa dentro da etapa e como chip no cartão; o menu de movimentação declara explicitamente que ela não restringe transição |
| **Q-09** — espera separada do trabalho | Cartão, detalhe e TL-07 sempre exibem espera de tomada e tempo de permanência como grandezas distintas |
| **Q-10** — sem responsável nomeado | Tarefa aguardando tomada nunca mostra dono; TL-06 é fila por elegibilidade, o que torna a recusa concorrente rotina |
| **Q-06** — reabertura restrita ao PO | TL-10 existe com faixa de exceção e a variante "sem permissão" renderizada |

---

## 6. Restrições estruturais — como conferir que foram respeitadas

**Nenhuma agregação de tempo por pessoa.** Confira TL-07: as duas tabelas cortam
por etapa e por condição. Não há coluna, filtro, gráfico, ranking ou ordenação
por pessoa em nenhuma das 11 telas — nem para o Product Owner. Em TL-06 o
contador pertence à tarefa, não a quem vai assumi-la. A `caption` da tabela de
tempo por etapa declara a ausência como restrição, para que ninguém a leia como
filtro que faltou.

**Nenhuma ação de escrita para acesso de leitura.** Em TL-07, o seletor
*Ver como* na topbar alterna entre gestor de outro time (leitura, padrão) e
Product Owner. No papel de leitura as ações de escrita não são renderizadas —
não existe botão desabilitado esperando permissão. TL-09 documenta o que
"leitura" significa em cada tela.

---

## 7. Perguntas que este protótipo existe para responder

| # | Pergunta | Onde decidir olhando |
| --- | --- | --- |
| **QD-04** | Fila ordenada por espera ou agrupada por projeto? | TL-06 traz as duas renderizadas: (A) espera decrescente no topo da página, (B) agrupada por projeto após o divisor. A põe o mais parado primeiro, que serve à dor; B é mais previsível para quem trabalha em um projeto só |
| **H-01** | O board vira o lugar de trabalho diário? | TL-06 é o teste. Se o reviewer não reconhecer a fila como o lugar onde descobre que a vez chegou, a direção depende de um hábito que ninguém tem |

---

## 8. Limites deste protótipo

- Não há backend, persistência nem validação real. Mover um cartão em TL-03 não
  reavalia transição contra estado corrente — isso é comportamento, e está na
  exploração de solução.
- Os contadores partem de valores plantados e avançam enquanto a página estiver
  aberta; servem para julgar legibilidade, não precisão.
- Não há numeração de RF. O inventário é derivado dos seis fluxos da exploração
  de solução; requisito numerado nasce no `/prd`, que ainda não rodou.
- Abaixo de 1024px não é alvo. A adaptação implementada vai de 1280px a 1024px:
  sidebar colapsa, colunas estreitam, grades de duas e três colunas empilham.
- O papel semântico de impedimento ainda **não** existe na coleção compartilhada.
  Adicioná-lo a `globals.css` e ao `tailwind.config.ts` antes do primeiro uso é
  trabalho a criar no `/tasks` (QD-01), e afeta outros sistemas governados por ela.
