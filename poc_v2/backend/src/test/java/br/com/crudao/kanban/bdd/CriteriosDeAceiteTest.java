package br.com.crudao.kanban.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Executa os cenarios de {@code features/kanban.feature} — um por criterio de aceite do PRD
 * (RF-001 a RF-019). Nome terminado em {@code Test} para entrar no surefire junto com o restante.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "br.com.crudao.kanban.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "summary")
class CriteriosDeAceiteTest {}
