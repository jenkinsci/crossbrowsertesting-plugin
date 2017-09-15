package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.ApiFactory;
import hudson.model.ItemGroup;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

public abstract class AbstractCBTStepDescriptor extends StepDescriptor {
    public String functionName,
        displayName = "";
    public AbstractCBTStepDescriptor(String functionName, String displayName) {
        this.functionName = functionName;
        this.displayName = displayName;
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
    @Override
    public String getFunctionName() {
        return this.functionName;
    }
    @Override
    public String getDisplayName() {
        return this.displayName;
    }
    public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath ItemGroup<?> context) {
        return CBTCredentials.fillCredentialsIdItems(context);
    }
    public FormValidation doTestConnection(@QueryParameter("username") final String username, @QueryParameter("authkey") final String authkey) throws IOException, ServletException {
        return CBTCredentials.testCredentials(username, authkey);
    }
}
