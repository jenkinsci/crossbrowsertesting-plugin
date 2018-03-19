package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.Screenshots;
import hudson.Extension;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.logging.Logger;

public class CBTScreenshotsStep extends AbstractCBTStep {
    private transient final static Logger log = Logger.getLogger(CBTScreenshotsStep.class.getName());
    public String browserList, loginProfile,  url = "";

    @DataBoundConstructor
    public CBTScreenshotsStep(String browserList, String loginProfile, String url) {
        this.browserList = browserList;
        this.loginProfile = loginProfile;
        this.url = url;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new CBTScreenshotsStepExecution(this, context);
    }

    public static class CBTScreenshotsStepExecution extends AbstractCBTStepExecution {
        private transient final static Logger log = Logger.getLogger(CBTScreenshotsStepExecution.class.getName());
        private transient CBTScreenshotsStep screenshotsStep;

        public CBTScreenshotsStepExecution(CBTScreenshotsStep step, StepContext context) throws Exception {
            super(step, context);
            screenshotsStep = step;
            if (screenshotsStep.getDescriptor().screenshotApi == null) {
                screenshotsStep.getDescriptor().screenshotApi = new Screenshots(getUsername(), getAuthkey());
            }
        }

        @Override
        public boolean start() throws Exception {
            if (screenshotsStep.getDescriptor().screenshotApi == null) {
                screenshotsStep.getDescriptor().screenshotApi = new Screenshots(getUsername(), getAuthkey());
            }
            log.finest("username="+username);
            log.finest("authkey="+authkey);
            log.finest("browserList="+screenshotsStep.browserList);
            log.finest("loginProfile"+screenshotsStep.loginProfile);
            log.finest("url="+screenshotsStep.url);

            boolean useTestResult = getContext().get(Boolean.class);
            CBTScreenshotsStepExecutionThread screenshotTest = new CBTScreenshotsStepExecutionThread(useTestResult, screenshotsStep.getDescriptor().screenshotApi, screenshotsStep, this);
            screenshotTest.start();
            return false;
        }
    }
    @Override
    public CBTScreenshotsStepDescriptor getDescriptor() {
        return (CBTScreenshotsStepDescriptor) super.getDescriptor();
    }
    @Extension
    public static final class CBTScreenshotsStepDescriptor extends AbstractCBTStepDescriptor {
        private transient final static Logger log = Logger.getLogger(CBTScreenshotsStepDescriptor.class.getName());
        Screenshots screenshotApi = null;
        private static String username, authkey = "";
        CBTCredentials credentials;

        @Override
        public String getFunctionName() {
            return functionNamePrefix()+"ScreenshotsTest";
        }
        @Override
        public String getDisplayName() {
            return "Run a CrossbrowserTesting.com Screenshots Test";
        }

        private void checkCredentials(String credentialsId) {
            CBTCredentials local_credentials = CBTCredentials.getCredentialsById(null, credentialsId);
            if (local_credentials != null) {
                credentials = local_credentials;
                username = local_credentials.getUsername();
                authkey = local_credentials.getAuthkey();
            }
            if (screenshotApi == null) {
                screenshotApi = new Screenshots(username, authkey);
                checkProxySettingsAndReloadRequest(screenshotApi);
            }
        }
        public ListBoxModel doFillBrowserListItems(@QueryParameter("credentialsId") final String credentialsId) {
            checkCredentials(credentialsId);
            ListBoxModel items = new ListBoxModel();
            try {
                if (screenshotApi == null) {
                    screenshotApi = new Screenshots(username, authkey);
                    checkProxySettingsAndReloadRequest(screenshotApi);
                }
                items.add("**SELECT A BROWSERLIST**", "");

                for (int i=0 ; i<screenshotApi.browserLists.size() ; i++) {
                    String browserList = screenshotApi.browserLists.get(i);
                    items.add(browserList);
                }
            } catch(NullPointerException npe) {}
            return items;
        }
        public ListBoxModel doFillLoginProfileItems(@QueryParameter("credentialsId") final String credentialsId) {
            checkCredentials(credentialsId);
            CBTCredentials local_credentials = CBTCredentials.getCredentialsById(null, credentialsId);
            if (local_credentials != null) {
                credentials = local_credentials;
                username = local_credentials.getUsername();
                authkey = local_credentials.getAuthkey();
            }
            ListBoxModel items = new ListBoxModel();
            try {
                if (screenshotApi == null) {
                    screenshotApi = new Screenshots(username, authkey);
                    checkProxySettingsAndReloadRequest(screenshotApi);
                }

                items.add("**SELECT A LOGIN PROFILE / SELENIUM SCRIPT**", "");

                for (int i=0 ; i<screenshotApi.loginProfiles.size() ; i++) {
                    String loginProfile = screenshotApi.loginProfiles.get(i);
                    items.add(loginProfile);
                }
            } catch(NullPointerException npe) {}
            return items;
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return false;
        }
    }
}
