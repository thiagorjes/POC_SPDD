import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

import { entrarComo } from '../suporte/cenario'

/**
 * Verificação **além dos cenários congelados**.
 *
 * Ela não realiza cenário nenhum e não entra na contagem de cobertura: existe
 * porque o critério de aceite 5 da TASK-01.7 exige auditoria automatizada de
 * acessibilidade AA, e DDR-005 tornou o nível obrigatório — sem execução, o
 * critério só poderia ser marcado por inspeção, que é exatamente o modo de
 * cumprimento sem poder de falha que as revisões anteriores recusaram.
 *
 * O recorte é `wcag2a`/`wcag2aa`/`wcag21a`/`wcag21aa`: regras de nível AAA e as
 * de boa prática do axe ficam de fora, porque afirmar mais do que a norma
 * exigida produz reprovação que ninguém decidiu.
 */
function auditoria(pagina: Parameters<typeof AxeBuilder>[0]['page']) {
  return new AxeBuilder({ page: pagina }).withTags([
    'wcag2a',
    'wcag2aa',
    'wcag21a',
    'wcag21aa',
  ])
}

test('TL-01 — a tela de entrada nao tem violacao de nivel AA', async ({ page }) => {
  await page.goto('/entrar')
  const resultado = await auditoria(page).analyze()
  expect(resultado.violations).toEqual([])
})

test('TL-01 — a recusa de autenticacao nao tem violacao de nivel AA', async ({ page }) => {
  await page.goto('/entrar?erro=provedor')
  const resultado = await auditoria(page).analyze()
  expect(resultado.violations).toEqual([])
})

test('TL-02 — a lista de projetos nao tem violacao de nivel AA', async ({ page }) => {
  await entrarComo(page, 'ana')
  await page.goto('/projetos')
  const resultado = await auditoria(page).analyze()
  expect(resultado.violations).toEqual([])
})

test('TL-11 — o painel de novo projeto nao tem violacao de nivel AA', async ({ page }) => {
  await entrarComo(page, 'admin')
  await page.goto('/projetos/novo')
  const resultado = await auditoria(page).analyze()
  expect(resultado.violations).toEqual([])
})
