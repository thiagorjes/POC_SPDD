package br.com.idsd.kanban.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * O teto de tamanho do corpo da requisicao, aplicado <b>antes</b> de qualquer
 * desserializacao.
 *
 * <p>ACH-05 da reexecucao de TASK-02.2. {@code @Size} sobre a lista de etapas so
 * e avaliado depois que Jackson ja materializou o array inteiro em memoria: o teto
 * de cem etapas protege o banco e nao a heap. Um corpo de dezenas de megabytes com
 * um milhao de etapas e recusado — depois de alocado. E alcanca toda rota que
 * aceita JSON, e nao apenas esta.
 *
 * <p><b>Sao duas guardas e nao uma, porque {@code Content-Length} e opcional.</b>
 * O cabecalho, quando vem, permite recusar sem ler um byte. Quando nao vem —
 * {@code Transfer-Encoding: chunked} —, a unica defesa e contar enquanto se le, e
 * e o que o envoltorio abaixo faz. Confiar so no cabecalho deixaria o teto ao
 * criterio de quem envia.
 *
 * <p>A recusa por cabecalho sai em {@code 413} com o corpo padrao do sistema,
 * montado aqui porque este filtro roda antes do despachante e nao ha tratador de
 * excecao a alcancar — e o mesmo caminho de {@link LimiteDeRequisicoes}. Ja o
 * estouro durante a leitura nao pode sair em {@code 413}: a essa altura o
 * despachante ja assumiu a requisicao, e a {@link IOException} chega ao tratador
 * global como corpo ilegivel, {@code 400}. A diferenca de codigo entre os dois
 * caminhos e consequencia de onde cada um e detectado, e esta declarada aqui em
 * vez de descoberta por quem comparar duas respostas.
 */
public class LimiteDeCorpo extends OncePerRequestFilter {

    private final long tetoEmBytes;
    private final ObjectMapper conversor;

    public LimiteDeCorpo(long tetoEmBytes, ObjectMapper conversor) {
        this.tetoEmBytes = tetoEmBytes;
        this.conversor = conversor;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {

        if (requisicao.getContentLengthLong() > tetoEmBytes) {
            ProblemaDetalhado.escrever(
                    resposta,
                    ProblemaDetalhado.de(
                            HttpStatus.PAYLOAD_TOO_LARGE,
                            "corpo-grande-demais",
                            "Conteúdo grande demais",
                            "O conteúdo enviado excede o tamanho aceito. Reduza a "
                                    + "quantidade de itens e tente de novo.",
                            requisicao.getRequestURI()),
                    conversor);
            return;
        }

        cadeia.doFilter(new CorpoLimitado(requisicao, tetoEmBytes), resposta);
    }

    /** Requisicao cujo corpo para de ser legivel ao ultrapassar o teto. */
    private static final class CorpoLimitado extends HttpServletRequestWrapper {

        private final long teto;

        private CorpoLimitado(HttpServletRequest original, long teto) {
            super(original);
            this.teto = teto;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream origem = super.getInputStream();
            return new ServletInputStream() {

                private long lidos;

                @Override
                public int read() throws IOException {
                    int byteLido = origem.read();
                    if (byteLido != -1 && ++lidos > teto) {
                        throw new IOException("corpo da requisicao acima do teto");
                    }
                    return byteLido;
                }

                @Override
                public int read(byte[] destino, int inicio, int quantidade) throws IOException {
                    int lidosAgora = origem.read(destino, inicio, quantidade);
                    if (lidosAgora > 0 && (lidos += lidosAgora) > teto) {
                        throw new IOException("corpo da requisicao acima do teto");
                    }
                    return lidosAgora;
                }

                @Override
                public boolean isFinished() {
                    return origem.isFinished();
                }

                @Override
                public boolean isReady() {
                    return origem.isReady();
                }

                @Override
                public void setReadListener(ReadListener ouvinte) {
                    origem.setReadListener(ouvinte);
                }
            };
        }
    }
}
