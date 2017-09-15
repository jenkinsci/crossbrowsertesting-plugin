package org.jenkinsci.plugins.cbt_jenkins;

public class SeleniumBuildAction extends AbstractBuildAction {
	/*
	 * Holds info about the Selenium Test
	 */
    private String operatingSystemApiName,
    			   browserApiName,
    			   resolution,
    			   buildName,
    			   buildNumber;

	public SeleniumBuildAction(final boolean showLink, final String os, final String browser, final String resolution) {
		super("Selenium");
        this.operatingSystemApiName = os;
        this.browserApiName = browser;
        this.resolution = resolution;
        if (showLink) {
        	setDisplayName("CBT Selenium Test (" + os + " " + browser + " " + resolution + ")");
        	this.iconFileName = "/plugin/crossbrowsertesting/img/cbtlogo.png";
        	setTestUrl(displayName);
        }
    }

	public String getOperatingSystem() {
		return operatingSystemApiName;
	}

	public String getBuildName() {
		return buildName;
	}
	public String getBuildNumber() {
		return buildNumber;
	}
	
	public void setOperatingSystem(String operatingSystemApiName) {
		this.operatingSystemApiName = operatingSystemApiName;
	}

	public String getBrowser() {
		return browserApiName;
	}

	public void setBrowser(String browserApiName) {
		this.browserApiName = browserApiName;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}
	public void setBuildName(String buildname) {
		this.buildName = buildname;
	}
	public void setBuildNumber(String buildnumber) {
		this.buildNumber = buildnumber;
	}
}
