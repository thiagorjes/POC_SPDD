# TASK-07.4 — Exportação do tempo por etapa em CSV

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-07.3
- **Cenários cobertos:** SCN-016.1
- **Origem:** RNF-007, RN-014

#### Contexto

A exportação é o que torna a métrica de sucesso apurável ao fim de cada período —
e a instrumentação dessas métricas precisa ser construída, porque nenhuma existe
hoje. Ela é a mesma consulta em outra representação, e por isso está sob as
mesmas regras.

#### O que deve ser feito

- [ ] Aceitar `Accept: text/csv` na rota de tempo por etapa.
- [ ] Devolver as três séries por etapa, uma linha por etapa e por série.
- [ ] Preservar a marca de baixa massa e a distinção entre não medido e zero.
- [ ] Não oferecer nenhum recorte por pessoa na exportação.
- [ ] Aplicar a mesma autorização e a mesma janela da consulta em JSON.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/consulta/TempoPorEtapaController.java` | alterar | negociação de conteúdo |
| `backend/src/main/java/br/com/idsd/kanban/internal/consulta/TempoPorEtapaCsv.java` | criar | serialização |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

A mesma rota `GET /v1/projetos/{projetoId}/tempo-por-etapa` com
`Accept: text/csv` devolve as três séries por etapa, para leitura ao fim de cada
período. **Sem recorte por pessoa** — a exportação está sob a mesma regra que
qualquer outra saída.

A janela `de` e `ate` vale igual, com o mesmo recorte por sobreposição, e a
autorização é a mesma da consulta em JSON.

Colunas: etapa, série, amostras, média, mediana, percentil 95 e a marca de baixa
massa. Projeto sem histórico não produz linhas de etapa.

#### Guia técnico — pontos de atenção

- **A exportação não é uma segunda consulta.** Ela reusa a mesma agregação; uma
  cópia divergiria da primeira quando a regra mudasse.
- **Nenhuma coluna de pessoa**, em nenhuma circunstância.
- **Não produza linhas com zero quando não há histórico** — a planilha herdaria a
  mentira que a resposta em JSON evita.
- **A marca de baixa massa precisa sobreviver ao formato**, ou quem lê a planilha
  toma amostra única por medida consolidada.
- **Separador e escape precisam ser consistentes** para abertura em planilha sem
  tratamento manual.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | A mesma rota devolve CSV quando o cabeçalho de aceitação o pede | requisição com o cabeçalho |
| 2 | Os números do CSV coincidem com os do JSON para a mesma janela | comparação das duas respostas |
| 3 | Nenhuma coluna identifica pessoa | inspeção do cabeçalho do arquivo |
| 4 | Projeto sem histórico não produz linhas de etapa | exportação em projeto novo |
| 5 | A marca de baixa massa aparece no CSV | exportação com série de amostra única |
| 6 | A autorização é a mesma da consulta em JSON | requisição por sujeito sem acesso |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
