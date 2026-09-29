import { notFound } from 'next/navigation'

import { BarraSuperior } from '@/componentes/barra-superior'
import { EditorDeFluxo } from '@/componentes/fluxo/editor-de-fluxo'
import { FalhaDaApi } from '@/lib/api/cliente'
import { obterFluxo } from '@/lib/api/etapas'
import { obterSessao } from '@/lib/api/sessao'

/**
 * TL-08 — configuração do fluxo de etapas (RF-017, TASK-02.7).
 *
 * A leitura é a própria tela de configuração e exige `CONFIGURAR`
 * (`EtapaController`), não `LER`: quem só participa vê as etapas pelo board,
 * que é outra rota e outro recorte. `404` (sem participação) vira a página
 * padrão de "não encontrado" do Next; `403` (participa mas não configura) tem
 * texto próprio aqui, porque esconder a tela é conveniência de navegação e a
 * recusa real continua sendo a do serviço — nunca o inverso.
 */
export default async function ConfiguracaoDoFluxo({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const sessao = await obterSessao()

  let fluxo: Awaited<ReturnType<typeof obterFluxo>> | undefined
  let recusa: FalhaDaApi | undefined
  try {
    fluxo = await obterFluxo(id)
  } catch (falha) {
    if (!(falha instanceof FalhaDaApi)) throw falha
    if (falha.problema.status === 404) notFound()
    recusa = falha
  }

  return (
    <>
      <BarraSuperior nome={sessao.nome} adminGlobal={sessao.adminGlobal} />
      <main id="principal" className="mx-auto max-w-3xl px-6 py-10">
        {fluxo ? (
          <EditorDeFluxo projetoId={id} etapasIniciais={fluxo.etapas} />
        ) : (
          <>
            <h1 className="text-xl font-semibold">
              {recusa?.problema.title ?? 'Sem permissão para configurar'}
            </h1>
            <p className="mt-2 text-sm text-texto-secundario">
              {recusa?.problema.detail ??
                'Você participa deste projeto, mas nenhum papel seu permite configurar o fluxo de etapas.'}
            </p>
          </>
        )}
      </main>
    </>
  )
}
