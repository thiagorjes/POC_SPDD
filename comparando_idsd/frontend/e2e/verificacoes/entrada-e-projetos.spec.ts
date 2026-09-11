import { expect, request, test } from '@playwright/test'

import { entrarComo } from '../suporte/cenario'

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
