package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.google.inject.Inject;
import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import javax.annotation.Nonnull;

public abstract class AbstractCBTStepExecution extends StepExecution {
    @StepContextParameter private transient Run<?,?> run;
    private BodyExecution body;
    private transient String username, authkey = "";
    @Inject(optional=true) private transient AbstractCBTStep step;
    private transient Job<?, ?> job;

    private void setUsernameAndAuthkey() throws Exception{
        job = run.getParent();
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
    }
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
        if (body!=null) {
            body.cancel(cause);
        }
    }

}
