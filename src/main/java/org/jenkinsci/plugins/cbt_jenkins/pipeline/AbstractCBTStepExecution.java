package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.google.inject.Inject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import javax.annotation.Nonnull;
import java.util.logging.Logger;

public abstract class AbstractCBTStepExecution extends StepExecution{
    private transient final static Logger log = Logger.getLogger(AbstractCBTStepExecution.class.getName());
    @StepContextParameter transient Run<?,?> run;
    @StepContextParameter transient TaskListener listener;
    @StepContextParameter private transient CBTCredentials credentials;
    transient Job<?, ?> job;
    transient String username, authkey = "";
    @Inject(optional=true) transient AbstractCBTStep step;
    BodyExecution body;

    public AbstractCBTStepExecution(AbstractCBTStep step, StepContext context) throws Exception {
        super(context);
        this.step = step;
        run = context.get(Run.class);
        listener = context.get(TaskListener.class);
        if (!(step instanceof CBTStep)) {
            log.finest("Step is not a CBTStep. It is a "+step.getClass().getName());
            credentials = context.get(CBTCredentials.class);
        }else {
            log.finest("Step is a CBTStep");
        }
        setUsernameAndAuthkey();
    }
    void setCredentials() throws Exception{
        job = run.getParent();
        credentials = CBTCredentials.getCredentialsById(job, step.getCredentialsId());
        if (credentials == null) {
            throw new Exception("no credentials provided");
        } else {
            username = credentials.getUsername();
            authkey = credentials.getAuthkey();
        }
    }
    void setUsernameAndAuthkey() throws Exception{
        job = run.getParent();
        if (credentials == null) {
            credentials = CBTCredentials.getCredentialsById(job, step.getCredentialsId());
            if (credentials == null) {
                throw new Exception("no credentials provided");
            } else {
                //step.setCredentialsId(credentials.getId());
                username = credentials.getUsername();
                authkey = credentials.getAuthkey();
            }
        } else {
            //step.setCredentialsId(credentials.getId());
            username = credentials.getUsername();
            authkey = credentials.getAuthkey();
        }
    }

    @Override public void stop(@Nonnull Throwable cause) throws Exception {
        if (body!=null) {
            body.cancel(cause);
        }
    }
}
