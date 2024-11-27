package com.darren.stock

import io.cucumber.core.options.Constants.FILTER_TAGS_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("junit-jupiter", "cucumber")
//@SelectClasses(JupiterTest::class) // This selector is picked up by Jupiter
@SelectClasspathResource("com/darren/stock") // This selector is picked up by Cucumber
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @Ignore")

class RunSuiteTest
