import { test, expect } from '@playwright/test'
import { entrarComo, semearProjeto, criarTarefa, revogarParticipacao } from './suporte/cenario'

/**
 * RF-020 e SCN-019.4 — o board se atualiza sozinho para quem esta olhando.
 *
 * Sao os quatro cenarios que obrigaram o Playwright. Cada um deles precisa de
 * duas sessoes simultaneas no mesmo board, e duas sessoes e exatamente o que
 * jsdom nao tem: sem segundo contexto de navegador nao existe "a outra pessoa",
 * e o que sobraria para verificar seria o cliente WebSocket falando consigo
 * mesmo. Rebaixa-los a integracao exigiria emenda de cenario motivada por
 * conveniencia de ferramenta.
 */

test.describe('duas sessoes no mesmo board', () => {
  test('SCN-020.1 — a criacao feita por uma aparece na outra sem recarregar', async ({
    browser,
  }) => {
    await semearProjeto('Alfa', ['ana', 'bruno'])

    const daAna = await browser.newContext()
    const doBruno = await browser.newContext()
    const paginaAna = await daAna.newPage()
    const paginaBruno = await doBruno.newPage()

    await entrarComo(paginaAna, 'ana')
    await entrarComo(paginaBruno, 'bruno')
    await paginaAna.goto('/projetos/alfa/board')
    await paginaBruno.goto('/projetos/alfa/board')

    await paginaAna.getByRole('button', { name: /nova tarefa/i }).click()
    await paginaAna.getByLabel(/t[ií]tulo/i).fill('Ajustar o relatorio')
    await paginaAna.getByRole('button', { name: /criar/i }).click()

    // Sem recarga nenhuma: se a assercao passar depois de um reload, ela nao
    // esta verificando o broadcast, e sim a consulta.
    await expect(
      paginaBruno.getByRole('article', { name: /Ajustar o relatorio/ }),
    ).toBeVisible({ timeout: 2000 })

    await daAna.close()
    await doBruno.close()
  })

  test('SCN-020.2 — a movimentacao feita por uma reposiciona o cartao na outra', async ({
    browser,
  }) => {
    await semearProjeto('Alfa', ['ana', 'bruno'])
    await criarTarefa('Alfa', 'Ajustar o relatorio')

    const daAna = await browser.newContext()
    const doBruno = await browser.newContext()
    const paginaAna = await daAna.newPage()
    const paginaBruno = await doBruno.newPage()
    await entrarComo(paginaAna, 'ana')
    await entrarComo(paginaBruno, 'bruno')
    await paginaAna.goto('/projetos/alfa/board')
    await paginaBruno.goto('/projetos/alfa/board')

    const cartao = paginaAna.getByRole('article', { name: /Ajustar o relatorio/ })
    await cartao.focus()
    await paginaAna.keyboard.press('Enter')
    await paginaAna.getByRole('menuitem', { name: /mover para etapa/i }).click()
    await paginaAna.getByRole('option', { name: /Desenvolvimento/ }).click()

    await expect(
      paginaBruno
        .getByRole('region', { name: /Desenvolvimento/ })
        .getByRole('article', { name: /Ajustar o relatorio/ }),
    ).toBeVisible({ timeout: 2000 })

    // O cartao tem de sair da coluna de origem: duplicar e o modo de falha de
    // quem trata o evento como insercao em vez de reposicionamento.
    await expect(
      paginaBruno.getByRole('article', { name: /Ajustar o relatorio/ }),
    ).toHaveCount(1)

    await daAna.close()
    await doBruno.close()
  })

  test('SCN-020.3 — a sessao que perdeu a conexao se recupera sem lacuna', async ({ browser }) => {
    await semearProjeto('Alfa', ['ana', 'bruno'])

    const daAna = await browser.newContext()
    const doBruno = await browser.newContext()
    const paginaAna = await daAna.newPage()
    const paginaBruno = await doBruno.newPage()
    await entrarComo(paginaAna, 'ana')
    await entrarComo(paginaBruno, 'bruno')
    await paginaAna.goto('/projetos/alfa/board')
    await paginaBruno.goto('/projetos/alfa/board')

    await doBruno.setOffline(true)
    await expect(paginaBruno.getByRole('status')).toContainText(/reconect|sem conex/i)

    await paginaAna.getByRole('button', { name: /nova tarefa/i }).click()
    await paginaAna.getByLabel(/t[ií]tulo/i).fill('Criada durante a queda')
    await paginaAna.getByRole('button', { name: /criar/i }).click()

    await doBruno.setOffline(false)

    // O `seq` existe para isto: o cliente percebe a lacuna e refaz a leitura.
    // A tela sem aviso e com dado velho e o desfecho que o mecanismo impede.
    await expect(
      paginaBruno.getByRole('article', { name: /Criada durante a queda/ }),
    ).toBeVisible({ timeout: 5000 })

    await daAna.close()
    await doBruno.close()
  })

  test('SCN-019.4 — quem perde a participacao para de receber o board na hora', async ({
    browser,
  }) => {
    await semearProjeto('Alfa', ['ana', 'bruno'])

    const daAna = await browser.newContext()
    const doBruno = await browser.newContext()
    const paginaAna = await daAna.newPage()
    const paginaBruno = await doBruno.newPage()
    await entrarComo(paginaAna, 'ana')
    await entrarComo(paginaBruno, 'bruno')
    await paginaBruno.goto('/projetos/alfa/board')

    await revogarParticipacao('Alfa', 'bruno')

    // A inscricao no canal precisa ser revogada junto com a participacao. Se
    // sobreviver, a sessao ja aberta continua recebendo o board de um projeto
    // ao qual a pessoa nao pertence mais — e nenhuma consulta revelaria isso,
    // porque ela nunca mais e feita.
    await paginaAna.goto('/projetos/alfa/board')
    await paginaAna.getByRole('button', { name: /nova tarefa/i }).click()
    await paginaAna.getByLabel(/t[ií]tulo/i).fill('Depois da saida')
    await paginaAna.getByRole('button', { name: /criar/i }).click()

    await expect(paginaBruno.getByText('Depois da saida')).toHaveCount(0)
    await expect(paginaBruno.getByRole('heading', { name: /sem acesso|nao encontrad/i }))
      .toBeVisible({ timeout: 5000 })

    await daAna.close()
    await doBruno.close()
  })
})
