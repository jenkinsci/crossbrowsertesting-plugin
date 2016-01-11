package org.jenkinsci.plugins.cbt_jenkins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BrowserList {
	
	private String requestURL = "http://crossbrowsertesting.com/api/v3/selenium/browsers";
	public ArrayList<Configuration> configurations = new ArrayList<Configuration>();
	
	public BrowserList() {
		String json="";
		try {
			json = get(requestURL);
		}catch (IOException ioe) {}
		try {
			parseJSON(json);
		}catch (JSONException jsone) {}
	}
	
	private void parseJSON(String json) throws JSONException {
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
	private String get(String urlStr) throws IOException{
		/*
		 * Get request
		 * returns JSON as a string
		 */
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}
		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();
		conn.disconnect();
		return sb.toString();
	}
	public Configuration getConfig(String configName) {
		Configuration c = new Configuration("","");
		
    	for (int i=0;i<configurations.size();i++) {
    		if (configName.equals(configurations.get(i).getName())) {
                c = configurations.get(i);
    		}
    	}
    	return c;
	}
}
class InfoPrototype {
	/*
	 * Almost all of the JSON Objects have a "name" and "api_name"
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
