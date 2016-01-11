package org.jenkinsci.plugins.cbt_jenkins;

import java.util.Map;

public class RunTest {
	private ProcessBuilder pb;
	Map<String, String> environment;
	public RunTest(Map<String, String> env) {
    	pb = new ProcessBuilder("/usr/local/bin/python", "/Users/michaelhollister/workplace/cbt_jenkins/work/jobs/WORK DAMMIT/workspace/getvars.py");
    	environment = pb.environment();
    	//Transfer the environment variables
    	for(Map.Entry<String, String> entry : env.entrySet()) {
    		environment.put(entry.getKey(), entry.getValue());
    	}
    	
	}
}
