package org.jenkinsci.plugins.cbt_jenkins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
//import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import net.sf.json.JSONObject;

import com.crossbrowsertesting.api.Account;
import com.crossbrowsertesting.api.LocalTunnel;
import com.crossbrowsertesting.plugin.Constants;

//import java.util.logging.Logger;

@SuppressWarnings("serial")
public class CBTBuildWrapper extends BuildWrapper implements Serializable {

	private LocalTunnel tunnel;
	private boolean useLocalTunnel,
					useTestResults;
    private List <JSONObject> seleniumTests,
    						  screenshotsTests;
    private String localTunnelPath,
    			   nodePath,
    			   tunnelName,
    			   credentialsId,
    			   username,
    			   authkey = "";
    private boolean pluginStartedTunnel = false;
    
    //private final static Logger log = Logger.getLogger(CBTBuildWrapper.class.getName());

    @DataBoundConstructor
    public CBTBuildWrapper(List<JSONObject> screenshotsTests, List<JSONObject> seleniumTests, boolean useLocalTunnel, boolean useTestResults, String credentialsId, String tunnelName) {
    	/*
    	 * Instantiated when the configuration is saved
    	 * Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    	 */
    	//System.out.println("in build wrapper");
    	this.screenshotsTests = screenshotsTests;
    	this.seleniumTests = seleniumTests;
    	
    	this.useLocalTunnel = useLocalTunnel;
    	this.useTestResults = useTestResults;
    	
    	this.tunnelName = tunnelName;
    	this.credentialsId = credentialsId;
    	
    	// reset the username and authkey for screenshots
        final CBTCredentials credentials = CBTCredentials.getCredentials(null, credentialsId);
        this.username = credentials.getUsername();
        this.authkey = credentials.getAuthkey();
        getDescriptor().setBuildCredentials(username, authkey);

    	//advanced options
    	//this.localTunnelPath = localTunnelPath;
    	//this.nodePath = nodePath;
    }

    public List<JSONObject> getSeleniumTests() {
    	return this.seleniumTests;
    }
    public List<JSONObject> getScreenshotsTests() {
    	return this.screenshotsTests;
    }
    public boolean getUseLocalTunnel() {
    	return this.useLocalTunnel;
    }
    public boolean getUseTestResults() {
    	return this.useTestResults;
    }
    public String getLocalTunnelPath() {
    	return this.localTunnelPath;
    }
    public String getNodePath() {
    	return this.nodePath;
    }
    public String getCredentialsId() {
    	return this.credentialsId;
    }
    public String getTunnelName() {
    	return this.tunnelName;
    }
    private File downloadTunnelBinary(FilePath workspace) throws IOException {
    	URL tunnelBinaryAddress;
    	String binaryName;
		if (System.getProperty("os.name").toLowerCase().contains("mac")) { // mac
			tunnelBinaryAddress = new URL("https://github.com/crossbrowsertesting/cbt-tunnel-nodejs/releases/download/v0.1.0/cbt-tunnels-macos");
			binaryName = "cbt_tunnels";
		}else if (System.getProperty("os.name").toLowerCase().contains("win")) { // windows
			tunnelBinaryAddress = new URL("https://github.com/crossbrowsertesting/cbt-tunnel-nodejs/releases/download/v0.1.0/cbt-tunnels-win.exe");
			binaryName = "cbt_tunnels.exe";
		}else if (System.getProperty("os.name").toLowerCase().contains("nix") ||
				System.getProperty("os.name").toLowerCase().contains("nux") ||
				System.getProperty("os.name").toLowerCase().contains("aix")) { // linux / unix ?
			tunnelBinaryAddress = new URL("https://github.com/crossbrowsertesting/cbt-tunnel-nodejs/releases/download/v0.1.0/cbt-tunnels-ubuntu");
			binaryName = "cbt_tunnels";
		}else {
			return null;
		}
		File binary = new File(workspace.toString(), binaryName);
    	FileUtils.copyURLToFile(tunnelBinaryAddress, binary);
		if (System.getProperty("os.name").toLowerCase().contains("mac") ||
				System.getProperty("os.name").toLowerCase().contains("nix") ||
				System.getProperty("os.name").toLowerCase().contains("nux") ||
				System.getProperty("os.name").toLowerCase().contains("aix")) { //unix needs executable permission
			binary.setExecutable(true);			
		}
				
    	return binary;
    }
    @SuppressWarnings("rawtypes")
	@Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
    	/*
    	 * called when you the build runs
    	 */
        final CBTCredentials credentials = CBTCredentials.getCredentials(build.getProject(), credentialsId);
        this.username = credentials.getUsername();
        this.authkey = credentials.getAuthkey();
        //track install
        Account account = new Account(username, authkey);
        account.init();
        if (account.connectionSuccessful) {
    		account.sendMixpanelEvent("Jenkins Plugin Downloaded");
        }
        
