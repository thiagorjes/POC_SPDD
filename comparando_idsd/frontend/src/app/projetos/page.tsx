import { BarraSuperior } from '@/componentes/barra-superior'
import { PaginaDeProjetos } from '@/componentes/pagina-de-projetos'
import { listarProjetos } from '@/lib/api/projetos'
import { obterSessao } from '@/lib/api/sessao'

/**
 * TL-02 — a lista de projetos.
 *
 * Não há guarda de rota escrita aqui: `chamar` não encontra token e redireciona
 * para o início do authorization code. É de propósito que a proteção more no
 * cliente REST e não na tela — rota nova que esqueça de chamar a guarda não
 * existe, porque toda rota que mostra dado precisa buscá-lo.
 */
export default async function Projetos() {
  const [sessao, pagina] = await Promise.all([obterSessao(), listarProjetos()])

  return (
    <>
      <BarraSuperior nome={sessao.nome} adminGlobal={sessao.adminGlobal} />
      <main id="principal">
        <PaginaDeProjetos sessao={sessao} projetos={pagina.conteudo} />
      </main>
    </>
  )
}
