package br.com.idsd.kanban.internal.tarefa;

import br.com.idsd.kanban.internal.acesso.UsuarioRepository;
import br.com.idsd.kanban.internal.projeto.Permissao;
import br.com.idsd.kanban.internal.projeto.ProjetoRepository;
import br.com.idsd.kanban.internal.projeto.ResolvedorDePermissao;
import br.com.idsd.kanban.shared.ProblemaDetalhado;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Leitura do board — {@code GET /v1/projetos/{projetoId}/board} (RF-003).
 *
 * <p>A regra de acesso e a mesma de todo o produto, e aqui ela e visivel na forma
 * mais pura: <b>participacao e o eixo da existencia, papel e o eixo da
 * capacidade</b>. Quem nao alcanca o projeto recebe {@code 404}, porque confirmar
 * que ele existe ja seria informacao. Quem alcanca mas nao tem
 * {@link Permissao#LER} recebe {@code 403}. Na pratica todo papel le — o
 * {@code gestor} inclusive, e e justamente por isso que RN-015 o descreve como
 * somente-leitura —, mas a checagem fica escrita e nao suposta: o dia em que um
 * papel sem leitura existir, a rota ja recusa.
 *
 * <p>A rota nao decide nada sobre o board. Ela recusa quem nao pode ler e delega a
 * {@link BoardQuery} — que por sua vez nao decide acesso. A separacao e proposital:
 * consulta que tambem autoriza tende a ser reusada sem a autorizacao.
 */
@RestController
@RequestMapping("/v1/projetos/{projetoId}/board")
public class BoardController {

    private final BoardQuery board;
    private final ResolvedorDePermissao resolvedor;
    private final UsuarioRepository usuarios;
    private final ProjetoRepository projetos;

    public BoardController(
            BoardQuery board,
            ResolvedorDePermissao resolvedor,
            UsuarioRepository usuarios,
            ProjetoRepository projetos) {
        this.board = board;
        this.resolvedor = resolvedor;
        this.usuarios = usuarios;
        this.projetos = projetos;
    }

    /** O board do projeto, com a grade completa e o {@code seq} do ultimo evento. */
    @GetMapping
    public ResponseEntity<Object> ler(
            JwtAuthenticationToken autenticacao, @PathVariable UUID projetoId) {
        UUID leitor = autorOuNulo(autenticacao);
        if (leitor == null) {
            // Token valido de quem ainda nao entrou pelo /v1/sessao: nao ha conta,
            // logo nao ha participacao, logo nada esta ao alcance dele.
            return naoEncontrado(projetoId);
        }

        ResolvedorDePermissao.Acesso acesso = resolvedor.acessoAoProjeto(leitor, projetoId);
        ResponseEntity<Object> recusa = recusaSeNaoLe(acesso, projetoId);
        if (recusa != null) {
            return recusa;
        }
        return ResponseEntity.ok(board.montar(projetoId, acesso.porAdministracaoGlobal()));
    }

    /** O usuario da sessao, ou {@code null} para token de quem ainda nao entrou. */
    private UUID autorOuNulo(JwtAuthenticationToken autenticacao) {
        return usuarios.findBySubjectId(autenticacao.getToken().getSubject())
                .map(usuario -> usuario.getId())
                .orElse(null);
    }

    /**
     * A recusa apropriada, ou {@code null} quando o sujeito pode ler.
     *
     * <p>O acesso chega <b>resolvido</b> porque quem chama tambem precisa dele: a
     * marca de administracao global viaja no corpo do board (RN-035, SCN-021.2), e
     * resolve-lo duas vezes seria a mesma decisao tomada em dois lugares.
     */
    private ResponseEntity<Object> recusaSeNaoLe(
            ResolvedorDePermissao.Acesso acesso, UUID projetoId) {
        if (acesso.semAlcance()) {
            return naoEncontrado(projetoId);
        }
        // O alcance global vem da marca no usuario e nao prova que o projeto existe;
        // sem esta linha, identificador inexistente chegaria a consulta e sairia 500.
        if (!projetos.existsById(projetoId)) {
            return naoEncontrado(projetoId);
        }
        if (!acesso.tem(Permissao.LER)) {
            return semPermissao(projetoId);
        }
        return null;
    }

    /**
     * Recusa que nao confirma nem nega a existencia do projeto.
     *
     * <p>O {@code detail} e generico de proposito: nenhum dado do projeto negado
     * entra na propria recusa.
     */
    private ResponseEntity<Object> naoEncontrado(UUID projetoId) {
        return resposta(ProblemaDetalhado.de(
                HttpStatus.NOT_FOUND,
                "projeto-nao-encontrado",
                "Projeto não encontrado",
                "Nenhum projeto com este identificador está ao seu alcance.",
                instancia(projetoId)));
    }

    private ResponseEntity<Object> semPermissao(UUID projetoId) {
        return resposta(ProblemaDetalhado.de(
                HttpStatus.FORBIDDEN,
                "sem-permissao",
                "Sem permissão para ler o board",
                "Você participa deste projeto, mas nenhum papel seu permite ler o board. "
                        + "Peça o papel a quem administra o projeto.",
                instancia(projetoId)));
    }

    private static String instancia(UUID projetoId) {
        return "/v1/projetos/" + projetoId + "/board";
    }

    private ResponseEntity<Object> resposta(ProblemDetail corpo) {
        return ResponseEntity.status(corpo.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(corpo);
    }
}
