package br.com.crudao.kanban.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datas em ISO-8601 UTC e duracoes em segundos nos payloads (Safeguards, secao 9). */
@Configuration
public class JacksonConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer kanbanObjectMapperCustomizer() {
    return builder ->
        builder
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .featuresToEnable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
  }
}
