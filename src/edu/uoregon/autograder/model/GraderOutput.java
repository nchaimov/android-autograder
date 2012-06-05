package edu.uoregon.autograder.model;

import edu.uoregon.autograder.util.ShellOutputError;

public class GraderOutput extends GraderData {
	
	public void addOutputString(String taskName, String value) {
		addString(taskName + "-output", value);
	}
	
	public void addErrorString(String taskName, String value) {
		addString(taskName + "-error", value);
	}
	
	public String getOutputString(String taskName) {
		if (strings != null)
			return strings.get(taskName + "-output");
		else return null;
	}
	
	public String getErrorString(String taskName) {
		if (strings != null)
			return strings.get(taskName + "-error");
		else return null;
	}
	
	public void setOutputAndError(String taskName, ShellOutputError oe) {
		if (oe.getOutput() != null) 
			addString(taskName+"-stdout", oe.getOutput());
		if (oe.getError() != null) 
			addString(taskName+"-stderr", oe.getError());
	}
}
