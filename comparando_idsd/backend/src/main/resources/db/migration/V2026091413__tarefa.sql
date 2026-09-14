-- Anel de projecao, parte 1: `tarefa`, o estado corrente.
--
-- A projecao e derivada do log e descartavel (SDR-001). Nada aqui e verdade
-- por si: e a leitura rapida de algo que `evento_tarefa` ja afirmou.
--
-- Migration aplicada nunca e alterada: correcao vem em migration nova.

CREATE TABLE tarefa (
    id              uuid        PRIMARY KEY,
    projeto_id      uuid        NOT NULL REFERENCES projeto (id),
    titulo          text        NOT NULL,
    descricao       text,
    -- Dimensao 1 de RN-002. A etapa nao e condicao, e a FK aponta para uma
    -- linha que nunca e apagada — a remocao de etapa e logica.
    etapa_id        uuid        NOT NULL REFERENCES etapa (id),
    -- Agrupamento visual do cartao (RN-023). So o estado corrente carrega
    -- raia: a serie de tempo nao, e e isso que torna a agregacao por raia
    -- inescrivivel (data-model.md secao 3).
    raia_id         uuid        REFERENCES raia (id),
    -- Dimensao 2 de RN-002.
    condicao        text        NOT NULL,
    -- NAO existe coluna de impedimento, e a ausencia e o mecanismo. A
    -- dimensao 3 e derivada da existencia de linha em `impedimento` com
    -- `desfecho IS NULL` (RN-032). Enquanto `IMPEDIDA` foi valor de
    -- `condicao`, toda movimentacao a sobrescrevia em silencio (INC-01).
    responsavel_id  uuid        REFERENCES usuario (id),
    assumida_em     timestamptz,
    episodio_atual  integer     NOT NULL DEFAULT 1,
    -- Bloqueio otimista de SDR-002. `@Version` no lado da aplicacao.
    versao          bigint      NOT NULL,
    criada_em       timestamptz NOT NULL,

    -- Dominio de RN-003, no banco **alem** da enumeracao em codigo. A
    -- enumeracao protege o caminho que passa pela aplicacao; a restricao
    -- protege o resto. `IMPEDIDA` nao pertence a este dominio.
    CONSTRAINT tarefa_condicao_valida CHECK (condicao IN (
        'AGUARDANDO_TOMADA',
        'EM_CURSO',
        'CONCLUIDA',
        'ENCERRADA_SEM_CONCLUSAO'
    ))
);

COMMENT ON TABLE tarefa IS
    'Estado corrente da tarefa — anel de projecao, derivado de evento_tarefa e '
    'reconstruivel (SDR-001, data-model.md secao 9). As tres dimensoes de '
    'RN-002 sao ortogonais: etapa (etapa_id), condicao (condicao) e marca de '
    'impedimento, que NAO mora aqui e e derivada de impedimento com desfecho '
    'nulo. Nao acrescente coluna de impedimento: a ausencia e o que impede '
    'qualquer escrita sobre tarefa de apagar a marca.';

-- Board de RF-003 e contagem por etapa de RF-015. O prefixo serve tambem a
-- todo filtro que parte do projeto, inclusive a FK `projeto_id`.
CREATE INDEX tarefa_projeto_etapa_condicao
    ON tarefa (projeto_id, etapa_id, condicao);

-- Fila de RF-014, que atravessa projetos: por isso a condicao vem primeiro, e
-- nao o projeto. Parcial porque a fila so olha quem aguarda tomada.
CREATE INDEX tarefa_aguardando_tomada
    ON tarefa (condicao, projeto_id)
    WHERE condicao = 'AGUARDANDO_TOMADA';

-- Devolucao ao pool na remocao de participacao (RN-027).
CREATE INDEX tarefa_responsavel
    ON tarefa (responsavel_id);

-- As duas FKs que nenhum indice acima cobre. `database.md` secao 4 manda
-- indexar toda chave estrangeira: sem isso, remover a linha referenciada
-- varre a tabela inteira — e as duas sao alcancaveis por operacao de
-- configuracao.
CREATE INDEX tarefa_etapa ON tarefa (etapa_id);
CREATE INDEX tarefa_raia ON tarefa (raia_id);
