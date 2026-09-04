CREATE TABLE workflow (
    id         UUID PRIMARY KEY,
    projeto_id UUID NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    nome       VARCHAR(200) NOT NULL,
    ativo      BOOLEAN NOT NULL DEFAULT false,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Exatamente um workflow ativo por projeto (A-9).
CREATE UNIQUE INDEX uk_workflow_ativo_por_projeto ON workflow (projeto_id) WHERE ativo;

CREATE TABLE etapa (
    id          UUID PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflow (id) ON DELETE CASCADE,
    nome        VARCHAR(120) NOT NULL,
    ordem       INTEGER NOT NULL CHECK (ordem >= 0),
    etapa_final BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_etapa_ordem UNIQUE (workflow_id, ordem)
);

-- No maximo uma etapa final por workflow (RN-004).
CREATE UNIQUE INDEX uk_etapa_final_por_workflow ON etapa (workflow_id) WHERE etapa_final;

CREATE TABLE transicao (
    id               UUID PRIMARY KEY,
    workflow_id      UUID NOT NULL REFERENCES workflow (id) ON DELETE CASCADE,
    etapa_origem_id  UUID NOT NULL REFERENCES etapa (id) ON DELETE CASCADE,
    etapa_destino_id UUID NOT NULL REFERENCES etapa (id) ON DELETE CASCADE,
    CONSTRAINT uk_transicao UNIQUE (workflow_id, etapa_origem_id, etapa_destino_id),
    CONSTRAINT ck_transicao_sem_self_loop CHECK (etapa_origem_id <> etapa_destino_id)
);

CREATE INDEX idx_transicao_origem ON transicao (etapa_origem_id);
CREATE INDEX idx_transicao_destino ON transicao (etapa_destino_id);
