package org.jenkinsci.plugins.cbt_jenkins;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class SeleniumTest {
    WebDriver driver;
    String user, api_key, name, build, browser_api_name, os_api_name, screen_resolution;
    
	SeleniumTest(String user, String api_key, String name, String build, String browser_api_name, String os_api_name, String screen_resolution) {
		this.user = user;
		this.api_key = api_key;
		setName(name);
		setBuild(build);
		setBrowser_api_name(browser_api_name);
		setOs_api_name(os_api_name);
		setScreen_resolution(screen_resolution);
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setBuild(String build) {
		this.build = build;
	}
	public void setBrowser_api_name(String browser_api_name) {
		this.browser_api_name = browser_api_name;
	}
	public void setOs_api_name(String os_api_name) {
		this.os_api_name = os_api_name;
	}
	public void setScreen_resolution(String screen_resolution) {
		this.screen_resolution = screen_resolution;
	}
	public void run() {
	    DesiredCapabilities caps = new DesiredCapabilities();
	    caps.setCapability("name", name);
	    caps.setCapability("build", build);
	    caps.setCapability("browser_api_name", browser_api_name);
	    caps.setCapability("os_api_name", os_api_name);
	    caps.setCapability("screen_resolution", screen_resolution);
//	    caps.setCapability("record_video", "true");
//	    caps.setCapability("record_network", "true");
	
		try {
			driver = new RemoteWebDriver(new URL("http://"+user+":"+api_key+"@hub.crossbrowsertesting.com:80/wd/hub"), caps);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	
	    //load the page url
	    System.out.println("Loading Url");
	    driver.get("http://crossbrowsertesting.github.io/selenium_example_page.html");
	
	    //maximize the window - DESKTOPS ONLY
	    //System.out.println("Maximizing window");
	    //driver.manage().window().maximize();
	    
	    //check the title
	    System.out.println(driver.getTitle());
	
	    //quit the browser
	    driver.quit();
  }
}