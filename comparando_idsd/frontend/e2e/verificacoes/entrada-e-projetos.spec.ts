import { expect, request, test } from '@playwright/test'

import { entrarComo, semearProjeto } from '../suporte/cenario'

/**
 * Verificações **além dos cenários congelados**.
 *
 * Não realizam cenário nenhum e não entram na contagem de cobertura. Existem
 * porque os critérios de aceite 2 e 7 da TASK-01.7 descrevem estados que
 * cenário congelado nenhum alcança — os três de `entrada.spec.ts` entram com
 * conta que participa de projeto —, e marcá-los por inspeção seria cumprimento
 * sem poder de falha, que é o que as revisões anteriores vêm recusando.
 *
 * `bruno` é a conta escolhida por não participar de projeto algum e não ter
 * administração global: é ela que produz os dois estados ao mesmo tempo.
 */

const API = process.env.API_URL ?? 'http://localhost:8080'
const PROVEDOR = process.env.PROVEDOR_URL ?? 'http://localhost:8180'

test('criterio 2 — conta sem participacao ve lista vazia com saida declarada, e nao erro', async ({
  page,
}) => {
  await entrarComo(page, 'bruno')
  await page.goto('/projetos')

  await expect(page.getByRole('heading', { name: 'Meus projetos' })).toBeVisible()
  await expect(page.getByText('Você ainda não participa de nenhum projeto')).toBeVisible()
  // Vazio é sucesso: nenhum cartão, e nenhuma superfície de erro.
  await expect(page.getByRole('listitem')).toHaveCount(0)
  // Restrito ao conteúdo da página: em desenvolvimento o próprio Next monta um
  // indicador com `role="alert"` fora do `main`, e medi-lo seria medir o
  // framework em vez do produto.
  await expect(page.getByRole('main').getByRole('alert')).toHaveCount(0)
})

test('criterio 7 — sem administracao global a acao nao e oferecida, e o servico recusa assim mesmo', async ({
  page,
}) => {
  await entrarComo(page, 'bruno')
  await page.goto('/projetos')
  // Âncora positiva antes da negativa: `toHaveCount(0)` é verde em qualquer
  // página que não seja esta, inclusive na do provedor.
  await expect(page.getByRole('heading', { name: 'Meus projetos' })).toBeVisible()
  await expect(page.getByRole('link', { name: /novo projeto|criar o primeiro projeto/i })).toHaveCount(
    0,
  )

  // A metade que importa: esconder a ação não autoriza nem recusa nada. A
  // requisição direta ao serviço, com o token da própria pessoa, continua sendo
  // recusada — se um dia deixar de ser, este teste falha e o da tela não.
  const contexto = await request.newContext()
  const autenticacao = await contexto.post(
    `${PROVEDOR}/realms/idsd/protocol/openid-connect/token`,
    {
      form: {
        grant_type: 'password',
        client_id: 'idsd-e2e',
        username: 'bruno',
        password: 'senha-de-teste',
      },
    },
  )
  const token = (await autenticacao.json()).access_token

  const recusa = await contexto.post(`${API}/v1/projetos`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { nome: 'Projeto que nao deve nascer', primeiroAdministradorId: crypto.randomUUID() },
  })
  expect(recusa.status()).toBe(403)
  await contexto.dispose()
})

/**
 * Critério de aceite 4 — a marca de alcance por administração global.
 *
 * Ela é **estruturalmente inalcançável** pelos cenários congelados (ACH-03 da
 * revisão): as três execuções de `entrada.spec.ts` entram com conta que
 * participa do projeto, e para quem participa a marca não aparece por
 * definição. `admin` é a única conta que produz o estado — ela cria o projeto
 * por RN-036 e **não** vira participante por RN-037, de modo que o alcance dela
 * é de escopo e não de participação, que é literalmente o que SCN-021.2 existe
 * para distinguir.
 */
test('criterio 4 — a administracao global ve o projeto marcado como alcance, sem participar dele', async ({
  browser,
}) => {
  // Nome único por execução: a suíte `e2e` não limpa o banco entre execuções,
  // e nome fixo faz o cartão acumular — a asserção passaria a medir quantas
  // vezes a suíte já rodou.
  const NOME = `Alcance sem participacao ${crypto.randomUUID().slice(0, 8)}`
  await semearProjeto(NOME, ['ana:project_admin'])

  // Duas sessões em contextos separados, e não a mesma página entrando duas
  // vezes: o cookie de sessão é `httpOnly` e sobrevive à navegação, então
  // reentrar na mesma página continuaria como a primeira conta.
  const daAdministracao = await browser.newContext()
  const daParticipante = await browser.newContext()
  try {
    const admin = await daAdministracao.newPage()
    await entrarComo(admin, 'admin')
    await admin.goto('/projetos')
    await expect(admin.getByRole('heading', { name: 'Meus projetos' })).toBeVisible()
    const cartao = admin.getByRole('listitem').filter({ hasText: NOME })
    await expect(cartao).toHaveCount(1)
    await expect(cartao.getByText('Alcance: administração global')).toBeVisible()

    // A contraparte é o que dá poder de falha à asserção acima: para quem
    // participa, o mesmo projeto aparece sem a marca. Sem ela, um emblema fixo
    // em todo cartão passaria neste teste.
    const ana = await daParticipante.newPage()
    await entrarComo(ana, 'ana')
    await ana.goto('/projetos')
    const mesmo = ana.getByRole('listitem').filter({ hasText: NOME })
    await expect(mesmo).toHaveCount(1)
    await expect(mesmo.getByText('Alcance: administração global')).toHaveCount(0)
  } finally {
    await daAdministracao.close()
    await daParticipante.close()
  }
})
