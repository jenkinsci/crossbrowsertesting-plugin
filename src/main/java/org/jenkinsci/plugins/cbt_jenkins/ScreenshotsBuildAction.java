package org.jenkinsci.plugins.cbt_jenkins;

import java.util.HashMap;

public class ScreenshotsBuildAction  extends AbstractBuildAction {
	/*
	 * Holds info about the Screenshots Test
	 */
	private String browserList,
				   url,
				   versionId,
				   downloadResultsZipPublicUrl;
	
	private HashMap<String, String> testinfo;
	
	ScreenshotsBuildAction(final boolean showLink, final String browserList, final String url) {
		super("Screenshots");
		this.showLink = showLink;
    	this.browserList = browserList;
    	this.url = url;
    }
	public String getBrowserList() {
		return browserList;
	}
	public String getUrl() {
		return url;
	}
	public String getVersionId() {
		return versionId;
	}
	public String getDownloadResultsZipPublicUrl() {
		return downloadResultsZipPublicUrl;
	}
	public HashMap<String, String> getTestinfo() {
		return testinfo;
	}
	public void setTestinfo(HashMap<String, String> info) {
		this.testinfo = info;
		String testid = info.get("screenshot_test_id");
		setTestId(testid);
		setTestPublicUrl(info.get("show_results_public_url"));
    	if (this.showLink) {
    		setDisplayName("CBT Screenshots Test (" + info.get("browser_list") + " " + info.get("url") + ")");
    		this.iconFileName = "/plugin/crossbrowsertesting/img/cbtlogo.png";
    		setTestUrl(testid); // using the test id as the results url because it is unique
    	}
	}
}
