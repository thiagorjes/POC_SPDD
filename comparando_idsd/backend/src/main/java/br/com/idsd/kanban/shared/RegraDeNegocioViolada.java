package br.com.idsd.kanban.shared;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Recusa por regra de negocio: {@code 422}, com a razao em linguagem de negocio.
 *
 * <p>E uma {@link ProblemaDetalhado.Falha} e nao um tipo paralelo, de proposito.
 * O tratador global ja traduz {@code Falha} no corpo unico de erro do sistema, e
 * criar uma segunda hierarquia obrigaria um segundo {@code @ExceptionHandler} —
 * que e como formato de erro diverge em silencio. Herdar traz de gra
 * {@code type}, {@code title}, {@code traceId} e o ponto de extensao.
 *
 * <p><b>O nome e o pacote sao fixados pela suite congelada</b>, que a importa de
 * {@code br.com.idsd.kanban.shared} em {@code EtapaServiceTest},
 * {@code CriacaoDeTarefaServiceTest} e {@code ImpedimentoServiceTest}. A suite
 * tambem fixa {@link #detalhe()}: ela afirma que a razao da recusa nomeia o campo
 * recusado, e para afirmar isso precisa le-la sem passar por HTTP.
 *
 * <p>O {@code 422} e o codigo que os contratos congelam para recusa de conteudo
 * bem formado — a linha entre ele e o {@code 400} esta em {@link TratadorDeErro}.
 */
public class RegraDeNegocioViolada extends ProblemaDetalhado.Falha {

    @Serial
    private static final long serialVersionUID = 1L;

    public RegraDeNegocioViolada(String slug, String titulo, String detalhe) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, slug, titulo, detalhe);
    }

    /**
     * A razao da recusa, legivel sem passar por HTTP.
     *
     * <p>Existe porque o teste unitario verifica <b>o conteudo</b> da recusa, e nao
     * apenas o tipo dela: recusa generica e indistinguivel de recusa correta para
     * quem le o corpo, e {@code getMessage()} herdado de {@code Throwable} nao diz
     * a quem le o teste que aquele texto e contrato.
     */
    public String detalhe() {
        return getMessage();
    }

    /**
     * Anexa a {@code errors} os itens que causaram a recusa.
     *
     * <p>{@code errors} e o membro que a tela usa para saber <b>onde</b> por o
     * aviso; sem ele a pessoa recebe a razao e nao sabe a que item ela se refere.
     * A forma de cada item e do dominio que levanta a falha.
     */
    public RegraDeNegocioViolada comErros(List<Map<String, Object>> erros) {
        com("errors", erros);
        return this;
    }
}
