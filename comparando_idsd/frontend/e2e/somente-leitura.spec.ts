import { test, expect } from '@playwright/test'
import { entrarComo, semearProjeto, criarTarefa } from './suporte/cenario'

/**
 * SCN-015.2 — o gestor le e nao interfere.
 *
 * C-02 virou restricao estrutural, e nao acordo de uso. O backend ja recusa
 * toda escrita para quem so tem `LER`, e isso e o que sustenta a garantia. Mas
 * a restricao tambem e uma promessa de interface: oferecer um botao que sempre
 * responde 403 e pior do que nao oferecer — ensina a pessoa que o sistema esta
 * quebrado, e nao que a acao nao lhe cabe. Este cenario e `e2e` porque o que se
 * verifica e a tela inteira montada sob um papel, e nao uma rota por vez.
 */

test.beforeEach(async ({ page }) => {
  await semearProjeto('Alfa', ['ana'])
  await semearProjeto('Alfa', ['denis:gestor'])
  await criarTarefa('Alfa', 'Ajustar o relatorio')
  await entrarComo(page, 'denis')
})

test('o painel de andamento e o board sao legiveis para o gestor', async ({ page }) => {
  await page.goto('/projetos/alfa/andamento')
  await expect(page.getByRole('table', { name: /andamento/i })).toBeVisible()

  await page.goto('/projetos/alfa/board')
  await expect(page.getByRole('article', { name: /Ajustar o relatorio/ })).toBeVisible()
})

test('nenhuma acao de escrita e oferecida ao gestor no board', async ({ page }) => {
  await page.goto('/projetos/alfa/board')

  await expect(page.getByRole('button', { name: /nova tarefa/i })).toHaveCount(0)
  await expect(page.getByRole('button', { name: /assumir/i })).toHaveCount(0)
  await expect(page.getByRole('button', { name: /sinalizar impedimento/i })).toHaveCount(0)

  const cartao = page.getByRole('article', { name: /Ajustar o relatorio/ })
  // Nem por arrasto: o cartao nao e arrastavel para quem so le.
  await expect(cartao).toHaveAttribute('aria-grabbed', 'false')
  await expect(cartao).not.toHaveAttribute('draggable', 'true')
})

test('o painel nao oferece recorte por pessoa ao gestor', async ({ page }) => {
  await page.goto('/projetos/alfa/andamento')

  // C-03: a visibilidade concedida ao gestor e sobre o fluxo, e nunca sobre
  // quem fez o que em quanto tempo. E o ponto em que a restricao mais seria
  // testada em uso real.
  await expect(page.getByLabel(/respons|pessoa|usu[aá]rio/i)).toHaveCount(0)
  await expect(page.getByRole('columnheader', { name: /respons|pessoa/i })).toHaveCount(0)
})

test('o gestor nao alcanca a tela de configuracao do fluxo', async ({ page }) => {
  await page.goto('/projetos/alfa/board')
  await expect(page.getByRole('link', { name: /configurar/i })).toHaveCount(0)

  // Digitar a URL tambem nao serve: a tela nao pode ser a unica guarda.
  await page.goto('/projetos/alfa/configuracao')
  await expect(page.getByRole('heading', { name: /sem permiss/i })).toBeVisible()
})
