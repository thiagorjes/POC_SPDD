import { defineConfig, globalIgnores } from 'eslint/config'
import nextVitals from 'eslint-config-next/core-web-vitals'
import nextTypescript from 'eslint-config-next/typescript'

/**
 * Configuração de lint (ACH-17 da revisão de TASK-01.7).
 *
 * O `next lint` saiu no major 16, e o script do `package.json` apontava para
 * ele: o verificador que o `definition-of-done.md` de `frontend/nextjs` nomeia
 * era inexecutável, e por isso o item nunca reprovou coisa alguma. Aqui o
 * ESLint é invocado direto, em flat config, que é o caminho que a própria
 * documentação da versão instalada prescreve.
 *
 * `core-web-vitals` sobe a erro as regras que afetam as métricas de campo, e
 * `typescript` acrescenta as do `typescript-eslint`. O DoD proíbe `any`,
 * `@ts-ignore` e `eslint-disable` sem justificativa — as duas primeiras são
 * regras destas coleções, e a terceira é obrigação de revisão.
 */
export default defineConfig([
  ...nextVitals,
  ...nextTypescript,
  globalIgnores([
    '.next/**',
    'out/**',
    'build/**',
    'next-env.d.ts',
    'playwright-report/**',
    'test-results/**',
  ]),
])
