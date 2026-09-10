package br.com.idsd.kanban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do backend.
 *
 * <p>Resource Server puro: nao ha {@code client secret} aqui. O authorization
 * code com PKCE vive no frontend, com client publico (TechSpec secao 8).
 */
@SpringBootApplication
public class Aplicacao {

    public static void main(String[] args) {
        SpringApplication.run(Aplicacao.class, args);
    }
}
