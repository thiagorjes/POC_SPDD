# TASK-07.2 — Andamento do projeto em modo somente-leitura

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-015.1, SCN-015.2, SCN-015.3
- **Origem:** RF-015, RN-014, RN-015, RN-026

#### Contexto

É a visão do gestor, e ela é somente-leitura por decisão estrutural: dar
visibilidade sem abrir interferência foi a condição para que o time aceitasse o
registro. A resposta não contém ação nem link de escrita, e a recusa é do
serviço, não da tela.

#### O que deve ser feito

- [ ] Implementar `GET /v1/projetos/{projetoId}/andamento`.
- [ ] Devolver a quantidade de tarefas por etapa e a lista de impedimentos
      abertos.
- [ ] Permitir o acesso ao papel de leitura, sem nenhuma ação na resposta.
- [ ] Recusar com `403` toda escrita solicitada por quem só tem leitura.
- [ ] Exibir o tempo acumulado do impedimento antigo sem acionar nada por
      decurso de prazo.
- [ ] Não oferecer nem aceitar nenhum recorte por pessoa.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/consulta/AndamentoController.java` | criar | rota de andamento |
| `backend/src/main/java/br/com/idsd/kanban/internal/consulta/AndamentoQuery.java` | criar | contagem por etapa e impedimentos abertos |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`GET /v1/projetos/{projetoId}/andamento`

- **Saída `200`:**
  `{ "porEtapa": [ { "etapaId", "etapaNome", "ordem", "terminal", "quantidade" } ], "impedimentosAbertos": [ { "tarefaId", "titulo", "etapaNome", "motivo", "impedidaDesde", "impedimentoDecorrido" } ] }`
- Acessível ao papel de leitura em modo somente-leitura. A resposta **não contém
  nenhuma ação nem link de escrita**, e toda escrita solicitada por outro caminho
  é recusada com `403`.
- Impedimento antigo aparece com o tempo acumulado à mostra e **nada é acionado**
  por decurso de prazo. Não há agendador neste sistema.
- Nenhum recorte por pessoa é oferecido nem aceito.

#### Guia técnico — pontos de atenção

- **Não crie agendador, alerta nem escalonamento por prazo.** A ausência é
  decisão registrada: o tempo contado é o que permitirá reabrir esse assunto com
  evidência, mais tarde.
- **A recusa de escrita é do serviço.** Omitir o botão na tela não é
  autorização.
- **Nenhum parâmetro de pessoa é aceito**, nem ignorado em silêncio: parâmetro
  desconhecido é recusa.
- **A contagem por etapa exclui as arquivadas.**

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | O andamento mostra a quantidade de tarefas por etapa e os impedimentos abertos | leitura após percurso conhecido |
| 2 | O papel de leitura acessa a consulta | requisição com esse papel |
| 3 | A resposta não contém nenhuma ação nem link de escrita | inspeção do corpo |
| 4 | Escrita solicitada por quem só tem leitura é recusada com `403` | tentativa em cada rota de escrita |
| 5 | Impedimento antigo aparece com o tempo acumulado e nada é acionado | leitura após impedimento de longa duração |
| 6 | Recorte por pessoa é recusado | requisição com parâmetro de pessoa |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
