package br.com.idsd.kanban.internal.projeto;

import br.com.idsd.kanban.internal.acesso.Usuario;
import br.com.idsd.kanban.internal.acesso.UsuarioRepository;
import br.com.idsd.kanban.shared.ProblemaDetalhado;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Criacao de projeto (RF-022, RN-036, RN-037).
 *
 * <p><b>Uma transacao, nao duas.</b> O projeto e a primeira participacao sao
 * gravados juntos porque a falha da segunda, se estivesse fora, deixaria em disco
 * um projeto que ninguem alcanca — nao ha como conceder participacao sem alguem ja
 * dentro dele. O projeto orfao nao seria um erro visivel: ele apareceria so para a
 * administracao global, e a pessoa nomeada nunca saberia que foi nomeada.
 *
 * <p><b>Quem cria nao vira participante.</b> Inserir tambem a participacao do
 * administrador global apagaria a marca {@code acessoPorAdministracaoGlobal} para
 * ele nesse projeto (SCN-021.2) e confundiria alcance com participacao — que sao
 * justamente as duas coisas que ADR-010 separa.
 *
 * <p>A autorizacao <b>nao</b> passa pelo {@link ResolvedorDePermissao}, e essa e a
 * unica rota do sistema de que isso vale: nao existe participacao a consultar antes
 * de o projeto existir, e criar projeto nao e permissao de projeto porque nenhum
 * papel existe antes dele. A checagem e sobre {@code adminGlobal} direto, e mora na
 * borda.
 */
@Service
public class ProjetoServico {

    private final ProjetoRepository projetos;
    private final ParticipacaoRepository participacoes;
    private final UsuarioRepository usuarios;

    public ProjetoServico(
            ProjetoRepository projetos,
            ParticipacaoRepository participacoes,
            UsuarioRepository usuarios) {
        this.projetos = projetos;
        this.participacoes = participacoes;
        this.usuarios = usuarios;
    }

    /**
     * Grava o projeto e a primeira {@code project_admin}, ou nao grava nada.
     *
     * @throws ProblemaDetalhado.Falha {@code 422} para nome em branco ou pessoa que
     *     ainda nao existe em {@code usuario}
     */
    @Transactional
    public Projeto criar(CriacaoDeProjeto pedido) {
        String nome = pedido.nome() == null ? "" : pedido.nome().strip();
        if (nome.isEmpty()) {
            throw new ProblemaDetalhado.Falha(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "nome-obrigatorio",
                    "Nome do projeto obrigatório",
                    "O nome do projeto não pode ficar em branco.");
        }

        Usuario primeiro = primeiroAdministrador(pedido);
        Projeto projeto = projetos.save(new Projeto(nome, descricao(pedido)));
        participacoes.save(new Participacao(primeiro, projeto, List.of(Papel.PROJECT_ADMIN)));
        return projeto;
    }

    /**
     * A pessoa nomeada, que precisa ja ter entrado uma vez.
     *
     * <p>A recusa diz isso com todas as letras porque a diferenca importa a quem
     * recebe: "erro seu" e "essa pessoa ainda nao entrou uma vez" pedem acoes
     * opostas, e so a segunda tem saida — pedir que a pessoa entre.
     */
    private Usuario primeiroAdministrador(CriacaoDeProjeto pedido) {
        if (pedido.primeiroAdministradorId() == null) {
            throw pessoaInvalida(
                    "É preciso nomear quem será a primeira administradora do projeto.");
        }
        return usuarios.findById(pedido.primeiroAdministradorId())
                .orElseThrow(() -> pessoaInvalida(
                        "A pessoa indicada ainda não entrou no sistema uma vez, "
                                + "e por isso não pode ser nomeada administradora."));
    }

    private ProblemaDetalhado.Falha pessoaInvalida(String detalhe) {
        return new ProblemaDetalhado.Falha(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "primeiro-administrador-invalido",
                "Primeira administradora inválida",
                detalhe);
    }

    private String descricao(CriacaoDeProjeto pedido) {
        if (pedido.descricao() == null) {
            return null;
        }
        String texto = pedido.descricao().strip();
        return texto.isEmpty() ? null : texto;
    }
}
