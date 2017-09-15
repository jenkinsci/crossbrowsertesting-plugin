package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

public abstract class AbstractCBTStep extends Step {
    public String credentialsId = "";
    public StepExecution execution = null;
    public  AbstractCBTStep(String credentialsId) {
        this.credentialsId = credentialsId;
    }
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }
    public String getCredentialsId() {
        return this.credentialsId;
    }
    public void setStepExecution(StepExecution se) {
        this.execution = se;
    }
    @Override
    public StepExecution start(StepContext context) throws Exception {
        return execution;
    }

}
