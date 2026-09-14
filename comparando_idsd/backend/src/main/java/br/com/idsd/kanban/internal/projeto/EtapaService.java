package br.com.idsd.kanban.internal.projeto;

import br.com.idsd.kanban.shared.RegraDeNegocioViolada;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O fluxo de etapas do projeto — leitura e substituicao (RF-017).
 *
 * <p><b>A substituicao e inteira e atomica.</b> Nao ha "criar etapa", "remover
 * etapa" nem "reordenar": a ordem e propriedade do conjunto, e as operacoes
 * individuais existiriam justamente para produzir o estado que RN-001 proibe — o
 * momento entre remover a ultima terminal e criar a proxima. Uma operacao so, numa
 * transacao so: ou o fluxo inteiro passa a valer, ou nada muda.
 *
 * <p><b>A verificacao da etapa terminal acontece antes de qualquer leitura de
 * fluxo e de qualquer escrita.</b> Validar depois de gravar e reverter parece
 * equivalente e nao e: entre a gravacao e o rollback, todo caminho que lesse
 * dentro da mesma transacao veria um projeto sem etapa terminal. A ordem aqui e
 * contrato, e {@code EtapaServiceTest} a verifica pela negativa — nenhuma escrita
 * chega ao repositorio quando a regra reprova.
 *
 * <p>Arquivar e remocao <b>logica</b>, sempre: a serie de tempo por etapa segue o
 * identificador para sempre (RN-021, RN-022), e por isso {@link EtapaRepositorio}
 * nao publica assinatura de remocao alguma.
 */
@Service
public class EtapaService {

    private static final Comparator<Etapa> POR_ORDEM =
            Comparator.comparingInt(Etapa::getOrdem).thenComparing(Etapa::getId);

    private final EtapaRepositorio repositorio;
    private final Optional<TarefasAtivasPorEtapa> tarefasAtivas;

    /**
     * Construtor da aplicacao.
     *
     * <p>{@link TarefasAtivasPorEtapa} chega em {@link Optional} porque o bean so
     * existe a partir de TASK-02.5, e exigi-lo antes disso deixaria o contexto sem
     * subir por uma dependencia que nada ainda pode satisfazer.
     */
    @Autowired
    public EtapaService(EtapaRepositorio repositorio, Optional<TarefasAtivasPorEtapa> tarefasAtivas) {
        this.repositorio = repositorio;
        this.tarefasAtivas = tarefasAtivas;
    }

    /**
     * Construtor de um colaborador so, que a suite congelada nomeia.
     *
     * <p>Nao e atalho de teste: ele declara que a regra de etapa terminal e
     * <b>aritmetica sobre a requisicao</b> e nao depende de tarefa alguma. E por
     * isso que {@code EtapaServiceTest} a verifica sem banco e sem a porta de
     * contagem — se um dia ela passar a precisar de uma das duas, este construtor
     * para de compilar o teste, que e o aviso desejado.
     */
    public EtapaService(EtapaRepositorio repositorio) {
        this(repositorio, Optional.empty());
    }

    /** O fluxo vigente: sem as arquivadas, na ordem declarada. */
    @Transactional(readOnly = true)
    public List<Etapa> fluxoVigente(UUID projetoId) {
        return repositorio.findByProjetoIdAndArquivadaEmIsNullOrderByOrdemAsc(projetoId);
    }

