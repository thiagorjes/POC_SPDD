CREATE TABLE raia (
    id         UUID PRIMARY KEY,
    projeto_id UUID NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    nome       VARCHAR(120) NOT NULL,
    ordem      INTEGER NOT NULL CHECK (ordem >= 0),
    padrao     BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_raia_ordem UNIQUE (projeto_id, ordem)
);

-- Exatamente uma raia padrao por projeto (RN-CB-005, A-7).
CREATE UNIQUE INDEX uk_raia_padrao_por_projeto ON raia (projeto_id) WHERE padrao;
