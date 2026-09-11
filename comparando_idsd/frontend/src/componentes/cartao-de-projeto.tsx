import Link from 'next/link'

import { Emblema } from '@/componentes/emblema'
import type { ProjetoResumo } from '@/lib/api/projetos'

/**
 * Rótulo da participação **pela capacidade**, e não pelo nome do papel
 * (ACH-20 da revisão de TASK-01.7).
 *
 * RF-002 fala em qual permissão a pessoa tem no projeto, e o protótipo rotula a
 * participação por capacidade — "escrita" e "leitura". O cartão exibia os
 * nomes dos papéis, que são vocabulário de configuração e não respondem à
 * pergunta do requisito; e o campo `permissoes` da resposta não era consumido
 * em ponto nenhum do código, só declarado no tipo.
 *
 * A partição é por poder de escrita porque é a única distinção que a lista
 * precisa fazer: o que a pessoa pode em detalhe depende da tela em que ela
 * está, e antecipar isso aqui duplicaria a decisão de autorização, que é do
 * serviço (RNF-004).
 */
const ESCRITA = ['ESCREVER_TAREFA', 'DESBLOQUEAR', 'ENCERRAR', 'REABRIR']

function rotuloDaParticipacao(permissoes: string[]): string | null {
  if (permissoes.length === 0) return null
  if (permissoes.some((permissao) => ESCRITA.includes(permissao))) return 'escrita'
  return 'leitura'
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
  const participacao = rotuloDaParticipacao(projeto.permissoes)

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
          {participacao ? `Participação: ${participacao}` : 'Sem permissão atribuída neste projeto'}
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
