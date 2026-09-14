-- Anel de projecao, parte 2: as tres series de tempo e o impedimento.
--
-- Migration aplicada nunca e alterada: correcao vem em migration nova.

CREATE TABLE intervalo_tarefa (
    id         bigserial   PRIMARY KEY,
    tarefa_id  uuid        NOT NULL,
    projeto_id uuid        NOT NULL,
    -- Etapa vigente na **abertura** do intervalo. Em IMPEDIMENTO e
    -- instantaneo, nao vinculo: sem ele, o bloco de impedimento por etapa que
    -- RF-016 devolve cairia num unico grupo nulo. Nao fere RN-009, que proibe
    -- a movimentacao encerrar ou reiniciar a contagem — e nao exige que o
    -- impedimento seja anonimo quanto a etapa.
    etapa_id   uuid        NOT NULL,
    -- PERMANENCIA, ESPERA_TOMADA, IMPEDIMENTO.
    tipo       text        NOT NULL,
    episodio   integer     NOT NULL,
    inicio     timestamptz NOT NULL,
    -- NULL = em curso.
    fim        timestamptz

    -- NAO ha coluna de pessoa, e nao havera (RN-014). A ausencia e o que torna
    -- estrutural a proibicao de agregar tempo por pessoa: sem coluna, nao ha o
    -- que agrupar. Acrescenta-la "so para o log" nao deixaria nenhum teste de
    -- cenario vermelho, e seis meses depois uma consulta a usaria.
    --
    -- NAO ha coluna de total (RN-008). As tres series nunca se somam entre si,
    -- e uma coluna de soma seria o convite a faze-lo.
    --
    -- A coluna gerada `duracao` entra na migration de ordem 6.
);

COMMENT ON TABLE intervalo_tarefa IS
    'As tres series de tempo da tarefa: PERMANENCIA, ESPERA_TOMADA e '
    'IMPEDIMENTO. Um mesmo instante pode estar coberto pelas tres da mesma '
    'tarefa — nao ha restricao de nao sobreposicao ENTRE tipos —, mas ha no '
    'maximo um intervalo aberto POR tipo por tarefa. Sem coluna de pessoa '
    '(RN-014) e sem coluna de total (RN-008), e as duas ausencias sao a regra '
    'e nao economia de esquema.';

-- Recorte da janela de RF-016.
CREATE INDEX intervalo_recorte
    ON intervalo_tarefa (projeto_id, etapa_id, tipo, inicio);

-- No maximo um intervalo aberto por tipo por tarefa. E o que impede o
-- impedimento duplicado de RN-010 no nivel do banco, e nao so na regra de
-- servico. Parcial em `fim IS NULL`: intervalo fechado se repete a vontade,
-- que e a serie historica.
CREATE UNIQUE INDEX intervalo_aberto_por_tipo
    ON intervalo_tarefa (tarefa_id, tipo)
    WHERE fim IS NULL;

-- Soma por episodio de RN-019 (SCN-013.3).
CREATE INDEX intervalo_por_episodio
    ON intervalo_tarefa (tarefa_id, episodio);

CREATE TABLE impedimento (
    id            uuid  PRIMARY KEY,
    tarefa_id     uuid  NOT NULL,
    -- Desnormalizado como no resto do anel: RF-014 filtra pelo conjunto de
    -- projetos da pessoa e RF-015 por um projeto.
    projeto_id    uuid  NOT NULL,
    -- Aponta para o intervalo de tipo IMPEDIMENTO. E a unica FK do anel de
    -- projecao para a serie de tempo, e existe porque o desfecho precisa saber
    -- qual intervalo fechar.
    intervalo_id  bigint NOT NULL REFERENCES intervalo_tarefa (id),
    motivo        text  NOT NULL,
    -- RN-010: a segunda sinalizacao anexa aqui, sem criar impedimento novo nem
    -- reiniciar contagem (SCN-009.3).
    anotacoes     jsonb NOT NULL DEFAULT '[]'::jsonb,
    -- Habilita SCN-010.2: quem sinalizou pode resolver.
    aberto_por    uuid  NOT NULL REFERENCES usuario (id),
    desfecho      text,
    resolvido_por uuid  REFERENCES usuario (id),
    resolvido_em  timestamptz
);

COMMENT ON TABLE impedimento IS
    'Dimensao 3 de RN-002, e a unica fonte dela: a marca de impedimento e '
    'derivada da existencia de linha aqui com desfecho IS NULL, nunca '
    'replicada em tarefa. So o registro do desfecho a apaga (RN-032), e isso e '
    'propriedade do esquema.';

COMMENT ON COLUMN impedimento.resolvido_em IS
    'Instante do desfecho. Nao consta da tabela de campos de TASK-02.3 nem de '
    'data-model.md secao 5, e entrou porque a suite congelada o exige: '
    'ImpedimentoServiceTest le e escreve resolvidoEm para verificar que a '
    'resolucao repetida de SCN-010.3 nao reescreve o instante ja registrado. '
    'Registrado como divergencia no historico de TASK-02.3.';

-- No maximo um impedimento aberto por tarefa (RN-010). Unico e parcial: a
-- tarefa acumula impedimentos resolvidos sem limite.
CREATE UNIQUE INDEX impedimento_aberto_por_tarefa
    ON impedimento (tarefa_id)
    WHERE desfecho IS NULL;

-- Listas de impedimento aberto de RF-014 e RF-015, e a marca do board de
-- RF-003 — a dimensao 3 e derivada, entao toda leitura que a exibe passa por
-- aqui. **Nao** e unico: um projeto tem quantos impedimentos abertos vierem.
CREATE INDEX impedimento_aberto_por_projeto
    ON impedimento (projeto_id)
    WHERE desfecho IS NULL;

-- As FKs que nenhum indice acima cobre (`database.md` secao 4).
CREATE INDEX impedimento_intervalo ON impedimento (intervalo_id);
CREATE INDEX impedimento_aberto_por_pessoa ON impedimento (aberto_por);
CREATE INDEX impedimento_resolvido_por ON impedimento (resolvido_por);

-- Projecao e gravavel inteira, inclusive apagavel: e ela que a rotina de
-- reconstrucao (data-model.md secao 9) refaz do zero. A restricao e do log.
GRANT SELECT, INSERT, UPDATE, DELETE ON intervalo_tarefa, impedimento
    TO aplicacao_kanban;
GRANT USAGE, SELECT ON SEQUENCE intervalo_tarefa_id_seq TO aplicacao_kanban;
