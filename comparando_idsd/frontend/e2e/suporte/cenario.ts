import { Page, request } from '@playwright/test'

/**
 * Suporte dos cenarios `e2e`.
 *
 * A montagem passa pela API sempre que o produto oferece a rota, pela mesma
 * razao que vale na suite de integracao: fixture escrita por dentro deixa a
 * suite cega a mudanca de contrato. Desde a emenda de 2026-09-10 isso inclui a
 * criacao de projeto e a primeira participacao, que passaram a ter rota
 * (RF-022) — o semeador externo saiu.
 *
 * A autenticacao passa pela tela do provedor de verdade. Injetar token no
 * armazenamento local seria mais rapido e tiraria de SCN-001.1 justamente o que
 * ele verifica.
 */

const API = process.env.API_URL ?? 'http://localhost:8080'

export type Pessoa = 'ana' | 'bruno' | 'carla' | 'denis' | 'admin'

/**
 * Conta promovida a administradora global pelo bootstrap do ambiente de teste
 * (ADR-010). E a unica que cria projeto, por RN-036, e nao participa de projeto
 * nenhum — o alcance dela e de escopo, nao de participacao.
 */
const ADMIN_GLOBAL: Pessoa = 'admin'

const SENHA = 'senha-de-teste'

export async function entrarComo(page: Page, pessoa: Pessoa) {
  await page.goto('/')
  if (page.url().includes('openid-connect/auth')) {
    await page.getByLabel(/usu[aá]rio|username/i).fill(pessoa)
    await page.getByLabel(/senha|password/i).fill(SENHA)
    await page.getByRole('button', { name: /entrar|sign in/i }).click()
  }
}

/**
 * Semeia projeto e participacoes.
 *
 * `papeis` aceita `pessoa` ou `pessoa:papel`; sem papel declarado a pessoa
 * entra como `dev`. Chamar duas vezes para o mesmo nome de projeto acrescenta
 * participantes em vez de recriar — os papeis sao acumulativos por BDR-001.
 */
export async function semearProjeto(nome: string, papeis: string[]) {
  const participantes = papeis.map((entrada) => {
    const [pessoa, papel] = entrada.split(':')
    return { pessoa: pessoa as Pessoa, papel: papel ?? 'dev' }
  })

  // Cada pessoa precisa existir antes de ser nomeada: ela passa a existir na
  // primeira entrada, pelo autoprovisionamento da sessao.
  const ids: Record<string, string> = {}
  for (const { pessoa } of participantes) {
    const dela = await apiComo(pessoa)
    ids[pessoa] = (await (await dela.get(`${API}/v1/sessao`)).json()).id
    await dela.dispose()
  }

  const admin = await apiComo(ADMIN_GLOBAL)
  const primeiro = participantes[0]
  const resposta = await admin.post(`${API}/v1/projetos`, {
    data: { nome, primeiroAdministradorId: ids[primeiro.pessoa] },
  })
  const projetoId = (await resposta.json()).id

  // A criacao nomeia uma project_admin e mais ninguem (RN-037). O papel de cada
  // participante e entao fixado pela rota de participacao, que **substitui** o
  // conjunto — inclusive o do primeiro, quando o cenario pediu outro papel que
  // nao o de administracao.
  for (const { pessoa, papel } of participantes) {
    if (pessoa === primeiro.pessoa && papel === 'project_admin') continue
    await admin.put(`${API}/v1/projetos/${projetoId}/participacoes/${ids[pessoa]}`, {
      data: { papeis: [papel] },
    })
  }

  // O projeto nasce sem fluxo (RN-038), e board sem etapa nao e cenario de
  // nenhum destes testes. A recusa que essa ausencia produz e verificada em
  // SCN-022.3, na suite de integracao, e nao aqui.
  await admin.put(`${API}/v1/projetos/${projetoId}/etapas`, {
    data: {
      etapas: [
        { nome: 'Backlog', ordem: 1, terminal: false },
        { nome: 'Desenvolvimento', ordem: 2, terminal: false },
        { nome: 'Review', ordem: 3, terminal: false },
        { nome: 'Concluido', ordem: 4, terminal: true },
      ],
    },
  })

  await admin.dispose()
  return projetoId
}

export async function revogarParticipacao(projeto: string, pessoa: Pessoa) {
  const contexto = await apiComo('ana')
  const id = await idDoProjeto(contexto, projeto)
  const usuarios = await (await contexto.get(`${API}/v1/projetos/${id}/participacoes`)).json()
  const alvo = usuarios.find((p: { nome: string }) => p.nome.toLowerCase() === pessoa)
  await contexto.delete(`${API}/v1/projetos/${id}/participacoes/${alvo.usuarioId}`)
  await contexto.dispose()
}

export async function criarTarefa(projeto: string, titulo: string, quem: Pessoa = 'ana') {
  const contexto = await apiComo(quem)
  const id = await idDoProjeto(contexto, projeto)
  await contexto.post(`${API}/v1/projetos/${id}/tarefas`, { data: { titulo } })
  await contexto.dispose()
}

export async function assumir(projeto: string, titulo: string, quem: Pessoa) {
  const contexto = await apiComo(quem)
  const projetoId = await idDoProjeto(contexto, projeto)
  const board = await (await contexto.get(`${API}/v1/projetos/${projetoId}/board`)).json()
  const tarefa = board.etapas
    .flatMap((etapa: { tarefas: { id: string; titulo: string }[] }) => etapa.tarefas)
    .find((t: { titulo: string }) => t.titulo === titulo)

  const atual = await (await contexto.get(`${API}/v1/tarefas/${tarefa.id}`)).json()
  await contexto.post(`${API}/v1/tarefas/${tarefa.id}/tomada`, {
    // Concorrencia por estado de origem declarado (SDR-002): o suporte segue a
    // mesma regra que o produto exige de qualquer cliente.
    data: { etapaId: atual.etapaId, condicao: atual.condicao, versao: atual.versao },
  })
  await contexto.dispose()
}

// ------------------------------------------------------------- utilitarios

async function idDoProjeto(contexto: Awaited<ReturnType<typeof request.newContext>>, nome: string) {
  const lista = await (await contexto.get(`${API}/v1/projetos`)).json()
  return lista.conteudo.find((p: { nome: string }) => p.nome === nome).id
}

async function apiComo(pessoa: Pessoa) {
  const token = await tokenDe(pessoa)
  return request.newContext({ extraHTTPHeaders: { Authorization: `Bearer ${token}` } })
}

async function tokenDe(pessoa: Pessoa) {
  const contexto = await request.newContext()
  const resposta = await contexto.post(
    `${process.env.PROVEDOR_URL ?? 'http://localhost:8180'}/realms/idsd/protocol/openid-connect/token`,
    {
      form: {
        grant_type: 'password',
        client_id: 'idsd-e2e',
        username: pessoa,
        password: SENHA,
      },
    },
  )
  const corpo = await resposta.json()
  await contexto.dispose()
  return corpo.access_token
}
