package edu.uoregon.autograder.util;

/**
 * A simple class to hold the results (stdout and stderr) of executing a shell command.
 *
 * @author Kurt Mueller
 */
public class ShellOutputError {

	private String output;
	private String error;
	public String getOutput() {
		return output;
	}
	public void setOutput(String output) {
		this.output = output;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("output: " + output == null ? "<none>" : output + "\n");
		buff.append("error: " + error == null ? "<none>" : error);
		return buff.toString();
	}
	
}
