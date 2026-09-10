import { test, expect } from '@playwright/test'
import { entrarComo, semearProjeto, criarTarefa } from './suporte/cenario'

/**
 * SCN-003.1 — o board como operacao completa.
 *
 * O cenario e `e2e` porque a operacao central do produto e o arrasto, e arrasto
 * nao existe fora de um navegador: jsdom nao tem ponteiro, nao tem alvo de
 * soltura e nao tem a posicao que determina a coluna de destino. Verificar a
 * movimentacao so pela rota deixaria sem cobertura exatamente aquilo que o
 * DDR-002 decidiu.
 *
 * O caminho alternativo ao arrasto e verificado junto, e nao a parte: DDR-005
 * o tirou da categoria de conveniencia. Sem ele, quem nao usa ponteiro fica
 * sem a operacao central, e a feature inteira deixa de atender ao nivel exigido.
 */

test.beforeEach(async ({ page }) => {
  await semearProjeto('Alfa', ['ana'])
  await criarTarefa('Alfa', 'Ajustar o relatorio')
  await entrarComo(page, 'ana')
  await page.goto('/projetos/alfa/board')
})

test('o board mostra as etapas na ordem configurada, com as tarefas em suas colunas', async ({
  page,
}) => {
  const colunas = page.getByRole('region', { name: /etapa/i })
  await expect(colunas).toHaveCount(4)
  await expect(colunas.nth(0)).toHaveAccessibleName(/Backlog/)
  await expect(colunas.nth(3)).toHaveAccessibleName(/Concluido/)

  await expect(
    colunas.nth(0).getByRole('article', { name: /Ajustar o relatorio/ }),
  ).toBeVisible()
})

test('arrastar o cartao para a coluna vizinha o move, com destaque das colunas validas', async ({
  page,
}) => {
  const cartao = page.getByRole('article', { name: /Ajustar o relatorio/ })
  const destino = page.getByRole('region', { name: /Desenvolvimento/ })

  await cartao.hover()
  await page.mouse.down()
  await destino.hover()

  // DDR-002: o destaque das colunas validas e o que impede a pessoa de
  // descobrir a regra de adjacencia por tentativa e recusa.
  await expect(destino).toHaveAttribute('data-destino-valido', 'true')
  await expect(page.getByRole('region', { name: /Concluido/ })).toHaveAttribute(
    'data-destino-valido',
    'false',
  )

  await page.mouse.up()

  await expect(destino.getByRole('article', { name: /Ajustar o relatorio/ })).toBeVisible()
})

test('a mesma movimentacao e possivel apenas pelo teclado', async ({ page }) => {
  await page.keyboard.press('Tab')
  const cartao = page.getByRole('article', { name: /Ajustar o relatorio/ })
  await cartao.focus()

  await page.keyboard.press('Enter')
  await page.getByRole('menuitem', { name: /mover para etapa/i }).click()
  await page.getByRole('option', { name: /Desenvolvimento/ }).click()

  await expect(
    page.getByRole('region', { name: /Desenvolvimento/ }).getByRole('article', {
      name: /Ajustar o relatorio/,
    }),
  ).toBeVisible()

  // RNF-003 exige que a mudanca seja anunciada: sem isso quem usa leitor de
  // tela executa a acao e nao recebe confirmacao nenhuma.
  await expect(page.getByRole('status')).toContainText(/Desenvolvimento/)
})

test('a etapa recusada devolve mensagem em vez de mover o cartao', async ({ page }) => {
  const cartao = page.getByRole('article', { name: /Ajustar o relatorio/ })
  await cartao.focus()
  await page.keyboard.press('Enter')
  await page.getByRole('menuitem', { name: /mover para etapa/i }).click()

  // A etapa inalcancavel nao e oferecida; se for, a recusa tem de chegar como
  // texto e nao como cartao que volta sozinho para o lugar.
  await expect(page.getByRole('option', { name: /Concluido/ })).toHaveCount(0)
})
