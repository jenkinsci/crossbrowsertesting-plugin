package org.jenkinsci.plugins.cbt_jenkins;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.crossbrowsertesting.api.Account;
import com.crossbrowsertesting.api.ApiFactory;
import com.crossbrowsertesting.api.Screenshots;
import com.crossbrowsertesting.api.Selenium;
import com.crossbrowsertesting.configurations.Browser;
import com.crossbrowsertesting.configurations.OperatingSystem;
import com.crossbrowsertesting.configurations.Resolution;
import com.crossbrowsertesting.plugin.Constants;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.ItemGroup;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

@Extension
public final class CBTDescriptor extends BuildWrapperDescriptor {
    private String  globalUsername,
                    globalAuthkey,
                    buildUsername,
                    buildAuthkey = "";
    Screenshots screenshotApi;
    Selenium seleniumApi = new Selenium();

    private final static Logger log = Logger.getLogger(CBTDescriptor.class.getName());
    
    public CBTDescriptor() throws IOException {
        /*
         * instantiated when the plugin is installed
         */
        super(CBTBuildWrapper.class);
        try {
            load();
        } catch (Exception ex) {
            log.finest("Unable to load XML file");
        }
    }
    
    public String getUsername() {
        if (buildUsername != null && buildAuthkey != null && !buildUsername.isEmpty() && !buildAuthkey.isEmpty()) {
            log.fine("using build username = "+buildUsername);
            return buildUsername;
        } else {
            log.fine("using global username");
            return globalUsername;
        }
    }
    public String getAuthkey() {
        if (buildUsername != null && buildAuthkey != null && !buildUsername.isEmpty() && !buildAuthkey.isEmpty()) {
            return buildAuthkey;
        } else {
            return globalAuthkey;
        }
    }
    public void setBuildCredentials(String username, String authkey) {
        buildUsername = username;
        buildAuthkey = authkey;
    }
    
