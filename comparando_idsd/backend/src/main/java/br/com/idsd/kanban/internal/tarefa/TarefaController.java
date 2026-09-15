package br.com.idsd.kanban.internal.tarefa;

import br.com.idsd.kanban.internal.acesso.UsuarioRepository;
import br.com.idsd.kanban.internal.projeto.Permissao;
import br.com.idsd.kanban.internal.projeto.ProjetoRepository;
import br.com.idsd.kanban.internal.projeto.ResolvedorDePermissao;
import br.com.idsd.kanban.shared.ProblemaDetalhado;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Criacao de tarefa — {@code POST /v1/projetos/{projetoId}/tarefas} (RF-004).
 *
 * <p><b>E a primeira borda de escrita sobre tarefa do sistema</b>, e e por isso que
 * o rebaixamento dos cinco achados de seguranca de TASK-02.4 caduca aqui: eles
 * foram rebaixados sob o argumento "nao ha chamador externo", com prazo declarado.
 * Este arquivo e o chamador externo, e as duas metades que ficaram para ele fecham
 * de forma <b>estrutural</b> e nao por checagem — o ator sai do principal
 * autenticado porque {@link NovaTarefaRequisicao} nao tem onde propor outro
 * (ACH-12), e o documento de {@code dados} e construido chave a chave em
 * {@link CriacaoDeTarefaService} porque nada do corpo chega ate ele (ACH-15).
 *
 * <p>Nenhuma decisao de acesso e tomada aqui: quem decide e
 * {@link ResolvedorDePermissao}. A regra e a mesma de todo o produto —
 * participacao e o eixo da existencia, papel e o eixo da capacidade: quem nao
 * alcanca o projeto recebe {@code 404}, e quem alcanca sem
 * {@link Permissao#ESCREVER_TAREFA} recebe {@code 403}. O papel {@code gestor} e
 * somente-leitura por RN-015 e cai no segundo caso (SCN-004.3), e a recusa e do
 * servidor: esconder o botao nao e a garantia.
 *
 * <p>A recusa de acesso acontece <b>antes</b> de o servico ser chamado, e portanto
 * antes de qualquer escrita. As recusas de regra — titulo em branco, projeto sem
 * fluxo — sobem do servico como {@code 422} pelo tratador global.
 */
@RestController
@RequestMapping("/v1/projetos/{projetoId}/tarefas")
public class TarefaController {

    private final CriacaoDeTarefaService criacao;
    private final ResolvedorDePermissao resolvedor;
    private final UsuarioRepository usuarios;
    private final ProjetoRepository projetos;

    public TarefaController(
            CriacaoDeTarefaService criacao,
            ResolvedorDePermissao resolvedor,
            UsuarioRepository usuarios,
            ProjetoRepository projetos) {
        this.criacao = criacao;
        this.resolvedor = resolvedor;
        this.usuarios = usuarios;
        this.projetos = projetos;
    }

    /**
     * Cria a tarefa e devolve o cartao, com {@code Location} para a leitura dela.
     *
     * <p>O identificador do ator e resolvido aqui, do principal autenticado, e
     * entregue ao servico como argumento. E o unico caminho por onde ele chega ao
     * log: a autoria do registro de auditoria nao e negociada com o cliente.
     */
    @PostMapping
    public ResponseEntity<Object> criar(
            JwtAuthenticationToken autenticacao,
            @PathVariable UUID projetoId,
            @RequestBody NovaTarefaRequisicao pedido) {
        UUID autor = autorOuNulo(autenticacao);
        ResponseEntity<Object> recusa = recusaSeNaoEscreve(autor, projetoId);
        if (recusa != null) {
            return recusa;
        }

        CartaoResposta cartao = criacao.criar(projetoId, pedido, autor);
        return ResponseEntity.created(URI.create("/v1/tarefas/" + cartao.id())).body(cartao);
    }

    /** O usuario da sessao, ou {@code null} para token de quem ainda nao entrou. */
    private UUID autorOuNulo(JwtAuthenticationToken autenticacao) {
        return usuarios.findBySubjectId(autenticacao.getToken().getSubject())
                .map(usuario -> usuario.getId())
                .orElse(null);
    }

    /**
     * A recusa apropriada, ou {@code null} quando o sujeito pode escrever.
     *
     * <p>Devolver a resposta em vez de lancar mantem os dois codigos visiveis lado a
     * lado — que e onde a diferenca entre eles precisa ser lida.
     */
    private ResponseEntity<Object> recusaSeNaoEscreve(UUID usuarioId, UUID projetoId) {
        if (usuarioId == null) {
            // Token valido de quem ainda nao entrou pelo /v1/sessao: nao ha conta,
            // logo nao ha participacao, logo nada esta ao alcance dele.
            return naoEncontrado(projetoId);
        }

        ResolvedorDePermissao.Acesso acesso = resolvedor.acessoAoProjeto(usuarioId, projetoId);
        if (acesso.semAlcance()) {
            return naoEncontrado(projetoId);
        }
        // O alcance global nao prova que o projeto existe: o resolvedor o concede a
        // partir da marca no usuario, sem consultar `projeto`. Sem esta linha, um
        // identificador inexistente seguiria para o servico e estouraria na chave
        // estrangeira — 500, e escrita tentada numa rota que devia recusar na borda.
        if (!projetos.existsById(projetoId)) {
            return naoEncontrado(projetoId);
        }
        if (!acesso.tem(Permissao.ESCREVER_TAREFA)) {
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
                "Sem permissão para criar tarefa",
                "Você participa deste projeto, mas nenhum papel seu permite criar tarefas. "
                        + "Peça o papel a quem administra o projeto.",
                instancia(projetoId)));
    }

    private static String instancia(UUID projetoId) {
        return "/v1/projetos/" + projetoId + "/tarefas";
    }

    private ResponseEntity<Object> resposta(ProblemDetail corpo) {
        return ResponseEntity.status(corpo.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(corpo);
    }
}
