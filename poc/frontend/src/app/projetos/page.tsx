'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { Erro, Skeleton } from '@/components/Estados';
import { useFeedback } from '@/components/Feedback';
import { Badge } from '@/components/ui/Badge';
import { Botao } from '@/components/ui/Botao';
import { EstadoVazio } from '@/components/ui/EstadoVazio';
import { PageHeader } from '@/components/ui/PageHeader';
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
      <PageHeader
        titulo="Projetos"
        acoes={
          eu?.adminGlobal && (
            <Botao variante="primary" onClick={() => setCriando((v) => !v)}>
              + Novo projeto
            </Botao>
          )
        }
      />

      {criando && (
        <form className="card" style={{ marginBottom: 'var(--espaco-scale-lg)' }} onSubmit={criar}>
          <div className="form-field">
            <label htmlFor="projeto-nome">Nome</label>
            <input
              id="projeto-nome"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              required
              maxLength={120}
            />
          </div>
          <div className="form-field">
            <label htmlFor="projeto-descricao">Descrição</label>
            <textarea
              id="projeto-descricao"
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              rows={3}
            />
          </div>
          <div className="modal-actions">
            <Botao variante="outline" type="button" onClick={() => setCriando(false)}>
              Cancelar
            </Botao>
            <Botao variante="primary" type="submit">
              Criar
            </Botao>
          </div>
        </form>
      )}

      {projetos.length === 0 ? (
        <EstadoVazio
          mensagem="Você ainda não participa de nenhum projeto."
          acao={
            eu?.adminGlobal && (
              <Botao variante="primary" onClick={() => setCriando(true)}>
                Criar primeiro projeto
              </Botao>
            )
          }
        />
      ) : (
        <div className="project-grid">
          {/*
            O card inteiro e o link (TL-02). Contadores de tarefas do prototipo nao sao exibidos:
            a resposta de /api/projetos nao os fornece.
          */}
          {projetos.map((projeto) => (
            <Link
              key={projeto.id}
              className="card project-card"
              href={`/projetos/${projeto.id}/board`}
            >
              <h3>{projeto.nome}</h3>
              {projeto.descricao && <p className="text-secondary">{projeto.descricao}</p>}
              <Badge variante={projeto.status === 'ATIVO' ? 'success' : 'neutro'}>
                {projeto.status === 'ATIVO' ? 'Ativo' : 'Finalizado'}
              </Badge>
            </Link>
          ))}
        </div>
      )}
    </>
  );
}
