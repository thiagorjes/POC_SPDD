'use client';

import { useState } from 'react';
import { useFeedback } from './Feedback';
import { ApiError, api } from '@/lib/api';
import type { Raia, UsuarioAtual } from '@/lib/types';

const TIPOS = ['FEATURE', 'BUG', 'TAREFA', 'MELHORIA'];

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
  const [tipo, setTipo] = useState(TIPOS[0]);
  const [raiaId, setRaiaId] = useState('');
  const [responsavelId, setResponsavelId] = useState('');
  const [erros, setErros] = useState<Record<string, string>>({});
  const [salvando, setSalvando] = useState(false);

  const criar = async () => {
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
      informar('Tarefa criada.');
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
    <div className="backdrop" role="dialog" aria-modal="true" aria-label="Nova tarefa">
      <div className="modal">
        <h2>Nova tarefa</h2>

        <label>
          Titulo
          <input value={titulo} maxLength={200} onChange={(e) => setTitulo(e.target.value)} />
        </label>
        {erros.titulo && <p className="erro-inline">{erros.titulo}</p>}

        <label>
          Descricao
          <textarea rows={4} value={descricao} onChange={(e) => setDescricao(e.target.value)} />
        </label>
        {erros.descricao && <p className="erro-inline">{erros.descricao}</p>}

        <label>
          Tipo
          <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
            {TIPOS.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </label>

        <label>
          Raia (opcional)
          <select value={raiaId} onChange={(e) => setRaiaId(e.target.value)}>
            <option value="">Raia padrao do projeto</option>
            {raias.map((r) => (
              <option key={r.id} value={r.id}>
                {r.nome}
              </option>
            ))}
          </select>
        </label>

        <label>
          Responsavel (opcional)
          <select value={responsavelId} onChange={(e) => setResponsavelId(e.target.value)}>
            <option value="">Sem responsavel</option>
            {usuarios.map((u) => (
              <option key={u.id} value={u.id}>
                {u.nome}
              </option>
            ))}
          </select>
        </label>

        <div style={{ display: 'flex', gap: 8 }}>
          <button className="primario" disabled={salvando} onClick={criar}>
            Criar
          </button>
          <button onClick={aoFechar}>Cancelar</button>
        </div>
      </div>
    </div>
  );
}
