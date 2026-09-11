import Link from 'next/link'
import { notFound } from 'next/navigation'

import { BarraSuperior } from '@/componentes/barra-superior'
import { FormularioDeNovoProjeto } from '@/componentes/formulario-de-novo-projeto'
import { PaginaDeProjetos } from '@/componentes/pagina-de-projetos'
import { PainelModal } from '@/componentes/painel-modal'
import { listarProjetos } from '@/lib/api/projetos'
import { obterSessao } from '@/lib/api/sessao'

/**
 * TL-11 — painel de novo projeto, sobre a lista.
 *
 * A lista e a barra superior ficam atrás, inertes, porque o painel é modal: sem
 * isso o leitor de tela continuaria percorrendo o conteúdo de baixo enquanto o
 * formulário está aberto, e o teclado continuaria alcançando a barra.
 *
 * O `notFound()` para quem não tem administração global é conveniência de
 * navegação e **não** o controle de acesso — a rota `POST /v1/projetos` recusa
 * com `403` independentemente do que esta tela mostre (RNF-004).
 */
export default async function NovoProjeto() {
  const sessao = await obterSessao()
  if (!sessao.adminGlobal) notFound()

  const pagina = await listarProjetos()

  return (
    <>
      {/*
        `inert` envolve **tudo** o que fica atrás, barra superior inclusive
        (ACH-09): antes ela ficava fora da região inerte e continuava focável,
        enquanto a camada de escurecimento a cobria visualmente e o diálogo se
        declarava modal — o teclado alcançava o que a tecnologia assistiva já
        ignorava.

        O escurecimento é uma camada por cima, e não opacidade no conteúdo:
        baixar a opacidade do texto baixa junto o contraste dele, e a auditoria
        de acessibilidade reprova com razão — 1,8:1 no texto secundário do
        cartão, contra os 4,5:1 que o nível AA exige.
      */}
      <div inert>
        <BarraSuperior nome={sessao.nome} adminGlobal={sessao.adminGlobal} />
        <main>
          <PaginaDeProjetos sessao={sessao} projetos={pagina.conteudo} />
        </main>
      </div>
      <div className="fixed inset-0 bg-black/40" aria-hidden="true" />

      {/*
        `id="principal"` migra para o painel enquanto ele está aberto: o atalho
        de salto do leiaute apontava para a região que acabou de ficar inerte,
        virando atalho para lugar nenhum (ACH-09).
      */}
      <PainelModal id="principal" rotuladoPor="np-titulo" aoFechar="/projetos">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 id="np-titulo" className="text-xl font-semibold">
              Novo projeto
            </h1>
            <p className="mt-1 text-sm text-texto-secundario">
              Visível apenas para a administração global
            </p>
          </div>
          <Link
            href="/projetos"
            aria-label="Fechar o formulário de novo projeto"
            className="rounded-lg border border-borda px-3 py-2 text-sm"
          >
            Fechar
          </Link>
        </div>

        <div className="mt-6">
          <FormularioDeNovoProjeto />
        </div>
      </PainelModal>
    </>
  )
}
