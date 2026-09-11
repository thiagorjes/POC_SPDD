# TASK-07.3 — Tempo por etapa com as três séries

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-016.1, SCN-016.2, SCN-016.3, SCN-018.3
- **Origem:** RF-016, RN-008, RN-014, RN-019, RN-023, RN-025, RN-029

#### Contexto

É a consulta que produz a linha de base que hoje não existe, e a que carrega as
duas regras mais fáceis de erodir do sistema: as três séries nunca se somam, e
nada é agregado por pessoa. As duas são garantidas por ausência — de campo de
total e de coluna de pessoa —, e esta task é onde essa ausência precisa ser
mantida deliberadamente.

#### O que deve ser feito

- [ ] Implementar `GET /v1/projetos/{projetoId}/tempo-por-etapa`, lendo os
      intervalos e nunca o log.
- [ ] Recortar a janela por **sobreposição**, e não pelo início do intervalo.
- [ ] Considerar amostra apenas o intervalo fechado.
- [ ] Devolver, por etapa e por tipo, o mais antigo ainda em curso em campo
      próprio.
- [ ] Devolver as três séries lado a lado, sem nenhum campo de total.
- [ ] Devolver "não medido" quando não há histórico, em vez de zeros.
- [ ] Marcar baixa massa quando houver amostra única, exibindo o valor com
      ressalva.
- [ ] Recusar com `400` qualquer recorte por pessoa e qualquer filtro por raia.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/consulta/TempoPorEtapaController.java` | criar | rota agregada |
| `backend/src/main/java/br/com/idsd/kanban/internal/consulta/TempoPorEtapaQuery.java` | criar | agregação por etapa e por tipo |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`GET /v1/projetos/{projetoId}/tempo-por-etapa`

- **Consulta:** `?de=<ISO-8601>&ate=<ISO-8601>`, opcionais. A janela recorta por
  **sobreposição**, não por início: intervalo que começou antes do limite
  inferior e terminou dentro da janela entra. Recortar pelo início descartaria
  justamente os intervalos longos, que são os que interessam.
- **Só intervalo fechado é amostra.** Intervalo em curso fica fora do agregado —
  incluí-lo faria a contagem de amostras mudar a cada consulta. O caso que isso
  deixaria de fora volta em campo próprio: o mais antigo em curso, por etapa e
  por tipo, com o instante de início e o decorrido.
- **Saída `200`:**
  `{ "medido": true, "porEtapa": [ { "etapaId", "etapaNome", "permanencia": { "amostras", "media", "p50", "p95", "baixaMassa", "emCursoMaisAntigo" }, "esperaTomada": { ... }, "impedimento": { ... } } ] }`
- **A etapa do impedimento é a vigente na abertura.** Sem esse instantâneo todo
  impedimento cairia num grupo único.
- **As três séries vêm lado a lado e nunca somadas.** Não existe campo de total
  no corpo, e a ausência é deliberada.
- **Sem histórico:** `{ "medido": false }`, sem a lista por etapa. Não é a lista
  com zeros — "ainda não medido" e "zero" são afirmações opostas.
- **Baixa massa:** verdadeiro quando há menos de duas amostras. O valor é exibido
  **com ressalva, nunca omitido**: amostra única produz média, mediana e
  percentil iguais entre si, e é essa coincidência que a ressalva avisa.
- **Não há parâmetro de pessoa, nem campo de pessoa na resposta**, e a tabela de
  origem não tem coluna de pessoa. Recorte por pessoa por qualquer caminho é
  `400`.
- **Nenhum filtro por raia é aceito.**
- **Episódios:** o agregado soma os episódios de tarefa reaberta; a série por
  tarefa os distingue, no detalhe da tarefa.

#### Guia técnico — pontos de atenção

- **Não acrescente campo de total**, nem "soma dos três" para conveniência da
  tela. Ele é o convite a tratar as séries como partes de um mesmo tempo.
- **Não acrescente parâmetro de pessoa**, nem para uso interno. A regra é
  estrutural, e nenhum teste falha quando alguém acrescenta um filtro — é por isso
  que a recusa explícita existe.
- **Recortar por início em vez de sobreposição é o erro silencioso desta task**:
  os números continuam saindo, só que sem os casos que importam.
- **Zero e não medido são respostas diferentes.**
- **A ressalva de baixa massa não omite o valor.** Omitir esconderia o único dado
  disponível quando o projeto começa.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | As três séries vêm por etapa, lado a lado, e não existe campo de total | inspeção do corpo |
| 2 | Intervalo iniciado antes da janela e terminado dentro dela entra no agregado | consulta com janela recortando o meio de um intervalo longo |
| 3 | Intervalo em curso não conta como amostra e aparece como o mais antigo em curso | leitura com tarefa parada há muito tempo |
| 4 | Projeto sem histórico devolve não medido, sem lista de etapas | consulta em projeto novo |
| 5 | Série com amostra única vem marcada como baixa massa, com o valor presente | leitura após um único intervalo fechado |
| 6 | Série com duas ou mais amostras vem sem ressalva | leitura após dois intervalos fechados |
| 7 | Recorte por pessoa é recusado com `400` por qualquer caminho | tentativa por parâmetro e por corpo |
| 8 | Filtro por raia é recusado | tentativa com o parâmetro |
| 9 | O agregado soma os episódios da tarefa reaberta e o detalhe por tarefa os separa | percurso com reabertura |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
