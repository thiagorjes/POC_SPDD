import { Locator, Page, expect, test } from '@playwright/test'

import { entrarComo } from '../suporte/cenario'

/**
 * Verificação **além dos cenários congelados** — RNF-005.
 *
 * Ela não realiza cenário nenhum e não entra na contagem de cobertura. Existe
 * porque o envelope não tinha dono: RNF-005 não era nomeado em nenhum dos 43
 * arquivos de task, em critério de aceite nenhum e em nenhuma linha do plano de
 * verificação — varredura devolvia zero nas três fontes (ACH-16 da revisão de
 * TASK-01.7). Sem isso o envelope reprovaria por construção no fechamento do
 * épico, com a causa a quarenta e três arquivos de distância de quem recebesse
 * a reprovação.
 *
 * A decisão de forma é medir **por tela, na task que dá à luz a tela**, e não
 * numa passagem única ao fim. Tela que nasce larga demais é barata de corrigir
 * no dia em que nasce e cara quatro épicos depois, quando o layout já foi
 * copiado pelas seguintes.
 *
 * As duas larguras são as duas extremas do requisito, e as duas importam por
 * razões opostas: 1280 px é onde o desenho foi feito, e 1024 px é o que o
 * requisito existe para proteger. Medir só a primeira deixaria passar
 * exatamente o caso em questão.
 *
 * Abaixo de 1024 px não é alvo declarado do produto, e por isso não é medido —
 * afirmar mais do que o requisito exige produz reprovação que ninguém decidiu.
 */
const LARGURAS = [
  { rotulo: '1280px', width: 1280, height: 900 },
  { rotulo: '1024px', width: 1024, height: 768 },
]

/**
 * Rolagem horizontal do documento.
 *
 * A tolerância de 1 px é deliberada: arredondamento de subpixel em borda ou
 * transformação produz diferença fracionária que não é rolagem para pessoa
 * nenhuma, e reprovar por ela transformaria a verificação em ruído — o caminho
 * curto para alguém desabilitá-la.
 *
 * A medida é do documento, e não de cada elemento: rolagem horizontal **dentro**
 * de uma região que a anuncia é permitida pelo requisito (é o caso previsto para
 * as colunas do board, em TASK-02.8), e o que ele proíbe é a página inteira
 * deslizar sem que nada indique.
 */
async function rolagemHorizontal(pagina: Page) {
  return await pagina.evaluate(() => {
    const raiz = document.scrollingElement ?? document.documentElement
    return raiz.scrollWidth - raiz.clientWidth
  })
}

/**
 * "Sem perda de ação" é o outro lado do requisito, e é o que uma asserção só de
 * rolagem deixaria passar: um controle empurrado para fora, ou colapsado a zero,
 * não produz rolagem nenhuma e some em silêncio. `toBeInViewport` é o que
 * distingue estar no DOM de estar alcançável.
 */
async function acoesAlcancaveis(acoes: Locator[]) {
  for (const acao of acoes) {
    await expect(acao).toBeVisible()
    await expect(acao).toBeInViewport()
  }
}

for (const largura of LARGURAS) {
  test.describe(`RNF-005 a ${largura.rotulo}`, () => {
    test.use({ viewport: { width: largura.width, height: largura.height } })

    test('TL-01 — a tela de entrada', async ({ page }) => {
      await page.goto('/entrar')
      const titulo = page.getByRole('heading', { name: 'Fluxo de Tarefas' })
      await expect(titulo).toBeVisible()

      await acoesAlcancaveis([page.getByRole('link', { name: /Entrar com a conta/i })])
      expect(await rolagemHorizontal(page)).toBeLessThanOrEqual(1)
    })

    test('TL-02 — a lista de projetos', async ({ page }) => {
      // `admin` e não `ana`: a ação de criar projeto só existe para a
      // administração global (RN-036), e é ela a ação da tela. Com uma conta
      // participante não haveria o que verificar do lado da alcançabilidade.
      await entrarComo(page, 'admin')
      await page.goto('/projetos')
      await expect(page.getByRole('heading', { name: 'Meus projetos' })).toBeVisible()

      await acoesAlcancaveis([
        page.getByRole('link', { name: 'Novo projeto' }),
        page.getByRole('button', { name: /Admin/i }),
      ])
      expect(await rolagemHorizontal(page)).toBeLessThanOrEqual(1)
    })

    test('TL-11 — o painel de novo projeto', async ({ page }) => {
      await entrarComo(page, 'admin')
      await page.goto('/projetos/novo')
      await expect(page.getByRole('heading', { name: 'Novo projeto' })).toBeVisible()

      await acoesAlcancaveis([
        page.getByLabel(/nome/i),
        page.getByRole('button', { name: 'Criar projeto' }),
        page.getByRole('link', { name: /Fechar o formul/i }),
      ])
      expect(await rolagemHorizontal(page)).toBeLessThanOrEqual(1)
    })
  })
}
