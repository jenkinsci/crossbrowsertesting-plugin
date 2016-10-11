package org.jenkinsci.plugins.cbt_jenkins;

import hudson.model.AbstractBuild;
import hudson.model.Action;

public abstract class AbstractBuildAction implements Action {
	//private AbstractProject<?, ?> project;
    private AbstractBuild<?, ?> build;
    
    private boolean active = false;
    
    private String testUrl = null;
    
    protected String displayName,
    				 iconFileName = null;
    
    protected boolean showLink;
    
    private String testPublicUrl,
    			   testid,
    			   testType;
    
    public AbstractBuildAction(String testType) {
    	this.testType = testType;
    }
    
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
    public String getTestId() {
    	return testid;
    }
    public String getTestType() {
    	return testType;
    }
    public AbstractBuild<?, ?> getBuild() {
        return build;
    }
    public void setBuild(AbstractBuild<?, ?> build) {
    	this.build = build;
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


}
