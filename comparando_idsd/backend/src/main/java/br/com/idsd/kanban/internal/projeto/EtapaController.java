package br.com.idsd.kanban.internal.projeto;

import br.com.idsd.kanban.internal.acesso.UsuarioRepository;
import br.com.idsd.kanban.shared.ProblemaDetalhado;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O fluxo de etapas do projeto — {@code GET} e {@code PUT}
 * {@code /v1/projetos/{projetoId}/etapas} (RF-017).
 *
 * <p><b>Nao ha rota de etapa individual, e a ausencia e a regra.</b> O fluxo e
 * substituido inteiro numa operacao porque a ordem e propriedade do conjunto e
 * porque a edicao etapa a etapa permitiria o estado sem terminal que RN-001
 * proibe. Acrescentar {@code POST /etapas} ou {@code DELETE /etapas/{id}} "por
 * conveniencia" reabriria exatamente esse estado.
 *
 * <p>Nenhuma decisao de acesso e tomada aqui: quem decide e
 * {@link ResolvedorDePermissao}, e este controlador escolhe apenas o codigo de
 * resposta. A regra e a mesma de {@link ProjetoController} e vale em todo o
 * produto — participacao e o eixo da existencia, papel e o eixo da capacidade:
 * quem nao alcanca o projeto recebe {@code 404}, e quem alcanca sem
 * {@link Permissao#CONFIGURAR} recebe {@code 403}. {@code product_owner} cai no
 * segundo caso, e e o exemplo que a suite congelada fixa: papel poderoso nao e
 * papel total (BDR-001).
 */
@RestController
@RequestMapping("/v1/projetos/{projetoId}/etapas")
public class EtapaController {

    private final EtapaService etapas;
    private final ResolvedorDePermissao resolvedor;
    private final UsuarioRepository usuarios;

    public EtapaController(
            EtapaService etapas, ResolvedorDePermissao resolvedor, UsuarioRepository usuarios) {
        this.etapas = etapas;
        this.resolvedor = resolvedor;
        this.usuarios = usuarios;
    }

    /**
     * O fluxo vigente.
     *
     * <p>Exige {@link Permissao#CONFIGURAR} e nao {@link Permissao#LER}: esta e a
     * leitura <b>da tela de configuracao</b>, e quem so le o projeto ve as etapas
     * pelo board, que e outra rota e outro recorte. Dar-lhe {@code LER} aqui faria
     * a rota de configuracao responder a quem nao pode configurar nada.
     */
    @GetMapping
    public ResponseEntity<Object> fluxo(
            JwtAuthenticationToken autenticacao, @PathVariable UUID projetoId) {
        ResponseEntity<Object> recusa = recusaSeNaoConfigura(autenticacao, projetoId);
        return recusa != null
                ? recusa
                : ResponseEntity.ok(EtapaResposta.Fluxo.de(etapas.fluxoVigente(projetoId)));
    }

    /**
     * Substitui o fluxo inteiro e devolve o resultado.
     *
     * <p>A recusa de acesso acontece <b>antes</b> de o servico ser chamado, e
     * portanto antes de qualquer escrita. As recusas de regra — fluxo sem etapa
     * terminal, etapa com tarefa ativa — sobem de {@link EtapaService} como
     * {@code 422} pelo tratador global, com {@code errors} nomeando o que travou.
     */
    @PutMapping
    public ResponseEntity<Object> substituir(
            JwtAuthenticationToken autenticacao,
            @PathVariable UUID projetoId,
            @Valid @RequestBody FluxoRequisicao pedido) {
        ResponseEntity<Object> recusa = recusaSeNaoConfigura(autenticacao, projetoId);
        return recusa != null
                ? recusa
                : ResponseEntity.ok(
                        EtapaResposta.Fluxo.de(etapas.substituirFluxo(projetoId, pedido)));
    }

    /**
     * A recusa apropriada, ou {@code null} quando o sujeito pode configurar.
     *
     * <p>Devolver a resposta em vez de lancar mantem os dois codigos visiveis lado a
     * lado — que e onde a diferenca entre eles precisa ser lida.
     */
    private ResponseEntity<Object> recusaSeNaoConfigura(
            JwtAuthenticationToken autenticacao, UUID projetoId) {
        UUID usuarioId = usuarios.findBySubjectId(autenticacao.getToken().getSubject())
                .map(usuario -> usuario.getId())
                .orElse(null);
        if (usuarioId == null) {
            // Token valido de quem ainda nao entrou pelo /v1/sessao: nao ha conta,
            // logo nao ha participacao, logo nada esta ao alcance dele.
            return naoEncontrado(projetoId);
        }

        ResolvedorDePermissao.Acesso acesso = resolvedor.acessoAoProjeto(usuarioId, projetoId);
        if (acesso.semAlcance()) {
            return naoEncontrado(projetoId);
        }
        if (!acesso.tem(Permissao.CONFIGURAR)) {
            return semPermissao(projetoId);
        }
        return null;
    }

    /**
     * Recusa que nao confirma nem nega a existencia do projeto.
     *
     * <p>O {@code detail} e generico de proposito: nenhum dado do projeto negado
     * entra na propria recusa (SCN-002.3).
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
                "Sem permissão para configurar",
                "Você participa deste projeto, mas nenhum papel seu permite configurar "
                        + "o fluxo de etapas. Peça o papel a quem administra o projeto.",
                instancia(projetoId)));
    }

    private static String instancia(UUID projetoId) {
        return "/v1/projetos/" + projetoId + "/etapas";
    }

    private ResponseEntity<Object> resposta(ProblemDetail corpo) {
        return ResponseEntity.status(corpo.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(corpo);
    }
}
