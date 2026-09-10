import { render, screen, within } from '@testing-library/react'
import { axe } from 'jest-axe'
import { TempoPorEtapa } from '../TempoPorEtapa'

/**
 * SCN-016.2 — as tres series aparecem separadas na leitura.
 *
 * O backend ja garante que a resposta nao traz soma. Isso nao basta: a soma
 * proibida por RN-008 pode nascer na tela, somando na renderizacao tres numeros
 * que chegaram separados, e nenhum teste de contrato veria. C-02 e C-03 viraram
 * restricao estrutural justamente porque acordo de uso nao resiste — e a tabela
 * agregada e a superficie onde a tentacao de "consolidar" e maior.
 */

const agregado = {
  etapas: [
    {
      etapa: { id: 'e1', nome: 'Backlog' },
      quantidade: 4,
      permanencia: { tempoTotal: 'PT96H', tempoMedio: 'PT24H' },
      esperaTomada: { tempoTotal: 'PT40H', tempoMedio: 'PT10H' },
      impedimento: { tempoTotal: 'PT8H', tempoMedio: 'PT2H' },
    },
    {
      etapa: { id: 'e2', nome: 'Desenvolvimento' },
      quantidade: 0,
      permanencia: { tempoTotal: 'PT0S', tempoMedio: 'PT0S' },
      esperaTomada: { tempoTotal: 'PT0S', tempoMedio: 'PT0S' },
      impedimento: { tempoTotal: 'PT0S', tempoMedio: 'PT0S' },
    },
  ],
}

describe('SCN-016.2 — a leitura separa as series e nao oferece soma', () => {
  it('apresenta uma coluna por serie, nomeada', () => {
    render(<TempoPorEtapa dados={agregado} />)

    const tabela = screen.getByRole('table', { name: /tempo por etapa/i })
    expect(within(tabela).getByRole('columnheader', { name: /perman/i })).toBeVisible()
    expect(within(tabela).getByRole('columnheader', { name: /espera/i })).toBeVisible()
    expect(within(tabela).getByRole('columnheader', { name: /impedimento/i })).toBeVisible()
  })

  it('nao apresenta coluna, celula ou rodape de total', () => {
    render(<TempoPorEtapa dados={agregado} />)

    // Um rodape "total" e a forma mais provavel de a soma reaparecer: ele
    // parece cortesia de tabela, e nao decisao de negocio.
    expect(screen.queryByRole('columnheader', { name: /total/i })).not.toBeInTheDocument()
    expect(screen.queryByTestId('rodape-total')).not.toBeInTheDocument()
    expect(screen.queryByRole('rowgroup', { name: /total/i })).not.toBeInTheDocument()
  })

  it('mantem a etapa sem tarefa na tabela', () => {
    render(<TempoPorEtapa dados={agregado} />)

    // Omitir a etapa vazia esconderia a etapa por onde nada passa, que e
    // justamente o achado que o painel existe para produzir.
    expect(screen.getByRole('row', { name: /Desenvolvimento/ })).toBeVisible()
  })

  it('nao oferece recorte por pessoa em lugar nenhum da leitura', () => {
    render(<TempoPorEtapa dados={agregado} />)

    // RN-014 e restricao estrutural; um seletor aqui a reintroduziria sem
    // tocar em backend nenhum.
    expect(screen.queryByLabelText(/respons|pessoa|usu[aá]rio/i)).not.toBeInTheDocument()
    expect(screen.queryByRole('columnheader', { name: /respons|pessoa/i })).not.toBeInTheDocument()
  })

  it('atende ao nivel de acessibilidade exigido', async () => {
    const { container } = render(<TempoPorEtapa dados={agregado} />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
