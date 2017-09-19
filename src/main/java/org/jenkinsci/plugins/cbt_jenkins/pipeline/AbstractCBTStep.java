package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import java.util.logging.Logger;

public abstract class AbstractCBTStep extends Step {
    private transient final static Logger log = Logger.getLogger(AbstractCBTStep.class.getName());
    public static String credentialsId = "";
    public static CBTCredentials credentials;
    public static String username, authkey = "";

    public AbstractCBTStep(){}
    public AbstractCBTStep(String credentialsId) {
        this.credentialsId = credentialsId;
        credentials = CBTCredentials.getCredentialsById(null, credentialsId);
    }
    public String getCredentialsId() {
        if (credentialsId == null || credentialsId.isEmpty()) {
            return credentials.getId();
        } else {
            return this.credentialsId;
        }
    }
    public static CBTCredentials getCredentials() {
        if (credentials == null) {
            return CBTCredentials.getCredentialsById(null, credentialsId);
        }else {
            return credentials;
        }
    }
    public static String getUsername() {
        if (username == null || username.isEmpty()) {
            return getCredentials().getUsername();
        }else {
            return username;
        }
    }
    public static String getAuthkey() {
        if (authkey == null || authkey.isEmpty()) {
            return getCredentials().getAuthkey();
        } else {
            return authkey;
        }
    }
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }
    @Override public AbstractCBTStepDescriptor getDescriptor() {
        return (AbstractCBTStepDescriptor) super.getDescriptor();
    }
}
