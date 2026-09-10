-- Massa de carga para RNF-009: 12 meses de historia, 5.000 tarefas.
--
-- Escrita direta no banco, e nao pela API, por uma razao de medicao e nao de
-- conveniencia: 5.000 tarefas com historia completa pela API levariam mais
-- tempo para semear do que a janela inteira do teste, e o que se quer medir e
-- a leitura sobre a massa, nao a escrita que a produziu.
--
-- A contrapartida esta declarada: esta massa nao verifica regra de negocio
-- nenhuma. Ela precisa ser gerada em conformidade com as invariantes que os
-- testes de cenario ja verificam — tres series por tarefa, intervalo fechado
-- com fim, marca de impedimento derivada de `impedimento.desfecho IS NULL` —
-- e qualquer divergencia aqui produz numero de desempenho sobre um formato
-- que o sistema nunca gera.
--
-- Uso: psql -v projeto_id=... -v ator_id=... -f massa-12-meses.sql

\set ON_ERROR_STOP on

BEGIN;

-- 5.000 tarefas distribuidas pelas 4 etapas do fluxo, com data de criacao
-- espalhada pelos 12 meses. A distribuicao uniforme e deliberada: concentrar
-- tudo no mes corrente deixaria os indices por data com seletividade irreal.
WITH etapas AS (
    SELECT id, ordem, row_number() OVER (ORDER BY ordem) - 1 AS indice
      FROM etapa WHERE projeto_id = :'projeto_id'
),
numeros AS (SELECT generate_series(1, 5000) AS n)
INSERT INTO tarefa (id, projeto_id, etapa_id, raia_id, titulo, condicao, criada_em, episodio_atual)
SELECT
    gen_random_uuid(),
    :'projeto_id',
    e.id,
    NULL,
    'Tarefa de carga ' || n.n,
    CASE WHEN n.n % 7 = 0 THEN 'CONCLUIDA'
         WHEN n.n % 3 = 0 THEN 'EM_CURSO'
         ELSE 'AGUARDANDO_TOMADA' END,
    now() - (n.n % 365) * interval '1 day',
    1
  FROM numeros n
  JOIN etapas e ON e.indice = n.n % (SELECT count(*) FROM etapas);

-- Permanencia: uma serie fechada por etapa ja percorrida, mais a aberta na
-- etapa atual. E o volume que faz `tempo-por-etapa` doer.
INSERT INTO intervalo_tarefa (tarefa_id, etapa_id, tipo, episodio, inicio, fim)
SELECT t.id, t.etapa_id, 'PERMANENCIA', 1, t.criada_em, NULL FROM tarefa t
 WHERE t.projeto_id = :'projeto_id';

INSERT INTO intervalo_tarefa (tarefa_id, etapa_id, tipo, episodio, inicio, fim)
SELECT t.id, t.etapa_id, 'ESPERA_TOMADA', 1,
       t.criada_em,
       CASE WHEN t.condicao = 'AGUARDANDO_TOMADA' THEN NULL
            ELSE t.criada_em + interval '4 hours' END
  FROM tarefa t WHERE t.projeto_id = :'projeto_id';

-- Impedimento em um decimo da massa, metade ainda aberta. Sem os abertos, a
-- consulta nunca exercita o caminho de intervalo sem fim, que e o que obriga
-- a agregacao a calcular contra `now()`.
WITH alvo AS (
    SELECT id, etapa_id, criada_em, row_number() OVER () AS n
      FROM tarefa WHERE projeto_id = :'projeto_id' AND id::text LIKE '%0%'
     LIMIT 500
),
aberto AS (
    INSERT INTO impedimento (id, tarefa_id, motivo, aberto_por, aberto_em, desfecho, resolvido_em)
    SELECT gen_random_uuid(), a.id, 'motivo de carga', :'ator_id',
           a.criada_em + interval '1 day',
           CASE WHEN a.n % 2 = 0 THEN 'resolvido na carga' ELSE NULL END,
           CASE WHEN a.n % 2 = 0 THEN a.criada_em + interval '3 days' ELSE NULL END
      FROM alvo a
    RETURNING tarefa_id, aberto_em, resolvido_em
)
INSERT INTO intervalo_tarefa (tarefa_id, etapa_id, tipo, episodio, inicio, fim)
SELECT ab.tarefa_id, t.etapa_id, 'IMPEDIMENTO', 1, ab.aberto_em, ab.resolvido_em
  FROM aberto ab JOIN tarefa t ON t.id = ab.tarefa_id;

COMMIT;

ANALYZE tarefa;
ANALYZE intervalo_tarefa;
ANALYZE impedimento;