        getDescriptor().seleniumApi.setRequest(username, authkey); // add credentials to requests
        // tunnel stuff
        if (!tunnelName.isEmpty() && useLocalTunnel) {
        	listener.getLogger().println(Constants.TUNNEL_USING_TUNNELNAME(tunnelName));
        	tunnel = new LocalTunnel(username, authkey, tunnelName);
        }else if(tunnelName.isEmpty() && useLocalTunnel){
        	listener.getLogger().println(Constants.TUNNEL_USING_DEFAULT);
        	tunnel = new LocalTunnel(username, authkey);
        }
    	//tunnel.queryTunnel();
    	//getDescriptor().checkProxySettingsAndReloadRequest(tunnel);
    	if (useLocalTunnel) {
    		File binary = downloadTunnelBinary(build.getWorkspace());
        	tunnel.queryTunnel();
        	getDescriptor().checkProxySettingsAndReloadRequest(tunnel);
    		if (!tunnel.isTunnelRunning) {
    			listener.getLogger().println(Constants.TUNNEL_NEED_TO_START);
    			Launcher.ProcStarter tunnelProcess = launcher.launch();
    			tunnelProcess.cmdAsSingleString(buildStartTunnelCommand(binary.getAbsolutePath()));
    			pluginStartedTunnel = true;
    			tunnelProcess.start();
    			//tunnel.start(nodePath, localTunnelPath);

    			listener.getLogger().println(Constants.TUNNEL_WAITING);
    			for (int i=1 ; i<15 && !tunnel.isTunnelRunning ; i++) {
    				//will check every 2 seconds for upto 30 to see if the tunnel connected
    				Thread.sleep(4000);
    				tunnel.queryTunnel();
    			}
    			if (tunnel.isTunnelRunning) {
    				listener.getLogger().println(Constants.TUNNEL_CONNECTED);
    			}else {
    				throw new Error(Constants.TUNNEL_START_FAIL);
    			}
    		}else {
    			listener.getLogger().println(Constants.TUNNEL_NO_NEED_TO_START);
    		}
    	}
    	
