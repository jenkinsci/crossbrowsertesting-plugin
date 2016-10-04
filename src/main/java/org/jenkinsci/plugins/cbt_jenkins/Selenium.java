package org.jenkinsci.plugins.cbt_jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Selenium {
	/*
	 * List of browsers for selenium testing
	 */

	private Request req = new Request("selenium");
	public ArrayList<Configuration> configurations = new ArrayList<Configuration>();
	
	public Selenium() {
		String json="";
		try {
			json = req.get("/browsers");
		}catch (IOException ioe) {}
		try {
			populateConfigurations(json);
		}catch (JSONException jsone) {}
		
	}
	public Selenium(String username, String apikey) {
		req = new Request("selenium", username, apikey);
		String json="";
		try {
			json = req.get("/browsers");
		}catch (IOException ioe) {}
		try {
			populateConfigurations(json);
		}catch (JSONException jsone) {}
		
	}
	
	private void populateConfigurations(String json) throws JSONException {
		JSONArray j_configurations = new JSONArray(json);
		for(int i=0; i<j_configurations.length();i++) {
			//parse out the OS info
			JSONObject j_config = j_configurations.getJSONObject(i);
			String config_api_name = j_config.getString("api_name");
			String config_name = j_config.getString("name");
			Configuration configuration = new Configuration(config_api_name, config_name);
			//parse out the browser info for the OS
			JSONArray j_browsers = j_config.getJSONArray("browsers");
			for(int j=0;j<j_browsers.length();j++) {
				JSONObject j_browser = j_browsers.getJSONObject(j);
				String browser_api_name = j_browser.getString("api_name");
				String browser_name = j_browser.getString("name");
				InfoPrototype browser = new InfoPrototype(browser_api_name, browser_name);
				configuration.browsers.add(browser);
			}
			//parse out the resolution info for the OS
			JSONArray resolutions = j_config.getJSONArray("resolutions");
			for(int j=0;j<resolutions.length();j++) {
				JSONObject j_resolution = resolutions.getJSONObject(j);
				String resolution_name = j_resolution.getString("name");
				InfoPrototype resolution = new InfoPrototype(resolution_name);
				configuration.resolutions.add(resolution);
			}
			configurations.add(configuration);
		}
	}
	public Configuration getConfig(String configName) {
		/*
		 * Gets the config from os api name
		 */
		Configuration c = new Configuration("","");
		
    	for (int i=0;i<configurations.size();i++) {
    		if (configName.equals(configurations.get(i).getApiName())) {
                c = configurations.get(i);
    		}
    	}
    	return c;
	}
	private String getBrowserName(Configuration config, String browserApiName) {
		/*
		 * Gets the browsers name from the browser api name
		 */
		InfoPrototype ip = new InfoPrototype("","");
        for (int i=0 ; i<config.browsers.size() ; i++) {
        	InfoPrototype configBrowser = config.browsers.get(i);
            if (configBrowser.getApiName().equals(browserApiName)) {
            	return configBrowser.getName();
        	}
    	}
		return "";
	}
	
	public String[] getSeleniumTestInfo(String name, String build, String browserApiName, String osApiName, String resolution) throws IOException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("name", name);
		params.put("build", build);
        Configuration config = getConfig(osApiName);
        String os = config.getName();
        String browser = getBrowserName(config, browserApiName);
		params.put("os", os);
		params.put("browser", browser);
		params.put("resolution",resolution);
		String json = req.get("", params);
		
		//got the test now need to parse out the id and publicUrl
		JSONObject j = new JSONObject(json);
		JSONArray seleniumTests = j.getJSONArray("selenium");
		JSONObject seleniumTest = seleniumTests.getJSONObject(0);
		int seleniumTestId = seleniumTest.getInt("selenium_test_id");
		String publicUrl = seleniumTest.getString("show_result_public_url");
		String[] testInfo = {Integer.toString(seleniumTestId), publicUrl};
		return testInfo;
		
	}
	private String apiSetAction(int seleniumTestId, String action, String value) throws IOException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("action", action);
		return req.put("/"+Integer.toString(seleniumTestId), params);
	}
	public void markPassOrFail(int seleniumTestId, boolean didPass) throws IOException{
		/*
		 * true = pass, false = fail
		 */
		if (didPass) {
			apiSetAction(seleniumTestId, "set_score", "pass");
		} else {
			apiSetAction(seleniumTestId, "set_score", "fail");
		}		
	}
	public void updateContributer(int seleniumTestId, String jenkinsVersion, String pluginVersion) throws IOException {
		/*
		 * contributer looks like "jenkins1.5|v0.21"
		 */
		apiSetAction(seleniumTestId, "set_contributer", "jenkins"+jenkinsVersion+"|v"+pluginVersion);	
	}
}

class InfoPrototype {
	/*
	 * Almost all of the JSON Objects have a "name" and "api_name"
	 * Doing it this way just because I'm lazy
	 */
	private String name;
	private String api_name;
	public InfoPrototype(String name) {
		this.name = name;
		this.api_name = "";
	}
	public InfoPrototype(String api_name, String name) {
		this.api_name = api_name;
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public String getApiName() {
		if (api_name.isEmpty() || api_name == null) {
			return name;
		}else {
			return api_name;
		}
	}
	public String toString() {
		if (name.isEmpty() || name == null) {
			return api_name;
		}else {
			return name;
		}
	}
}
class Configuration extends InfoPrototype{
	public ArrayList<InfoPrototype> resolutions = new ArrayList<InfoPrototype>();
	public ArrayList<InfoPrototype> browsers = new ArrayList<InfoPrototype>();

	public Configuration(String api_name, String name) {
		super(api_name, name);			
	}
	public String getResolutionApiName(String name) {
		String api_name="";
		for (InfoPrototype resolution : resolutions) {
			if (name.equals(resolution.getName())) {
				api_name = resolution.getApiName();
			}
		}
		return api_name;
	}
	public String getBrowsersApiName(String name) {
		String api_name="";
		for (InfoPrototype browser : browsers) {
			if (name.equals(browser.getName())) {
				api_name = browser.getApiName();
			}
		}
		return api_name;
	}
	
}
