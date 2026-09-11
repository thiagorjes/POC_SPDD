import Link from 'next/link'
import { notFound } from 'next/navigation'

import { BarraSuperior } from '@/componentes/barra-superior'
import { FormularioDeNovoProjeto } from '@/componentes/formulario-de-novo-projeto'
import { PaginaDeProjetos } from '@/componentes/pagina-de-projetos'
import { listarProjetos } from '@/lib/api/projetos'
import { obterSessao } from '@/lib/api/sessao'

/**
 * TL-11 — painel de novo projeto, sobre a lista.
 *
 * A lista fica atrás, `aria-hidden`, porque o painel é modal: sem isso o leitor
 * de tela continuaria percorrendo o conteúdo de baixo enquanto o formulário
 * está aberto.
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
      <BarraSuperior nome={sessao.nome} adminGlobal={sessao.adminGlobal} />

      {/*
        `inert` retira a lista da navegação por teclado e da árvore de
        acessibilidade enquanto o painel está aberto — sem ele o foco sairia do
        formulário e continuaria percorrendo o que está atrás.

        O escurecimento é uma camada por cima, e não opacidade no conteúdo:
        baixar a opacidade do texto baixa junto o contraste dele, e a auditoria
        de acessibilidade reprova com razão — 1,8:1 no texto secundário do
        cartão, contra os 4,5:1 que o nível AA exige.
      */}
      <main id="principal" inert>
        <PaginaDeProjetos sessao={sessao} projetos={pagina.conteudo} />
      </main>
      <div className="fixed inset-0 bg-black/40" aria-hidden="true" />

      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="np-titulo"
        className="fixed inset-y-0 right-0 z-10 w-painel-lateral max-w-full overflow-y-auto border-l border-borda bg-superficie p-6 shadow-2xl"
      >
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
      </div>
    </>
  )
}
