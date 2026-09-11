-- Anel de configuracao do fluxo: as etapas que o board tem e as raias que o
-- dividem. A etapa e a primeira das tres dimensoes de estado da tarefa.
--
-- Migration aplicada nunca e alterada: correcao vem em migration nova.

CREATE TABLE etapa (
    id           uuid        PRIMARY KEY,
    projeto_id   uuid        NOT NULL REFERENCES projeto (id),
    -- Mutavel. Renomear vale dali em diante e nunca reescreve o historico,
    -- porque a serie de tempo segue o `id` e nao o nome (RN-023).
    nome         text        NOT NULL,
    -- Define adjacencia para a regra de transicao (RN-021).
    ordem        integer     NOT NULL,
    -- RN-001 exige ao menos uma por projeto. O piso e regra de servico e nao
    -- do esquema: nenhuma restricao de tabela expressa "ao menos uma linha
    -- satisfaz P", e a substituicao do fluxo e inteira numa transacao.
    terminal     boolean     NOT NULL DEFAULT false,
    -- A remocao e logica. Apagar a etapa fisicamente destruiria a serie de
    -- tempo: historico e intervalos a referenciam para sempre.
    arquivada_em timestamptz
);

-- Unico **parcial**: duas etapas ativas nao compartilham ordem no mesmo
-- projeto, e arquivar libera a ordem para outra. Restricao total impediria
-- reusar a ordem de uma etapa desativada, que e operacao legitima.
CREATE UNIQUE INDEX etapa_projeto_ordem_unico
    ON etapa (projeto_id, ordem)
    WHERE arquivada_em IS NULL;

-- O caminho de leitura do fluxo vigente — filtrar por projeto, descartar as
-- arquivadas, ordenar — e **este mesmo indice**, e por isso nao existe um
-- segundo declarado para ele: seria prefixo identico, nao aceleraria leitura
-- nenhuma e cobraria em toda escrita (ACH-15 da revisao de TASK-01.5).
-- Se um dia a restricao for reordenada para (ordem, projeto_id), a consulta
-- mais quente do board degrada e o indice de leitura passa a ser necessario.

CREATE TABLE raia (
    id           uuid        PRIMARY KEY,
    projeto_id   uuid        NOT NULL REFERENCES projeto (id),
    nome         text        NOT NULL,
    ordem        integer     NOT NULL,
    arquivada_em timestamptz
);

CREATE UNIQUE INDEX raia_projeto_ordem_unico
    ON raia (projeto_id, ordem)
    WHERE arquivada_em IS NULL;

-- Nenhuma tabela fora deste anel referencia `raia`, e a ausencia e o
-- mecanismo: a raia nao restringe transicao e nao entra em agregacao. Nao
-- acrescente `raia_id` a `intervalo_tarefa` nem a nada do anel de projecao —
-- a garantia vive no esquema, nao na disciplina de quem escreve a consulta.
