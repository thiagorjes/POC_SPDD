-- V7: intervalos temporais persistidos — fonte de verdade do lead-time (RF-006/RF-007, RN-002).
-- Timestamps sempre gerados pelo banco: nenhuma coluna de tempo e preenchida pela JVM (R-6).

CREATE TABLE periodo_etapa (
    id           UUID PRIMARY KEY,
    tarefa_id    UUID        NOT NULL REFERENCES tarefa (id) ON DELETE CASCADE,
    projeto_id   UUID        NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    etapa_id     UUID        NOT NULL REFERENCES etapa (id) ON DELETE RESTRICT,
    iniciado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    encerrado_em TIMESTAMPTZ,
    CONSTRAINT ck_periodo_etapa_ordem_temporal
        CHECK (encerrado_em IS NULL OR encerrado_em >= iniciado_em)
);

-- No maximo um periodo de etapa aberto por tarefa.
CREATE UNIQUE INDEX uq_periodo_etapa_aberto
    ON periodo_etapa (tarefa_id) WHERE encerrado_em IS NULL;

CREATE INDEX idx_periodo_etapa_dashboard
    ON periodo_etapa (projeto_id, etapa_id, iniciado_em);

CREATE TABLE periodo_impedimento (
    id           UUID PRIMARY KEY,
    tarefa_id    UUID        NOT NULL REFERENCES tarefa (id) ON DELETE CASCADE,
    projeto_id   UUID        NOT NULL REFERENCES projeto (id) ON DELETE CASCADE,
    etapa_id     UUID        NOT NULL REFERENCES etapa (id) ON DELETE RESTRICT,
    motivo       VARCHAR(500),
    iniciado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    encerrado_em TIMESTAMPTZ,
    CONSTRAINT ck_periodo_impedimento_ordem_temporal
        CHECK (encerrado_em IS NULL OR encerrado_em >= iniciado_em)
);

-- No maximo um periodo de impedimento aberto por tarefa.
CREATE UNIQUE INDEX uq_periodo_impedimento_aberto
    ON periodo_impedimento (tarefa_id) WHERE encerrado_em IS NULL;

CREATE INDEX idx_periodo_impedimento_dashboard
    ON periodo_impedimento (projeto_id, etapa_id, iniciado_em);
