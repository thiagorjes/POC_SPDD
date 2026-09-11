import type { Config } from 'tailwindcss'

/**
 * Os papéis semânticos vêm de `docs/design/kanban-tarefas/design-tokens.json`,
 * que por DDR-004 realiza o design system da coleção `frontend/nextjs`. Aqui
 * eles são apenas apontados para as variáveis CSS declaradas em
 * `globals.css` — os valores literais moram lá, num lugar só, para que o tema
 * escuro seja troca de variável e não segunda tabela de cores.
 *
 * `impedimento` é papel próprio e não reuso de `destructive` (DDR-007, QD-01):
 * impedimento é condição legítima do trabalho, não falha de quem registra.
 */
const config: Config = {
  darkMode: ['class', '[data-tema="escuro"]'],
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        fundo: 'var(--cor-fundo)',
        superficie: 'var(--cor-superficie)',
        texto: 'var(--cor-texto)',
        'texto-secundario': 'var(--cor-texto-secundario)',
        borda: 'var(--cor-borda)',
        primario: 'var(--cor-primario)',
        'primario-contraste': 'var(--cor-primario-contraste)',
        secundario: 'var(--cor-secundario)',
        destrutivo: 'var(--cor-destrutivo)',
        impedimento: 'var(--cor-impedimento)',
        info: 'var(--cor-info)',
        sucesso: 'var(--cor-sucesso)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        sm: 'var(--raio-sm)',
        DEFAULT: 'var(--raio-md)',
        md: 'var(--raio-md)',
        lg: 'var(--raio-lg)',
      },
      height: {
        controle: 'var(--altura-controle)',
        'controle-sm': 'var(--altura-controle-sm)',
      },
      width: {
        'painel-lateral': 'var(--largura-painel-lateral)',
      },
    },
  },
  plugins: [],
}

export default config