    /**
     * Substitui o fluxo inteiro do projeto e devolve o resultado.
     *
     * <p>A ordem dos passos e a propria regra: o bloqueio do projeto primeiro;
     * terminal, contra a requisicao; depois o fluxo vigente; depois a recusa de
     * arquivamento; e so entao a escrita.
     */
    // ACH-01. O teto da transacao e o companheiro do `lock_timeout` da sessao: um
    // limita quanto se espera para **entrar**, o outro quanto se pode segurar
    // depois de entrar. Sem o segundo, quem adquiriu o bloqueio e travou em
    // qualquer outro ponto mantem os demais esperando ate o `lock_timeout` de cada
    // um, repetidamente. Dez segundos sao folgados para a operacao: um projeto,
    // teto de cem etapas, e ela e administrativa e rara.
    @Transactional(timeout = 10)
    public List<Etapa> substituirFluxo(UUID projetoId, FluxoRequisicao requisicao) {
        Objects.requireNonNull(requisicao, "a requisicao e obrigatoria");

        // SDR-005 — antes de ler o fluxo vigente, e nao depois. Tudo o que vem
        // abaixo, inclusive as recusas, decide sobre um conjunto que mais ninguem
        // pode estar substituindo ao mesmo tempo. Bloquear depois da leitura nao
        // serviria de nada: o conjunto lido ja poderia estar obsoleto, e o dano
        // concorrente aqui e por ausencia — a etapa que o outro criou nao esta no
        // corpo de quem perdeu, logo nao e arquivada nem reordenada, e some sem
        // aviso. Versao otimista nao alcanca o caso: nao ha linha sobre a qual
        // conflitar.
        repositorio.bloquearProjeto(projetoId);

        exigirConjuntoBemFormado(requisicao.etapas());
        List<FluxoRequisicao.EtapaDesejada> desejadas = exigirTerminal(requisicao);

        Map<UUID, Etapa> vigentes = new LinkedHashMap<>();
        Map<UUID, Integer> ordensOriginais = new LinkedHashMap<>();
        for (Etapa etapa : repositorio.findByProjetoIdAndArquivadaEmIsNullOrderByOrdemAsc(projetoId)) {
            vigentes.put(etapa.getId(), etapa);
            ordensOriginais.put(etapa.getId(), etapa.getOrdem());
        }

        // Toda recusa acontece antes de qualquer escrita, inclusive antes do passo
        // intermediario — que ja e escrita. Validar depois e reverter parece
        // equivalente e nao e: entre a gravacao e o rollback, qualquer leitura
        // dentro da mesma transacao ve o estado que a regra proibe.
        exigirQueOsIdentificadoresSejamDoFluxo(desejadas, vigentes.keySet());

        // A recusa por tarefa ativa vem <b>antes</b> de qualquer escrita, e por isso
        // e calculada sobre os identificadores, aqui, e nao depois de deslocar as
        // ordens: o passo intermediario ja e escrita, e a transacao revertida
        // deixaria o estado deslocado visivel a quem lesse dentro dela.
        recusarSeContemTarefaAtiva(omitidas(desejadas, vigentes));

        // Passo intermediario exigido pelo indice unico parcial, que nao e adiavel:
        // sem ele, um fluxo que apenas troca duas etapas de posicao viola a
        // unicidade no meio da transacao, com estado final valido.
        //
        // Ele so roda quando alguma ordem vigente e de fato disputada (ACH-16): sem
        // a condicao, toda substituicao pagava dois UPDATE por etapa vigente,
        // inclusive a que nao muda nada.
        if (haDisputaDeOrdem(desejadas, vigentes, ordensOriginais)) {
            repositorio.liberarOrdens(List.copyOf(vigentes.values()));
        }

        List<Etapa> resultado = new ArrayList<>(desejadas.size());
        List<Etapa> tocadas = new ArrayList<>();
        for (FluxoRequisicao.EtapaDesejada desejada : desejadas) {
            Etapa etapa = aplicar(projetoId, desejada, vigentes);
            resultado.add(etapa);
            tocadas.add(etapa);
        }

        // O que sobrou em `vigentes` e o que o corpo omitiu, e omitir e o que
        // arquiva.
        Instant agora = Instant.now();
        List<Etapa> arquivadas = List.copyOf(vigentes.values());
        for (Etapa etapa : arquivadas) {
            etapa.arquivar(agora);
            tocadas.add(etapa);
        }

        repositorio.substituirFluxo(projetoId, List.copyOf(tocadas));

        // A ordem original e devolvida <b>depois</b> de a linha estar arquivada em
        // disco, e a ordem dos dois passos e a garantia (ACH-17). Restituir antes
        // funcionava por acidente: dependia de o Hibernate emitir INSERT antes de
        // UPDATE na fila de acoes e de a linha sair do indice parcial no mesmo
        // statement que restaura a ordem — um flush intermediario acrescentado por
        // qualquer motivo futuro quebrava o caminho, com 500 intermitente. Depois do
        // flush acima a etapa ja nao esta no indice parcial, e a ordem restituida
        // nao disputa nada. Ela e devolvida porque a etapa permanece na serie de
        // tempo: deixa-la com a ordem da faixa de trabalho gravaria em disco um
        // numero que nunca foi a posicao dela no fluxo.
        for (Etapa etapa : arquivadas) {
            etapa.reconfigurar(
                    etapa.getNome(),
                    ordensOriginais.getOrDefault(etapa.getId(), etapa.getOrdem()),
                    etapa.isTerminal());
        }

        resultado.sort(POR_ORDEM);
        return List.copyOf(resultado);
    }

