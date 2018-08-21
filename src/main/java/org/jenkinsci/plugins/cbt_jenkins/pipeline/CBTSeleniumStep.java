package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.Selenium;
import com.crossbrowsertesting.configurations.Browser;
import com.crossbrowsertesting.configurations.OperatingSystem;
import com.crossbrowsertesting.configurations.Resolution;
import com.crossbrowsertesting.plugin.Constants;
import hudson.EnvVars;
import hudson.Extension;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.cbt_jenkins.SeleniumBuildAction;
import org.jenkinsci.plugins.workflow.steps.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

public class CBTSeleniumStep extends AbstractCBTStep {
    private transient final static Logger log = Logger.getLogger(CBTSeleniumStep.class.getName());
    private static Selenium seleniumApi = new Selenium();
    public String operatingSystem, browser, resolution = "";

    @DataBoundConstructor
    public CBTSeleniumStep(String operatingSystem, String browser, String resolution) {
        this.operatingSystem = operatingSystem;
        this.browser = browser;
        this.resolution = resolution;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new CBTSeleniumStepExecution(this, context);
    }

    public static class CBTSeleniumStepExecution extends AbstractCBTStepExecution {
        private transient final static Logger log = Logger.getLogger(CBTSeleniumStepExecution.class.getName());
        private transient CBTSeleniumStep seleniumStep;

        public CBTSeleniumStepExecution(CBTSeleniumStep step, StepContext context) throws Exception {
            super(step, context);
            this.seleniumStep = step;
        }

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
            seleniumApi.setRequest(getUsername(), getAuthkey());
            String browserIconClass = seleniumApi.operatingSystems2.get(seleniumStep.operatingSystem).browsers2.get(seleniumStep.browser).getIconClass();
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
            JSONArray browsersJSON = new JSONArray();
            JSONObject browserJSON = new JSONObject();
            browserJSON.put("operating_system", seleniumStep.operatingSystem);
            browserJSON.put("browser", seleniumStep.browser);
            browserJSON.put("resolution", seleniumStep.resolution);
            browserJSON.put("browserName", browserName);
            browsersJSON.put(browserJSON);
            log.finest("browser = "+browserJSON.toString());
            log.finest("browsers = "+browsersJSON.toString());
            env.put(Constants.OPERATINGSYSTEM, seleniumStep.operatingSystem);
            env.put(Constants.BROWSER, seleniumStep.browser);
            env.put(Constants.RESOLUTION, seleniumStep.resolution);
            env.put(Constants.BROWSERNAME, browserName);
            env.put(Constants.BROWSERS, browsersJSON.toString());
            env.put(Constants.USERNAME, getUsername());
            env.put(Constants.APIKEY, getAuthkey()); // for legacy
            env.put(Constants.AUTHKEY, getAuthkey());
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
                Queue<Map<String, String>> tests = seleniumApi.getSeleniumTestInfo2(buildname, buildnumber, seleniumStep.browser, seleniumStep.operatingSystem, seleniumStep.resolution);
                Map<String, String> test = tests.poll();
                if(test == null) {
                    // Unable to find the test based on normal caps
                    // Look for jenkins caps to be set
					log.warning("Unable to find test launched with Jenkins. Checking for 'jenkins_build' and 'jenkins_name' capabilities.");
                    test = seleniumApi.getSeleniumTestInfoWithJenkinsCaps(buildname, buildnumber, seleniumStep.browser, seleniumStep.operatingSystem, seleniumStep.resolution);
                    if(test == null) {
                        // User is hard-coding BuildName and BuildNumber, but not setting jenkinsName and jenkinsBuild in caps
                        String msg = "Unable to find test launched with Jenkins. "+
                                    "Are you using the Jenkins environment variables for the 'build' and 'name' caps? "+
                                    "If not, you should pass 'jenkins_build' and 'jenkins_name' caps using the jenkins environment variables."+
                                    "Check out the examples directory to see this in action.";
                        log.severe(msg);
                        throw new Error(msg);
                    }
                }

                String testId = test.get("selenium_test_id");
                String publicLink = test.get("show_result_public_url");

                if(testId == null || testId.isEmpty()) {
                    String errorMessage = "Unable to locate selenium test id and public results link.";
                    if(test.containsKey("error_message")) {
                        errorMessage += test.get("error_message");
                    }
                    log.warning(errorMessage);
                } else {
                    boolean useTestResults = context.get(Boolean.class);
                    SeleniumBuildAction sba = new SeleniumBuildAction(useTestResults, seleniumStep.operatingSystem, seleniumStep.browser, seleniumStep.resolution);
                    sba.setTestId(testId);
                    sba.setTestUrl(testId);
                    sba.setTestPublicUrl(publicLink);
                    sba.setBuildName(buildname);
                    sba.setBuildNumber(buildnumber);
                    run.addAction(sba);
                }
            }
        }
    }
    @Extension
    public static final class CBTSeleniumStepDescriptor extends AbstractCBTStepDescriptor {
        private transient final static Logger log = Logger.getLogger(CBTSeleniumStepDescriptor.class.getName());

        @Override
        public String getFunctionName() {
            return functionNamePrefix()+"SeleniumTest";
        }
        @Override
        public String getDisplayName() {
            return "Run a CrossbrowserTesting.com Selenium Test";
        }

        public ListBoxModel doFillOperatingSystemItems() {
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
        public ListBoxModel doFillBrowserItems(@QueryParameter("operatingSystem") final String operating_system) {
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
        public ListBoxModel doFillResolutionItems(@QueryParameter("operatingSystem") final String operating_system) {
            ListBoxModel items = new ListBoxModel();
            if (operating_system.isEmpty()) {
                items.add("**SELECT A RESOLUTION**", "");
            }
            try {
                OperatingSystem config = seleniumApi.operatingSystems2.get(operating_system);
                for (int i=0 ; i<config.resolutions.size() ; i++) {
                    Resolution configResolution = config.resolutions.get(i);
                    items.add(configResolution.getName());
                }
            } catch(NullPointerException npe) {}
            return items;
        }
    }
}
