CREATE TABLE periodo_etapa (
    id           UUID PRIMARY KEY,
    tarefa_id    UUID NOT NULL REFERENCES tarefa (id) ON DELETE CASCADE,
    projeto_id   UUID NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    etapa_id     UUID NOT NULL REFERENCES etapa (id) ON DELETE RESTRICT,
    iniciado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    encerrado_em TIMESTAMPTZ,
    CONSTRAINT ck_periodo_etapa_intervalo CHECK (encerrado_em IS NULL OR encerrado_em >= iniciado_em)
);

-- Invariante: no maximo um periodo de etapa aberto por tarefa.
CREATE UNIQUE INDEX uk_periodo_etapa_aberto ON periodo_etapa (tarefa_id) WHERE encerrado_em IS NULL;
CREATE INDEX idx_periodo_etapa_dashboard ON periodo_etapa (projeto_id, etapa_id, iniciado_em);

CREATE TABLE periodo_impedimento (
    id           UUID PRIMARY KEY,
    tarefa_id    UUID NOT NULL REFERENCES tarefa (id) ON DELETE CASCADE,
    projeto_id   UUID NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    etapa_id     UUID NOT NULL REFERENCES etapa (id) ON DELETE RESTRICT,
    motivo       VARCHAR(500),
    iniciado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    encerrado_em TIMESTAMPTZ,
    CONSTRAINT ck_periodo_impedimento_intervalo CHECK (encerrado_em IS NULL OR encerrado_em >= iniciado_em)
);

-- Invariante: no maximo um periodo de impedimento aberto por tarefa.
CREATE UNIQUE INDEX uk_periodo_impedimento_aberto ON periodo_impedimento (tarefa_id) WHERE encerrado_em IS NULL;
CREATE INDEX idx_periodo_impedimento_dashboard ON periodo_impedimento (projeto_id, etapa_id, iniciado_em);