    	// screenshots stuff
    	if (screenshotsTests != null && !screenshotsTests.isEmpty()) {
    		Iterator<JSONObject> screenshotsIterator = screenshotsTests.iterator();
    		while(screenshotsIterator.hasNext()) {
    			//System.out.println("in screenshot loop");
	    		JSONObject ssTest = screenshotsIterator.next();
    			String screenshotsBrowserList = ssTest.getString("browserList");
    			String screenshotsUrl = ssTest.getString("url");
    			
		    	HashMap<String, String> screenshotTestResultsInfo = getDescriptor().screenshotApi.runScreenshotTest(screenshotsBrowserList, screenshotsUrl);
		    	screenshotTestResultsInfo.put("browser_list", screenshotsBrowserList);
		    	screenshotTestResultsInfo.put("url", screenshotsUrl);
		    	ScreenshotsBuildAction ssBuildAction = new ScreenshotsBuildAction(useTestResults, screenshotsBrowserList, screenshotsUrl);
		    	ssBuildAction.setBuild(build);
		    	ssBuildAction.setTestinfo(screenshotTestResultsInfo);
		    	
		    	if (screenshotTestResultsInfo.containsKey("error")) {
		    		listener.getLogger().println("[ERROR] 500 error returned for Screenshot Test");
		    	} else {
		    		build.addAction(ssBuildAction);
		    		if (!screenshotTestResultsInfo.isEmpty()) {
		    			listener.getLogger().println("\n-----------------------");
		    			listener.getLogger().println("SCREENSHOT TEST RESULTS");
		    			listener.getLogger().println("-----------------------");
		    		}
				    for (Map.Entry<String, String> screenshotResultsEntry : screenshotTestResultsInfo.entrySet()) {
				    	listener.getLogger().println(screenshotResultsEntry.getKey() + ": "+ screenshotResultsEntry.getValue());
				    }
		    	}
    		}
    	}
    	return new CBTEnvironment(build);
    }
    private String buildStartTunnelCommand(String tunnelPath) {
    	if (tunnelPath == null) {
    		tunnelPath = "cbt_tunnels";
    	}
        String startTunnelCmd = "\"" + tunnelPath + "\" --username " + username + " --authkey " + authkey;
        if (!tunnelName.isEmpty()) {
        	startTunnelCmd += " --tunnelname "+ tunnelName;
        }
        if (tunnel.useProxy()) {
        	startTunnelCmd += " --proxyPort " + Integer.toString(tunnel.proxyPort());
			String proxyUrl = tunnel.proxyUrl();
			if (tunnel.useProxyCredentials()) {
				proxyUrl = tunnel.proxyUsername() + ":" + tunnel.proxyPassword() + "@" + proxyUrl;
			}
			startTunnelCmd += " --proxyIp " + proxyUrl;
        }
    	return startTunnelCmd;
    }
    
    @Override
    public CBTDescriptor getDescriptor() {
    	return (CBTDescriptor) super.getDescriptor();
    }
    
	@SuppressWarnings("rawtypes")
    private class CBTEnvironment extends BuildWrapper.Environment {
		private AbstractBuild build;
    	private CBTEnvironment(final AbstractBuild build) {
    		this.build = build;
    	}
    	private void makeSeleniumBuildActionFromJSONObject(JSONObject config) {
    		String buildname = build.getFullDisplayName().substring(0, build.getFullDisplayName().length()-(String.valueOf(build.getNumber()).length()+1));
			String buildnumber = String.valueOf(build.getNumber());
			String operatingSystemApiName = config.getString("operating_system");
			String browserApiName = config.getString("browser");
			String resolution = config.getString("resolution");
	    	SeleniumBuildAction seBuildAction = new SeleniumBuildAction(useTestResults, operatingSystemApiName, browserApiName, resolution);
	    	seBuildAction.setBuild(build);
	    	seBuildAction.setBuildName(buildname);
	    	seBuildAction.setBuildNumber(buildnumber);
	    	build.addAction(seBuildAction);
    	}
    	private JSONObject addBrowserNameToJSONObject(JSONObject config) {
			String operatingSystemApiName = config.getString("operating_system");
			String browserApiName = config.getString("browser");
			// Javascript Selenium Tests have an extra capability "browserName"
			String browserIconClass = getDescriptor().seleniumApi.operatingSystems2.get(operatingSystemApiName).browsers2.get(browserApiName).getIconClass();
			String browserName = "";
			if (browserIconClass.equals("ie")) {
				browserName = "internet explorer";
			} else if (browserIconClass.equals("safari-mobile")) {
				browserName = "safari";
			} else {
				browserName = browserIconClass;
			}
			config.put("browserName", browserName);
			return config;
    	}
    	@Override
    	public void buildEnvVars(Map<String, String> env) {
    		// selenium environment variables
    		String buildname = build.getFullDisplayName().substring(0, build.getFullDisplayName().length()-(String.valueOf(build.getNumber()).length()+1));
			String buildnumber = String.valueOf(build.getNumber());
    		JSONArray browsers = new JSONArray();
    		for (JSONObject config : seleniumTests) {
    			config = addBrowserNameToJSONObject(config);
    			makeSeleniumBuildActionFromJSONObject(config);
				browsers.put(config);
    		}
        	if ( seleniumTests.size() == 1 ){
        		JSONObject seTest = seleniumTests.get(0);
        		seTest = addBrowserNameToJSONObject(seTest);
    			String operatingSystemApiName = seTest.getString("operating_system");
    			String browserApiName = seTest.getString("browser");
    			String resolution = seTest.getString("resolution");
    			String browserName = seTest.getString("browserName");
    			env.put(Constants.OPERATINGSYSTEM, operatingSystemApiName);
    			env.put(Constants.BROWSER, browserApiName);
    			env.put(Constants.RESOLUTION, resolution);
    			env.put(Constants.BROWSERNAME, browserName);
        	}
    		env.put(Constants.BROWSERS, browsers.toString());
    		env.put(Constants.USERNAME, username);
    		env.put(Constants.APIKEY, authkey);
    		env.put(Constants.AUTHKEY, authkey);
    		env.put(Constants.BUILDNAME, buildname);
    		env.put(Constants.BUILDNUMBER, buildnumber);
    		super.buildEnvVars(env);
    	}
    	@Override
    	public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
    		/*
    		 * Runs after the build
    		 */
			HashMap<String, Queue<Map<String, String>>> seleniumEnvironments = new HashMap<String, Queue<Map<String, String>>>();
			for (SeleniumBuildAction se : build.getActions(SeleniumBuildAction.class)) {
				//System.out.println("build action stuff");
				// this is to catch a user that puts the same configuration more than once
				// instead of making the call to "/selenium" multiple times, it only calls it once and reuses the results
					String key = se.getBrowser()+se.getOperatingSystem()+se.getResolution();
					
					String buildName = se.getBuildName();
					String buildNumber = se.getBuildNumber();
					String browserApiName = se.getBrowser();
					String osApiName = se.getOperatingSystem();
					String resolution = se.getResolution();
					
					if (!seleniumEnvironments.containsKey(key)) {
						Queue<Map<String, String>> tests = getDescriptor().seleniumApi.getSeleniumTestInfo2(buildName, buildNumber, browserApiName, osApiName, resolution);
						seleniumEnvironments.put(key, tests);
					}
					Map<String, String> testInfo = seleniumEnvironments.get(key).poll();
					String seleniumTestId = "";
					String publicUrl = "";
					try {
						seleniumTestId = testInfo.get("selenium_test_id");
						publicUrl = testInfo.get("show_result_public_url");
					}catch (NullPointerException npe) {}
					String jenkinsVersion = build.getHudsonVersion();
					String pluginVersion = getDescriptor().getVersion();

					se.setTestId(seleniumTestId);
					se.setTestPublicUrl(publicUrl);
					se.setTestUrl(seleniumTestId);
					if(seleniumTestId == null || seleniumTestId.isEmpty()) {
						// lets get a phony test id for the contributer if we cant find one for some reason
						seleniumTestId = getDescriptor().seleniumApi.getSeleniumTestId(buildName, buildNumber, browserApiName, osApiName, resolution);
					}
					getDescriptor().seleniumApi.updateContributer(seleniumTestId, Constants.JENKINS_CONTRIBUTER, jenkinsVersion, pluginVersion);
			}
			// we need to wait for the screenshots tests to finish (definitely before closing the tunnel)
			boolean isAtLeastOneScreenshotTestActive;
			do {
				listener.getLogger().println("trying to see if there are still screenshots running");
				isAtLeastOneScreenshotTestActive = false;
				for (ScreenshotsBuildAction ss : build.getActions(ScreenshotsBuildAction.class)) {
					// checks each screenshot_test_id to see if the test is finished
					if(getDescriptor().screenshotApi.isTestRunning(ss.getTestId())) {
						listener.getLogger().println("at least one screenshots test is still running");
						isAtLeastOneScreenshotTestActive = true;
					}
					Thread.sleep(30000);
				}
				// if any of the tests say they are still running. try again
			}while(isAtLeastOneScreenshotTestActive);
			if (pluginStartedTunnel) {				
				tunnel.stop();
    			for (int i=1 ; i<20 && tunnel.isTunnelRunning; i++) {
    				//will check every 15 seconds for up to 5 minutes to see if the tunnel disconnected
    				Thread.sleep(15000);
    				tunnel.queryTunnel();
    			}
    			if (!tunnel.isTunnelRunning) {
    				listener.getLogger().println(Constants.TUNNEL_STOP);
    			} else {
    				listener.getLogger().println(Constants.TUNNEL_STOP_FAIL);
    			}
			}
			return true;
    	}
    }
}
