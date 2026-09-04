'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AppShell } from '@/components/AppShell';
import { useFeedback } from '@/components/FeedbackProvider';
import { Skeleton } from '@/components/Skeleton';
import { usePermissoes } from '@/hooks/usePermissoes';
import { api } from '@/lib/api';
import { PERMISSAO, type ChaveToggle, type ProjetoResponse } from '@/lib/types';

/**
 * Catalogo fechado de papeis (BDR-001) espelhado do seed V1__usuario_papel_permissao.sql. Nao ha
 * endpoint que exponha a matriz papel x permissao — a tabela e apresentacional e somente leitura
 * (RN-014: o catalogo nao e editavel pela aplicacao).
 */
const PAPEIS = [
  { codigo: 'admin', nome: 'Administrador Global', global: true },
  { codigo: 'project_admin', nome: 'Administrador do Projeto', global: false },
  { codigo: 'product_owner', nome: 'Product Owner', global: false },
  { codigo: 'dev', nome: 'Desenvolvedor', global: false },
  { codigo: 'gestor', nome: 'Gestor', global: false },
  { codigo: 'user', nome: 'Usuário', global: false },
];

const PERMISSOES = [
  { codigo: PERMISSAO.PROJETO_ADMINISTRAR, descricao: 'Administrar o projeto' },
  { codigo: PERMISSAO.PROJETO_VISUALIZAR, descricao: 'Visualizar o projeto e seu board' },
  { codigo: PERMISSAO.WORKFLOW_GERENCIAR, descricao: 'Gerenciar workflows, etapas e transições' },
  { codigo: PERMISSAO.RAIA_GERENCIAR, descricao: 'Gerenciar raias' },
  { codigo: PERMISSAO.USUARIO_ASSOCIAR, descricao: 'Associar usuários e papéis ao projeto' },
  { codigo: PERMISSAO.TAREFA_GERENCIAR, descricao: 'Criar, editar e excluir tarefas' },
  { codigo: PERMISSAO.TAREFA_MOVER, descricao: 'Mover tarefas entre etapas' },
  { codigo: PERMISSAO.TAREFA_FINALIZAR, descricao: 'Mover para a etapa final e desfinalizar' },
  { codigo: PERMISSAO.TAREFA_IMPEDIR, descricao: 'Marcar e desmarcar impedimento' },
  { codigo: PERMISSAO.TAREFA_ATRIBUIR, descricao: 'Atribuir tarefa a terceiros' },
  { codigo: PERMISSAO.DASHBOARD_VISUALIZAR, descricao: 'Visualizar o dashboard de lead-time' },
];

/** Mapeamento default papel -> permissoes, identico ao seed (RN-011, RN-013, RN-014). */
const MATRIZ: Record<string, readonly string[]> = {
  admin: PERMISSOES.map((permissao) => permissao.codigo),
  project_admin: PERMISSOES.map((permissao) => permissao.codigo),
  product_owner: [
    PERMISSAO.PROJETO_VISUALIZAR,
    PERMISSAO.TAREFA_GERENCIAR,
    PERMISSAO.TAREFA_MOVER,
    PERMISSAO.TAREFA_FINALIZAR,
    PERMISSAO.TAREFA_IMPEDIR,
    PERMISSAO.TAREFA_ATRIBUIR,
    PERMISSAO.DASHBOARD_VISUALIZAR,
  ],
  dev: [
    PERMISSAO.PROJETO_VISUALIZAR,
    PERMISSAO.TAREFA_GERENCIAR,
    PERMISSAO.TAREFA_MOVER,
    PERMISSAO.TAREFA_IMPEDIR,
    PERMISSAO.DASHBOARD_VISUALIZAR,
  ],
  gestor: [PERMISSAO.PROJETO_VISUALIZAR, PERMISSAO.DASHBOARD_VISUALIZAR],
  user: [],
};

const TOGGLES: { chave: ChaveToggle; rotulo: string; ajuda: string }[] = [
  {
    chave: 'DEV_PODE_EXCLUIR_TAREFA',
    rotulo: 'Dev pode excluir tarefa',
    ajuda: 'Quando desabilitado, o dev não vê a ação de excluir card.',
  },
  {
    chave: 'DEV_PODE_FINALIZAR_TAREFA',
    rotulo: 'Dev pode finalizar tarefa',
    ajuda: 'Permite ao dev mover cards para a etapa final (RN-011).',
  },
  {
    chave: 'DEV_PODE_EDITAR_TAREFA_INICIADA',
    rotulo: 'Dev pode editar tarefa iniciada',
    ajuda: 'Libera edição de campos estruturais após o início (RF-003).',
  },
  {
    chave: 'GESTOR_PODE_VER_BOARD',
    rotulo: 'Gestor pode ver board',
    ajuda: 'Quando desabilitado, o gestor acessa apenas o dashboard (RN-013).',
  },
];

