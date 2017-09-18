package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.ApiFactory;
import com.crossbrowsertesting.api.Screenshots;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.cbt_jenkins.ScreenshotsBuildAction;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CBTScreenshotsStep extends AbstractStepImpl {
    private transient final static Logger log = Logger.getLogger(CBTScreenshotsStep.class.getName());

    private static String username, authkey = "";
    private static Screenshots screenshotApi = null;
    private static CBTCredentials credentials;

    private String credentialsId = "";
    public String browserList, loginProfile,  url = "";

    @DataBoundConstructor
    public CBTScreenshotsStep(String browserList, String loginProfile, String url) {
        this.browserList = browserList;
        this.loginProfile = loginProfile;
        this.url = url;

    }
    public void setCredentialsId(String credentialsId) {
        log.finest("screenshotsstep credentials = "+credentialsId);
        if (credentialsId != null && !credentialsId.isEmpty()) {
            this.credentialsId = credentialsId;
        }
    }
    public String getCredentialsId() {
        return this.credentialsId;
    }

    public static class CBTScreenshotsStepExecution extends StepExecution {
        private transient final static Logger log = Logger.getLogger(CBTScreenshotsStepExecution.class.getName());
        @StepContextParameter private transient Run<?,?> run;
        @StepContextParameter private transient TaskListener listener;

        @Inject(optional=true) private transient CBTScreenshotsStep step;
        private BodyExecution body;
        private transient Screenshots screenshotsApi;
        private transient String username, authkey = "";

        @Override
        public boolean start() throws Exception {
            Job<?, ?> job = run.getParent();
            CBTCredentials credentials = getContext().get(CBTCredentials.class);
            if (credentials == null) {
                credentials = CBTCredentials.getCredentialsById(job, step.getCredentialsId());
                if (credentials == null) {
                    throw new Exception("no credentials provided");
                } else {
                    step.setCredentialsId(credentials.getId());
                    username = credentials.getUsername();
                    authkey = credentials.getAuthkey();
                }
            } else {
                step.setCredentialsId(credentials.getId());
                username = credentials.getUsername();
                authkey = credentials.getAuthkey();
            }
            screenshotsApi = new Screenshots(username, authkey);
            log.finest("username="+username);
            log.finest("authkey="+authkey);
            log.finest("browserList="+step.browserList);
            log.finest("loginProfile"+step.loginProfile);
            log.finest("url="+step.url);

            boolean useTestResult = getContext().get(Boolean.class);
            ScreenshotsStepExecutionThread screenshotTest = new ScreenshotsStepExecutionThread(useTestResult);
            screenshotTest.start();
            return false;
        }
        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            log.entering(CBTScreenshotsStepExecution.class.getName(), "stop");
            if (body!=null) {
                body.cancel(cause);
            }
        }
        public class ScreenshotsStepExecutionThread extends Thread {
            /*
             * starts and monitors the screenshots test in a seperate thread
             */
            Boolean useTestResult = false;
            public ScreenshotsStepExecutionThread(Boolean useTestResult) {
                super("screenshotTest");
                this.useTestResult = useTestResult;
            }

            @Override
            public void run() {
                HashMap<String, String> screenshotTestResultsInfo = new HashMap<String, String>();
                boolean useLoginProfile = true;
                if (step.loginProfile == null || step.loginProfile.equals("**SELECT A LOGIN PROFILE / SELENIUM SCRIPT**") || step.loginProfile.isEmpty()) {
                    useLoginProfile = false;
                    step.loginProfile = "";
                }
                boolean screenshotsTestStarted = false;
                for (int i=1; i<=12 && !screenshotsTestStarted;i++) { // in windows it takes 4 -5 attempts before the screenshots test begins
                    if (useLoginProfile) {
                        screenshotTestResultsInfo = screenshotsApi.runScreenshotTest(step.browserList, step.url, step.loginProfile);
                    } else {
                        screenshotTestResultsInfo = screenshotsApi.runScreenshotTest(step.browserList, step.url);
                    }
                    if (screenshotTestResultsInfo.containsKey("screenshot_test_id") && screenshotTestResultsInfo.get("screenshot_test_id") != null) {
                        log.fine("screenshot test started: "+ screenshotTestResultsInfo.get("screenshot_test_id"));
                        screenshotsTestStarted = true;
                    } else {
                        log.fine("screenshot test did not start... going to try again: "+ i);
                        try {
                            TimeUnit.SECONDS.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (screenshotTestResultsInfo.containsKey("error")) {
                    listener.getLogger().println("[ERROR] 500 error returned for Screenshot Test");
                    CBTScreenshotsStepExecution.this.getContext().onFailure(new Exception("[ERROR] 500 error returned for Screenshot Test"));
                } else {
                    screenshotTestResultsInfo.put("browser_list", step.browserList);
                    screenshotTestResultsInfo.put("url", step.url);
                    ScreenshotsBuildAction ssBuildAction = new ScreenshotsBuildAction(useTestResult, step.browserList, step.url);
                    ssBuildAction.setTestinfo(screenshotTestResultsInfo);
                    ssBuildAction.setLoginProfile(step.loginProfile);
                    run.addAction(ssBuildAction);
                    if (!screenshotTestResultsInfo.isEmpty()) {
                        listener.getLogger().println("\n-----------------------");
                        listener.getLogger().println("SCREENSHOT TEST RESULTS");
                        listener.getLogger().println("-----------------------");
                    }
                    for (Map.Entry<String, String> screenshotResultsEntry : screenshotTestResultsInfo.entrySet()) {
                        listener.getLogger().println(screenshotResultsEntry.getKey() + ": "+ screenshotResultsEntry.getValue());
                    }
                    monitorScreenshotsTest(screenshotTestResultsInfo.get("screenshot_test_id"));
                }
            }
            private void monitorScreenshotsTest(String screenshotTestId) {
                log.finest("screenshots_test_id=" + screenshotTestId);
                try {
                    int count = 1;
                    listener.getLogger().println("waiting for screenshots test " + screenshotTestId + " to finish...");
                    while (screenshotsApi.testIsRunning(screenshotTestId)) {
                        try {
                            if (count == 1) {
                                listener.getLogger().println("screenshot test " + screenshotTestId + " is still running");
                            }
                            TimeUnit.SECONDS.sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        count++;
                    }
                    CBTScreenshotsStepExecution.this.getContext().onSuccess(screenshotTestId); // report a successful test
                    listener.getLogger().println("screenshot test " + screenshotTestId + " finished");
                } catch (IOException e) {
                    log.severe(e.toString());
                    CBTScreenshotsStepExecution.this.getContext().onFailure(e); // report a failed test
                }
            }
        }
    }

    @Extension
    public static final class CBTScreenshotsStepDescriptor extends AbstractStepDescriptorImpl {
        private transient final static Logger log = Logger.getLogger(CBTScreenshotsStepDescriptor.class.getName());

        public CBTScreenshotsStepDescriptor() {
            super(CBTScreenshotsStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "cbtScreenshotsTest";
        }
        @Override
        public String getDisplayName() {
            return "Run a CrossbrowserTesting.com Screenshots Test";
        }
        public ListBoxModel doFillnullItems() {
            return new ListBoxModel();
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
        public ListBoxModel doFillBrowserListItems(@QueryParameter("credentialsId") final String credentialsId) {
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
            ListBoxModel items = new ListBoxModel();
            items.add("**SELECT A BROWSERLIST**", "");
            try {
                for (int i=0 ; i<screenshotApi.browserLists.size() ; i++) {
                    String browserList = screenshotApi.browserLists.get(i);
                    items.add(browserList);
                }
            } catch(NullPointerException npe) {}
            return items;
        }
        public ListBoxModel doFillLoginProfileItems(@QueryParameter("credentialsId") final String credentialsId) {
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
            ListBoxModel items = new ListBoxModel();
            items.add("**SELECT A LOGIN PROFILE / SELENIUM SCRIPT**", "");
            try {
                for (int i=0 ; i<screenshotApi.loginProfiles.size() ; i++) {
                    String loginProfile = screenshotApi.loginProfiles.get(i);
                    items.add(loginProfile);
                }
            } catch(NullPointerException npe) {}
            return items;
        }
        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath ItemGroup<?> context) {
            return CBTCredentials.fillCredentialsIdItems(context);
        }
        public FormValidation doTestConnection(@QueryParameter("username") final String username, @QueryParameter("authkey") final String authkey) throws IOException, ServletException {
            return CBTCredentials.testCredentials(username, authkey);
        }
        @Override
        public boolean takesImplicitBlockArgument() {
            return false;
        }
    }
}
