'use client';

import { useState } from 'react';
import { useFeedback } from './Feedback';
import { Botao } from './ui/Botao';
import { ApiError, api } from '@/lib/api';
import type { Raia, UsuarioAtual } from '@/lib/types';

const TIPOS: { valor: string; rotulo: string }[] = [
  { valor: 'FEATURE', rotulo: 'Feature' },
  { valor: 'BUG', rotulo: 'Bug' },
  { valor: 'TAREFA', rotulo: 'Tarefa' },
  { valor: 'MELHORIA', rotulo: 'Melhoria' },
];

/** TL-05. A tarefa sempre nasce na etapa de menor ordem do workflow ativo (RN-CB-004). */
export function NovaTarefaModal({
  projetoId,
  raias,
  usuarios,
  aoFechar,
  aoCriar,
}: {
  projetoId: string;
  raias: Raia[];
  usuarios: UsuarioAtual[];
  aoFechar: () => void;
  aoCriar: () => void;
}) {
  const { informar, reportar } = useFeedback();
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [tipo, setTipo] = useState(TIPOS[0].valor);
  const [raiaId, setRaiaId] = useState('');
  const [responsavelId, setResponsavelId] = useState('');
  const [erros, setErros] = useState<Record<string, string>>({});
  const [salvando, setSalvando] = useState(false);

  const criar = async (evento: React.FormEvent) => {
    evento.preventDefault();
    setSalvando(true);
    setErros({});
    try {
      await api.criarTarefa(projetoId, {
        titulo,
        descricao: descricao || undefined,
        tipo,
        raiaId: raiaId || undefined,
        responsavelId: responsavelId || undefined,
      });
      informar('Card criado.');
      aoCriar();
      aoFechar();
    } catch (e) {
      if (e instanceof ApiError && e.campos.length > 0) {
        setErros(Object.fromEntries(e.campos.map((c) => [c.campo, c.mensagem])));
      } else {
        reportar(e);
      }
    } finally {
      setSalvando(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="nova-tarefa-titulo">
        <div className="page-header">
          <h1 id="nova-tarefa-titulo" style={{ fontSize: 'var(--fonte-scale-lg)' }}>
            Novo card
          </h1>
          <div>
            <Botao variante="text" aria-label="Fechar" onClick={aoFechar}>
              ✕
            </Botao>
          </div>
        </div>

        <form onSubmit={criar}>
          <div className="form-field">
            <label htmlFor="nt-titulo">Título</label>
            <input
              id="nt-titulo"
              value={titulo}
              maxLength={200}
              required
              aria-required="true"
              aria-invalid={erros.titulo ? 'true' : undefined}
              aria-describedby={erros.titulo ? 'nt-titulo-erro' : undefined}
              placeholder="Resumo objetivo do que precisa ser feito"
              onChange={(e) => setTitulo(e.target.value)}
            />
            {erros.titulo && (
              <span className="form-error" id="nt-titulo-erro" role="alert">
                {erros.titulo}
              </span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="nt-descricao">Descrição</label>
            <textarea
              id="nt-descricao"
              rows={4}
              value={descricao}
              aria-invalid={erros.descricao ? 'true' : undefined}
              aria-describedby={erros.descricao ? 'nt-descricao-erro' : undefined}
              onChange={(e) => setDescricao(e.target.value)}
            />
            {erros.descricao && (
              <span className="form-error" id="nt-descricao-erro" role="alert">
                {erros.descricao}
              </span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="nt-tipo">Tipo</label>
            <select id="nt-tipo" value={tipo} onChange={(e) => setTipo(e.target.value)}>
              {TIPOS.map((t) => (
                <option key={t.valor} value={t.valor}>
                  {t.rotulo}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="nt-raia">Raia (opcional)</label>
            <select id="nt-raia" value={raiaId} onChange={(e) => setRaiaId(e.target.value)}>
              <option value="">Raia padrão do projeto</option>
              {raias.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="nt-responsavel">Responsável (opcional)</label>
            <select
              id="nt-responsavel"
              value={responsavelId}
              onChange={(e) => setResponsavelId(e.target.value)}
            >
              <option value="">Sem responsável</option>
              {usuarios.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.nome}
                </option>
              ))}
            </select>
          </div>

          <Botao variante="primary" type="submit" className="full" carregando={salvando}>
            {salvando ? 'Criando…' : 'Criar card'}
          </Botao>
        </form>
      </div>
    </div>
  );
}
