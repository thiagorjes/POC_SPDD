# Contrato — fila pessoal e consultas agregadas

_Cobre RF-014, RF-015, RF-016 | Base: `/v1`_
_Normas: `_shared/api-standards.md`, SDR-001, BDR-002_

---

## `GET /v1/fila` — RF-014

Atravessa **todos** os projetos em que a pessoa participa. Não recebe `projetoId`.

- **Consulta:** `?ordem=maiorEspera|menorEspera` — padrão `maiorEspera`, o mais
  parado no topo (QD-04, SCN-014.1). `menorEspera` é a inversão que SCN-014.2
  exige.
- **Saída `200`:**
  ```
  {
    "aguardandoTomada": [ { "tarefaId", "titulo", "projeto": { "id", "nome" },
                            "etapa": { "id", "nome" }, "esperaDesde", "esperaDecorrida" } ],
    "impedimentosSobMinhaResponsabilidade": [ { "tarefaId", "titulo", "projeto",
                            "motivo", "impedidaDesde", "impedimentoDecorrido" } ]
  }
  ```
- A segunda lista traz os impedimentos abertos dos projetos em que a pessoa tem
  permissão de desbloqueio; nos projetos sem ninguém com a permissão, traz para
  todos os participantes (RN-024, BDR-002, SCN-019.3).
- Fila vazia: `200` com as duas listas vazias e **nenhum tempo** no corpo
  (SCN-014.3). Zero não é resposta aqui — é RN-025 aplicado à fila.
- `gestor` recebe as duas listas vazias no projeto em que só tem leitura: não há o
  que assumir.

---

## `GET /v1/projetos/{projetoId}/andamento` — RF-015

- **Saída `200`:**
  ```
  {
    "porEtapa": [ { "etapaId", "etapaNome", "ordem", "terminal", "quantidade" } ],
    "impedimentosAbertos": [ { "tarefaId", "titulo", "etapaNome", "motivo",
                               "impedidaDesde", "impedimentoDecorrido" } ]
  }
  ```
- Acessível a `gestor` em modo somente-leitura. A resposta **não contém nenhuma
  ação nem link de escrita**, e toda escrita solicitada por outro caminho é
  recusada com `403` (RN-015, SCN-015.2).
- Impedimento antigo aparece com o tempo acumulado à mostra e **nada é acionado**
  por decurso de prazo (RN-026, SCN-015.3). Não há agendador neste sistema.
- Nenhum recorte por pessoa é oferecido nem aceito (RN-014).

---

## `GET /v1/projetos/{projetoId}/tempo-por-etapa` — RF-016

A consulta que produz a linha de base que hoje não existe. Lê `intervalo_tarefa`,
nunca o log.

- **Consulta:** `?de=<ISO-8601>&ate=<ISO-8601>` — opcionais. A janela recorta por
  **sobreposição**, não por `inicio`: intervalo que começou antes de `de` e
  terminou dentro da janela entra. Recortar por `inicio` descartaria justamente os
  intervalos longos, que são os que interessam.

**Só intervalo fechado é amostra.** `fim IS NULL` fica fora do agregado — incluí-lo
faria `amostras` mudar a cada consulta, e a média de uma coisa que ainda não
terminou não é comparável com a de coisas que terminaram. O caso que isso deixaria
de fora é o que mais importa ao gestor, e por isso ele volta em campo próprio:
`emCursoMaisAntigo` por etapa e por tipo, com `desde` e `decorrido`. É a mesma
informação que RF-015 já mostra em lista, agora agregada.
- **Saída `200`:**
  ```
  {
    "medido": true,
    "porEtapa": [ {
      "etapaId", "etapaNome",
      "permanencia":   { "amostras", "media", "p50", "p95", "baixaMassa": false,
                         "emCursoMaisAntigo": { "desde", "decorrido" } | null },
      "esperaTomada":  { "amostras", "media", "p50", "p95", "baixaMassa": false,
                         "emCursoMaisAntigo": { "desde", "decorrido" } | null },
      "impedimento":   { "amostras", "media", "p50", "p95", "baixaMassa": false,
                         "emCursoMaisAntigo": { "desde", "decorrido" } | null }
    } ]
  }
  ```

**A etapa do impedimento é a vigente na abertura** (`data-model.md` §5). Sem esse
instantâneo o bloco `impedimento` dentro de cada etapa não existiria: todo
impedimento cairia num grupo único, e as etapas mostrariam a mesma coisa ou nada.
O instantâneo não fere RN-009, que proíbe a movimentação encerrar ou reiniciar a
contagem — e ela continua não encerrando nem reiniciando.

**As três séries vêm lado a lado e nunca somadas** (RN-008, SCN-016.1). Não existe
campo de total no corpo, e essa ausência é deliberada: um campo de soma seria o
convite a tratá-las como partes de um mesmo tempo.

**Sem histórico:** `{ "medido": false }`, sem `porEtapa`. Não é `porEtapa` com
zeros — RN-025 e SCN-016.3 tratam "ainda não medido" e "zero" como afirmações
opostas, e devolver zero seria mentir com número.

**Baixa massa:** `baixaMassa: true` quando `amostras < 2` — isto é, apenas a série
de amostra única. A partir de duas amostras a série vem sem ressalva. O valor é
exibido **com ressalva, nunca omitido** (RN-029): amostra única produz média,
p50 e p95 iguais entre si, e é essa coincidência — não a ausência de número — que
a ressalva avisa ao leitor. Decidido pelo demandante em 2026-09-09 (Q-001).

**RN-014 é estrutural:** não há parâmetro de pessoa, não há campo de pessoa na
resposta, e a tabela de origem não tem coluna de pessoa (`data-model.md` §5).
Solicitação de recorte por pessoa por qualquer caminho é `400` — parâmetro
desconhecido —, e é o que SCN-016.2 verifica.

**Episódios:** o agregado soma os episódios de tarefa reaberta e a série por
tarefa os distingue (RN-019, SCN-013.3). O detalhe por tarefa vem em
`GET /v1/tarefas/{tarefaId}` com os intervalos agrupados por episódio.

**Exportação (RNF-007):** a mesma rota com `Accept: text/csv` devolve as três
séries por etapa, para leitura ao fim de cada período. Sem recorte por pessoa —
a exportação está sob RN-014 como qualquer outra saída.

---

## `GET /v1/projetos/{projetoId}/raias` como filtro — não existe

Nenhuma rota desta página aceita `raiaId`. RN-023 e SCN-018.3: raia não entra em
agregação, e a ausência do parâmetro é a forma de garanti-lo.