    /**
     * RN-001 — ao menos uma etapa terminal, verificada <b>antes</b> de tudo.
     *
     * <p>Lista vazia e recusada pela mesma regra e nao por uma checagem a parte:
     * fluxo sem etapa nenhuma tambem e fluxo sem terminal, e trata-lo como caso
     * proprio abriria a porta para os dois divergirem.
     */
    private List<FluxoRequisicao.EtapaDesejada> exigirTerminal(FluxoRequisicao requisicao) {
        List<FluxoRequisicao.EtapaDesejada> desejadas = requisicao.etapas();

        if (desejadas.stream().noneMatch(FluxoRequisicao.EtapaDesejada::terminal)) {
            throw new RegraDeNegocioViolada(
                    "fluxo-sem-etapa-terminal",
                    "Fluxo sem etapa terminal",
                    "O fluxo precisa de ao menos uma etapa terminal — aquela em que a tarefa "
                            + "é considerada encerrada. O fluxo atual do projeto não foi alterado.");
        }
        return desejadas;
    }

    /**
     * A forma do <b>conjunto</b>, que anotacao por campo nao alcanca.
     *
     * <p>Os tres defeitos abaixo chegavam ao servico e saiam como {@code 500}
     * (ACH-01, ACH-02 e ACH-03 da revisao de TASK-02.2), quando o contrato pede
     * {@code 422}: {@code @Valid} cascateia em cada item e nao diz nada sobre
     * unicidade de {@code id}, unicidade de {@code ordem} nem presenca do item.
     *
     * <p>O {@code id} repetido derrubava a rota porque a segunda ocorrencia
     * encontrava o mapa de vigentes ja esvaziado. A {@code ordem} repetida so
     * colidia no flush, como violacao de {@code etapa_projeto_ordem_unico} sem
     * tradutor. Nenhum dos dois chega mais a escrita alguma: <b>toda recusa
     * acontece antes</b>, que e a regra que a task escreveu por extenso.
     */
    private void exigirConjuntoBemFormado(List<FluxoRequisicao.EtapaDesejada> desejadas) {
        Objects.requireNonNull(desejadas, "as etapas sao obrigatorias");

        if (desejadas.stream().anyMatch(Objects::isNull)) {
            throw new RegraDeNegocioViolada(
                    "etapa-ausente-na-lista",
                    "Etapa vazia na lista",
                    "A lista de etapas contém um item vazio. Recarregue a configuração "
                            + "e tente de novo. O fluxo atual do projeto não foi alterado.");
        }

        recusarRepetidos(
                desejadas.stream().map(FluxoRequisicao.EtapaDesejada::id).filter(Objects::nonNull),
                "etapaId",
                "etapa-repetida",
                "Etapa repetida na configuração",
                "A mesma etapa aparece mais de uma vez na configuração enviada. "
                        + "O fluxo atual do projeto não foi alterado.");

        recusarRepetidos(
                desejadas.stream().map(FluxoRequisicao.EtapaDesejada::ordem),
                "ordem",
                "ordem-repetida",
                "Duas etapas na mesma posição",
                "Duas etapas foram enviadas com a mesma posição no fluxo, e a posição "
                        + "é única por projeto. O fluxo atual do projeto não foi alterado.");
    }

    /** Recusa, nomeando cada valor que apareceu mais de uma vez. */
    private void recusarRepetidos(
            Stream<?> valores,
            String campo,
            String slug,
            String titulo,
            String detalhe) {
        Set<Object> vistos = new LinkedHashSet<>();
        List<Map<String, Object>> repetidos = new ArrayList<>();
        valores.forEach(valor -> {
            if (!vistos.add(valor)) {
                repetidos.add(Map.of(campo, valor));
            }
        });

        if (!repetidos.isEmpty()) {
            throw new RegraDeNegocioViolada(slug, titulo, detalhe).comErros(repetidos);
        }
    }

