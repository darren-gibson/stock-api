package com.darren.stock

import io.cucumber.junit.platform.engine.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.Suite

@Suite
//@IncludeEngines("cucumber")
//@SelectPackages("com.darren.stock")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.darren.stock.steps")
@ConfigurationParameter(key = Constants.FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
//@ConfigurationParameter(key = Constants.FEATURES_PROPERTY_NAME, value = "*.features")
class RunCucumberTest