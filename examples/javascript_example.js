#!/usr/local/bin/node
//See https://code.google.com/p/selenium/wiki/WebDriverJs for detailed instructions

var webdriver = require('selenium-webdriver'),
    SeleniumServer = require('selenium-webdriver/remote').SeleniumServer;

var caps = {
    name : process.env.CBT_BUILD_NAME,     // Need to set these to something else?
    build :  process.env.CBT_BUILD_NUMBER, // Check below to learn how
    browser_api_name : process.env.CBT_BROWSER,
    os_api_name : process.env.CBT_OPERATING_SYSTEM,
    screen_resolution : process.env.CBT_RESOLUTION,
    browserName : process.env.CBT_BROWSERNAME,
    username : process.env.CBT_USERNAME,
    password : process.env.CBT_APIKEY,
    record_video: true,
    // Need to set the build and name capabilites to something else?
    // Just set the jenkins_name and jenkins_build caps to the proper environment variables
    // that Jenkins sets. This allows Jenkins to locate the test after it has run:

    // name: "Hard-coded build name"
    // build: "1.0.0"
    // jenkins_name: process.env.CBT_BUILD_NAME,
    // jenkins_build:  process.env.CBT_BUILD_NUMBER,
};

var cs = "http://hub.crossbrowsertesting.com:80/wd/hub";

var driver = new webdriver.Builder()
    .usingServer(cs)
    .withCapabilities(caps)
    .build();

driver.get("http://google.com");
driver.findElement(webdriver.By.name('q')).sendKeys('cross browser testing');
driver.findElement(webdriver.By.name('btnG')).click();


driver.quit();
