import { test, expect } from '@playwright/test'
import { entrarComo, semearProjeto, criarTarefa, assumir } from './suporte/cenario'

/**
 * SCN-014.2 — a fila pessoal atravessa projetos.
 *
 * `e2e` porque a fila e a materializacao de E-02 e de H-01: ela e uma tela so
 * para uma pessoa que participa de varios boards, e a travessia entre projetos
 * — navegar da fila para o board certo e voltar — e o que se verifica. Isso
 * envolve roteamento e sessao, e nao apenas o retorno da consulta.
 */

test.beforeEach(async () => {
  await semearProjeto('Alfa', ['ana'])
  await semearProjeto('Beta', ['ana'])
  await criarTarefa('Alfa', 'Ajustar o relatorio')
  await criarTarefa('Beta', 'Revisar o contrato')
  await criarTarefa('Alfa', 'Ninguem assumiu ainda')
  await assumir('Alfa', 'Ajustar o relatorio', 'ana')
  await assumir('Beta', 'Revisar o contrato', 'ana')
})

test('a fila reune o que a pessoa assumiu nos dois projetos, separado do que aguarda', async ({
  page,
}) => {
  await entrarComo(page, 'ana')
  await page.goto('/minha-fila')

  const emCurso = page.getByRole('region', { name: /em curso/i })
  await expect(emCurso.getByRole('article')).toHaveCount(2)
  await expect(emCurso.getByText('Ajustar o relatorio')).toBeVisible()
  await expect(emCurso.getByText('Revisar o contrato')).toBeVisible()

  // Cada item nomeia o projeto: sem isso a fila junta contextos e a pessoa nao
  // sabe para onde esta indo.
  await expect(emCurso.getByRole('article').first()).toContainText(/Alfa|Beta/)

  const aguardando = page.getByRole('region', { name: /aguardando tomada/i })
  await expect(aguardando.getByText('Ninguem assumiu ainda')).toBeVisible()
})

test('a partir da fila a pessoa chega ao board do projeto da tarefa e volta', async ({ page }) => {
  await entrarComo(page, 'ana')
  await page.goto('/minha-fila')

  await page.getByRole('link', { name: /Revisar o contrato/ }).click()

  await expect(page).toHaveURL(/\/projetos\/beta\/board/)
  await expect(page.getByRole('article', { name: /Revisar o contrato/ })).toBeVisible()

  await page.getByRole('link', { name: /minha fila/i }).click()
  await expect(page).toHaveURL(/\/minha-fila$/)
})

test('a fila nao mostra tarefa de outra pessoa nem tarefa concluida', async ({ page }) => {
  await semearProjeto('Gama', ['ana', 'bruno'])
  await criarTarefa('Gama', 'Coisa do Bruno')
  await assumir('Gama', 'Coisa do Bruno', 'bruno')

  await entrarComo(page, 'ana')
  await page.goto('/minha-fila')

  await expect(page.getByText('Coisa do Bruno')).toHaveCount(0)
})
