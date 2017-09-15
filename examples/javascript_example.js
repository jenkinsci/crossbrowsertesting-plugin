#!/usr/local/bin/node
//See https://code.google.com/p/selenium/wiki/WebDriverJs for detailed instructions

var webdriver = require('selenium-webdriver'),
    SeleniumServer = require('selenium-webdriver/remote').SeleniumServer;
 
var caps = {
    name : process.env.CBT_BUILD_NAME,
    build :  process.env.CBT_BUILD_NUMBER,
    browser_api_name : process.env.CBT_BROWSER,
    os_api_name : process.env.CBT_OPERATING_SYSTEM, 
    screen_resolution : process.env.CBT_RESOLUTION,
    browserName : process.env.CBT_BROWSERNAME,
    username : process.env.CBT_USERNAME,
    password : process.env.CBT_APIKEY,
    record_video: true
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
