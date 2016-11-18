package org.jenkinsci.plugins.cbt_jenkins;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

public class CBTBuildWrapper extends BuildWrapper implements Serializable {

	private static String username;
	private static String apikey;
	private static Screenshots screenshotApi;
	private static Selenium seleniumApi = new Selenium();
	private static LocalTunnel tunnel;
	private static boolean useLocalTunnel;
	private static boolean useTestResults;
		
	// we'll save these off so that the info repopulates when you reload the configure page
    private List <JSONObject> seleniumTests;
    private List <JSONObject> screenshotsTests;
    
    private static String localTunnelPath = "";
    private static String nodePath = "";

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor" 
    @DataBoundConstructor
    public CBTBuildWrapper(List<JSONObject> screenshotsTests, List<JSONObject> seleniumTests, boolean useLocalTunnel, boolean useTestResults, String localTunnelPath, String nodePath) {
    	username = getDescriptor().getUsername();
    	apikey = getDescriptor().getApikey();
    	
    	seleniumApi.setRequest(username, apikey); // add credentials to requests

    	this.screenshotsTests = screenshotsTests;
    	this.seleniumTests = seleniumTests;
    	
    	this.useLocalTunnel = useLocalTunnel;
    	this.useTestResults = useTestResults;
    	
    	//advanced options
    	this.localTunnelPath = localTunnelPath;
    	this.nodePath = nodePath;
    	
    	tunnel = new LocalTunnel(username, apikey);
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
    
    /*
     *  Main function
     */
    @SuppressWarnings("rawtypes")
	@Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

    	listener.getLogger().println(build.getFullDisplayName());
    	FilePath workspace = build.getWorkspace();
    	// This is where you 'build' the project.
    	if (useLocalTunnel) {
    		listener.getLogger().println("Going to use tunnel");
    		if (!tunnel.isTunnelRunning) {
    			listener.getLogger().println("Tunnel is currently not running. Need to start one.");
    			tunnel.start(nodePath, localTunnelPath);
    			listener.getLogger().println("Waiting for the tunnel to establish a connection.");
    			for (int i=1 ; i<15 && !tunnel.isTunnelRunning ; i++) {
    				//will check every 2 seconds for upto 30 to see if the tunnel connected
    				Thread.sleep(4000);
    				tunnel.queryTunnel();
    			}
    			if (tunnel.isTunnelRunning) {
    				listener.getLogger().println("Tunnel is now connected.");
    			}else {
    				throw new Error("The local tunnel did not connect within 30 seconds");
    			}
    		}else {
    			listener.getLogger().println("Tunnel is already running. No need to start a new one.");
    		}
    	}
    	if (screenshotsTests != null && !screenshotsTests.isEmpty()) {
    		Iterator<JSONObject> screenshotsIterator = screenshotsTests.iterator();
    		while(screenshotsIterator.hasNext()) {
	    		JSONObject ssTest = screenshotsIterator.next();
    			String screenshotsBrowserList = ssTest.getString("browserList");
    			String screenshotsUrl = ssTest.getString("url");
    			
		    	HashMap<String, String> screenshotTestResultsInfo = screenshotApi.runScreenshotTest(screenshotsBrowserList, screenshotsUrl);
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
    	
    	// Do the selenium tests
    	if (seleniumTests != null && !seleniumTests.isEmpty()) {
	    	listener.getLogger().println("\n---------------------");
	    	listener.getLogger().println("SELENIUM TEST RESULTS");
	    	listener.getLogger().println("---------------------");
	    	
	    	Iterator<JSONObject> i = seleniumTests.iterator();

	    	while(i.hasNext()) {
	    		JSONObject seTest = i.next();
    			String operatingSystemApiName = seTest.getString("operatingSystem");
    			String browserApiName = seTest.getString("browser");
    			String resolution = seTest.getString("resolution");
	
	    		workspace = build.getWorkspace();
		    	
		    	//really bad way to remove the build number from the name...
	    		String buildname = build.getEnvironment().get("JOB_NAME");
	    		String buildnumber = build.getEnvironment().get("BUILD_NUMBER");
	    		
				for (FilePath executable : workspace.list()) {
			    	// build the environment variables list
			    	EnvVars env = new EnvVars();
			    	env.put("CBT_USERNAME", username);
			    	env.put("CBT_APIKEY", apikey);
			    	env.put("CBT_BUILD_NAME", buildname);
			    	env.put("CBT_BUILD_NUMBER", buildnumber);
			    	env.put("CBT_OPERATING_SYSTEM", operatingSystemApiName);
			    	env.put("CBT_BROWSER", browserApiName);
			    	env.put("CBT_RESOLUTION", resolution);
			    	
					String fileName = executable.getName();
					//Extract extension
					String extension = "";
					int l = fileName.lastIndexOf('.');
					if (l > 0) {
					    extension = fileName.substring(l+1);
					}					
					
					// supported extensions
					if (extension.equals("py") || extension.equals("rb") || extension.equals("jar") || extension.equals("js") || (extension.equals("exe")) || extension.equals("sh") || extension.equals("bat")) {
						boolean isJavascriptTest = false; // JS selenium tests have an extra cap
						ArgumentListBuilder cmd = new ArgumentListBuilder();
						// figure out how to launch it					
						if (extension.equals("py") || extension.equals("rb") || extension.equals("jar") || extension.equals("js") || extension.equals("sh")) { //executes with full filename
							if (extension.equals("py")) { //python
								cmd.add("python");
							}else if (extension.equals("rb")) { //ruby
								cmd.add("ruby");
							}else if (extension.equals("jar")) { //java jar
								cmd.add("java");
								cmd.add("-jar");
							}else if (extension.equals("js")) { //node javascript
								cmd.add("node");
								isJavascriptTest = true;
							}else if (extension.equals("sh")) { // custom shell script
								cmd.add("sh");
								isJavascriptTest = true;
							}
							cmd.add(executable.getName());
						} else if (extension.equals("exe") || extension.equals("bat")) { //exe csharp
							FilePath csharpScriptPath = new FilePath(workspace, executable.getName()); 
							cmd.add(csharpScriptPath.toString());
							isJavascriptTest = true;
						}
						if (isJavascriptTest) {
							// Javascript Selenium Tests have an extra capability "browserName"
							String browserIconClass = seleniumApi.getIconClass(operatingSystemApiName, browserApiName);
							String browserName = "";
							if (browserIconClass.equals("ie")) {
								browserName = "internet explorer";
							} else if (browserIconClass.equals("safari-mobile")) {
								browserName = "safari";
							} else {
								browserName = browserIconClass;
							}
							env.put("CBT_BROWSERNAME", browserName);
						}
						launcher = launcher.decorateByEnv(env); //set the environment variables
						
				    	// log the environment variables to the Jenkins build console
				    	listener.getLogger().println("\nEnvironment Variables");
				    	listener.getLogger().println("---------------------");
				    	for (Map.Entry<String, String> envvar : env.entrySet()) {
				    		listener.getLogger().println(envvar.getKey() + ": "+ envvar.getValue());
				    	}
				    	Launcher.ProcStarter lp = launcher.launch();
				    	lp.pwd(workspace); //set the working directory
						
						lp.cmds(cmd);
						listener.getLogger().println("\nErrors/Output");
						listener.getLogger().println("-------------");
						//write the output from the script to the console
						lp.stdout(listener);
				    	lp.join(); //run the tests
				    	SeleniumBuildAction seBuildAction = new SeleniumBuildAction(useTestResults, operatingSystemApiName, browserApiName, resolution);
				    	seBuildAction.setBuild(build);
				    	seBuildAction.setBuildName(buildname);
				    	seBuildAction.setBuildNumber(buildnumber);
				    	build.addAction(seBuildAction);
					}
				}
	    	}
    	}
		return new Environment() {
			public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
				for (SeleniumBuildAction se : build.getActions(SeleniumBuildAction.class)) {
						String[] testInfo = seleniumApi.getSeleniumTestInfo(se.getBuildName(), se.getBuildNumber(), se.getBrowser(), se.getOperatingSystem(), se.getResolution());
						
						String seleniumTestId = testInfo[0];
						String publicUrl = testInfo[1];
						String jenkinsVersion = build.getHudsonVersion();
						String pluginVersion = getDescriptor().getVersion();

						se.setTestId(seleniumTestId);
						se.setTestPublicUrl(publicUrl);
						seleniumApi.updateContributer(seleniumTestId, jenkinsVersion, pluginVersion);
				}
				// we need to wait for the screenshots tests to finish (definitely before closing the tunnel)
				boolean isAtLeastOneSeleniumTestActive;
				do {
					isAtLeastOneSeleniumTestActive = false;
					for (ScreenshotsBuildAction ss : build.getActions(ScreenshotsBuildAction.class)) {
						// checks each screenshot_test_id to see if the test is finished
						if(screenshotApi.isTestRunning(ss.getTestId())) {
							isAtLeastOneSeleniumTestActive = true;
						}
						Thread.sleep(30000);
					}
					// if any of the tests say they are still running. try again
				}while(isAtLeastOneSeleniumTestActive);
				if (tunnel.jenkinsStartedTheTunnel) {					
					tunnel.stop();
	    			for (int i=1 ; i<4 && tunnel.isTunnelRunning; i++) {
	    				//will check every 15 seconds for up to 1 minute to see if the tunnel disconnected
	    				Thread.sleep(15000);
	    				tunnel.queryTunnel();
	    			}
	    			if (!tunnel.isTunnelRunning) {
	    				listener.getLogger().println("Tunnel is now disconnected.");
	    			} else {
	    				listener.getLogger().println("[WARNING]: Failed disconnecting the local tunnel");
	    			}
				}
				return true;
			}
		};
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
        private String 	cbtUsername,
        				cbtApikey = "";
        
		public DescriptorImpl() throws IOException {
            load();
        }
		
    	public String getUsername() {
    		return cbtUsername;
    	}
    	public String getApikey() {
    		return cbtApikey;
    	}
    	public String getVersion() {
    		String fullVersion = getPlugin().getVersion();
    		String stuffToIgnore = fullVersion.split("^\\d+[\\.]?\\d*")[1];
    		return fullVersion.substring(0, fullVersion.indexOf(stuffToIgnore));
    	}

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
   
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
/*
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }
*/
    	
        public ListBoxModel doFillOperatingSystemItems() {
        	ListBoxModel items = new ListBoxModel();
            for (int i=0 ; i<seleniumApi.configurations.size() ; i++) {
            	Configuration config = seleniumApi.configurations.get(i);
                items.add(config.getName(), config.getApiName());
            }          
            return items;
        }
        public ListBoxModel doFillBrowserItems(@QueryParameter String operatingSystem) {
            ListBoxModel items = new ListBoxModel();
            Configuration config = seleniumApi.getConfig(operatingSystem);
            for (int i=0 ; i<config.browsers.size() ; i++) {
            	InfoPrototype configBrowser = config.browsers.get(i);
                items.add(configBrowser.getName(), configBrowser.getApiName());
        	}
            return items;
        }
        public ListBoxModel doFillResolutionItems(@QueryParameter String operatingSystem) {
            ListBoxModel items = new ListBoxModel();
            Configuration config = seleniumApi.getConfig(operatingSystem);
            for (int i=0 ; i<config.resolutions.size() ; i++) {
            	InfoPrototype configResolution = config.resolutions.get(i);
                items.add(configResolution.getName());
        	}
            return items;
        }
        public ListBoxModel doFillBrowserListItems() {
			screenshotApi = new Screenshots(cbtUsername, cbtApikey);
            ListBoxModel items = new ListBoxModel();

            for (int i=0 ; i<screenshotApi.browserLists.size() ; i++) {
            	String browserList = screenshotApi.browserLists.get(i);
                items.add(browserList);
            }
            return items;
        }
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "CrossBrowserTesting.com";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist configuration information,
            // set that to properties and call save().
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
        	cbtUsername = formData.getString("username");
        	cbtApikey = formData.getString("apikey");
            save();
            return super.configure(req,formData);            
        }
    }
}

