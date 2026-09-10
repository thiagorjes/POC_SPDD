package br.com.idsd.kanban.internal.projeto;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Catalogo fechado de papeis de projeto (BDR-001, RN-015).
 *
 * <p>E enumeracao, e nao tabela, de proposito: papel em tabela abriria
 * composicao de permissao em runtime — bastaria inserir uma linha para criar um
 * papel que ninguem aprovou. Aqui, papel novo exige mudanca de codigo, revisao e
 * migration.
 *
 * <p>Papeis acumulam no mesmo projeto: quem e {@code project_admin} e {@code dev}
 * tem a uniao das duas permissoes.
 */
public enum Papel {

    /** Administra o projeto: configura o fluxo, as raias e a participacao. */
    PROJECT_ADMIN(
            "project_admin",
            Permissao.LER,
            Permissao.ESCREVER_TAREFA,
            Permissao.DESBLOQUEAR,
            Permissao.ENCERRAR,
            Permissao.CONFIGURAR),

    /**
     * Responde pelo produto. E a unica linha do catalogo que encerra sem
     * conclusao sem administrar o projeto, e a unica que reabre (RN-015).
     */
    PRODUCT_OWNER(
            "product_owner",
            Permissao.LER,
            Permissao.ESCREVER_TAREFA,
            Permissao.DESBLOQUEAR,
            Permissao.ENCERRAR,
            Permissao.REABRIR),

    /** Executa o trabalho. Escreve tarefa, mas nao desbloqueia nem encerra. */
    DEV("dev", Permissao.LER, Permissao.ESCREVER_TAREFA),

    /**
     * Somente-leitura, e estruturalmente (C-02 do shape): nao recebe acao de
     * escrita na interface e e recusado se a solicitar por outro caminho.
     */
    GESTOR("gestor", Permissao.LER),

    /**
     * Participa sem papel atribuido. Nao le o board: participacao sozinha nao
     * concede nada, e essa e a diferenca entre estar no projeto e ter papel
     * nele.
     */
    USER("user");

    private final String codigo;
    private final Set<Permissao> permissoes;

    Papel(String codigo, Permissao... permissoes) {
        this.codigo = codigo;
        this.permissoes = permissoes.length == 0
                ? Collections.unmodifiableSet(EnumSet.noneOf(Permissao.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(permissoes)));
    }

    /**
     * Forma persistida e exposta do papel. A coluna {@code participacao_papel.papel}
     * e o contrato de API falam este codigo, nao o nome da constante.
     */
    @JsonValue
    public String getCodigo() {
        return codigo;
    }

    public Set<Permissao> getPermissoes() {
        return permissoes;
    }

    /** Converte o codigo persistido de volta ao papel. Codigo desconhecido reprova. */
    public static Papel porCodigo(String codigo) {
        for (Papel papel : values()) {
            if (papel.codigo.equals(codigo)) {
                return papel;
            }
        }
        throw new IllegalArgumentException("papel fora do catalogo: " + codigo);
    }

    /**
     * Persiste o codigo, e nao o nome da constante.
     *
     * <p>Nested de proposito: o mapeamento so existe por causa desta enumeracao,
     * e mante-lo aqui impede que o codigo persistido seja renomeado sem que quem
     * o faz veja a coluna que depende dele. Codigo desconhecido na leitura
     * levanta excecao — o catalogo fechado nao serve para nada se linha invalida
     * no banco virar papel silenciosamente nulo.
     */
    @jakarta.persistence.Converter
    public static class ConversorJpa
            implements jakarta.persistence.AttributeConverter<Papel, String> {

        @Override
        public String convertToDatabaseColumn(Papel papel) {
            return papel == null ? null : papel.codigo;
        }

        @Override
        public Papel convertToEntityAttribute(String codigo) {
            return codigo == null ? null : porCodigo(codigo);
        }
    }
}