    /**
     * Se alguma ordem vigente e disputada nesta substituicao.
     *
     * <p>Ela e disputada quando uma etapa vigente muda de posicao, ou quando uma
     * posicao hoje ocupada passa a ser de outra etapa — inclusive de uma criada
     * agora, ou liberada por arquivamento. Fora desses casos o passo intermediario
     * nao evita colisao nenhuma e so custa dois {@code UPDATE} por etapa.
     */
    private boolean haDisputaDeOrdem(
            List<FluxoRequisicao.EtapaDesejada> desejadas,
            Map<UUID, Etapa> vigentes,
            Map<UUID, Integer> ordensOriginais) {
        if (vigentes.isEmpty()) {
            return false;
        }
        for (FluxoRequisicao.EtapaDesejada desejada : desejadas) {
            Integer original = desejada.id() == null ? null : ordensOriginais.get(desejada.id());
            if (original == null || original != desejada.ordem()) {
                // Posicao pedida por quem ainda nao a tinha: se ela ja pertence a
                // alguma vigente, as duas se cruzam dentro da transacao.
                boolean ocupada = ordensOriginais.values().stream()
                        .anyMatch(ordem -> ordem == desejada.ordem());
                if (ocupada) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Todo {@code id} enviado precisa ser de uma etapa ativa <b>deste</b> projeto.
     *
     * <p>Identificador desconhecido nao e criacao: criar uma etapa nova com o
     * {@code id} pedido atenderia a requisicao e reaproveitaria um identificador que
     * a serie de tempo ja usa — o historico passaria a apontar para uma etapa que
     * nunca o produziu. Recusar e o unico desfecho que o preserva.
     */
    private void exigirQueOsIdentificadoresSejamDoFluxo(
            List<FluxoRequisicao.EtapaDesejada> desejadas, Set<UUID> vigentes) {
        List<Map<String, Object>> estranhos = desejadas.stream()
                .map(FluxoRequisicao.EtapaDesejada::id)
                .filter(id -> id != null && !vigentes.contains(id))
                .map(id -> Map.<String, Object>of("etapaId", id))
                .toList();

        if (!estranhos.isEmpty()) {
            throw new RegraDeNegocioViolada(
                    "etapa-fora-do-fluxo",
                    "Etapa não pertence ao fluxo vigente",
                    "Uma das etapas enviadas não faz parte do fluxo atual deste projeto. "
                            + "Recarregue a configuração e tente de novo.")
                    .comErros(estranhos);
        }
    }

    /** As etapas vigentes que o corpo nao mencionou — e que portanto serao arquivadas. */
    private List<Etapa> omitidas(
            List<FluxoRequisicao.EtapaDesejada> desejadas, Map<UUID, Etapa> vigentes) {
        Set<UUID> preservadas = desejadas.stream()
                .map(FluxoRequisicao.EtapaDesejada::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return vigentes.values().stream()
                .filter(etapa -> !preservadas.contains(etapa.getId()))
                .toList();
    }

    /** Cria, ou preserva a etapa vigente reconfigurando-a, conforme o {@code id}. */
    private Etapa aplicar(
            UUID projetoId, FluxoRequisicao.EtapaDesejada desejada, Map<UUID, Etapa> vigentes) {
        if (desejada.id() == null) {
            return new Etapa(projetoId, desejada.nome(), desejada.ordem(), desejada.terminal());
        }

        // O identificador ja foi conferido contra o fluxo vigente <b>e</b> contra a
        // repeticao dentro do proprio corpo, as duas antes de qualquer escrita, e
        // por isso aqui ele existe. Era so a primeira das duas conferencias que
        // existia, e a segunda ocorrencia de um id repetido encontrava o mapa ja
        // esvaziado por esta mesma linha (ACH-01).
        Etapa vigente = vigentes.remove(desejada.id());
        vigente.reconfigurar(desejada.nome(), desejada.ordem(), desejada.terminal());
        return vigente;
    }

    /**
     * RF-017 — etapa com tarefa ativa nao e arquivada, e a recusa diz qual.
     *
     * <p>Sem a porta publicada a contagem e zero e o laco nao recusa nada. Ver
     * {@link TarefasAtivasPorEtapa} para por que isso e ausencia de massa e nao
     * ausencia de regra.
     */
    private void recusarSeContemTarefaAtiva(List<Etapa> aArquivar) {
        if (tarefasAtivas.isEmpty() || aArquivar.isEmpty()) {
            return;
        }

        // Uma consulta para todas as etapas, e nao uma por etapa (ACH-07): esta
        // transacao ja segura o bloqueio pessimista do projeto, e cada ida ao banco
        // aqui e tempo em que ninguem mais configura este projeto.
        Map<UUID, Long> contagem = tarefasAtivas.get().contarEm(
                aArquivar.stream().map(Etapa::getId).toList());

        List<Map<String, Object>> bloqueadas = new ArrayList<>();
        for (Etapa etapa : aArquivar) {
            long ativas = contagem.getOrDefault(etapa.getId(), 0L);
            if (ativas > 0) {
                bloqueadas.add(Map.of(
                        "etapaId", etapa.getId(),
                        "nome", etapa.getNome(),
                        "tarefasAtivas", ativas));
            }
        }

        if (!bloqueadas.isEmpty()) {
            throw new RegraDeNegocioViolada(
                    "etapa-com-tarefa-ativa",
                    "Etapa ainda tem tarefas",
                    "Uma etapa só pode sair do fluxo depois de esvaziada: mova as tarefas "
                            + "que ainda estão nela para outra etapa e tente de novo. "
                            + "O fluxo atual do projeto não foi alterado.")
                    .comErros(bloqueadas);
        }
    }
}
