package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.ApiFactory;
import com.crossbrowsertesting.api.Selenium;
import com.crossbrowsertesting.configurations.Browser;
import com.crossbrowsertesting.configurations.OperatingSystem;
import com.crossbrowsertesting.configurations.Resolution;
import com.crossbrowsertesting.plugin.Constants;
import com.google.inject.Inject;
import hudson.EnvVars;
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
import org.jenkinsci.plugins.cbt_jenkins.SeleniumBuildAction;
import org.jenkinsci.plugins.workflow.steps.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

public class CBTSeleniumStep extends AbstractStepImpl {
    private transient final static Logger log = Logger.getLogger(CBTSeleniumStep.class.getName());

    private static String username, authkey = "";
    private static CBTCredentials credentials;
    private static Selenium seleniumApi = new Selenium();

    private String credentialsId = "";
    public String operatingSystem, browser, resolution = "";

    @DataBoundConstructor
    public CBTSeleniumStep(String operatingSystem, String browser, String resolution) {
        this.operatingSystem = operatingSystem;
        this.browser = browser;
        this.resolution = resolution;
    }
    public void setCredentialsId(String credentialsId) {
        if (credentialsId != null && !credentialsId.isEmpty()) {
            this.credentialsId = credentialsId;
        }
    }
    public String getCredentialsId() {
        return this.credentialsId;
    }

    public static class CBTSeleniumStepExecution extends StepExecution {
        private transient final static Logger log = Logger.getLogger(CBTSeleniumStepExecution.class.getName());
        @StepContextParameter
        private transient Run<?,?> run;
        @StepContextParameter private transient TaskListener listener;
        @Inject(optional=true)
        private transient CBTSeleniumStep step;
        private BodyExecution body;
        private transient Selenium seleniumApi = new Selenium();
        private String username, authkey = "";

        public class CBTSeleniumEnvironmentVariables extends EnvironmentExpander {
            private transient final Logger log = Logger.getLogger(CBTSeleniumStepExecution.class.getName());

            private final Map<String, String> envVars;
            public CBTSeleniumEnvironmentVariables(Map<String, String> envVars) {
                this.envVars = envVars;
            }

