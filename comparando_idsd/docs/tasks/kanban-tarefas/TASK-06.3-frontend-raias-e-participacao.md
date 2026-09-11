# TASK-06.3 — Frontend: raias na configuração e tela de participação

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-05.5, TASK-06.1, TASK-06.2
- **Cenários cobertos:** SCN-018.1, SCN-019.1
- **Origem:** RF-018, RF-019, telas TL-08 e TL-09, DDR-005

#### Contexto

Duas telas de administração do projeto: as raias, ao lado do fluxo de etapas, e a
participação com os papéis. A segunda é onde o efeito de uma mudança é mais
sensível — remover alguém devolve tarefas ao pool, e a tela precisa dizer isso
antes de confirmar.

#### O que deve ser feito

- [ ] Acrescentar a configuração de raias à tela de configuração do fluxo.
- [ ] Implementar `/projetos/:id/config/participacao` listando participantes e
      papéis.
- [ ] Permitir atribuir papéis acumuláveis, com efeito imediato refletido na
      interface.
- [ ] Avisar, antes de confirmar a remoção, que as tarefas assumidas pela pessoa
      voltarão ao pool.
- [ ] Não oferecer filtro por raia em nenhuma tela de consulta agregada.
- [ ] Garantir conformidade WCAG 2.1 AA.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/app/projetos/[id]/config/participacao/page.tsx` | criar | tela TL-09 |
| `frontend/components/config/Raias.tsx` | criar | editor de raias em TL-08 |
| `frontend/lib/api/raias.ts` | criar | leitura e substituição |
| `frontend/lib/api/participacoes.ts` | criar | listagem, papéis e remoção |
| `frontend/app/projetos/[id]/config/fluxo/page.tsx` | alterar | integra o editor de raias |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/`, e todo arquivo de
verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Protótipos de referência:
`docs/design/kanban-tarefas/prototypes/TL-08-configuracao-do-fluxo.html` e
`TL-09-participacao-e-permissoes.html`.

Chamadas: `GET` e `PUT /v1/projetos/{projetoId}/raias`;
`GET /v1/projetos/{projetoId}/participacoes`;
`PUT /v1/projetos/{projetoId}/participacoes/{usuarioId}` com
`{ papeis: [ ... ] }`; `DELETE /v1/projetos/{projetoId}/participacoes/{usuarioId}`,
que responde `204`.

Papéis são acumuláveis e o seletor precisa permitir múltipla escolha, com o
catálogo vindo do serviço — nunca uma lista fixa no cliente.

#### Guia técnico — pontos de atenção

- **A remoção tem efeito colateral que a pessoa não vê.** Avisar sobre a
  devolução das tarefas antes de confirmar é requisito da tela, não cortesia.
- **O catálogo de papéis vem do serviço.** Uma lista fixa no cliente sai de
  sincronia em silêncio.
- **Nunca ofereça filtro por raia nas consultas agregadas** — nem desabilitado.
- **Papel atribuído vale imediatamente**, e a interface precisa refletir isso sem
  exigir novo login.
- **Nenhuma informação apenas por cor** na identificação de papéis.
- Esconder controle de quem não configura é conveniência; a recusa real é do
  serviço.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Raias configuradas aparecem como agrupamento no board | percurso em navegador |
| 2 | Atribuir papel muda o que a pessoa vê e pode fazer sem novo login | percurso com duas sessões |
| 3 | A remoção avisa que as tarefas assumidas voltarão ao pool antes de confirmar | inspeção do diálogo |
| 4 | Nenhuma tela de consulta agregada oferece filtro por raia | inspeção das telas de andamento e de tempo por etapa |
| 5 | O seletor de papéis permite múltipla escolha a partir do catálogo do serviço | inspeção da requisição que popula o seletor |
| 6 | As telas passam em auditoria de acessibilidade AA | verificação automatizada sem violação de nível AA |
| 7 | **RNF-005** — TL-09 é funcional a 1280 px e a 1024 px de largura, sem perda de ação nem rolagem horizontal não indicada | verificação automatizada nas duas larguras extremas (ACH-16 da revisão de TASK-01.7) |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
