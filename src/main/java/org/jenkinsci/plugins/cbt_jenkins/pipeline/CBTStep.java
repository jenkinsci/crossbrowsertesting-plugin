package org.jenkinsci.plugins.cbt_jenkins.pipeline;

import com.crossbrowsertesting.api.ApiFactory;
import com.crossbrowsertesting.api.LocalTunnel;
import com.crossbrowsertesting.plugin.Constants;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.cbt_jenkins.CBTBuildWrapper;
import org.jenkinsci.plugins.cbt_jenkins.CBTCredentials;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Logger;


public class CBTStep extends AbstractStepImpl {
    private transient final static Logger log = Logger.getLogger(CBTStep.class.getName());

    public boolean useLocalTunnel,
            useTestResults= false;
    public String credentialsId,
            localTunnelPath,
            tunnelName = "";

    @DataBoundConstructor
    public CBTStep(boolean useLocalTunnel, boolean useTestResults, String credentialsId, String tunnelName, String localTunnelPath) {
        this.credentialsId = credentialsId;
        setLocalTunnelPath(localTunnelPath);
        setTunnelName(tunnelName);
        setUseLocalTunnel(useLocalTunnel);
        setUseTestResults(useTestResults);
    }
    private void setLocalTunnelPath(String localTunnelPath) {
        if (localTunnelPath == null) {
            this.localTunnelPath = "";
        } else {
            this.localTunnelPath = localTunnelPath;
        }
    }
    private void setTunnelName(String tunnelName) {
        if (tunnelName == null) {
            this.tunnelName = "";
        } else {
            this.tunnelName = tunnelName;
        }
    }
    private void setUseLocalTunnel(boolean useLocalTunnel) {
        try { // prevent null pointer
            if (useLocalTunnel != true) {
                this.useLocalTunnel = false;
            } else {
                this.useLocalTunnel = useLocalTunnel;
            }
        } catch (NullPointerException npe) {
            this.useLocalTunnel = false;
        }
    }
    private void setUseTestResults(boolean useTestResults) {
        try { // prevent null pointer
            if (useTestResults != true) {
                this.useTestResults = false;
            } else {
                this.useTestResults = useTestResults;
            }
        } catch (NullPointerException npe) {
            this.useTestResults = false;
        }
    }

    public static class CBTStepExecution extends AbstractStepExecutionImpl {
        private transient final static Logger log = Logger.getLogger(CBTStepExecution.class.getName());
        @StepContextParameter private transient Run<?,?> run;
        @Inject(optional=true) private transient CBTStep step;
        @StepContextParameter private transient TaskListener listener;
        private BodyExecution body;

        private transient LocalTunnel tunnel = null;
        private String username, authkey = "";

        @Override
        public boolean start() throws Exception {
            Job<?,?> job = run.getParent();
            if (!(job instanceof TopLevelItem)) {
                throw new Exception(job + " must be a top-level job");
            }
            CBTCredentials credentials = CBTCredentials.getCredentialsById(job, step.credentialsId);

            if (credentials == null) {
                throw new Exception("no credentials provided");
            }else {
                username = credentials.getUsername();
                log.finest("username = "+username);
                authkey = credentials.getAuthkey();
                log.finest("authkey = "+authkey);
                if (step.useLocalTunnel) {
                    if (!step.tunnelName.isEmpty()) {
                        listener.getLogger().println(Constants.TUNNEL_USING_TUNNELNAME(step.tunnelName));
                        tunnel = new LocalTunnel(username, authkey, step.tunnelName);
                    }else if(step.tunnelName.isEmpty()){
                        listener.getLogger().println(Constants.TUNNEL_USING_DEFAULT);
                        tunnel = new LocalTunnel(username, authkey);
                    }
                    CBTStepDescriptor.checkProxySettingsAndReloadRequest(tunnel);
                    tunnel.queryTunnel();
                    if (!tunnel.isTunnelRunning) {
                        listener.getLogger().println(Constants.TUNNEL_NEED_TO_START);
                        try {
                            if (step.localTunnelPath != null && step.localTunnelPath.equals("")) {
                                log.fine("using embedded local tunnel");
                                tunnel.start(true);
                            } else {
                                log.fine("using specified local tunnel");
                                tunnel.start(step.localTunnelPath);
                            }
                            listener.getLogger().println(Constants.TUNNEL_WAITING);
                            for (int i=1 ; i<15 && !tunnel.isTunnelRunning ; i++) {
                                //will check every 2 seconds for upto 30 to see if the tunnel connected
                                Thread.sleep(4000);
                                tunnel.queryTunnel();
                            }
                            if (tunnel.isTunnelRunning) {
                                listener.getLogger().println(Constants.TUNNEL_CONNECTED);
                            }else {
                                throw new Error(Constants.TUNNEL_START_FAIL);
                            }
                        }catch (URISyntaxException | IOException e) {
                            log.finer("err: "+e);
                            throw new Error(Constants.TUNNEL_START_FAIL);
                        }
                    }else {
                        listener.getLogger().println(Constants.TUNNEL_NO_NEED_TO_START);
                    }
                }
            }
            body = getContext().newBodyInvoker()
                    .withContexts(credentials, step.useTestResults)
                    .withCallback(new CBTStepTailCall())
                    .start();
            return false;
        }
        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            if (body!=null) {
                body.cancel(cause);
            }
        }
        public class CBTStepTailCall extends BodyExecutionCallback.TailCall {
            private transient final Logger log = Logger.getLogger(CBTStepTailCall.class.getName());


            @Override
            protected void finished(StepContext context) throws Exception {
                if (tunnel != null && tunnel.pluginStartedTheTunnel) {
                    for (int i=1 ; i<20 && tunnel.isTunnelRunning; i++) {
                        try {
                            log.info("about to kill the tunnel using the api");
                            tunnel.stop();
                            log.info("done killing the tunnel using the api");
                        }catch(IOException ioe) {
                            // most likely got a bad gateway but we're going to delete the cbt_tunnel binary on exit anyway so the tunnel will still be killed
                            log.warning("got IOException while killing the tunnel");
                        }
                        //will check every 15 seconds for up to 5 minutes to see if the tunnel disconnected
                        log.fine("waiting for the tunnel to die");
                        Thread.sleep(15000);
                        boolean tunnelIsRunning = tunnel.queryTunnel();
                        log.fine("tunnelIsRunning: "+tunnelIsRunning);
                        log.fine("tunnel.isTunnelRunning: "+tunnel.isTunnelRunning);
                    }
                    if (!tunnel.isTunnelRunning) {
                        listener.getLogger().println(Constants.TUNNEL_STOP);
                    } else {
                        listener.getLogger().println(Constants.TUNNEL_STOP_FAIL);
                    }
                }
            }
        }
    }
    @Extension
    public static final class CBTStepDescriptor extends AbstractStepDescriptorImpl {
        private transient final static Logger log = Logger.getLogger(CBTStepDescriptor.class.getName());

        public CBTStepDescriptor() {
            super(CBTStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "cbt";
        }
        @Override
        public String getDisplayName() {
            return "CrossBrowserTesting.com";
        }
        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }
        public static void checkProxySettingsAndReloadRequest(ApiFactory af) {
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
        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath ItemGroup<?> context) {
            return CBTCredentials.fillCredentialsIdItems(context);
        }
        public FormValidation doTestConnection(@QueryParameter("username") final String username, @QueryParameter("authkey") final String authkey) throws IOException, ServletException {
            return CBTCredentials.testCredentials(username, authkey);
        }
    }
}
