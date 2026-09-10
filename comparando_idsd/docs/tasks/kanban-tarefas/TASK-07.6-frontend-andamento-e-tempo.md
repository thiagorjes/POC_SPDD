# TASK-07.6 — Frontend: andamento e tempo por etapa

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-07.2, TASK-07.4, TASK-07.5
- **Cenários cobertos:** SCN-015.1, SCN-016.1, SCN-016.3
- **Origem:** RF-015, RF-016, tela TL-07, DDR-005

#### Contexto

A tela do gestor e a da medição, na mesma rota. É onde a restrição estrutural
fica visível: não há recorte por pessoa em lugar nenhum, e não há botão de ação —
visibilidade sem interferência foi a condição para que o registro fosse aceito
pelo time.

#### O que deve ser feito

- [ ] Implementar `/projetos/:id/andamento` com o painel de andamento e o de
      tempo por etapa.
- [ ] Exibir a quantidade por etapa e a lista de impedimentos abertos com o
      tempo acumulado.
- [ ] Exibir as três séries lado a lado, sem nenhum total.
- [ ] Exibir a ressalva de baixa massa junto do valor, nunca omitindo o número.
- [ ] Exibir "ainda não medido" quando não há histórico, e nunca zeros.
- [ ] Oferecer a janela de datas e a exportação em CSV.
- [ ] Não oferecer nenhum recorte por pessoa nem filtro por raia.
- [ ] Garantir conformidade WCAG 2.1 AA.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/app/projetos/[id]/andamento/page.tsx` | criar | tela TL-07 |
| `frontend/components/consulta/TempoPorEtapa.tsx` | criar | três séries lado a lado |
| `frontend/components/consulta/Impedimentos.tsx` | criar | lista com tempo acumulado |
| `frontend/lib/api/consultas.ts` | criar | andamento, tempo por etapa e exportação |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/`, e todo arquivo de
verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Protótipo de referência:
`docs/design/kanban-tarefas/prototypes/TL-07-andamento-tempo-por-etapa.html`.

Chamadas: `GET /v1/projetos/{projetoId}/andamento`;
`GET /v1/projetos/{projetoId}/tempo-por-etapa?de=&ate=`; e a mesma rota com
`Accept: text/csv` para a exportação.

Quando a resposta traz não medido, a tela diz "ainda não medido" e não desenha
gráfico com zeros. Quando a série vem marcada como baixa massa, o valor aparece
**com a ressalva ao lado**, explicando que média, mediana e percentil coincidem
por haver amostra única.

O mais antigo em curso, por etapa e por série, é exibido separado das amostras
fechadas.

#### Guia técnico — pontos de atenção

- **Nenhum controle de escrita nesta tela**, nem escondido: a resposta não traz
  ação e a tela não inventa nenhuma.
- **Nenhum recorte por pessoa em nenhum ponto** — nem seletor desabilitado, nem
  agrupamento, nem legenda.
- **Não some as três séries em um total, nem em um gráfico empilhado.**
  Empilhamento é soma visual e viola a mesma regra.
- **Não desenhe zeros quando não há medição.**
- **A ressalva de baixa massa acompanha o valor**, e não o substitui.
- **Nenhuma informação apenas por cor** — as três séries precisam de rótulo ou
  padrão além do tom, o que também importa para daltonismo.
- **Sem filtro por raia**, nem na interface.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | O gestor acompanha etapas e impedimentos sem nenhum controle de escrita | inspeção da tela com papel de leitura |
| 2 | As três séries aparecem lado a lado, sem total e sem empilhamento | inspeção visual |
| 3 | Projeto sem histórico exibe "ainda não medido" e nenhum zero | percurso em projeto novo |
| 4 | Série de amostra única exibe o valor com a ressalva ao lado | percurso com um único intervalo fechado |
| 5 | A janela de datas altera o recorte e a exportação respeita a mesma janela | comparação entre tela e arquivo |
| 6 | Nenhum recorte por pessoa e nenhum filtro por raia são oferecidos | inspeção dos controles |
| 7 | A tela passa em auditoria de acessibilidade AA | verificação automatizada sem violação de nível AA |
| 8 | As séries são distinguíveis sem depender de cor | inspeção em modo monocromático |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
