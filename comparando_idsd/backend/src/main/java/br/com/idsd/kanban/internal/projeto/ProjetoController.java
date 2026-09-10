package br.com.idsd.kanban.internal.projeto;

import br.com.idsd.kanban.internal.acesso.UsuarioRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Leitura de projetos — {@code GET /v1/projetos} e {@code GET /v1/projetos/{id}}
 * (RF-002, RF-021).
 *
 * <p><b>A relacao lista por participacao, nao por permissao.</b> Quem participa
 * sem papel algum ve o projeto na lista e recebe {@code 403} ao abri-lo; quem nao
 * participa nao ve nada e recebe {@code 404}. A regra e unica em todo o produto
 * (TechSpec v1.8): participacao e o eixo da existencia, papel e o eixo da
 * capacidade. Responder {@code 404} ao participante sem papel contradiria o
 * {@code 200} que ele acabou de receber uma rota antes, e tiraria dele a unica
 * informacao acionavel que tem — pedir o papel a quem administra.
 *
 * <p>A distincao vem de {@link ResolvedorDePermissao.Acesso#participa()} e nunca
 * de conjunto de permissoes vazio: os dois casos produzem conjunto vazio, e
 * deriva-la dai e exatamente o erro que a marca existe para evitar.
 *
 * <p>Nenhuma decisao de acesso e tomada aqui. O resolvedor continua sendo o ponto
 * unico; este controlador escolhe apenas o codigo de resposta.
 */
@RestController
@RequestMapping("/v1/projetos")
public class ProjetoController {

    private static final Comparator<Papel> POR_CODIGO = Comparator.comparing(Papel::getCodigo);

    private final ProjetoRepository projetos;
    private final UsuarioRepository usuarios;
    private final ResolvedorDePermissao resolvedor;

    public ProjetoController(
            ProjetoRepository projetos,
            UsuarioRepository usuarios,
            ResolvedorDePermissao resolvedor) {
        this.projetos = projetos;
        this.usuarios = usuarios;
        this.resolvedor = resolvedor;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ProjetoResumo.Pagina relacao(JwtAuthenticationToken autenticacao) {
        Sujeito sujeito = sujeito(autenticacao);
        if (sujeito == null) {
            // Token valido de quem ainda nao entrou pelo /v1/sessao: nao ha conta,
            // logo nao ha participacao. Lista vazia, nunca erro — a conta nasce na
            // entrada (ADR-006) e nao na leitura.
            return ProjetoResumo.Pagina.de(List.of());
        }

        List<ProjetoResumo> conteudo =
                agrupar(projetos.visiveisPara(sujeito.id(), sujeito.adminGlobal()), sujeito)
                        .stream()
                        .map(Alcance::comoResumo)
                        .toList();
        return ProjetoResumo.Pagina.de(conteudo);
    }

    @GetMapping("/{projetoId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Object> detalhe(
            JwtAuthenticationToken autenticacao, @PathVariable UUID projetoId) {
        Sujeito sujeito = sujeito(autenticacao);
        if (sujeito == null) {
            return naoEncontrado(projetoId);
        }

        List<Alcance> alcances = agrupar(
                projetos.alcancadoPor(sujeito.id(), projetoId, sujeito.adminGlobal()), sujeito);
        if (alcances.isEmpty()) {
            return naoEncontrado(projetoId);
        }

        Alcance alcance = alcances.get(0);
        if (!alcance.acesso().tem(Permissao.LER)) {
            return semPermissao(projetoId);
        }
        return ResponseEntity.ok(alcance.comoDetalhe());
    }

    /**
     * O sujeito por tras do token, ou {@code null} se ele ainda nao tem conta.
     *
     * <p>{@code adminGlobal} sai do registro gravado e jamais de claim: e ADR-003 e
     * RNF-004 no ponto em que seria mais barato desobedece-los.
     */
    private Sujeito sujeito(JwtAuthenticationToken autenticacao) {
        return usuarios.findBySubjectId(autenticacao.getToken().getSubject())
                .map(usuario -> new Sujeito(usuario.getId(), usuario.isAdminGlobal()))
                .orElse(null);
    }

    /**
     * Colapsa as linhas do join em um alcance por projeto, preservando a ordem da
     * consulta.
     */
    private List<Alcance> agrupar(List<ProjetoConsulta> linhas, Sujeito sujeito) {
        Map<UUID, ProjetoConsulta> cabecalhos = new LinkedHashMap<>();
        Map<UUID, List<Papel>> papeis = new LinkedHashMap<>();

        for (ProjetoConsulta linha : linhas) {
            cabecalhos.putIfAbsent(linha.projetoId(), linha);
            List<Papel> acumulados =
                    papeis.computeIfAbsent(linha.projetoId(), chave -> new ArrayList<>());
            if (linha.papel() != null) {
                acumulados.add(linha.papel());
            }
        }

        List<Alcance> alcances = new ArrayList<>(cabecalhos.size());
        for (ProjetoConsulta cabecalho : cabecalhos.values()) {
            List<Papel> doProjeto = papeis.get(cabecalho.projetoId());
            doProjeto.sort(POR_CODIGO);
            alcances.add(new Alcance(
                    cabecalho,
                    List.copyOf(doProjeto),
                    resolvedor.acessoDerivado(
                            doProjeto, cabecalho.participa(), sujeito.adminGlobal())));
        }
        return alcances;
    }

    /**
     * Recusa que nao confirma nem nega a existencia do projeto.
     *
     * <p>O {@code detail} e generico de proposito: repetir aqui o nome ou a
     * descricao do projeto entregaria, na propria recusa, o que ela existe para
     * proteger (SCN-002.3).
     */
    private ResponseEntity<Object> naoEncontrado(UUID projetoId) {
        return problema(
                HttpStatus.NOT_FOUND,
                "https://errors.idsd/projeto-nao-encontrado",
                "Projeto não encontrado",
                "Nenhum projeto com este identificador está ao seu alcance.",
                projetoId);
    }

    private ResponseEntity<Object> semPermissao(UUID projetoId) {
        return problema(
                HttpStatus.FORBIDDEN,
                "https://errors.idsd/sem-permissao",
                "Sem permissão neste projeto",
                "Você participa deste projeto, mas nenhum papel seu permite lê-lo. "
                        + "Peça o papel a quem administra o projeto.",
                projetoId);
    }

    private ResponseEntity<Object> problema(
            HttpStatus status, String tipo, String titulo, String detalhe, UUID projetoId) {
        ProblemDetail corpo = ProblemDetail.forStatusAndDetail(status, detalhe);
        corpo.setType(URI.create(tipo));
        corpo.setTitle(titulo);
        corpo.setInstance(URI.create("/v1/projetos/" + projetoId));
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(corpo);
    }

    /** Quem esta pedindo, resolvido do que esta gravado. */
    private record Sujeito(UUID id, boolean adminGlobal) {}

    /** Um projeto mais o acesso que o sujeito tem a ele. */
    private record Alcance(
            ProjetoConsulta projeto,
            List<Papel> papeis,
            ResolvedorDePermissao.Acesso acesso) {

        ProjetoResumo comoResumo() {
            return new ProjetoResumo(
                    projeto.projetoId(),
                    projeto.nome(),
                    projeto.descricao(),
                    papeis,
                    acesso.permissoes(),
                    acesso.porAdministracaoGlobal());
        }

        ProjetoDetalhe comoDetalhe() {
            return new ProjetoDetalhe(
                    projeto.projetoId(),
                    projeto.nome(),
                    projeto.descricao(),
                    papeis,
                    acesso.permissoes(),
                    acesso.porAdministracaoGlobal());
        }
    }
}
