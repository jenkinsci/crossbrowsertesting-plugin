package org.jenkinsci.plugins.cbt_jenkins;

import java.util.HashMap;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;

public class CBTJenkinsBuildAction implements Action {
	private AbstractProject<?, ?> project;
    private AbstractBuild<?, ?> build;
    private boolean isRunning = false;

    
    private String displayName = null;
    private String testUrl = null;
    private String iconFileName = null;
    private String testPublicUrl;
    private String testid;
    private String testtype;    
    public EnvVars environmentVariables;
    
    

    @Override
    public String getIconFileName() {
    	return iconFileName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
    @Override
    public String getUrlName() {
        return testUrl;
    }
    public String getTestPublicUrl() {
    	return testPublicUrl;
    }
    public String getTestType() {
    	return testtype;
    }
    public String getTestId() {
    	return testid;
    }
    public int getBuildNumber() {
        return this.build.number;
    }
    public AbstractBuild<?, ?> getBuild() {
        return build;
    }
   
    public void setTestId(String id) {
    	this.testid = id;
    }
    public void setDisplayName(String dn) {
    	 // the displayname does not wrap after 46 characters
    	 // so it will bleed into the view
    	int maxCharactersViewable = 46;
    	if (dn.length() > maxCharactersViewable - 3) {
    		// going to cut the string down and add "..."
    		dn = dn.substring(0, maxCharactersViewable - 3);
    		dn += "...";
    	}
    	displayName = dn;
    }
    public void setTestPublicUrl(String url) {
    	this.testPublicUrl = url;
    }
    public void setTestUrl(String testUrl) {
    	//make it a little more url safe
    	this.testUrl = testUrl.replaceAll("[:.()|/ ]", "").toLowerCase();
    }
    // For Selenium
    CBTJenkinsBuildAction(final String testtype, final EnvVars env, final boolean showLink, final AbstractBuild<?, ?> build) {
        this.testtype = testtype;
        this.environmentVariables = env;
        if (showLink) {
        	setDisplayName("CBT Selenium Test (" + env.get("CBT_OPERATING_SYSTEM") + " " + env.get("CBT_BROWSER") + " " + env.get("CBT_RESOLUTION") + ")");
        	this.iconFileName = "/plugin/crossbrowsertesting/img/cbtlogo.png";
        	setTestUrl(displayName);
        }
        this.build = build;
    }
     // For Screenshots
    CBTJenkinsBuildAction(final String testtype, HashMap<String, String> ssInfo, final boolean showLink, final AbstractBuild<?, ?> build) {
    	this.testtype = testtype;
    	setTestId(ssInfo.get("screenshot_test_id"));
    	this.testPublicUrl = ssInfo.get("show_results_public_url");
    	if (showLink) {
    		setDisplayName("CBT Screenshots Test (" + ssInfo.get("browser_list") + " " + ssInfo.get("url") + ")");
    		this.iconFileName = "/plugin/crossbrowsertesting/img/cbtlogo.png";
    		setTestUrl(this.testid);
    	}
    	this.build = build;
    }


}
