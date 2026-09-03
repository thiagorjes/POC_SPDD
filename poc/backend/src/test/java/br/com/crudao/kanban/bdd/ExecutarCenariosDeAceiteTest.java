package br.com.crudao.kanban.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Executa os cenarios de aceite no ciclo unitario ({@code surefire}) — o container ja e
 * compartilhado com a suite de integracao.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "br.com.crudao.kanban.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "summary")
public class ExecutarCenariosDeAceiteTest {}
