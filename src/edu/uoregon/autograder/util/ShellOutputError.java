package edu.uoregon.autograder.util;

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
