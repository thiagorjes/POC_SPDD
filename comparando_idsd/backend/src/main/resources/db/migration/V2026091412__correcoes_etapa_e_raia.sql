-- Correcoes da revisao de TASK-02.1 (2026-09-14) sobre o anel de configuracao.
--
-- Esta migration existe porque a de ordem 2 ja foi aplicada, e migration
-- aplicada nunca e alterada — e a mesma razao da ordem 7 do `data-model.md` §8.
-- Os comentarios da V2026091111 afirmam coisas que a revisao desmentiu, e
-- comentario em arquivo aplicado nao pode ser corrigido no lugar: a verdade
-- passa para o catalogo do banco, por `COMMENT ON`, que e onde ela fica
-- alcancavel por quem inspeciona o esquema em vez de ler o historico.

-- ACH-06 — o indice unico de ordem da raia nao tinha origem normativa e sai.
--
-- A task manda criar o indice unico parcial de ordem por projeto no guia de
-- `etapa`; `data-model.md` §6 lista **apenas** o de `etapa`; e o contrato de
-- `PUT /v1/projetos/{id}/raias` nao preve 422 por ordem duplicada. A restricao
-- era escopo, e escopo que apareceria como erro de banco nao mapeado na task
-- que implementa a rota, longe da causa.
--
-- Ela tambem nao tem a justificativa que a de `etapa` tem. A ordem da etapa e
-- invariante de negocio porque a adjacencia de RN-005 depende dela: duas etapas
-- ativas com a mesma ordem tornam "a etapa seguinte" ambigua. A raia e
-- agrupamento livre **sem semantica fixa** (RN-023) — a ordem dela e
-- apresentacao, e duas raias empatadas produzem no maximo um desempate
-- arbitrario na exibicao, nunca uma transicao ambigua.
DROP INDEX IF EXISTS raia_projeto_ordem_unico;

-- ACH-02 — a garantia sobre a raia e estreita, e a generalizacao era falsa.
COMMENT ON TABLE raia IS
    'Agrupamento visual do board, sem semantica fixa (RN-023): nao restringe '
    'transicao e nao entra em agregacao. A garantia e do esquema e e ESTREITA. '
    'tarefa.raia_id EXISTE — a raia e o agrupamento visual do cartao. O que '
    'nenhuma tabela carrega e raia na serie de tempo: evento_tarefa e '
    'intervalo_tarefa nao tem coluna de raia, e nenhuma rota agregada aceita '
    'filtro por raia. Agregar por raia exigiria juntar a serie de tempo a '
    'tarefa pelo estado CORRENTE, e o estado corrente nao diz em que raia a '
    'tarefa estava quando o intervalo correu. Nao acrescente raia a '
    'intervalo_tarefa nem a evento_tarefa. Corrige o comentario final da '
    'V2026091111, desmentido por data-model.md, board-e-tarefas.md e pela '
    'suite congelada RaiasIT (ACH-02 / TechSpec v1.11).';

-- ACH-10 — as citacoes de regra da V2026091111 estavam trocadas.
COMMENT ON COLUMN etapa.nome IS
    'Mutavel. Renomear vale dali em diante e nunca reescreve o historico, '
    'porque a serie de tempo segue o id e nao o nome (RN-021, RN-022). A '
    'V2026091111 citava RN-023, que e a regra da raia.';

COMMENT ON COLUMN etapa.ordem IS
    'Define adjacencia para a regra de transicao (RN-005, com a excecao '
    'nomeada do retorno a primeira etapa). A V2026091111 citava RN-021, que e '
    'a regra do rename. Unica por projeto entre as ativas.';

-- ACH-07 — o indice parcial de etapa NAO e adiavel, e nao tem como ser.
--
-- DEFERRABLE so existe em UNIQUE CONSTRAINT, e restricao parcial nao e
-- expressavel como constraint no PostgreSQL: nao ha versao adiavel de
-- etapa_projeto_ordem_unico. A consequencia recai sobre TASK-02.2:
-- `PUT /v1/projetos/{id}/etapas` substitui o fluxo inteiro numa requisicao, o
-- que inclui trocar a ordem de duas etapas ativas — e a troca direta viola o
-- indice no meio da transacao, ainda que o estado final seja valido.
--
-- Quem implementar a substituicao precisa de um passo intermediario: arquivar
-- ou deslocar as ordens para uma faixa livre antes de reatribui-las, tudo na
-- mesma transacao. A V2026091111 declara cuidadosamente um outro gatilho de
-- reabertura (reordenar a restricao para (ordem, projeto_id)) e omitia este.
COMMENT ON INDEX etapa_projeto_ordem_unico IS
    'Unico PARCIAL em arquivada_em IS NULL: duas etapas ativas nao compartilham '
    'ordem no projeto, e arquivar libera a ordem. NAO E ADIAVEL — DEFERRABLE so '
    'existe em UNIQUE CONSTRAINT e restricao parcial nao e constraint. A '
    'substituicao do fluxo inteiro que troca a ordem de duas etapas ativas viola '
    'este indice no meio da transacao e exige passo intermediario (ACH-07). E '
    'tambem o caminho de leitura do fluxo vigente, por isso nao existe um '
    'segundo indice declarado para ele.';

-- ACH-09 — o REVOKE DELETE nao e feito aqui, e a razao esta escrita.
--
-- A garantia dupla que `data-model.md` exige para a serie de tempo — aplicacao
-- e banco — pressupoe uma role de aplicacao distinta do dono do schema, e ela
-- nao existe: em desenvolvimento e no Testcontainers a aplicacao conecta como
-- o proprio dono, e REVOKE contra o dono nao surte efeito. Executa-lo aqui
-- produziria exatamente a classe de defeito que esta revisao inteira trata —
-- mecanismo declarado que nao e o implementado —, com a agravante de parecer
-- resolvido.
--
-- A concessao restrita a role de aplicacao e conteudo declarado da migration de
-- ordem 4 (`data-model.md` §8), e e la que `etapa` e `raia` entram junto de
-- `evento_tarefa`. Ficou como criterio de aceite 10 de TASK-02.3. Ate entao a
-- garantia de nao-remocao e de um lado so, o da aplicacao, e agora ela e real:
-- os repositorios nao publicam remocao fisica nenhuma (ACH-01).