/** TL-09 — Papéis, permissões e toggles do projeto (RF-013, RF-014). */
export default function AdminPapeisPage() {
  const projetoId = String(useParams().id);
  const feedback = useFeedback();
  const permissoes = usePermissoes(projetoId);

  const [projeto, setProjeto] = useState<ProjetoResponse | null>(null);
  const [valores, setValores] = useState<Record<string, boolean>>({});
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const [projetoCarregado, togglesCarregados] = await Promise.all([
        api.projeto(projetoId),
        api.toggles(projetoId),
      ]);
      setProjeto(projetoCarregado);
      setValores(togglesCarregados);
    } catch (erro) {
      feedback.reportar(erro);
    } finally {
      setCarregando(false);
    }
    // feedback e estavel; a carga depende apenas do projeto.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projetoId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function alternar(chave: ChaveToggle, ativo: boolean) {
    const anterior = valores;
    const proximo = { ...valores, [chave]: ativo };
    setValores(proximo);
    setSalvando(true);
    try {
      setValores(await api.atualizarToggles(projetoId, { toggles: proximo }));
      feedback.sucesso('Configuração salva com sucesso.');
    } catch (erro) {
      setValores(anterior);
      feedback.reportar(erro);
    } finally {
      setSalvando(false);
    }
  }

  const podeAdministrar = permissoes.possui(PERMISSAO.PROJETO_ADMINISTRAR);
  const bloqueado = !podeAdministrar || !permissoes.projetoAtivo || salvando;

  return (
    <AppShell projetoId={projetoId} projetoNome={projeto?.nome}>
      <div className="page-header">
        <h1>Papéis e Permissões</h1>
        <div className="page-header__actions">
          <Link href={`/projetos/${projetoId}/admin`} className="btn btn-outline">
            Workflow
          </Link>
          <Link href={`/projetos/${projetoId}/admin/usuarios`} className="btn btn-outline">
            Usuários
          </Link>
        </div>
      </div>

      <section className="card" aria-label="Matriz de papéis e permissões">
        <h2 className="section-title">Matriz de permissões (somente leitura)</h2>
        <p className="text-secondary">
          O catálogo de papéis e permissões é fechado e não editável pela aplicação. Ajustes de
          comportamento por projeto são feitos pelos toggles abaixo.
        </p>
        <table>
          <thead>
            <tr>
              <th scope="col">Permissão</th>
              {PAPEIS.map((papel) => (
                <th key={papel.codigo} scope="col">
                  {papel.nome}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {PERMISSOES.map((permissao) => (
              <tr key={permissao.codigo}>
                <th scope="row">
                  {permissao.descricao}
                  <br />
                  <span className="text-secondary">{permissao.codigo}</span>
                </th>
                {PAPEIS.map((papel) => {
                  const concedida = (MATRIZ[papel.codigo] ?? []).includes(permissao.codigo);
                  return (
                    <td key={papel.codigo}>
                      <span
                        aria-label={concedida ? 'Concedida' : 'Não concedida'}
                        className={concedida ? 'badge badge-success' : 'badge badge-neutral'}
                      >
                        {concedida ? '✓' : '—'}
                      </span>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="card" aria-label="Toggles do projeto">
        <h2 className="section-title">Toggles de comportamento</h2>
        {carregando && <Skeleton linhas={4} />}
        {!carregando &&
          TOGGLES.map((toggle) => (
            <div key={toggle.chave} className="form-field toggle">
              <label htmlFor={`toggle-${toggle.chave}`}>{toggle.rotulo}</label>
              <input
                id={`toggle-${toggle.chave}`}
                type="checkbox"
                disabled={bloqueado}
                aria-describedby={`toggle-${toggle.chave}-desc`}
                checked={valores[toggle.chave] === true}
                onChange={(evento) => void alternar(toggle.chave, evento.target.checked)}
              />
              <span id={`toggle-${toggle.chave}-desc`} className="text-secondary">
                {toggle.ajuda}
              </span>
            </div>
          ))}
      </section>
    </AppShell>
  );
}
