# Design System — Frontend Next.js (Banestes)

> Materializa [`../_shared/design-principles.md`](../_shared/design-principles.md) para a
> stack Next.js + Tailwind + Radix. Igual para **qualquer app web Next.js do Banestes** —
> não é específico do Internet Banking.
> Extraído por engenharia reversa de `transacional-ibanking-web` em 2026-09-01.
> **Fonte de verdade dos valores:** o repo do design system (hoje: `tailwind.config.ts` +
> `src/app/globals.css` + `src/components/` do `transacional-ibanking-web`). Este documento e
> [`design-tokens.json`](design-tokens.json) são derivados — regenerar quando o theme ou as
> CSS custom properties mudarem. `screen-inventory` e componentes de rota **não** entram aqui.

## Stack visual

Next.js 16 (App Router) · React 19 · Tailwind CSS 3.4 · Radix UI (headless) · CVA ·
`tailwind-merge` · shadcn/ui (estilo *new-york*, baseColor *zinc*, CSS variables).
Tema claro (default) e escuro via classe `.dark` (`darkMode: ['selector']`).
Catálogo vivo: Storybook 10 (`.storybook/`) com addons `a11y` e `themes`.

## Mecânica de composição

- Comportamento: primitivo Radix. Aparência: classes Tailwind organizadas com
  `class-variance-authority` (CVA). Mesclagem: `cn()` = `clsx` + `tailwind-merge`
  (`components/_lib/utils.ts` → no app fica em `@/lib/utils`).
- Toda API pública aceita `className`; subpartes aceitam `containerClass`/`inputClass`/`contentClass`.
- `'use client'` nos primitivos interativos e compostos com estado; ausente nos
  apresentacionais (`Card`, `Alert`, `Badge`, `Divider`, `LabelValue`).
- Import agregado: `import { Button, Input } from '@/components/ui'` (barrel).

## Tokens

Valores completos e legíveis por máquina em [`design-tokens.json`](design-tokens.json).

### Cor semântica (CSS custom properties, `:root` e `.dark`)

Papéis: `background`/`foreground`, `primary` (+`hover`/`active`/`foreground`), `secondary`
(+`hover`/`active`/`foreground`/`border`), `ghost-foreground` (+`hover`/`active`),
`destructive` (+`hover`/`active`/`foreground`/`border`), `ring`/`ring-destructive`.
Ação primária = `#004b8d` (azul institucional). Todos com par claro/escuro.
Consumir como `bg-primary`, `text-secondary-foreground`, `ring-ring` — **nunca hex cru**.

### Cor de marca (rampas — insumo, não uso direto)

`brand.blueDark` (base `500 = #004B8D`), `brand.ocean` (base `500 = #007CC3`),
`grayDark` (`50 = #F5F5F6` … `950 = #0C111D`, base do tema dark). 11 tons cada.

### Dívida de tokenização

Cruas ainda em uso localizado: `border-gray-300` (borda de input), `text-gray-500/600/700`,
`bg-blue-50` (badge default), `bg-red-50`/`text-red-600` (erro), `bg-emerald-50`/`text-emerald-700`
(badge segurança). Candidatas a `info`/`success`/`input-border`/`muted-foreground`.

### Tipografia

Inter Variable (`next/font/local`, `src/assets/fonts/`), `--font-inter`, fallback
`ui-sans-serif, system-ui, sans-serif`, classe `font-sans`. `html { font-size: 16px }`.
Pesos restritos: `font-regular` 400 · `font-medium` 500 · `font-semibold` 600.
Escala fechada (8 degraus, px / line-height): `xs` 12/18 · `sm` 14/20 · `base` 16/24 ·
`lg` 18/28 · `xl` 20/30 · `2xl` 24/32 · `3xl` 30/38 · `4xl` 36/44.

### Espaçamento, raio, elevação

- Espaçamento: escala default do Tailwind + tokens custom (base `--spacing: 16px`):
  `sm` 6 · `md` 8 · `lg` 12 · `2xl` 20 · `3xl` 24 · `4xl` 32 · `5xl` 40 · `6xl` 44 ·
  `7xl` 64 · `8xl` 80 (px). Container de página: `p-4xl`.
- Raio: base `--radius: 16px`. `sm` 2 · `md` 6 · `lg` 8 · `xl` 12 · `2xl` 16 · `full`.
  Botão = `rounded-lg`; Card/Badge/Alert = `rounded-xl`.
- Elevação: sem tokens custom. `shadow-sm` (inputs, alerts), `shadow-none` (Card),
  `shadow-xl` (popover do DatePicker). Padronizar em tokens se a profundidade crescer.

### Breakpoints e responsividade

Default do Tailwind (`sm` 640 · `md` 768 · `lg` 1024 · `xl` 1280 · `2xl` 1536), sem override.
**Desktop-first:** base para `≥ xl`, `max-xl:`/`max-lg:` adaptam para baixo.
Página padrão: coluna dupla em `xl` (`xl:w-[55%]` / `xl:w-[45%]`), empilhada abaixo.

### Movimento

Plugin `tailwindcss-animate`. Keyframes: `accordion-down/up`, `collapsible-down/up`
(0.2s ease-out). `transition-colors` em Button/Input; `duration-200` no chevron do Accordion.
Loading: `animate-spin` no spinner do Button; Lottie (`BanestesLottieAnimation`) para carga
em tela/inline.

