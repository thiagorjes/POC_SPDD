import { test, expect } from '@playwright/test'

import { entrarComo } from '../suporte/cenario'

/**
 * Verificacoes de TL-11 — fora da contagem de cenarios congelados.
 *
 * A revisao de TASK-01.7 registrou que nenhum teste, de nenhuma ferramenta,
 * submete o formulario de novo projeto: a acao de servidor inteira estava sem
 * cobertura. Estas verificacoes fecham as duas correcoes que dependem dela.
 *
 * ACH-08 — a recusa de conteudo sem campo nomeado sai ao lado do controle, e
 * nao num aviso generico no topo. E a recusa que esta tela mais produz, porque
 * o identificador da conta e texto livre por nao haver rota que liste contas.
 *
 * ACH-09 — o painel e modal de verdade: recebe o foco ao abrir, fecha por
 * Escape, e o que fica atras sai da ordem de tabulacao. Nada disso e detectavel
 * por auditoria automatica, que e a razao de o criterio 5 estar verde com o
 * defeito de pe.
 *
 * O caminho de sucesso fica **deliberadamente de fora**: ele grava projeto no
 * ambiente, e verificacao que suja o estado compartilhado passa a depender da
 * ordem em que roda. A recusa nao grava nada.
 */

/** Bem formado e inexistente: o servico converte e nao encontra. */
const CONTA_INEXISTENTE = '00000000-0000-4000-8000-000000000000'

test.beforeEach(async ({ page }) => {
  await entrarComo(page, 'admin')
  await page.goto('/projetos/novo')
})

test('ACH-08 — a recusa da conta inexistente aparece ao lado do campo da conta', async ({
  page,
}) => {
  const campo = page.getByLabel('Primeira administradora do projeto')
  await expect(campo).toBeVisible()

  await page.getByLabel('Nome do projeto').fill('Projeto que nao deve nascer')
  await campo.fill(CONTA_INEXISTENTE)
  await page.getByRole('button', { name: 'Criar projeto' }).click()

  // A assercao e sobre **onde** a mensagem esta, e nao sobre a redacao dela:
  // o campo passa a invalido e a descricao acessivel dele carrega a recusa.
  await expect(campo).toHaveAttribute('aria-invalid', 'true')
  const idDaMensagem = await campo.getAttribute('aria-describedby')
  expect(idDaMensagem).toBe('np-admin-msg')
  await expect(page.locator('#np-admin-msg')).not.toBeEmpty()

  // A contraparte: se a mensagem tivesse ficado no alerta do topo, a assercao
  // acima passaria igual num formulario que exibisse as duas coisas.
  await expect(page.getByRole('main').getByRole('alert')).toHaveCount(0)

  // E nada foi criado.
  await page.goto('/projetos')
  await expect(page.getByText('Projeto que nao deve nascer')).toHaveCount(0)
})

test('ACH-09 — o painel recebe o foco ao abrir e fecha por Escape', async ({ page }) => {
  const painel = page.getByRole('dialog')
  await expect(painel).toBeFocused()

  await page.keyboard.press('Escape')
  await expect(page).toHaveURL(/\/projetos$/)
  await expect(page.getByRole('dialog')).toHaveCount(0)
})

test('ACH-09 — o que fica atras do painel sai da ordem de tabulacao', async ({ page }) => {
  // A barra superior estava **fora** da regiao inerte enquanto a cortina a
  // cobria visualmente: alcancavel por teclado e invisivel ao mouse. Tabular a
  // partir do painel nunca pode chegar la.
  const foraDoPainel = page.locator('[inert]')
  await expect(foraDoPainel).toHaveCount(1)
  await expect(foraDoPainel.getByRole('banner')).toHaveCount(1)

  // A afirmacao e a que importa e nada alem dela: tabular nunca alcanca o que
  // esta atras. Exigir que o foco fique **dentro** do painel seria afirmar
  // confinamento circular, que nao foi implementado de proposito — o link de
  // salto do layout e a sobreposicao de desenvolvimento do proprio Next ficam
  // legitimamente fora, e um teste que os reprovasse mediria outra coisa.
  for (let i = 0; i < 12; i += 1) {
    await page.keyboard.press('Tab')
    const atras = await page.evaluate(() => {
      const inerte = document.querySelector('[inert]')
      return inerte !== null && inerte.contains(document.activeElement)
    })
    expect(atras).toBe(false)
  }
})
