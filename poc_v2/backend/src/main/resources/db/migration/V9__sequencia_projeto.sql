CREATE TABLE sequencia_projeto (
    projeto_id UUID PRIMARY KEY REFERENCES projeto (id) ON DELETE CASCADE,
    ultimo_seq BIGINT NOT NULL DEFAULT 0
);
