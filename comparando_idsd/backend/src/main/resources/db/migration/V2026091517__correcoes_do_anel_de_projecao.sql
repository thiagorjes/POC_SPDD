-- Correcoes do anel de projecao e do comentario do anel de verdade.
--
-- Fecha ACH-02, ACH-03, ACH-06 e ACH-07 da revisao de TASK-02.3, apos as
-- emendas de data-model.md na TechSpec v1.15.
--
-- Migration aplicada nunca e alterada: por isso as correcoes das ordens 3, 4 e
-- 5 entram aqui, e nao la.

-- ---------------------------------------------------------------------------
-- ACH-03: bloqueio otimista em `impedimento`.
--
-- A segunda sinalizacao de RN-010 nao insere linha: ela le o array de
-- `anotacoes`, anexa um elemento e regrava o documento inteiro (SCN-009.3).
-- Duas sinalizacoes concorrentes leem o mesmo array e a ultima grava por cima,
-- e uma anotacao some sem erro e sem rastro — a perda de escrita silenciosa
-- que SDR-002 existe para impedir e que `tarefa` ja evita.
--
-- O unico parcial sobre `tarefa_id` NAO cobre isso: ele impede dois
-- impedimentos ABERTOS, nao duas escritas no mesmo impedimento.
ALTER TABLE impedimento
    ADD COLUMN versao bigint NOT NULL DEFAULT 0;

-- O DEFAULT existe so para a linha ja gravada, que nao ha nenhuma: a partir
-- daqui quem atribui e o @Version do Hibernate.
ALTER TABLE impedimento
    ALTER COLUMN versao DROP DEFAULT;

COMMENT ON COLUMN impedimento.versao IS
    'Bloqueio otimista (SDR-002). Existe por causa de `anotacoes`, cuja '
    'escrita e leitura-modificacao-escrita sobre documento JSON: sem versao, a '
    'segunda sinalizacao concorrente sobrescreve a primeira em silencio.';

-- ---------------------------------------------------------------------------
-- ACH-06: integridade referencial DENTRO do anel de projecao.
--
-- Regra unica, agora escrita em data-model.md §5: referencia dentro do anel e
-- chave estrangeira; referencia para fora do anel nao e.
--
-- Sem cascata de proposito. A tarefa nao e removida fisicamente em nenhum
-- caminho do produto; a restricao existe para tornar a linha orfa impossivel,
-- nao para propagar remocao. E o que da a rotina de reconstrucao (§9) a ordem
-- que ela pressupoe — apagar impedimento e intervalo antes de `tarefa` — como
-- garantia do banco, e nao como disciplina de quem escreve a rotina.
ALTER TABLE intervalo_tarefa
    ADD CONSTRAINT intervalo_tarefa_fk FOREIGN KEY (tarefa_id)
        REFERENCES tarefa (id);

ALTER TABLE impedimento
    ADD CONSTRAINT impedimento_tarefa_fk FOREIGN KEY (tarefa_id)
        REFERENCES tarefa (id);

-- `projeto_id` nas tres tabelas e `etapa_id` em `intervalo_tarefa` seguem sem
-- FK, e isso agora e decisao declarada e nao omissao: sao instantaneos
-- historicos, nao vinculos vivos. Etapa arquivada ou substituida pela
-- reconfiguracao de fluxo nao pode invalidar a serie de tempo que ja a
-- atravessou.

-- ---------------------------------------------------------------------------
-- ACH-07: a coluna-chave morta do indice da fila.
--
-- Dentro de um indice parcial cujo predicado fixa `condicao`, a coluna
-- `condicao` e constante em toda tupla: nunca discrimina, ocupa espaco em cada
-- entrada e empurra `projeto_id` para a segunda posicao. O indice novo serve a
-- mesma consulta de RF-014, menor.
DROP INDEX tarefa_aguardando_tomada;

CREATE INDEX tarefa_aguardando_tomada
    ON tarefa (projeto_id)
    WHERE condicao = 'AGUARDANDO_TOMADA';

-- ---------------------------------------------------------------------------
-- ACH-02: o comentario de catalogo afirmava garantia que nao esta em vigor.
--
-- O comentario final da migration de ordem 4 registrava honestamente que a
-- revogacao ainda nao surte efeito, mas comentario de arquivo .sql nao viaja
-- para o catalogo: quem le a tabela por \d+ recebia so a versao que afirma a
-- garantia dupla. A ressalva passa a viajar junto.
COMMENT ON TABLE evento_tarefa IS
    'Anel de verdade: log somente de insercao (SDR-001). Sem UPDATE e sem '
    'DELETE. A garantia foi desenhada dupla — nenhum metodo de repositorio os '
    'expoe e o grupo aplicacao_kanban recebe apenas SELECT, INSERT —, mas HOJE '
    'so a primeira metade esta em vigor: a aplicacao conecta como dono do '
    'schema e superusuario, contra quem a revogacao e inerte (ACH-01 da '
    'revisao de TASK-02.3, pendencia 21). Nao ha FK para tarefa: o log '
    'sobrevive a projecao, que e descartavel e refeita do zero.';
