package org.jenkinsci.plugins.cbt_jenkins;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Screenshots {

	String username, apikey;
	List<String> browserLists;
	Request req;
	
	public Screenshots(String username, String apikey) {
		this.username = username;
		this.apikey = apikey;
		req = new Request("screenshots", username, apikey);
		
		browserLists = new LinkedList<String>();
		populateBrowserLists();
	}
	private void populateBrowserLists() {
		browserLists.add(""); //add blank one
		String json="";
		try {
			json = req.get("/browserlists");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONArray j_browserLists = new JSONArray(json);
		for(int i=0; i<j_browserLists.length();i++) {
			JSONObject j_browserList = j_browserLists.getJSONObject(i);
			String browser_list_name = j_browserList.getString("browser_list_name");
			browserLists.add(browser_list_name);
		}
	}
	public HashMap<String, String> runScreenshotTest(String selectedBrowserList, String url) {
		String json = "";
		HashMap params = new HashMap();
		params.put("url", url);
		params.put("browser_list_name", selectedBrowserList);
		try {
			json = req.post("/", params);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return parseResults(json);
	}
	private HashMap<String, String> parseResults(String json) {
		HashMap<String, String> results = new HashMap<String, String>();
		JSONObject screenshotResults = new JSONObject(json);
		JSONArray screenshotVersions = screenshotResults.getJSONArray("versions");
		JSONObject latestScreenshotVersion = screenshotVersions.getJSONObject(screenshotVersions.length()-1);
		
		results.put("screenshot_test_id", Integer.toString(screenshotResults.getInt("screenshot_test_id")));
		results.put("url", screenshotResults.getString("url"));
		results.put("version_id", Integer.toString(latestScreenshotVersion.getInt("version_id")));
		results.put("download_results_zip_public_url", latestScreenshotVersion.getString("download_results_zip_public_url"));
		results.put("show_results_public_url", latestScreenshotVersion.getString("show_results_public_url"));
		
		return results;
	}
	
}
