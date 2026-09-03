'use client';

import { use, useCallback, useEffect, useState } from 'react';
import { AvisoSomenteLeitura, Erro, SemPermissao, Skeleton, Vazio } from '@/components/Estados';
import { useFeedback } from '@/components/Feedback';
import { usePermissoes } from '@/hooks/usePermissoes';
import { api } from '@/lib/api';
import type { MembroProjeto, Papel, UsuarioAtual } from '@/lib/types';

/**
 * TL-10. Papeis sao cumulativos por (usuario, projeto). O papel protegido nao e oferecido na
 * associacao, e mesmo assim o backend recusa a delegacao (RN-006).
 */
export default function UsuariosPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { informar, reportar } = useFeedback();
  const { carregando: carregandoPermissoes, pode, projetoAtivo } = usePermissoes(id);

  const [membros, setMembros] = useState<MembroProjeto[]>([]);
  const [papeis, setPapeis] = useState<Papel[]>([]);
  const [usuarios, setUsuarios] = useState<UsuarioAtual[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [usuarioId, setUsuarioId] = useState('');
  const [codigoPapel, setCodigoPapel] = useState('');

  const carregar = useCallback(async () => {
    setErro(null);
    try {
      const [listaMembros, listaPapeis, listaUsuarios] = await Promise.all([
        api.membros(id),
        api.papeis(id),
        api.usuarios(),
      ]);
      setMembros(listaMembros);
      setPapeis(listaPapeis);
      setUsuarios(listaUsuarios);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar os membros.');
    } finally {
      setCarregando(false);
    }
  }, [id]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  if (carregando || carregandoPermissoes) {
    return <Skeleton linhas={6} />;
  }
  if (!pode('usuario:associar')) {
    return <SemPermissao />;
  }
  if (erro) {
    return <Erro mensagem={erro} aoTentarNovamente={carregar} />;
  }

  const delegaveis = papeis.filter((papel) => !papel.protegido);

  const associar = async (evento: React.FormEvent) => {
    evento.preventDefault();
    try {
      await api.associar(id, usuarioId, codigoPapel);
      informar('Papel associado.');
      setUsuarioId('');
      setCodigoPapel('');
      await carregar();
    } catch (e) {
      reportar(e);
    }
  };

  const desassociar = async (membroId: string, papel: string) => {
    try {
      await api.desassociar(id, membroId, papel);
      informar('Papel removido.');
      await carregar();
    } catch (e) {
      reportar(e);
    }
  };

  return (
    <>
      {!projetoAtivo && <AvisoSomenteLeitura />}

      <section className="cartao">
        <h2>Membros do projeto</h2>
        {membros.length === 0 ? (
          <Vazio titulo="Nenhum usuario associado a este projeto." />
        ) : (
          <table className="tabela">
            <thead>
              <tr>
                <th>Nome</th>
                <th>E-mail</th>
                <th>Papeis</th>
              </tr>
            </thead>
            <tbody>
              {membros.map((membro) => (
                <tr key={membro.usuarioId}>
                  <td>{membro.nome}</td>
                  <td>{membro.email}</td>
                  <td style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    {membro.papeis.map((papel) => {
                      const protegido = papeis.find((p) => p.codigo === papel)?.protegido ?? false;
                      return (
                        <span key={papel} className="badge">
                          {papel}
                          {!protegido && projetoAtivo && (
                            <button
                              aria-label={`Remover papel ${papel} de ${membro.nome}`}
                              onClick={() => desassociar(membro.usuarioId, papel)}
                            >
                              ×
                            </button>
                          )}
                        </span>
                      );
                    })}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="cartao">
        <h2>Associar papel</h2>
        <form style={{ display: 'flex', gap: 8 }} onSubmit={associar}>
          <select
            value={usuarioId}
            onChange={(e) => setUsuarioId(e.target.value)}
            required
            disabled={!projetoAtivo}
            aria-label="Usuario"
          >
            <option value="">Usuario…</option>
            {usuarios.map((usuario) => (
              <option key={usuario.id} value={usuario.id}>
                {usuario.nome}
              </option>
            ))}
          </select>
          <select
            value={codigoPapel}
            onChange={(e) => setCodigoPapel(e.target.value)}
            required
            disabled={!projetoAtivo}
            aria-label="Papel"
          >
            <option value="">Papel…</option>
            {delegaveis.map((papel) => (
              <option key={papel.codigo} value={papel.codigo}>
                {papel.nome}
              </option>
            ))}
          </select>
          <button className="primario" type="submit" disabled={!projetoAtivo}>
            Associar
          </button>
        </form>
      </section>
    </>
  );
}
