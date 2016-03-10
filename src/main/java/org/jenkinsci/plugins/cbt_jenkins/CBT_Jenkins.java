package org.jenkinsci.plugins.cbt_jenkins;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class CBT_Jenkins extends Builder implements Serializable, SimpleBuildStep {
	/**
	 * 
	 */
	private static String username;
	private static String apikey;
	private static BrowserList browserList;
    private final String browserApiName;
    private final String operatingSystemApiName;
    private final String resolution;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public CBT_Jenkins(String browser, String operatingSystem, String resolution) {
    	username = getDescriptor().getUsername();
    	apikey = getDescriptor().getApikey();
        Configuration c = browserList.getConfig(operatingSystem);
        operatingSystemApiName = c.getApiName();
    	browserApiName = c.getBrowsersApiName(browser);
    	this.resolution = resolution;
    }
    /*
     *  Main function
     */
    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        // This is where you 'build' the project.
    	
    	//really bad way to remove the build number from the name...
    	String buildname = build.getFullDisplayName().substring(0, build.getFullDisplayName().length()-(String.valueOf(build.getNumber()).length()+1));

    	// Set the environment variables
    	EnvVars env = new EnvVars();
    	env.put("CBT_USERNAME", username);
    	env.put("CBT_APIKEY", apikey);
    	env.put("CBT_BUILD_NAME", buildname);
    	env.put("CBT_BUILD_NUMBER", String.valueOf(build.getNumber()));
    	env.put("CBT_OPERATING_SYSTEM", operatingSystemApiName);
    	env.put("CBT_BROWSER", browserApiName);
    	env.put("CBT_RESOLUTION", resolution);
    	
    	// log the environment variables to the Jenkins build console
    	listener.getLogger().println("ENVIRONMENT VARIABLES");
    	for (Map.Entry<String, String> envvar : env.entrySet()) {
    		listener.getLogger().println(envvar.getKey() + ": "+ envvar.getValue());
    	}
    	
    	launcher = launcher.decorateByEnv(env); //add them to the tasklauncher
    	
    	Launcher.ProcStarter lp = launcher.launch();
    	lp.pwd(workspace); //set the working directory

		for (FilePath executable : workspace.list()) {
			ArgumentListBuilder cmd = new ArgumentListBuilder();
			String fileName = executable.getName();
	    	//Extract extension
			String extension = "";
			int i = fileName.lastIndexOf('.');
			if (i > 0) {
			    extension = fileName.substring(i+1);
			}
			// figure out how to launch it
			if (extension.equals("py") || extension.equals("rb") || extension.equals("jar") || extension.equals("js")) { //executes with full filename
				if (extension.equals("py")) { //python
					cmd.add("python");
				}else if (extension.equals("rb")) { //ruby
					cmd.add("ruby");
				}else if (extension.equals("jar")) { //java jar
					cmd.add("java");
					cmd.add("-jar");
				}else if (extension.equals("js")) { //node javascript
					cmd.add("node");
				}
				cmd.add(executable.getName());
			}else if (extension.equals("exe")) { //executes with only the basename
				//csharp
				cmd.add(executable.getBaseName());
			}
			lp.cmds(cmd);
			listener.getLogger().println("Errors/Output");
			//write the output from the script to the console
			lp.stdout(listener);
		}
    	lp.join(); //run the tests
    }
    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private String cbtUsername = "";
        private String cbtApikey = "";
    	public DescriptorImpl() {
        	browserList = new BrowserList();
            load();
        }
    	public String getUsername() {
    		return cbtUsername;
    	}
    	public String getApikey() {
    		return cbtApikey;
    	}
        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
   
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
/*
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }
*/
        public ListBoxModel doFillOperatingSystemItems() {
            ListBoxModel items = new ListBoxModel();
            for (Configuration config : browserList.configurations) {
                items.add(config.getName());
            }
            return items;
        }
        public ListBoxModel doFillBrowserItems(@QueryParameter String operatingSystem) {
            ListBoxModel items = new ListBoxModel();
            Configuration config = browserList.getConfig(operatingSystem);
            for (InfoPrototype browser : config.browsers) {
                items.add(browser.getName());
        	}
            return items;
        }
        public ListBoxModel doFillResolutionItems(@QueryParameter String operatingSystem) {
            ListBoxModel items = new ListBoxModel();
            Configuration config = browserList.getConfig(operatingSystem);
            for (InfoPrototype resolution : config.resolutions) {
                items.add(resolution.getName());
        	}
            return items;
        }
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "CrossBrowserTesting.com";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist  configuration information,
            // set that to properties and call save().
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
        	cbtUsername = formData.getString("username");
        	cbtApikey = formData.getString("apikey");
            save();
            return super.configure(req,formData);            
        }
    }
}

