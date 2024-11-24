package com.darren.stock

import io.cucumber.junit.platform.engine.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.Suite
import java.util.logging.Logger

@Suite
@IncludeEngines("cucumber")
//@SelectPackages("com.darren.stock")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.darren.stock.steps")
//@ConfigurationParameter(key = Constants.FEATURES_PROPERTY_NAME, value = "*.features")
class RunCucumberTest {
    private val logger = Logger.getLogger("RunCucumberTest")

    fun setUp() {
        logger.info("hello")
    }
}