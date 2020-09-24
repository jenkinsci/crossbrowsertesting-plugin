package org.jenkinsci.plugins.cbt_jenkins;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class SeleniumBuildActionTest {
	private Boolean link = true;
	private String os = "Mac10.15";
	private String browser = "Chrome83";
	private String resolution = "1366x768";

    @Test
    public void testSeleniumBuildCaps(){

       	SeleniumBuildAction seleniumBuild = new SeleniumBuildAction(link, os, browser, resolution);

       	String osName = seleniumBuild.getOperatingSystem();
       	String browserName = seleniumBuild.getBrowser();
       	String res = seleniumBuild.getResolution();


        Assertions.assertTrue(osName.equals(os));
        Assertions.assertTrue(browserName.equals(browser));
        Assertions.assertTrue(res.equals(resolution));

	}
}
