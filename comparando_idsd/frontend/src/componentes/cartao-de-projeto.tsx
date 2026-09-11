import Link from 'next/link'

import { Emblema } from '@/componentes/emblema'
import type { ProjetoResumo } from '@/lib/api/projetos'

const NOME_DO_PAPEL: Record<string, string> = {
  project_admin: 'administração do projeto',
  product_owner: 'product owner',
  dev: 'desenvolvimento',
  gestor: 'leitura',
}

/**
 * Cartão de projeto da lista (TL-02).
 *
 * A marca de alcance por administração global existe para que escopo não se
 * confunda com participação (RN-035, SCN-021.2): quem chega ali pelo alcance
 * global não está dentro do projeto, e o cartão diz isso com palavra e não com
 * cor.
 */
export function CartaoDeProjeto({ projeto }: { projeto: ProjetoResumo }) {
  const papeis = projeto.papeis.map((papel) => NOME_DO_PAPEL[papel] ?? papel)

  return (
    <li>
      <Link
        href={`/projetos/${projeto.id}`}
        className="flex h-full flex-col gap-3 rounded-xl border border-borda bg-superficie p-5 hover:border-[var(--cor-borda-forte)]"
      >
        <div className="flex items-start justify-between gap-3">
          <h2 className="text-lg font-semibold">{projeto.nome}</h2>
          {projeto.acessoPorAdministracaoGlobal ? (
            <Emblema papel="alcance">Alcance: administração global</Emblema>
          ) : null}
        </div>

        <p className="text-sm text-texto-secundario">
          {papeis.length > 0
            ? `Participação: ${papeis.join(', ')}`
            : 'Sem papel atribuído neste projeto'}
        </p>

        {projeto.fluxoConfigurado === false ? (
          <div>
            <Emblema papel="impedimento">Fluxo não configurado — não aceita tarefa</Emblema>
          </div>
        ) : null}
      </Link>
    </li>
  )
}