            @Override
            public void expand(@Nonnull EnvVars env) throws IOException, InterruptedException {
                env.overrideAll(this.envVars);
            }
        }
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
            seleniumApi.setRequest(username, authkey);
            String browserIconClass = seleniumApi.operatingSystems2.get(step.operatingSystem).browsers2.get(step.browser).getIconClass();
            String browserName = "";
            if (browserIconClass.equals("ie")) {
                browserName = "internet explorer";
            } else if (browserIconClass.equals("safari-mobile")) {
                browserName = "safari";
            } else {
                browserName = browserIconClass;
            }
            // set environment variables
            Map<String, String> env = new HashMap<String, String>();
            JSONArray browsers = new JSONArray();
            JSONObject browser = new JSONObject();
            browsers.put(browser);
            browser.put("operating_system", step.operatingSystem);
            browser.put("browser", step.browser);
            browser.put("resolution", step.resolution);
            browser.put("browserName", browserName);
            log.finest("browser = "+browser.toString());
            log.finest("browsers = "+browsers.toString());
            env.put(Constants.OPERATINGSYSTEM, step.operatingSystem);
            env.put(Constants.BROWSER, step.browser);
            env.put(Constants.RESOLUTION, step.resolution);
            env.put(Constants.BROWSERNAME, browserName);
            env.put(Constants.BROWSERS, browsers.toString());
            env.put(Constants.USERNAME, username);
            env.put(Constants.APIKEY, authkey); // for legacy
            env.put(Constants.AUTHKEY, authkey);
            String buildname = run.getFullDisplayName().substring(0, run.getFullDisplayName().length()-(String.valueOf(run.getNumber()).length()+1));
            env.put(Constants.BUILDNAME, buildname);
            String buildnumber = String.valueOf(run.getNumber());
            env.put(Constants.BUILDNUMBER, buildnumber);
            body = getContext().newBodyInvoker()
                    .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class), new CBTSeleniumEnvironmentVariables(env)))
                    .withCallback(new CBTSeleniumStepTailCall(buildname, buildnumber))
                    .start();
            return false;
        }
        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            if (body!=null) {
                body.cancel(cause);
            }
        }
        public class CBTSeleniumStepTailCall extends BodyExecutionCallback.TailCall {
            private transient final Logger log = Logger.getLogger(CBTSeleniumStepTailCall.class.getName());

            String buildname,
                buildnumber = "";

            public CBTSeleniumStepTailCall(String buildname, String buildnumber) {
                this.buildname = buildname;
                this.buildnumber = buildnumber;
            }

            @Override
            protected void finished(StepContext context) throws Exception {
                Queue<Map<String, String>> tests = seleniumApi.getSeleniumTestInfo2(buildname, buildnumber, step.browser, step.operatingSystem, step.resolution);
                Map<String, String> test = tests.poll();
                boolean useTestResults = context.get(Boolean.class);
                SeleniumBuildAction sba = new SeleniumBuildAction(useTestResults, step.operatingSystem, step.browser, step.resolution);
                sba.setTestId(test.get("selenium_test_id"));
                sba.setTestUrl(test.get("selenium_test_id"));
                sba.setTestPublicUrl(test.get("show_result_public_url"));
                sba.setBuildName(buildname);
                sba.setBuildNumber(buildnumber);
                run.addAction(sba);
            }
        }
    }

    @Extension
    public static final class CBTSeleniumStepDescriptor extends AbstractStepDescriptorImpl {
        private transient final static Logger log = Logger.getLogger(CBTSeleniumStepDescriptor.class.getName());

        public CBTSeleniumStepDescriptor() {
            super(CBTSeleniumStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "cbtSeleniumTest";
        }
        @Override
        public String getDisplayName() {
            return "Run a CrossbrowserTesting.com Selenium Test";
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
                } catch(NullPointerException npe) {} // no proxy credentials were set
                af.getRequest().setProxy(hostname, port);
                af.init();
            } catch(NullPointerException npe) {} // dont need to use a proxy
        }

        public ListBoxModel doFillOperatingSystemItems() {
            checkProxySettingsAndReloadRequest(seleniumApi);
            ListBoxModel items = new ListBoxModel();
            try {
                for (int i=0 ; i<seleniumApi.operatingSystems.size() ; i++) {
                    OperatingSystem config = seleniumApi.operatingSystems.get(i);
                    items.add(config.getName(), config.getApiName());
                }
            } catch(NullPointerException npe) {}
            return items;
        }
        public ListBoxModel doFillBrowserItems(@QueryParameter("operatingSystem") final String operating_system) {
            ListBoxModel items = new ListBoxModel();
            try {
                OperatingSystem config = seleniumApi.operatingSystems2.get(operating_system);
                for (int i=0 ; i<config.browsers.size() ; i++) {
                    Browser configBrowser = config.browsers.get(i);
                    items.add(configBrowser.getName(), configBrowser.getApiName());
                }
            } catch(NullPointerException npe) {}
            return items;
        }
        public ListBoxModel doFillResolutionItems(@QueryParameter("operatingSystem") final String operating_system) {
            ListBoxModel items = new ListBoxModel();
            try {
                OperatingSystem config = seleniumApi.operatingSystems2.get(operating_system);
                for (int i=0 ; i<config.resolutions.size() ; i++) {
                    Resolution configResolution = config.resolutions.get(i);
                    items.add(configResolution.getName());
                }
            } catch(NullPointerException npe) {}
            return items;
        }
        public ListBoxModel doFillnullItems() { //  catch for null values
            return new ListBoxModel();
        }
        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath ItemGroup<?> context) {
            return CBTCredentials.fillCredentialsIdItems(context);
        }
        public FormValidation doTestConnection(@QueryParameter("username") final String username, @QueryParameter("authkey") final String authkey) throws IOException, ServletException {
            return CBTCredentials.testCredentials(username, authkey);
        }
        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }
    }
}
