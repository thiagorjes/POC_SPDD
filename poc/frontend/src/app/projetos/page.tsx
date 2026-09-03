'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { Erro, Skeleton, Vazio } from '@/components/Estados';
import { useFeedback } from '@/components/Feedback';
import { api } from '@/lib/api';
import type { Projeto, UsuarioAtual } from '@/lib/types';

/** TL-02. O botao de criar so aparece para {@code adminGlobal}; o backend revalida (ADR-007). */
export default function ProjetosPage() {
  const { informar, reportar } = useFeedback();
  const [projetos, setProjetos] = useState<Projeto[] | null>(null);
  const [eu, setEu] = useState<UsuarioAtual | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);
  const [nome, setNome] = useState('');
  const [descricao, setDescricao] = useState('');

  const carregar = async () => {
    setErro(null);
    try {
      const [lista, atual] = await Promise.all([api.projetos(), api.eu()]);
      setProjetos(lista);
      setEu(atual);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar os projetos.');
    }
  };

  useEffect(() => {
    void carregar();
  }, []);

  const criar = async (evento: React.FormEvent) => {
    evento.preventDefault();
    try {
      await api.criarProjeto({ nome, descricao: descricao || undefined });
      informar('Projeto criado.');
      setNome('');
      setDescricao('');
      setCriando(false);
      await carregar();
    } catch (e) {
      reportar(e);
    }
  };

  if (erro) {
    return <Erro mensagem={erro} aoTentarNovamente={carregar} />;
  }
  if (!projetos) {
    return <Skeleton linhas={5} />;
  }

  return (
    <>
      <header style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 24 }}>
        <h1>Projetos</h1>
        {eu?.adminGlobal && (
          <button className="primario" onClick={() => setCriando((v) => !v)}>
            Novo projeto
          </button>
        )}
      </header>

      {criando && (
        <form className="cartao" style={{ display: 'grid', gap: 8, marginBottom: 24 }} onSubmit={criar}>
          <label>
            Nome
            <input value={nome} onChange={(e) => setNome(e.target.value)} required maxLength={120} />
          </label>
          <label>
            Descricao
            <textarea value={descricao} onChange={(e) => setDescricao(e.target.value)} rows={3} />
          </label>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="primario" type="submit">
              Criar
            </button>
            <button type="button" onClick={() => setCriando(false)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      {projetos.length === 0 ? (
        <Vazio titulo="Voce ainda nao participa de nenhum projeto." />
      ) : (
        <ul className="lista-cartoes">
          {projetos.map((projeto) => (
            <li key={projeto.id} className="cartao">
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                <Link href={`/projetos/${projeto.id}/board`}>
                  <strong>{projeto.nome}</strong>
                </Link>
                <span className="badge">{projeto.status === 'ATIVO' ? 'Ativo' : 'Finalizado'}</span>
              </div>
              {projeto.descricao && <p>{projeto.descricao}</p>}
              <nav style={{ display: 'flex', gap: 12 }}>
                <Link href={`/projetos/${projeto.id}/board`}>Board</Link>
                <Link href={`/projetos/${projeto.id}/dashboard`}>Dashboard</Link>
                <Link href={`/projetos/${projeto.id}/admin`}>Administracao</Link>
              </nav>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
