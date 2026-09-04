'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { useFeedback } from '@/components/FeedbackProvider';
import { EstadoErro, EstadoVazio, Skeleton } from '@/components/Skeleton';
import { api } from '@/lib/api';
import type { ProjetoResponse } from '@/lib/types';

/** TL-02 — Lista de projetos do usuario (RF-008). */
export default function ProjetosPage() {
  const [projetos, setProjetos] = useState<ProjetoResponse[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);
  const [nome, setNome] = useState('');
  const [nomeInvalido, setNomeInvalido] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const feedback = useFeedback();

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro(null);
    try {
      setProjetos(await api.projetos());
    } catch {
      setErro('Não foi possível carregar os projetos.');
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function criar(evento: React.FormEvent) {
    evento.preventDefault();
    if (nome.trim().length === 0) {
      setNomeInvalido(true);
      return;
    }
    setEnviando(true);
    try {
      await api.criarProjeto({ nome: nome.trim() });
      feedback.sucesso('Projeto criado com sucesso.');
      setCriando(false);
      setNome('');
      await carregar();
    } catch (erroCriacao) {
      feedback.reportar(erroCriacao);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <AppShell>
      <div className="page-header">
        <h1>Projetos</h1>
        <button className="btn btn-primary" type="button" onClick={() => setCriando(true)}>
          + Novo projeto
        </button>
      </div>

      {carregando && <Skeleton linhas={3} />}

      {!carregando && erro && <EstadoErro mensagem={erro} aoTentar={() => void carregar()} />}

      {!carregando && !erro && projetos.length === 0 && (
        <EstadoVazio>
          <p>Você ainda não tem projetos associados.</p>
          <button className="btn btn-primary" type="button" onClick={() => setCriando(true)}>
            Criar primeiro projeto
          </button>
        </EstadoVazio>
      )}

      {!carregando && !erro && projetos.length > 0 && (
        <section aria-label="Lista de projetos" className="project-grid">
          {projetos.map((projeto) =>
            projeto.status === 'ATIVO' ? (
              <Link
                key={projeto.id}
                className="card project-card"
                href={`/projetos/${projeto.id}/board`}
              >
                <h3>{projeto.nome}</h3>
                <p className="text-secondary">{projeto.descricao ?? 'Sem descrição'}</p>
                <span className="badge badge-success">Ativo</span>
              </Link>
            ) : (
              <div key={projeto.id} className="card project-card">
                <h3>{projeto.nome}</h3>
                <p className="text-secondary">Somente leitura</p>
                <span className="badge badge-neutral">Finalizado</span>
              </div>
            ),
          )}
        </section>
      )}

      {criando && (
        <div className="modal-overlay">
          <div className="modal" role="dialog" aria-modal="true" aria-labelledby="novo-projeto">
            <div className="page-header">
              <h1 id="novo-projeto">Novo projeto</h1>
              <button
                className="btn btn-text"
                type="button"
                aria-label="Fechar"
                onClick={() => setCriando(false)}
              >
                ✕
              </button>
            </div>
            <form onSubmit={criar} aria-label="Formulário de novo projeto">
              <div className="form-field">
                <label htmlFor="projeto-nome">Nome *</label>
                <input
                  id="projeto-nome"
                  type="text"
                  required
                  aria-required="true"
                  aria-invalid={nomeInvalido}
                  aria-describedby={nomeInvalido ? 'projeto-nome-erro' : undefined}
                  value={nome}
                  onChange={(evento) => {
                    setNome(evento.target.value);
                    setNomeInvalido(false);
                  }}
                />
                {nomeInvalido && (
                  <span id="projeto-nome-erro" className="form-error" role="alert">
                    Informe o nome do projeto.
                  </span>
                )}
              </div>
              <button
                className="btn btn-primary btn--full"
                type="submit"
                disabled={enviando}
                aria-busy={enviando}
              >
                {enviando ? 'Criando…' : 'Criar projeto'}
              </button>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  );
}
