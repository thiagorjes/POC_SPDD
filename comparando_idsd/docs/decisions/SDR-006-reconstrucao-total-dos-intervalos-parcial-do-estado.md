---
id: SDR-006
type: SDR
status: accepted
date: 2026-09-15
supersedes: —
superseded-by: —
---

# SDR-006 — Reconstrução total da série de tempo, parcial e declarada do estado

## Decisão

A rotina de reconstrução da projeção tem **dois alcances diferentes**, e a
diferença é propriedade do desenho, não limitação de implementação:

- **`intervalo_tarefa` — reconstrução total.** A série de tempo é integralmente
  determinada pelo log: o catálogo de `data-model.md` §4 diz, para cada tipo de
  evento, que intervalo fecha e que intervalo abre, e `ocorrido_em` dá o
  instante. A rotina reproduz a série inteira, e divergência entre a série
  gravada e a reexecutada é defeito.

- **`tarefa` e `impedimento` — reconstrução parcial, sobre linha existente.** A
  rotina reescreve **apenas os campos que o log determina** e preserva os
  demais. Ela **não cria e não remove** linha nessas duas tabelas: o conjunto de
  tarefas e de impedimentos de um projeto não é sua saída, é seu insumo.

Os campos determinados pelo log, e portanto reescritos:

| Tabela | Reescrito pela rotina | Preservado |
| --- | --- | --- |
| `tarefa` | `etapa_id`, `condicao`, `responsavel_id`, `assumida_em`, `episodio_atual` | `titulo`, `descricao`, `raia_id`, `versao`, `criada_em` |
| `impedimento` | `desfecho`, `resolvido_por`, `anotacoes` | `id`, `intervalo_id`, `motivo`, `aberto_por`, `versao` |

## Motivação

ACH-02 da revisão de TASK-02.4: a §9 prometia reescrever as três tabelas **do
zero**, e o log não sustenta a promessa. Três lacunas, de naturezas distintas:

- **`raia_id` não existe em evento nenhum.** E isso não é esquecimento: §5
  declara que só o estado corrente carrega a raia, e que é justamente essa
  ausência na série de tempo que torna a agregação por raia impossível (RN-023).
  Acrescentá-la ao log para viabilizar a reconstrução desfaria a garantia que a
  ausência produz.
- **`descricao` não tem evento que a registre**, porque não há cenário
  congelado de edição de descrição. Inventar o evento aqui seria requisito
  nascido na TechSpec, sem cenário e sem procedência.
- **`impedimento.id` é UUID gerado e não está no log.** Reconstruir do zero
  produziria outro identificador, e `intervalo_tarefa.id` é FK a partir dele —
  foi exatamente por isso que a implementação de TASK-02.4 precisou casar linha
  gravada com linha reexecutada e reescrever no lugar, em vez de apagar e
  inserir. O esquema já impedia o "do zero"; a spec é que não sabia.

**A promessa estreitada é a que SDR-001 sempre fundamentou.** O título daquele
DR é "eventos imutáveis com projeção **de intervalos**". A alegação de
descartabilidade sempre foi sobre a série de tempo — que é onde o risco mora,
porque é ela que se constrói por acumulação de escritas e é ela que uma escrita
fora do caminho canônico corromperia em silêncio. `tarefa.titulo` errado é
visível na primeira tela; `intervalo_tarefa` com um fechamento a menos não é
visível em lugar nenhum até alguém somar.

## Alternativas descartadas

**Ampliar o log até a promessa original ficar verdadeira** — acrescentar
`raia_id` ao evento, criar evento de edição de descrição, gravar a identidade do
impedimento. Descartada por três razões independentes, e a primeira bastaria:
registrar a raia na série de tempo **desfaz RN-023**, que é regra de negócio e
não detalhe de modelagem. Além disso, exigiria migration sobre o esquema de
TASK-02.3, já implementado e medido, e "editar descrição" não tem cenário
congelado — seria devolução ao `/prd`.

**Manter a promessa e aceitar que a rotina a descumpra** — deixar o texto como
está e tratar a divergência como defeito de implementação. Descartada porque é o
pior dos mundos: o critério 6 permaneceria insatisfazível por implementação
correta nenhuma, e todo `/implement` futuro gastaria o orçamento tentando
alcançar um alvo que o esquema torna inalcançável.

## Consequências

- **O limite fica declarado, e é um limite real:** `raia_id`, `descricao` e
  `titulo` corrompidos **não são curáveis** pela reconstrução. Se vierem a
  precisar de cura, o caminho é outro — e a decisão de abrir esse caminho é do
  `/prd`, não desta rotina.
- A rotina pressupõe que a linha exista. Tarefa presente no log e ausente de
  `tarefa` é **divergência que ela reporta e não corrige** — reconstruir a linha
  exigiria o título, que o log só carrega se o formato de `dados` o garantir.
- O critério 6 passa a ser satisfazível: a comparação é entre a série de tempo
  capturada e a reexecutada, mais os campos determinados de `tarefa` e
  `impedimento`, e não entre as tabelas inteiras.
- `versao` é reescrita pelo bloqueio otimista como em qualquer atualização; ela
  não é preservada no sentido de conservar o número, e sim de não ser derivada
  do log.
