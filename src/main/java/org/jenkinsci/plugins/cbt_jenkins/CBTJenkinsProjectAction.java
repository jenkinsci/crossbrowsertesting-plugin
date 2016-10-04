package org.jenkinsci.plugins.cbt_jenkins;

import java.util.ArrayList;
import java.util.List;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;

public class CBTJenkinsProjectAction implements Action{
	   private AbstractProject<?, ?> project;

	    @Override
	    public String getIconFileName() {
	        return "/plugin/testExample/img/project_icon.png";
	    }

	    @Override
	    public String getDisplayName() {
	        return "Test Example Project Action";
	    }

	    @Override
	    public String getUrlName() {
	        return "testExamplePA";
	    }

	    public AbstractProject<?, ?> getProject() {
	        return this.project;
	    }

	    public String getProjectName() {
	        return this.project.getName();
	    }

	    public List<String> getProjectMessages() {
	    	
	        List<String> projectMessages = new ArrayList<String>();
	        /*
	        List<? extends AbstractBuild<?, ?>> builds = project.getBuilds();
	        String projectMessage="";
	        final Class<CBTJenkinsBuildAction> buildClass = CBTJenkinsBuildAction.class;

	        for (AbstractBuild<?, ?> currentBuild : builds) {
	            projectMessage = "Build #"+currentBuild.getAction(buildClass).getBuildNumber()
	                    +": "+currentBuild.getAction(buildClass).getMessage();
	            projectMessages.add(projectMessage);
	        }
	        */
	        return projectMessages;
	    }

	    CBTJenkinsProjectAction(final AbstractProject<?, ?> project) {
	        this.project = project;
	    }
}
