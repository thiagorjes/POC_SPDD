-- Recorte de terminais no board (RN-039, RNF-011, SDR-007).
--
-- `data-model.md` secao 8, ordem 8. Migration nova e nao alteracao da ordem 3,
-- que ja foi aplicada: migration aplicada nunca e alterada.
--
-- O board devolve tarefa terminal apenas enquanto o desfecho for dos ultimos
-- 30 dias. O recorte e aplicado NA CONSULTA, contra esta coluna, e nao
-- derivado em tempo de leitura: derivar exigiria agregacao por tarefa sobre
-- `evento_tarefa`, a maior tabela do sistema, encarecendo a leitura na exata
-- proporcao da historia que o recorte existe para esconder (SDR-007).
--
-- O recorte e da tela, nunca do registro: nenhum evento e apagado e nenhum
-- intervalo e fechado por causa dele (RN-022, RNF-008).

ALTER TABLE tarefa
    ADD COLUMN tornou_se_terminal_em timestamptz NULL;

COMMENT ON COLUMN tarefa.tornou_se_terminal_em IS
    'Instante do desfecho — conclusao ou encerramento sem conclusao. NULL '
    'enquanto a tarefa nao e terminal, e volta a NULL na reabertura (RN-019). '
    'Projecao: o log a determina inteiramente, e por isso ela e reconstruivel '
    'por SDR-006, ao contrario de raia_id e descricao. Sustenta o recorte de '
    '30 dias do board (RN-039) e o envelope de RNF-011.';

-- Parcial, e a escolha e de custo e nao de estilo: num projeto vivo a tarefa
-- terminal e a minoria da tabela, e um indice total carregaria em cada entrada
-- a linha que o predicado do board nunca alcanca.
CREATE INDEX tarefa_terminal_por_projeto
    ON tarefa (projeto_id, tornou_se_terminal_em)
    WHERE tornou_se_terminal_em IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Povoamento retroativo.
--
-- O instante vem do log: o `ocorrido_em` do ultimo evento terminal de cada
-- tarefa que HOJE esta em condicao terminal. Tarefa reaberta depois do
-- desfecho nao e alcancada pelo filtro de `condicao`, e por isso continua NULL
-- — que e exatamente o estado correto dela.
--
-- E a mesma propriedade que torna a coluna curavel por SDR-006: derivavel do
-- log sozinho.
UPDATE tarefa t
SET tornou_se_terminal_em = ultimo.ocorrido_em
FROM (
    SELECT DISTINCT ON (e.tarefa_id) e.tarefa_id, e.ocorrido_em
    FROM evento_tarefa e
    WHERE e.tipo IN ('TAREFA_CONCLUIDA', 'TAREFA_ENCERRADA_SEM_CONCLUSAO')
    ORDER BY e.tarefa_id, e.id DESC
) AS ultimo
WHERE t.id = ultimo.tarefa_id
  AND t.condicao IN ('CONCLUIDA', 'ENCERRADA_SEM_CONCLUSAO');