## Dimensões de controle

| Elemento | Valor |
|---|---|
| Botão `md` (default) | altura 44 · pad-x 16 · texto `base` · ícone 24 · icon-only 48 |
| Botão `sm` | altura 36 · pad-x 12 · texto `sm` · ícone 20 · icon-only 36 |
| Input / Select trigger | altura 44 · pad-x 12 · texto `sm` · borda 1px `gray-300` · raio `md` |
| Navbar fixa | altura 80 (`h-[5rem]`), `border-b`, `z-50` |
| Container de conteúdo | `pt-[5rem]` + `p-4xl` |
| Ícones | 16 / 18 / 20 / 24 |
| Alvo de toque | mín. 36px; ações primárias 44px |

## Iconografia

Principal: `@phosphor-icons/react` (ícones nomeados, `size={n}` ou `size-*`, cor por `text-*`).
Dentro de alguns primitivos shadcn: `@radix-ui/react-icons`. SVGs locais em
`src/assets/icons/` (via SVGR); logos em `src/assets/logos/` (`BanestesLogo` fg `blue`|`white`).
Regra: ícone sem texto ⇒ `Button iconOnly` + `aria-label` (exigido pelo tipo).

## Tema claro / escuro

Classe `.dark` na raiz. Storybook alterna via `withThemeByClassName` (`light` sem classe /
`dark`). Componente novo funciona nos dois **só com tokens semânticos** — sem hex, sem
`grayDark.*`/`brand.*` direto em superfície/texto.

## Acessibilidade

Alvo **WCAG 2.1 AA**. Garantido hoje: `aria-label` obrigatório em botão icon-only (tipo TS);
`aria-disabled`/`aria-busy` em loading; `role="alert"` no `Alert`; foco visível
`focus-visible:ring-2 ring-offset-2`; labels via `htmlFor`/`id`; teclado via Radix.
A endereçar: promover `addon-a11y` de `todo` para `error`; auditar contraste das cores cruas.

## Componentes

Código reutilizável em [`components/`](components/) (`ui/` = primitivos, raiz = compostos,
`_lib/utils.ts` = `cn()`). Espelho de `src/components/` sem `__tests__`.

### Primitivos (`components/ui/`) — base shadcn *new-york*

`Button` (`variant`: primary/secondary/ghost/destructive · `size`: md/sm · `iconOnly` exige
`aria-label` · `asChild` · `isLoading`+`loadingLabel`), `Input` (`label`, `error`,
`leftElement`/`rightElement`, `showPasswordEye`), `InputAddon`, `InputOTP`, `Label`,
`Select`, `Checkbox`, `Switch`, `Card`, `Alert` (default/destructive), `Badge`
(default/outline/destructive), `Accordion`, `Collapsible`, `Tabs`, `Dialog`, `Sheet`
(top/right/bottom/left), `Drawer` (`vaul`), `Popover`, `DropdownMenu`, `Command` (`cmdk`),
`Tooltip`, `Table` (+ `@tanstack/react-table`), `Calendar` (`react-day-picker`, locale ptBR),
`Toaster` (`sonner`).

### Compostos (`components/`) — domínio bancário, reutilizáveis

`MaskInput` / `CpfCnpjMaskInput` / `DateInput` (`react-imask` + `Input`), `DatePicker`,
`SelectInput`, `Combobox`, `AsyncSelect` (query + loading/erro/vazio), `Stepper`, `Divider`,
`LabelValue` (comprovantes), `ErrorView` (+ retry), `BackNavigationBtn`, `LinkIconBtn`
(cartão-atalho h72), `TooltipIcon`, `ModuloSegurancaBadge`, `BanestesLogo`,
`BanestesLottieAnimation`, `ComposeProviders`.

> Componentes locais de rota (`src/app/**/_components/`) **não** fazem parte do design system —
> ficam no sistema. Inventário de telas: artefato do sistema (`screen-inventory.md`).

## Portar para um novo app Next.js

1. Copiar `components/ui/` + `_lib/utils.ts` (→ `@/lib/utils`).
2. Portar `tailwind.config.ts` (cores/fontSize/spacing/radius) + `globals.css` (`:root`/`.dark`).
3. Instalar deps: Radix + `class-variance-authority`, `clsx`, `tailwind-merge`,
   `tailwindcss-animate`, `@phosphor-icons/react`.
4. Copiar compostos conforme necessidade (dependem de `@tanstack/react-query`,
   `react-hook-form`, `dayjs`, `react-imask`).
5. Manter Storybook como contrato visual (portar/adicionar stories).

## Checklist

- [ ] Superfície/texto só com token semântico (`bg-primary`, `text-*-foreground`) — sem hex, sem `brand.*`/`grayDark.*`
- [ ] Componente novo = Radix + CVA + `cn()` + `className` componível
- [ ] Funciona em claro e escuro; story cobrindo variantes e 2 temas
- [ ] Ícone sem texto ⇒ `Button iconOnly` + `aria-label`
- [ ] Alturas 44/36, raio `lg`(botão)/`xl`(card), foco `ring-2 ring-offset-2`
- [ ] Tipografia dentro da escala de 8 degraus e pesos 400/500/600
- [ ] Token novo adicionado como par claro/escuro em `globals.css` + `tailwind.config.ts` antes do uso
