-- Fecho do anel de projecao: a unicidade da sequencia de eventos e a duracao
-- calculada dos intervalos.
--
-- As duas coisas ficam aqui, e nao nas migrations que criam as tabelas, porque
-- e assim que `data-model.md` secao 8 as ordena — a ordem 6 e o passo que
-- torna o log resincronizavel e a serie de tempo agregavel.
--
-- `projeto.seq_atual` **nao** entra aqui: ela nasce na ordem 1, onde
-- TASK-01.3 corretamente a criou (correcao de 2026-09-10, ACH-02). Um
-- ADD COLUMN sobre coluna existente faria a migracao falhar contra qualquer
-- banco ja migrado, com o backend parado esperando pelo servico de migracao.
--
-- Migration aplicada nunca e alterada: correcao vem em migration nova.

-- Resincronizacao por `seq` do ADR-004. Unico porque duplicata torna a lacuna
-- indetectavel — e detectar lacuna e a rede de seguranca que ADR-004 nomeia
-- para RNF-002. O contador vem de `projeto.seq_atual`, incrementado no banco
-- dentro da transacao de escrita (SDR-004); rollback nao deixa buraco porque o
-- incremento reverte junto.
CREATE UNIQUE INDEX evento_tarefa_projeto_seq_unico
    ON evento_tarefa (projeto_id, seq);

-- Duracao calculada e armazenada. Gerada pelo banco e nao escrita pela
-- aplicacao: duracao divergente de (fim - inicio) seria indetectavel e
-- contaminaria os percentis de RF-016 sem deixar rastro. NULL enquanto o
-- intervalo esta em curso, que e o que `fim IS NULL` significa.
ALTER TABLE intervalo_tarefa
    ADD COLUMN duracao interval GENERATED ALWAYS AS (fim - inicio) STORED;

COMMENT ON COLUMN intervalo_tarefa.duracao IS
    'Gerada pelo banco a partir de (fim - inicio). NAO e coluna de total: ela '
    'e a duracao de UM intervalo, e RN-008 continua proibindo somar as tres '
    'series entre si.';

-- Percentis de RF-016. Sem ele o plano e varredura do recorte mais ordenacao
-- por duracao calculada. Parcial em `fim IS NOT NULL` porque intervalo aberto
-- nao tem duracao e nao entra em percentil nenhum.
CREATE INDEX intervalo_por_duracao
    ON intervalo_tarefa (projeto_id, etapa_id, tipo, duracao)
    WHERE fim IS NOT NULL;
