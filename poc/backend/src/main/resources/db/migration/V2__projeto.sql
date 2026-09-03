-- V2: projeto e seus toggles (conjunto fechado, RF-016 / BDR-001).

CREATE TABLE projeto (
    id            UUID PRIMARY KEY,
    nome          VARCHAR(200) NOT NULL UNIQUE,
    descricao     VARCHAR(4000),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ATIVO'
                  CHECK (status IN ('ATIVO', 'FINALIZADO')),
    finalizado_em TIMESTAMPTZ,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    versao        BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE projeto_toggle (
    projeto_id UUID        NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    chave      VARCHAR(64) NOT NULL
               CHECK (chave IN ('DEV_PODE_EXCLUIR_TAREFA',
                                'DEV_PODE_FINALIZAR_TAREFA',
                                'DEV_PODE_EDITAR_TAREFA_INICIADA',
                                'GESTOR_PODE_VER_BOARD')),
    habilitado BOOLEAN     NOT NULL,
    PRIMARY KEY (projeto_id, chave)
);