    public String getDisplayName() {
        /*
         * This human readable name is used in the configuration screen.
         */
        return Constants.DISPLAYNAME;
    }
    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
        return true;
    }

    @Deprecated
    public String getVersionOld() {
        log.entering(this.getClass().getName(), "getVersion");
        /*
         * Get the version of plugin
         */
        String fullVersion = getPlugin().getVersion();
        log.finest("fullVersion: "+fullVersion);
        String stuffToIgnore = fullVersion.split("^\\d+[\\.]?\\d*")[1];
        log.finest("stuffToIgnore: "+stuffToIgnore);
        String pluginVersion = fullVersion.substring(0, fullVersion.indexOf(stuffToIgnore));
        log.finest("pluginVersion: "+pluginVersion);
        log.exiting(this.getClass().getName(), "getVersion");
        return pluginVersion;
    }

    public String getVersion() {
        /*
         * Get the version of plugin
         */
        log.entering(this.getClass().getName(), "getVersion");
        String pluginVersion = getPlugin().getVersion();
        log.finest("pluginVersion: "+pluginVersion);
        log.exiting(this.getClass().getName(), "getVersion");
        return pluginVersion;
    }
    public void checkProxySettingsAndReloadRequest(ApiFactory af) {
        // gets the proxy settings and reloads the Api Requests with them
        Jenkins jenkins = Jenkins.getInstance();
        try {
            String hostname = jenkins.proxy.name;
            int port = jenkins.proxy.port; // why is this throwing a null pointer if not set???
            try { // we'll do these too, just in case it throws a NPE too
                String proxyUsername = jenkins.proxy.getUserName();
                String proxyPassword = jenkins.proxy.getPassword();
                if (proxyUsername != null && proxyPassword != null && !proxyUsername.isEmpty() && !proxyPassword.isEmpty()) {
                    af.getRequest().setProxyCredentials(proxyUsername, proxyPassword);
                }
            } catch(NullPointerException npe) {
                //System.out.println("no proxy credentials were set");
            } // no proxy credentials were set
            af.getRequest().setProxy(hostname, port);
            af.init();
        } catch(NullPointerException npe) {
            //System.out.println("dont need to use a proxy");
        } // dont need to use a proxy   
    }
    public ListBoxModel doFillOperating_systemItems() {
        checkProxySettingsAndReloadRequest(seleniumApi);
        
        ListBoxModel items = new ListBoxModel();
        items.add("**SELECT AN OPERATING SYSTEM**", "");
        try {
            for (int i=0 ; i<seleniumApi.operatingSystems.size() ; i++) {
                OperatingSystem config = seleniumApi.operatingSystems.get(i);
                items.add(config.getName(), config.getApiName());
            }
        } catch(NullPointerException npe) {}
        return items;
    }
    public ListBoxModel doFillBrowserItems(@QueryParameter String operating_system) {
        ListBoxModel items = new ListBoxModel();
        if (operating_system.isEmpty()) {
            items.add("**SELECT A BROWSER**", "");
        }
        try {
            OperatingSystem config = seleniumApi.operatingSystems2.get(operating_system);
            for (int i=0 ; i<config.browsers.size() ; i++) {
                Browser configBrowser = config.browsers.get(i);
                items.add(configBrowser.getName(), configBrowser.getApiName());
            }
        } catch(NullPointerException npe) {}

        return items;
    }
    public ListBoxModel doFillResolutionItems(@QueryParameter String operating_system) {
        ListBoxModel items = new ListBoxModel();
        try {
            if (operating_system.isEmpty()) {
                items.add("**SELECT A RESOLUTION**", "");
            }

            OperatingSystem config = seleniumApi.operatingSystems2.get(operating_system);

            for (int i=0 ; i<config.resolutions.size() ; i++) {
                Resolution configResolution = config.resolutions.get(i);
                items.add(configResolution.getName());
            }
        } catch(NullPointerException npe) {}
        return items;
    }
    public ListBoxModel doFillBrowserListItems() {
        ListBoxModel items = new ListBoxModel();

        if (getUsername() == null || getUsername().equals("")) {
            items.add("**Please add Username/Authkey**", "");
            return items;
        }
        screenshotApi = new Screenshots(getUsername(), getAuthkey());
        checkProxySettingsAndReloadRequest(screenshotApi);

        try {
            if (screenshotApi == null && getUsername() != null) {
                screenshotApi = new Screenshots(getUsername(), getAuthkey());
                checkProxySettingsAndReloadRequest(screenshotApi);
                items.add("**SELECT A BROWSERLIST**", "");
            }
            for (int i=0 ; i<screenshotApi.browserLists.size() ; i++) {
                String browserList = screenshotApi.browserLists.get(i);
                items.add(browserList);
            }
        } catch(NullPointerException npe) {

            items.add("*** Please add Username/Authkey ***");
        }
        return items;
    }
    public ListBoxModel doFillLoginProfileItems() {
        ListBoxModel items = new ListBoxModel();

        if (getUsername() == null || getUsername().equals("")) {
            items.add("**Please add Username/Authkey**", "");
            return items;
        }

        screenshotApi = new Screenshots(getUsername(), getAuthkey());
        checkProxySettingsAndReloadRequest(screenshotApi);
        try {
            if (screenshotApi == null && getUsername() != null) {
                screenshotApi = new Screenshots(getUsername(), getAuthkey());
                checkProxySettingsAndReloadRequest(screenshotApi);
            }
            items.add("**SELECT A LOGIN PROFILE / SELENIUM SCRIPT**", "");
            
            for (int i=0 ; i<screenshotApi.loginProfiles.size() ; i++) {
                String loginProfile = screenshotApi.loginProfiles.get(i);
                items.add(loginProfile);
            }
        } catch(NullPointerException npe) {
            items.add("**Please add Username/Authkey**", "");

        }
        return items;
    }
    public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath ItemGroup<?> context) {
        return new StandardUsernameListBoxModel().withAll(CBTCredentials.all(context));
    }
    public FormValidation doTestConnection(@QueryParameter("username") final String username, @QueryParameter("authkey") final String authkey) throws IOException, ServletException {
        Account account = new Account(username, authkey);
        account.init();
        if (account.connectionSuccessful) {
            account.sendMixpanelEvent("Jenkins Plugin Downloaded"); //track install
            return FormValidation.ok(Constants.AUTH_SUCCESS);
        } else {
            return FormValidation.error(Constants.AUTH_FAIL);
        }
    }
}
