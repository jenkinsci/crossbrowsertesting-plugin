package org.jenkinsci.plugins.cbt_jenkins;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;

import hudson.FilePath;

public class LocalTunnel {
	private Request req;
	public boolean isTunnelRunning = false;
	public boolean jenkinsStartedTheTunnel = false;
	public Process tunnelProcess;
	public int tunnelID;
	private String username, apikey;
	
	public LocalTunnel(String username, String apikey) {
		this.username = username;
		this.apikey = apikey;
		req = new Request("tunnels", username, apikey);
		queryTunnel();
	}
	public boolean queryTunnel() throws JSONException {
		String json="";
		try {
			json = req.get("?num=1&active=true");
		}catch (IOException ioe) {}
		try {
			JSONObject res = new JSONObject(json);
			JSONArray tunnels = res.getJSONArray("tunnels");
			boolean isActive = false;
			for (int i=0; i<tunnels.length();i++) {
				JSONObject tunnel = tunnels.getJSONObject(i);
				tunnelID = tunnel.getInt("tunnel_id");
				isActive = tunnel.getBoolean("active");
			}
			isTunnelRunning = isActive;
			return isActive;
		}catch (JSONException jsone) {
			return false;
		}
	}
	public void start(FilePath workspace) throws IOException {
		//FilePath killfile = workspace.createTempFile("killfile", null);
		String tunnelCommand = "cbt_tunnels --username " + username + " --authkey " +apikey;
		tunnelProcess = Runtime.getRuntime().exec(tunnelCommand);
		jenkinsStartedTheTunnel = true;
	}
	public void stop() throws IOException, InterruptedException {
		queryTunnel();
		String json = req.delete("/"+Integer.toString(tunnelID));
		tunnelProcess.destroy();
	}
}
