package org.jenkinsci.plugins.cbt_jenkins;

import java.util.HashMap;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;

public class CBTJenkinsBuildAction implements Action {
	private AbstractProject<?, ?> project;
    private AbstractBuild<?, ?> build;

    private String testtype;
    private String displayName;
    private String testUrl;
    private String testPublicUrl;
    private String testid;
    
    public EnvVars environmentVariables;
    

    @Override
    public String getIconFileName() {
        return "/plugin/crossbrowsertesting/img/cbtlogo.png";
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
    public void setTestId(String id) {
    	this.testid = id;
    }
    public void setTestPublicUrl(String url) {
    	this.testPublicUrl = url;
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }
    public void setTestUrl(String testUrl) {
    	this.testUrl = testUrl.replaceAll("[:.()|/ ]", "").toLowerCase();
    }

    CBTJenkinsBuildAction(final String testtype, final EnvVars env, final AbstractBuild<?, ?> build) {
        this.testtype = testtype;
        this.environmentVariables = env;
    	this.displayName = "CBT Selenium Test (" + env.get("CBT_OPERATING_SYSTEM") + " " + env.get("CBT_BROWSER") + " " + env.get("CBT_RESOLUTION") + ")";
    	setTestUrl(displayName); //make it a little more url safe
        this.build = build;
    }
    CBTJenkinsBuildAction(final String testtype, HashMap<String, String> ssInfo, final AbstractBuild<?, ?> build) {
    	this.testtype = testtype;
    	setTestId(ssInfo.get("screenshot_test_id"));
    	this.testPublicUrl = ssInfo.get("show_results_public_url");
    	this.displayName = "CBT Screenshots Test (" + ssInfo.get("browser_list") + " " + ssInfo.get("url") + ")";
    	setTestUrl(displayName); //make it a little more url safe
    	this.build = build;
    }


}
