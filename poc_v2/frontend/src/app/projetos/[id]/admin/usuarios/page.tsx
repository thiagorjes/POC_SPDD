'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { useFeedback } from '@/components/FeedbackProvider';
import { EstadoErro, EstadoVazio, Skeleton } from '@/components/Skeleton';
import { usePermissoes } from '@/hooks/usePermissoes';
import { api, mensagemDeErro } from '@/lib/api';
import { iniciais } from '@/lib/format';
import {
  PERMISSAO,
  type MembroProjetoResponse,
  type ProjetoResponse,
  type UsuarioResumoResponse,
} from '@/lib/types';

/** Papeis associaveis a projeto: o catalogo fechado menos os globais/protegidos (RN-006). */
const PAPEIS_DE_PROJETO = [
  { codigo: 'project_admin', nome: 'Project Admin' },
  { codigo: 'product_owner', nome: 'Product Owner' },
  { codigo: 'dev', nome: 'Dev' },
  { codigo: 'gestor', nome: 'Gestor' },
  { codigo: 'user', nome: 'Usuário' },
];

/** TL-10 — Usuários do projeto (RF-015). Trocar o papel = desassociar o anterior e associar o novo. */
export default function AdminUsuariosPage() {
  const projetoId = String(useParams().id);
  const feedback = useFeedback();
  const permissoes = usePermissoes(projetoId);

  const [projeto, setProjeto] = useState<ProjetoResponse | null>(null);
  const [membros, setMembros] = useState<MembroProjetoResponse[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [associando, setAssociando] = useState(false);
  const [disponiveis, setDisponiveis] = useState<UsuarioResumoResponse[]>([]);
  const [novoUsuarioId, setNovoUsuarioId] = useState('');
  const [novoPapel, setNovoPapel] = useState('dev');

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro(null);
    try {
      const [projetoCarregado, membrosCarregados, disponiveisCarregados] = await Promise.all([
        api.projeto(projetoId),
        api.membros(projetoId),
        api.usuariosDisponiveis(projetoId),
      ]);
      setProjeto(projetoCarregado);
      setMembros(membrosCarregados);
      setDisponiveis(disponiveisCarregados);
    } catch (erroCarga) {
      setErro(mensagemDeErro(erroCarga));
    } finally {
      setCarregando(false);
    }
  }, [projetoId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function associar() {
    try {
      await api.associarPapel(projetoId, {
        usuarioId: novoUsuarioId,
        codigoPapel: novoPapel,
      });
      feedback.sucesso('Usuário associado ao projeto.');
      setNovoUsuarioId('');
      setAssociando(false);
      await carregar();
    } catch (erroAssociacao) {
      feedback.reportar(erroAssociacao);
    }
  }

  async function trocarPapel(membro: MembroProjetoResponse, codigoPapel: string) {
    try {
      for (const papelAtual of membro.papeis) {
        await api.desassociarPapel(projetoId, membro.usuarioId, papelAtual);
      }
      await api.associarPapel(projetoId, { usuarioId: membro.usuarioId, codigoPapel });
      feedback.sucesso('Papel atualizado.');
      await carregar();
    } catch (erroTroca) {
      feedback.reportar(erroTroca);
      await carregar();
    }
  }

  async function remover(membro: MembroProjetoResponse) {
    try {
      for (const papelAtual of membro.papeis) {
        await api.desassociarPapel(projetoId, membro.usuarioId, papelAtual);
      }
      feedback.sucesso('Usuário removido do projeto.');
      await carregar();
    } catch (erroRemocao) {
      feedback.reportar(erroRemocao);
    }
  }

  const podeAssociar =
    permissoes.possui(PERMISSAO.USUARIO_ASSOCIAR) && permissoes.projetoAtivo;

  return (
    <AppShell projetoId={projetoId} projetoNome={projeto?.nome}>
      <div className="page-header">
        <h1>Usuários do Projeto</h1>
        <div className="page-header__actions">
          <Link href={`/projetos/${projetoId}/admin`} className="btn btn-outline">
            Voltar ao Admin
          </Link>
          <button
            className="btn btn-primary"
            type="button"
            disabled={!podeAssociar}
            onClick={() => setAssociando(true)}
          >
            + Associar usuário
          </button>
        </div>
      </div>

      {carregando && <Skeleton linhas={3} />}
      {!carregando && erro && <EstadoErro mensagem={erro} aoTentar={() => void carregar()} />}

      {!carregando && !erro && membros.length === 0 && (
        <EstadoVazio>
          Nenhum usuário associado a este projeto ainda.
          <br />
          <button
            className="btn btn-primary"
            type="button"
            disabled={!podeAssociar}
            onClick={() => setAssociando(true)}
          >
            Associar usuário
          </button>
        </EstadoVazio>
      )}

      {!carregando && !erro && membros.length > 0 && (
        <section aria-label="Usuários associados">
          <table>
            <thead>
              <tr>
                <th>Usuário</th>
                <th>Papel</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {membros.map((membro) => (
                <tr key={membro.usuarioId}>
                  <td>
                    <span className="avatar avatar--sm">{iniciais(membro.nome)}</span>{' '}
                    {membro.nome}
                    <br />
                    <span className="text-secondary">{membro.email}</span>
                  </td>
                  <td>
                    <select
                      aria-label={`Papel de ${membro.nome}`}
                      disabled={!podeAssociar}
                      value={membro.papeis[0] ?? ''}
                      onChange={(evento) => void trocarPapel(membro, evento.target.value)}
                    >
                      {membro.papeis[0] === undefined && <option value="">Sem papel</option>}
                      {PAPEIS_DE_PROJETO.map((papel) => (
                        <option key={papel.codigo} value={papel.codigo}>
                          {papel.nome}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <button
                      className="btn btn-text"
                      type="button"
                      disabled={!podeAssociar}
                      onClick={() => void remover(membro)}
                    >
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {associando && (
        <div className="modal-overlay">
          <div
            className="modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="associar-titulo"
          >
            <h1 id="associar-titulo">Associar usuário</h1>
            <div className="form-field">
              <label htmlFor="associar-usuario">Usuário</label>
              <select
                id="associar-usuario"
                aria-describedby="associar-usuario-desc"
                disabled={disponiveis.length === 0}
                value={novoUsuarioId}
                onChange={(evento) => setNovoUsuarioId(evento.target.value)}
              >
                <option value="">Selecione um usuário</option>
                {disponiveis.map((usuario) => (
                  <option key={usuario.id} value={usuario.id}>
                    {usuario.nome} — {usuario.email}
                  </option>
                ))}
              </select>
              <span id="associar-usuario-desc" className="text-secondary">
                {disponiveis.length === 0
                  ? 'Nenhum usuário disponível. O usuário precisa ter feito login ao menos uma vez para existir na base (provisionamento JIT).'
                  : 'Apenas usuários já provisionados e que ainda não são membros deste projeto.'}
              </span>
            </div>
            <div className="form-field">
              <label htmlFor="associar-papel">Papel</label>
              <select
                id="associar-papel"
                value={novoPapel}
                onChange={(evento) => setNovoPapel(evento.target.value)}
              >
                {PAPEIS_DE_PROJETO.map((papel) => (
                  <option key={papel.codigo} value={papel.codigo}>
                    {papel.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" type="button" onClick={() => setAssociando(false)}>
                Cancelar
              </button>
              <button
                className="btn btn-primary"
                type="button"
                disabled={novoUsuarioId === ''}
                onClick={() => void associar()}
              >
                Associar
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}
