-- V3: papeis acumulaveis escopados ao par (usuario, projeto) — BDR-001.

CREATE TABLE usuario_projeto_papel (
    usuario_id      UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    projeto_id      UUID        NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    papel_id        UUID        NOT NULL REFERENCES papel (id)   ON DELETE RESTRICT,
    atribuido_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
    atribuido_por_id UUID       REFERENCES usuario (id) ON DELETE SET NULL,
    PRIMARY KEY (usuario_id, projeto_id, papel_id)
);

CREATE INDEX idx_upp_projeto ON usuario_projeto_papel (projeto_id);
CREATE INDEX idx_upp_usuario ON usuario_projeto_papel (usuario_id);
