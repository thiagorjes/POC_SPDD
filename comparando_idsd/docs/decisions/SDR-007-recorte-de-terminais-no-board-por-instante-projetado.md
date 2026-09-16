---
id: SDR-007
type: SDR
status: accepted
date: 2026-09-16
supersedes: —
superseded-by: —
---

# SDR-007 — Recorte de terminais no board por instante projetado

## Decisão

O recorte de 30 dias que RN-039 impõe ao board é aplicado **na consulta**, por
comparação com uma coluna projetada em `tarefa`:

- Nasce `tarefa.tornou_se_terminal_em` (`timestamptz NULL`), escrita pelos
  eventos de conclusão e de encerramento sem conclusão, **anulada** pela
  reabertura, e reconstruível pela rotina de SDR-006 — o log a determina
  inteiramente, ao contrário de `raia_id` e `descricao`.
- O board acrescenta à consulta de tarefas o predicado
  `condicao não terminal OR tornou_se_terminal_em >= :agora - 30 dias`, servido
  pelo índice parcial `(projeto_id, tornou_se_terminal_em)`.
- `:agora` é o instante único da transação `readOnly` da leitura, e não
  `now()` avaliado por linha.
- **O recorte é da tela, nunca do registro.** Nenhum evento é apagado, nenhum
  intervalo é fechado, e a tarefa recortada continua íntegra em
  `GET /v1/tarefas/{tarefaId}` e nas consultas de RF-015 e RF-016 (RN-022,
  RNF-008).

## Motivação

ACH-03 da revisão técnica do board: a leitura mais exercitada do produto
devolvia **toda** tarefa já criada no projeto — sem recorte, sem janela e sem
paginação — e não havia envelope de tempo de resposta que a cobrisse. RNF-009
cobre RF-015 e RF-016, não RF-003.

O critério de aceite existente — "o número de consultas não cresce com a
quantidade de tarefas" — é um **proxy** e não substitui o envelope: ele limita
idas ao banco e não limita nada do que custa num board de projeto antigo, que é
materializar e serializar linhas que ninguém vai olhar. Um board correto por
esse critério pode levar minutos.

O demandante decidiu o par: RN-039 recorta e RNF-011 mede, com p95 ≤ 2 s sobre
5.000 tarefas — o mesmo volume de referência de RNF-009, para que um único
arnês de carga sintética sirva aos dois.

## Alternativas descartadas

**Derivar o instante em tempo de leitura.** O dado existe: o último evento
terminal de cada tarefa tem `ocorrido_em`, e o último `PERMANENCIA` fechado
tem `fim`. Recusada porque é agregação por tarefa dentro da consulta do board —
subconsulta correlacionada ou `join` com `group by` sobre `evento_tarefa`, que é
a maior tabela do sistema e cresce para sempre. O recorte existe para baratear a
leitura; derivá-lo a encareceria exatamente na proporção da história que ele
pretende esconder.

**Paginar o board.** Recusada porque o board não é lista: é grade de células, e
a página teria de ser da célula e não da resposta, o que muda o contrato de
todas as telas e contradiz SCN-003.2 — a etapa vazia por paginação é
indistinguível da etapa vazia por ausência de trabalho.

**Recortar por `criada_em` em vez de pelo desfecho.** Recusada porque o board
mostra trabalho, e trabalho parado há um ano é a coisa que mais precisa
aparecer. A idade que autoriza a saída da tela é a do **desfecho**, não a do
nascimento.

**Coluna sem índice parcial.** Recusada por medida de custo, não de estilo: num
projeto vivo a tarefa terminal é a minoria da tabela, e índice total carregaria
em cada entrada a linha que o predicado nunca alcança.

## Consequências

- **Reabrir devolve a tarefa ao board pela porta comum** — ela deixa de ser
  terminal e o recorte não a alcança mais. Anular a coluna na reabertura não é
  detalhe: preservá-la manteria fora do board uma tarefa reaberta há dez
  minutos, cujo desfecho anterior é de meses atrás.
- **A janela de 30 dias é constante de produto e não de configuração.** Ela
  entra no PRD como RN-039 e não como parâmetro; torná-la ajustável por projeto
  criaria requisito que o gate de spec não viu. Gatilho de reabertura: reclamação
  de time que perde de vista trabalho concluído dentro do ciclo dele, se o ciclo
  for maior que a janela.
- **Nasce migration retroativa** (`data-model.md` §8, ordem 8): o povoamento
  inicial da coluna vem do log, o que é a mesma propriedade que a torna curável
  por SDR-006.
- **RNF-011 só é mensurável com o recorte em vigor.** Medir o envelope sobre a
  consulta sem recorte mediria outra coisa, e passaria ou falharia pelo volume
  de história do projeto de teste em vez de pelo desenho.
