import { test, expect } from '@playwright/test'
import { entrarComo, semearProjeto } from './suporte/cenario'

/**
 * RF-001 — entrada no sistema.
 *
 * Estes dois cenarios sao `e2e` e nao poderiam ser outra coisa: o fluxo de
 * autenticacao atravessa navegador, provedor de identidade e aplicacao, e o que
 * se verifica e o redirecionamento de ida e volta. Rebaixa-los a integracao
 * verificaria o resource server recebendo um token que alguem ja tinha — nao o
 * caminho pelo qual a pessoa o obtem.
 */

test.describe('SCN-001.1 — quem nao esta autenticado e levado ao provedor', () => {
  test('o acesso direto a uma rota interna passa pelo provedor e volta ao destino', async ({
    page,
  }) => {
    await page.goto('/projetos')

    // ADR-003: quem autentica e o provedor. A aplicacao nao tem tela de senha,
    // e a ausencia dela e parte do que se verifica aqui.
    await expect(page).toHaveURL(/\/realms\/idsd\/protocol\/openid-connect\/auth/)
    await expect(page.getByLabel(/senha/i)).toBeVisible()

    await entrarComo(page, 'ana')

    // Voltar para a raiz em vez do destino pedido e o modo de falha comum, e
    // ele so aparece quando alguem chega por link.
    await expect(page).toHaveURL(/\/projetos$/)
  })
})

test.describe('SCN-001.2 — a sessao autenticada identifica a pessoa', () => {
  test('o nome vindo do provedor aparece na aplicacao, sem cadastro proprio', async ({ page }) => {
    await semearProjeto('Alfa', ['ana'])
    await entrarComo(page, 'ana')

    await page.goto('/projetos')
    await expect(page.getByRole('button', { name: /Ana/ })).toBeVisible()

    // Nao ha cadastro local de pessoa a preencher: o provisionamento acontece
    // a partir da identidade federada, e uma tela pedindo dados aqui indicaria
    // um segundo cadastro que ADR-003 recusa.
    await expect(page.getByRole('heading', { name: /complete seu cadastro/i })).toHaveCount(0)
  })

  test('sair encerra a sessao e o acesso volta a exigir autenticacao', async ({ page }) => {
    await entrarComo(page, 'ana')
    await page.goto('/projetos')

    await page.getByRole('button', { name: /Ana/ }).click()
    await page.getByRole('menuitem', { name: /sair/i }).click()

    await page.goto('/projetos')
    await expect(page).toHaveURL(/openid-connect\/auth/)
  })
})
