import { BotaoLink } from '@/componentes/botao'
import { CartaoDeProjeto } from '@/componentes/cartao-de-projeto'
import type { ProjetoResumo } from '@/lib/api/projetos'
import type { Sessao } from '@/lib/api/sessao'

/**
 * O conteúdo de TL-02, separado da rota porque `/projetos/novo` o renderiza
 * atrás do painel — a mesma lista, e não uma segunda versão dela.
 *
 * Lista vazia é sucesso e tem bloco próprio, nunca tela de erro. E os dois
 * vazios são diferentes de propósito: quem não participa de nada não tem saída
 * a partir daqui, porque participação é concedida por outro; a administração
 * global tem, porque é ela quem cria o primeiro projeto. Sem essa diferença um
 * sistema recém-instalado não sairia do zero por dentro do produto.
 */
export function PaginaDeProjetos({
  sessao,
  projetos,
}: {
  sessao: Sessao
  projetos: ProjetoResumo[]
}) {
  return (
    <div className="mx-auto max-w-6xl p-8">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Meus projetos</h1>
          <p className="mt-1 text-sm text-texto-secundario">
            {projetos.length === 0
              ? 'Nenhum projeto ao seu alcance.'
              : `${projetos.length} projeto(s) ao seu alcance.`}
          </p>
        </div>

        {/*
          A ação só é emitida para quem tem administração global (RN-036).
          Escondê-la não autoriza ninguém: a recusa continua sendo do serviço,
          que responde 403 a qualquer requisição direta.
        */}
        {sessao.adminGlobal && projetos.length > 0 ? (
          <BotaoLink href="/projetos/novo">Novo projeto</BotaoLink>
        ) : null}
      </div>

      {projetos.length > 0 ? (
        <ul className="mt-8 grid gap-4 lg:grid-cols-3">
          {projetos.map((projeto) => (
            <CartaoDeProjeto key={projeto.id} projeto={projeto} />
          ))}
        </ul>
      ) : (
        <div className="mt-8 rounded-xl border border-dashed border-borda p-8">
          {sessao.adminGlobal ? (
            <>
              <p className="text-base font-medium">Ainda não existe projeto neste sistema</p>
              <p className="mt-2 max-w-2xl text-sm text-texto-secundario">
                Quem tem administração global é quem cria o primeiro projeto. A criação nomeia, na
                mesma operação, a conta que será a primeira administradora dele.
              </p>
              <div className="mt-6">
                <BotaoLink href="/projetos/novo">Criar o primeiro projeto</BotaoLink>
              </div>
            </>
          ) : (
            <>
              <p className="text-base font-medium">Você ainda não participa de nenhum projeto</p>
              <p className="mt-2 max-w-2xl text-sm text-texto-secundario">
                A participação é concedida por quem configura cada projeto. Enquanto ela não
                existir, não há board a abrir nem fila a receber. Peça acesso a quem responde pela
                configuração do projeto.
              </p>
            </>
          )}
        </div>
      )}
    </div>
  )
}
