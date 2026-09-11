'use server'

import { revalidatePath } from 'next/cache'

import { FalhaDaApi } from '@/lib/api/cliente'
import { criarProjeto } from '@/lib/api/projetos'

export type EstadoDoFormulario = {
  erros?: Record<string, string>
  recusa?: string
  criado?: { id: string; nome: string }
}

/**
 * Ação de servidor da criação de projeto.
 *
 * A validação daqui existe para dizer o que falta antes da ida ao servidor, e
 * não para autorizar nada: `403` para quem não tem administração global e `422`
 * para conta inexistente continuam sendo do serviço, e é lá que a decisão vive
 * (RNF-004). Por isso a recusa recebida é exibida como veio, em vez de a tela
 * decidir antes se valia a pena perguntar.
 */
export async function criarProjetoAcao(
  _anterior: EstadoDoFormulario,
  dados: FormData,
): Promise<EstadoDoFormulario> {
  const nome = String(dados.get('nome') ?? '').trim()
  const descricao = String(dados.get('descricao') ?? '').trim()
  const primeiroAdministradorId = String(dados.get('primeiroAdministradorId') ?? '').trim()

  const erros: Record<string, string> = {}
  if (!nome) erros.nome = 'Informe o nome do projeto.'
  if (!primeiroAdministradorId) {
    erros.primeiroAdministradorId =
      'Escolha quem será a administradora do projeto. Sem ela o projeto nasceria sem ninguém dentro, e ninguém poderia conceder a primeira participação.'
  }
  if (Object.keys(erros).length > 0) return { erros }

  try {
    const projeto = await criarProjeto({
      nome,
      ...(descricao ? { descricao } : {}),
      primeiroAdministradorId,
    })
    revalidatePath('/projetos')
    return { criado: { id: projeto.id, nome: projeto.nome } }
  } catch (falha) {
    if (!(falha instanceof FalhaDaApi)) throw falha

    // O serviço nomeia o campo em `errors[].campo` — aproveitar isso põe a
    // mensagem ao lado do controle certo em vez de num aviso genérico no topo.
    const porCampo: Record<string, string> = {}
    for (const item of falha.problema.errors ?? []) {
      if (item.campo && item.mensagem) porCampo[item.campo] = item.mensagem
    }
    if (Object.keys(porCampo).length > 0) return { erros: porCampo }

    return { recusa: falha.problema.detail ?? 'A criação do projeto foi recusada.' }
  }
}
