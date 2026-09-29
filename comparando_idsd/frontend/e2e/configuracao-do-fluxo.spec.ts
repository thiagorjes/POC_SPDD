import { test, expect } from '@playwright/test'
import { entrarComo, semearProjeto, criarTarefa } from './suporte/cenario'

/**
 * TASK-02.7 — TL-08, configuração do fluxo de etapas.
 *
 * Cobre os critérios de aceite 1 a 5 da task. SCN-017.1/.2/.3 já têm
 * verificação de contrato em `internal/projeto/ConfiguracaoDoFluxoIT.java`
 * (backend); os testes abaixo verificam o **comportamento de tela** que a
 * task exige e que nenhum teste de backend alcança — percurso completo,
 * preservação de rascunho após recusa e operação inteira por teclado.
 *
 * Rota: `/projetos/:id/config/fluxo`, do mapa de telas
 * (`docs/design/kanban-tarefas/screen-map.md:46`) e da própria task. Os
 * rótulos usados nos locators vêm do protótipo de referência
 * (`docs/design/kanban-tarefas/prototypes/TL-08-configuracao-do-fluxo.html`),
 * o único insumo de forma disponível antes da implementação existir.
 *
 * **Achado, não contornado aqui:** o protótipo desenha, para "remover etapa
 * com tarefas", um fluxo de migração (`<select id="migrar">`) que a task e o
 * contrato atual não têm — `PUT /etapas` apenas recusa com `422` nomeando a
 * etapa (SCN-017.3), sem opção de destino. O critério 3 é testado pela forma
 * que o contrato sustenta hoje; a divergência de protótipo é achado para o
 * `/prd` ou `/design` decidirem, não algo para o `/implement` inventar.
 *
 * **Achado, não contornado aqui:** `somente-leitura.spec.ts` (congelado,
 * SCN-015.2) navega para `/projetos/alfa/configuracao`, e não para
 * `/projetos/alfa/config/fluxo` como o mapa de telas declara. As duas rotas
 * não podem estar certas ao mesmo tempo; como aquele arquivo é cenário
 * congelado, não é alterado aqui. Fica registrado para o `/tasks` ou revisão
 * humana reconciliar antes de TASK-02.7 fechar.
 */

test.beforeEach(async ({ page }) => {
  await semearProjeto('Fluxo', ['ana:project_admin'])
  await entrarComo(page, 'ana')
})

test('Critério 1 — configurar o fluxo e voltar ao board mostra as etapas na ordem definida', async ({
  page,
}) => {
  await page.goto('/projetos/fluxo/config/fluxo')
  await expect(page.getByRole('heading', { name: /configura[cç][aã]o do fluxo/i })).toBeVisible()

  await page.getByRole('button', { name: /adicionar etapa/i }).click()
  const nomes = page.getByLabel(/nome da etapa/i)
  await nomes.last().fill('Homologação')

  await page.getByRole('button', { name: /salvar fluxo/i }).click()
  await expect(page.getByRole('status')).toBeVisible()

  await page.goto('/projetos/fluxo/board')
  const colunas = page.getByRole('region', { name: /etapa/i })
  await expect(colunas).toHaveCount(5)
  await expect(colunas.nth(4)).toHaveAccessibleName(/Homologa[cç][aã]o/)
})

test('Critério 2 — submeter sem etapa terminal exibe a recusa e mantém o rascunho', async ({
  page,
}) => {
  await page.goto('/projetos/fluxo/config/fluxo')

  // Desmarcar a única etapa terminal (Concluido, por `fluxoPadrao`): a
  // submissão deve ser recusada antes de qualquer coisa mudar no servidor.
  await page.getByRole('checkbox', { name: /[ée] terminal/i }).uncheck()
  const rascunho = 'Nome que não pode se perder'
  await page.getByLabel(/nome da etapa/i).first().fill(rascunho)

  await page.getByRole('button', { name: /salvar fluxo/i }).click()

  await expect(page.getByRole('alert')).toContainText(/etapa terminal/i)
  // O rascunho digitado antes da recusa continua na tela — recarregar do
  // servidor depois do 422 é a falha mais provável desta task (guia técnico).
  await expect(page.getByLabel(/nome da etapa/i).first()).toHaveValue(rascunho)
})

test('Critério 3 — arquivar etapa com tarefas ativas identifica a etapa que impede', async ({
  page,
}) => {
  // A tarefa nasce na primeira etapa (RN-004, RN-006): não é preciso mover
  // nada para ter uma etapa com trabalho ativo dentro dela.
  await criarTarefa('Fluxo', 'Em andamento')

  await page.goto('/projetos/fluxo/config/fluxo')
  // Remover a etapa da lista é o que a arquiva (guia técnico da task).
  await page.getByRole('button', { name: /remover a etapa backlog/i }).click()
  await page.getByRole('button', { name: /salvar fluxo/i }).click()

  await expect(page.getByRole('alert')).toContainText(/backlog/i)
})

test('Critério 4 — renomear etapa não cria uma etapa nova nem perde o andamento', async ({
  page,
}) => {
  await criarTarefa('Fluxo', 'Qualquer coisa')

  await page.goto('/projetos/fluxo/andamento')
  const tabela = page.getByRole('table', { name: /andamento/i })
  await expect(tabela).toBeVisible()
  const linhasAntes = await tabela.getByRole('row').count()
  await expect(tabela.getByRole('row', { name: /backlog/i })).toBeVisible()

  await page.goto('/projetos/fluxo/config/fluxo')
  await page.getByLabel(/nome da etapa/i).first().fill('Fila de entrada')
  await page.getByRole('button', { name: /salvar fluxo/i }).click()
  await expect(page.getByRole('status')).toBeVisible()

  await page.goto('/projetos/fluxo/andamento')
  // Mesmo número de linhas: renomear não cria uma segunda etapa nem some com
  // a que já tinha andamento acumulado.
  await expect(tabela.getByRole('row')).toHaveCount(linhasAntes)
  await expect(tabela.getByRole('row', { name: /backlog/i })).toHaveCount(0)
  await expect(tabela.getByRole('row', { name: /fila de entrada/i })).toBeVisible()
})

test('Critério 5 — toda a edição do fluxo é possível apenas com teclado', async ({ page }) => {
  await page.goto('/projetos/fluxo/config/fluxo')

  // Reordenar aqui é por botão (protótipo), não por arrasto: o caminho por
  // teclado não é uma via alternativa a implementar à parte, é o único
  // caminho que esses controles já têm.
  await page.keyboard.press('Tab')
  await page.getByRole('button', { name: /adicionar etapa/i }).focus()
  await page.keyboard.press('Enter')

  const novoNome = page.getByLabel(/nome da etapa/i).last()
  await novoNome.focus()
  await page.keyboard.type('Homologação')

  await page
    .getByRole('button', { name: /mover a etapa homologa[cç][aã]o para cima/i })
    .focus()
  await page.keyboard.press('Enter')

  await page.getByRole('button', { name: /salvar fluxo/i }).focus()
  await page.keyboard.press('Enter')

  await expect(page.getByRole('status')).toBeVisible()
  await expect(page.getByLabel(/nome da etapa/i).nth(3)).toHaveValue('Homologação')
})
