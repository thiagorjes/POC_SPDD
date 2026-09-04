# Princípios de Design / UI-UX (transversal a frontend)

> Método e decisões de design **agnósticos de framework**. Vale para qualquer stack web
> (Next.js, React SPA, etc.). A materialização concreta — valores de token, primitivos,
> mecânica de composição — fica em `frontend/<stack>/design-system.md`.
> Base: engenharia reversa de `transacional-ibanking-web` (2026-09-01).

## 1. Princípios

1. **Tokens semânticos primeiro.** Componentes consomem papéis (`primary`, `secondary`,
   `ghost`, `destructive`, `ring`, `background`/`foreground`), nunca cor crua. Paletas de
   marca (rampas) só existem para *derivar* tokens, não para uso direto em superfície/texto.
2. **Headless + composição.** Comportamento e acessibilidade vêm de primitivos headless;
   aparência vem de utilitários de estilo organizados por variante. Sem biblioteca de UI
   opinada e fechada; sem 100% custom.
3. **`className` sempre componível.** Toda API pública de componente aceita `className` (e
   `*Class` para subpartes) e resolve conflito de forma determinística.
4. **Acessível por padrão.** Ícone sem texto exige rótulo acessível (idealmente reforçado
   pelo sistema de tipos). Estados `disabled`/`busy`, `role` semântico, foco visível
   padronizado e navegação por teclado são requisito, não acabamento.
5. **Alvo de acessibilidade explícito: WCAG 2.1 AA.** O verificador de a11y roda no
   catálogo de componentes e deve evoluir de "aviso" para "erro" no CI.
6. **Desktop-first** (para produtos cujo uso dominante é desktop): o layout base mira a
   viewport ampla e adapta para baixo. Documentar o breakpoint dominante por stack.
7. **Localização pt-BR:** textos em português, datas `dd/mm/aaaa`, moeda BRL, locale de
   calendário pt-BR.
8. **Catálogo de componentes é contrato vivo.** Todo primitivo tem entrada no catálogo
   cobrindo variantes e os dois temas; o catálogo é a referência visual, não a tela real.
9. **Tema claro e escuro** em paridade: todo token semântico tem par claro/escuro e todo
   componente novo funciona nos dois **consumindo só tokens semânticos**.

## 2. Categorias de token (estrutura, não valores)

Cada stack define os valores em seu `design-system.md` / `design-tokens.json`, mas a
**estrutura** é comum:

| Categoria | Forma |
|---|---|
| Cor semântica | pares claro/escuro por papel (`background`, `foreground`, `primary(+hover/active/foreground)`, `secondary(+…)`, `ghost`, `destructive(+…)`, `ring`) |
| Cor de marca | rampas nomeadas (`50…950`) — insumo para tokens, não uso direto |
| Tipografia | 1 família + fallback; **pesos restritos** (ex.: 400/500/600); **escala fechada** de N degraus com line-height fixo por degrau |
| Espaçamento | escala base + tokens nomeados para os passos recorrentes |
| Raio | escala fechada; raio-padrão por tipo de componente (ex.: botão vs. card) |
| Elevação | tokens de sombra quando a linguagem de profundidade existir (senão, documentar o uso ad-hoc como dívida) |
| Breakpoints | nomeados; declarar qual é o dominante e a direção de adaptação |
| Movimento | keyframes e durações nomeados; transições padronizadas por interação |

## 3. Processo para estender

1. **Cor nova?** Primeiro verificar se um token semântico já cobre. Se for um papel novo e
   recorrente, adicionar o **par claro/escuro** na origem dos tokens e expor ao sistema de
   estilo. Só então usar.
2. **Componente novo?** Primitivo headless (se há comportamento) + camada de variantes +
   `className` componível + só tokens semânticos. Seguir a anatomia dos primitivos
   existentes (mesmas alturas de controle, raios, padrão de foco).
3. **Adicionar entrada no catálogo** cobrindo variantes e os dois temas.
4. **Atualizar** o `design-system.md` da stack e regenerar o `design-tokens.json` se o tema
   mudou.

## 4. O que NÃO entra aqui

- Valores concretos de token (hex, px) → `frontend/<stack>/design-tokens.json`
- Inventário e API de componentes de uma stack → `frontend/<stack>/design-system.md`
- Telas, rotas e componentes de rota de um app → artefato do **sistema** (`screen-inventory`),
  nunca nas guidelines
- Dívidas técnicas do design de um codebase específico → backlog do sistema

## Checklist

- [ ] Superfícies e textos usam token semântico, nunca cor de marca crua
- [ ] Comportamento vem de primitivo headless; aparência por variantes + `className`
- [ ] Ícone sem texto tem rótulo acessível; foco visível; teclado ok
- [ ] Componente novo funciona em claro e escuro só com tokens semânticos
- [ ] Entrada no catálogo criada/atualizada (variantes + 2 temas)
- [ ] Token novo = par claro/escuro na origem antes do uso
- [ ] pt-BR / `dd/mm/aaaa` / BRL
