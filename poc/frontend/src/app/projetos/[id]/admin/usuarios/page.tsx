'use client';

import { use, useCallback, useEffect, useState } from 'react';
import { AvisoSomenteLeitura, Erro, SemPermissao, Skeleton } from '@/components/Estados';
import { useFeedback } from '@/components/Feedback';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Botao } from '@/components/ui/Botao';
import { EstadoVazio } from '@/components/ui/EstadoVazio';
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

      <section className="secao">
        <h2>Usuários do projeto</h2>
        {membros.length === 0 ? (
          <EstadoVazio mensagem="Nenhum usuário associado a este projeto ainda." />
        ) : (
          <table>
            <thead>
              <tr>
                <th>Usuário</th>
                <th>E-mail</th>
                <th>Papéis</th>
              </tr>
            </thead>
            <tbody>
              {membros.map((membro) => (
                <tr key={membro.usuarioId}>
                  <td>
                    <span className="linha">
                      <Avatar nome={membro.nome} tamanho={22} />
                      {membro.nome}
                    </span>
                  </td>
                  <td>{membro.email}</td>
                  <td>
                    <span className="linha">
                      {membro.papeis.map((papel) => {
                        const protegido = papeis.find((p) => p.codigo === papel)?.protegido ?? false;
                        return (
                          <span className="linha" key={papel} style={{ gap: 'var(--espaco-scale-xs)' }}>
                            <Badge variante="neutro">{papel}</Badge>
                            {!protegido && projetoAtivo && (
                              <Botao
                                variante="text"
                                aria-label={`Remover papel ${papel} de ${membro.nome}`}
                                onClick={() => desassociar(membro.usuarioId, papel)}
                              >
                                Remover
                              </Botao>
                            )}
                          </span>
                        );
                      })}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="secao">
        <h2>Associar usuário</h2>
        <form className="form-row" onSubmit={associar}>
          <div className="form-field">
            <label htmlFor="assoc-usuario">Usuário</label>
            <select
              id="assoc-usuario"
              value={usuarioId}
              onChange={(e) => setUsuarioId(e.target.value)}
              required
              disabled={!projetoAtivo}
            >
              <option value="">Selecione…</option>
              {usuarios.map((usuario) => (
                <option key={usuario.id} value={usuario.id}>
                  {usuario.nome}
                </option>
              ))}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="assoc-papel">Papel</label>
            <select
              id="assoc-papel"
              value={codigoPapel}
              onChange={(e) => setCodigoPapel(e.target.value)}
              required
              disabled={!projetoAtivo}
            >
              <option value="">Selecione…</option>
              {delegaveis.map((papel) => (
                <option key={papel.codigo} value={papel.codigo}>
                  {papel.nome}
                </option>
              ))}
            </select>
          </div>
          <Botao variante="primary" type="submit" disabled={!projetoAtivo}>
            + Associar usuário
          </Botao>
        </form>
      </section>
    </>
  );
}
