package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.Screenshots;
import org.jenkinsci.plugins.cbt_jenkins.ScreenshotsBuildAction;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CBTScreenshotsStepExecutionThread extends Thread {
    /*
     * starts and monitors the screenshots test in a seperate thread
     */
    Screenshots screenshotsApi;
    CBTScreenshotsStep screenshotsStep;
    CBTScreenshotsStep.CBTScreenshotsStepExecution stepExecution;
    private final static Logger log = Logger.getLogger(CBTScreenshotsStepExecutionThread.class.getName());


    Boolean useTestResult = false;
    public CBTScreenshotsStepExecutionThread(Boolean useTestResult,
                                             Screenshots screenshotsApi,
                                             CBTScreenshotsStep step,
                                             CBTScreenshotsStep.CBTScreenshotsStepExecution stepExecution) {
        super("screenshotTest");
        this.useTestResult = useTestResult;
        this.screenshotsApi = screenshotsApi;
        this.screenshotsStep = step;
        this.stepExecution = stepExecution;
    }

    @Override
    public void run() {
        HashMap<String, String> screenshotTestResultsInfo = new HashMap<String, String>();
        boolean useLoginProfile = true;
        if (screenshotsStep.loginProfile == null || screenshotsStep.loginProfile.equals("**SELECT A LOGIN PROFILE / SELENIUM SCRIPT**") || screenshotsStep.loginProfile.isEmpty()) {
            useLoginProfile = false;
            screenshotsStep.loginProfile = "";
        }
        boolean screenshotsTestStarted = false;
        for (int i=1; i<=12 && !screenshotsTestStarted;i++) { // in windows it takes 4 -5 attempts before the screenshots test begins
            if (useLoginProfile) {
                screenshotTestResultsInfo = screenshotsApi.runScreenshotTest(screenshotsStep.browserList, screenshotsStep.url, screenshotsStep.loginProfile);
            } else {
                screenshotTestResultsInfo = screenshotsApi.runScreenshotTest(screenshotsStep.browserList, screenshotsStep.url);
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
            stepExecution.listener.getLogger().println("[ERROR] 500 error returned for Screenshot Test");
            stepExecution.getContext().onFailure(new Exception("[ERROR] 500 error returned for Screenshot Test"));
        } else {
            screenshotTestResultsInfo.put("browser_list", screenshotsStep.browserList);
            screenshotTestResultsInfo.put("url", screenshotsStep.url);
            ScreenshotsBuildAction ssBuildAction = new ScreenshotsBuildAction(useTestResult, screenshotsStep.browserList, screenshotsStep.url);
            ssBuildAction.setTestinfo(screenshotTestResultsInfo);
            ssBuildAction.setLoginProfile(screenshotsStep.loginProfile);
            stepExecution.run.addAction(ssBuildAction);
            if (!screenshotTestResultsInfo.isEmpty()) {
                stepExecution.listener.getLogger().println("\n-----------------------");
                stepExecution.listener.getLogger().println("SCREENSHOT TEST RESULTS");
                stepExecution.listener.getLogger().println("-----------------------");
            }
            for (Map.Entry<String, String> screenshotResultsEntry : screenshotTestResultsInfo.entrySet()) {
                stepExecution.listener.getLogger().println(screenshotResultsEntry.getKey() + ": "+ screenshotResultsEntry.getValue());
            }
            monitorScreenshotsTest(screenshotTestResultsInfo.get("screenshot_test_id"));
        }
    }
    private void monitorScreenshotsTest(String screenshotTestId) {
        log.finest("screenshots_test_id=" + screenshotTestId);
        try {
            int count = 1;
            stepExecution.listener.getLogger().println("waiting for screenshots test " + screenshotTestId + " to finish...");
            while (screenshotsApi.testIsRunning(screenshotTestId)) {
                try {
                    if (count == 1) {
                        stepExecution.listener.getLogger().println("screenshot test " + screenshotTestId + " is still running");
                    }
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count++;
            }
            stepExecution.getContext().onSuccess(screenshotTestId); // report a successful test
            stepExecution.listener.getLogger().println("screenshot test " + screenshotTestId + " finished");
        } catch (IOException e) {
            log.severe(e.toString());
            stepExecution.getContext().onFailure(e); // report a failed test
        }
    }
}

