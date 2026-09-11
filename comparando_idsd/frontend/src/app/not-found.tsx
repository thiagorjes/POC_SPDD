import { Alerta } from '@/componentes/alerta'
import { BotaoLink } from '@/componentes/botao'

/**
 * Recusa de navegação (ACH-10 da revisão de TASK-01.7).
 *
 * O caminho que mais a produz é o `notFound()` de `/projetos/novo`, que é a
 * conveniência de navegação de quem não tem administração global — sem esta
 * tela ele entregava a página padrão do framework, em inglês, dentro de um
 * documento declarado em português.
 *
 * O texto não distingue "não existe" de "não é seu": a distinção é justamente o
 * que a recusa protege, e dizê-la aqui devolveria pela tela a existência que o
 * serviço se recusou a confirmar (SCN-002.3).
 */
export default function NaoEncontrado() {
  return (
    <main id="principal" className="mx-auto flex min-h-screen max-w-xl flex-col justify-center p-8">
      <Alerta tom="recusa" titulo="Esta página não está disponível">
        <p>
          Ou ela não existe, ou não está ao seu alcance. Se você esperava alcançá-la, peça acesso a
          quem responde pela configuração do projeto.
        </p>
      </Alerta>

      <div className="mt-8">
        <BotaoLink href="/projetos">Voltar aos projetos</BotaoLink>
      </div>
    </main>
  )
}
