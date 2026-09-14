-- Anel de verdade: `evento_tarefa`. Log somente de insercao, fonte de tudo o
-- que a projecao afirma (SDR-001).
--
-- Migration aplicada nunca e alterada: correcao vem em migration nova.

CREATE TABLE evento_tarefa (
    -- Ordem total de gravacao. E `bigserial` e nao uuid porque a reconstrucao
    -- da projecao (data-model.md secao 9) le em ordem de `id`.
    id               bigserial   PRIMARY KEY,
    tarefa_id        uuid        NOT NULL,
    -- Desnormalizado de proposito: todo filtro de consulta parte do projeto, e
    -- o log nao pode depender de juntar-se a projecao para ser lido.
    projeto_id       uuid        NOT NULL,
    -- Catalogo fechado (data-model.md secao 4). Sem restricao de verificacao:
    -- o catalogo cresce a cada epico, e uma restricao aqui transformaria cada
    -- tipo novo numa migration sobre a tabela mais sensivel do sistema.
    tipo             text        NOT NULL,
    ocorrido_em      timestamptz NOT NULL,
    -- Quem agiu. NULL em evento decorrente de configuracao — por exemplo a
    -- devolucao ao pool por remocao de participacao (RN-027).
    --
    -- E o unico lugar do sistema onde a pessoa aparece ligada a um instante, e
    -- isso e deliberado: RN-014 proibe **agregar tempo** por pessoa, nao
    -- guardar quem fez o que. Ver `intervalo_tarefa`, que nao tem coluna de
    -- pessoa nenhuma.
    ator_id          uuid,
    -- 1 na criacao; incrementa a cada reabertura (RN-019).
    episodio         integer     NOT NULL,
    etapa_origem_id  uuid,
    etapa_destino_id uuid,
    condicao_origem  text,
    condicao_destino text,
    -- Sequencia por projeto, para o payload do NOTIFY e a resincronizacao do
    -- cliente (ADR-004). Atribuida no banco, dentro da transacao de escrita,
    -- por UPDATE projeto SET seq_atual = seq_atual + 1 ... RETURNING (SDR-004).
    -- A unicidade de (projeto_id, seq) entra na migration de ordem 6.
    seq              bigint      NOT NULL,
    -- Motivo do impedimento, desfecho, titulo na criacao. **Nunca dado de
    -- cliente** (IDSD 4.10.1).
    dados            jsonb
);

COMMENT ON TABLE evento_tarefa IS
    'Anel de verdade: log somente de insercao (SDR-001). Sem UPDATE e sem '
    'DELETE, e a garantia e dupla — nenhum metodo de repositorio os expoe e a '
    'role de aplicacao recebe apenas SELECT, INSERT. Nao ha FK para tarefa: o '
    'log sobrevive a projecao, que e descartavel e reconstruida do zero pela '
    'rotina da secao 9 do data-model. Nao crie gatilho de NOTIFY aqui — a '
    'publicacao e da aplicacao, em afterCommit (SDR-004), e dois publicadores '
    'fariam o cliente contabilizar seq repetido como estado inconsistente.';

-- Historico da tarefa e reconstrucao da projecao: os dois leem por tarefa, em
-- ordem de gravacao.
CREATE INDEX evento_tarefa_por_tarefa ON evento_tarefa (tarefa_id, id);

-- ------------------------------------------------------------------------
-- Concessao restrita (data-model.md secao 8, ordem 4)
-- ------------------------------------------------------------------------
--
-- A role e de **grupo**, sem login. A role de login da aplicacao recebe estes
-- privilegios por pertencimento, e nunca por concessao direta: assim o
-- conjunto do que a aplicacao pode fazer fica declarado aqui, versionado, e
-- nao espalhado pela configuracao de cada ambiente.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'aplicacao_kanban') THEN
        CREATE ROLE aplicacao_kanban NOLOGIN;
    END IF;
END
$$;

-- O log: le e acrescenta, nada mais. Nem TRUNCATE, que contorna DELETE e
-- apagaria a verdade inteira — e o caminho que mais aparece em script de
-- limpeza mal calibrado.
GRANT SELECT, INSERT ON evento_tarefa TO aplicacao_kanban;
GRANT USAGE, SELECT ON SEQUENCE evento_tarefa_id_seq TO aplicacao_kanban;
REVOKE UPDATE, DELETE, TRUNCATE ON evento_tarefa FROM PUBLIC;
REVOKE UPDATE, DELETE, TRUNCATE ON evento_tarefa FROM aplicacao_kanban;

-- A projecao continua inteiramente gravavel: a restricao e do log, e nao do
-- banco. Uma role somente-leitura quebraria o sistema em vez de proteger o
-- log.
GRANT SELECT, INSERT, UPDATE, DELETE ON
    usuario, projeto, participacao, participacao_papel, tarefa
    TO aplicacao_kanban;

-- Criterio de aceite 10 desta task, vindo de ACH-09 da revisao de TASK-02.1: a
-- remocao de `etapa` e de `raia` e **logica**, por `arquivada_em`. Apagar a
-- linha destruiria a serie de tempo por etapa que RF-016 existe para produzir,
-- e a garantia estava de um lado so — a dos repositorios, que nao publicam
-- remocao fisica. Aqui ela ganha o segundo lado.
GRANT SELECT, INSERT, UPDATE ON etapa, raia TO aplicacao_kanban;
REVOKE DELETE, TRUNCATE ON etapa, raia FROM PUBLIC;
REVOKE DELETE, TRUNCATE ON etapa, raia FROM aplicacao_kanban;

-- O que falta, e falta fora desta migration: a role de LOGIN da aplicacao
-- precisa ser membro de `aplicacao_kanban` e NAO pode ser superusuario nem
-- dona das tabelas. Superusuario ignora privilegio; o dono reconcede a si
-- mesmo o que foi revogado. Hoje a aplicacao conecta como o dono do schema,
-- que no compose e no Testcontainers e o proprio superusuario bootstrap — de
-- modo que **as revogacoes acima nao surtem efeito ainda** e a garantia segue
-- de um lado so. Criar a role de login, montar seu segredo e apontar
-- `BANCO_USUARIO` para ela e mudanca de `docker/compose.yaml`, fora do escopo
-- declarado desta task. Registrado no historico de TASK-02.3.
--
-- Comentario de catalogo (`COMMENT ON ROLE`) ficou de fora de proposito: ele
-- exige superusuario, e uma migration que so aplica sob superusuario contradiz
-- exatamente o desenho que ela instala.

