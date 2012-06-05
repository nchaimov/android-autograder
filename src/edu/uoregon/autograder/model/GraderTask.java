package edu.uoregon.autograder.model;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.uoregon.autograder.util.ShellOutputError;

public abstract class GraderTask {
	
	public static final String DEFAULT_TASK = "grader_default_task";
	public static final String TASK_STATUS = "Task status";
	public static final String SUCCESS = "Success";
	public static final String ERROR_PREFIX = "Error: ";
	public static final String FINISHED = "Completed normally";
	public static final String STARTED = "Started";
	
	public enum Status {
		DISABLED,
		NOT_STARTED,
		IN_PROGRESS,
		COMPLETED,
		ERROR
	}
	
	public static String statusAsString(Status status) {
		if (status == Status.ERROR) return "error";
		if (status == Status.COMPLETED) return "completed";
		if (status == Status.DISABLED) return "disabled";
		if (status == Status.IN_PROGRESS) return "in progress";
		if (status == Status.NOT_STARTED) return "not started";
		else return "unknown";
	}
	
	protected Grader parent;
	
	private String taskName;
	
	public Status status;

	private GraderTask() {	// make sure only two-arg constructor is used
	}
	
	public GraderTask(String taskName, Grader parent) {
		this.taskName = taskName;
		this.parent = parent;
		this.status = Status.NOT_STARTED;
		this.log = new LinkedHashMap<String, String>();
	}
	
	// task output stuff here
	// idea is that this will map commands to results, so a typical entry will look like:
	// "install Robotium APK" : "success"
	private Map<String, String> log;
	
	public Map<String, String> getLog() { return log; }
	
	protected void log(String command, String result) {
		log.put(command,  result);
	}
	
	protected void log(String command, ShellOutputError oe) {
		if (oe.getOutput() != null)
			log(command+" stdout", oe.getOutput());
		if (oe.getError() != null)
			log(command+" stderr", oe.getError());
	}
	
	// when starting the task
	protected void logStarted() {
		//log(TASK_STATUS, STARTED);
		status = Status.IN_PROGRESS;
	}
	
	// when finished with the task but don't know status
	protected void logFinished() {
		status = Status.COMPLETED;	// not necessarily, could be abnormal
	}

	// when finished with the task with an error
	protected void logError(String errorString) {
		log(TASK_STATUS, ERROR_PREFIX + errorString);
		status = Status.ERROR;
	}
	
	// just log a successful operation during execution; this does NOT signal finishing the task
	protected void logSuccess(String taskString) {
		log(taskString, SUCCESS);
	}
	
	public Status getStatus() {
		return status;
	}
	
	protected GraderData getInput() {
		return parent.getInput();
	}
	
	/*protected GraderOutput getOutput() {
		return parent.getOutput();
	}*/
	
	protected String getGraderId() {
		return parent.getGraderId();
	}
	
	public abstract void doTask();
	
	public String getTaskName() {
		return taskName;
	}
	
	public synchronized String toHTML() {
		String statusHTML = statusAsString(status);
		if (status == Status.IN_PROGRESS) statusHTML = "<font color=\"red\">" + statusHTML + "</font>";
		StringBuffer buff = new StringBuffer();
		buff.append("<br>");
		/*if (status == Status.IN_PROGRESS)
			buff.append("<table class=\"inprogress\">");
		else*/
			buff.append("<table>");
		buff.append("<tr><th colspan=2><b>" + taskName + " status: " + statusHTML + "</b></th></tr>\n");
		for (String key: log.keySet()) {
			buff.append("<tr><td>"+key+"</td><td>"+log.get(key).replace("\n", "<br>")+"</td></tr>\n");
		}
		buff.append("</table>");
		return buff.toString();
	}

	// task workflow-related stuff below here
	private Map<String, GraderTask> resultTaskMap;
	
	private String nextStepKey;	// key in the key-task resultTaskMap, set by GraderTask implementations upon completion
								// if no default next task is set
	
	public void setNextStep(String nextStep) {
		this.nextStepKey = nextStep;
	}

	public void addResultTaskMapping(String result, GraderTask task) {
		if (resultTaskMap == null) {
			resultTaskMap = new LinkedHashMap<String, GraderTask>();
		}
		resultTaskMap.put(result, task);
	}

	public void addDefaultNextTask(GraderTask task) {
		if (resultTaskMap == null) {
			resultTaskMap = new LinkedHashMap<String, GraderTask>();
		}
		resultTaskMap.put(DEFAULT_TASK, task);
	}

	public GraderTask getNextTask() {
		if (resultTaskMap != null) {
/*			if (nextStep == null) {
				System.err.println("No next step found");
				return;
			}*/
			GraderTask nextTask = resultTaskMap.get(nextStepKey);
			if (nextTask != null) {
				System.out.println("Found next task: " + nextTask.taskName);
				return nextTask;
			} else {
				System.out.println("No next task found for result " + nextStepKey);
			}
			nextTask = resultTaskMap.get(DEFAULT_TASK);
			if (nextTask != null) {
				System.out.println("Found default next task: " + nextTask.taskName);
				return nextTask;
			} else {
				System.out.println("No default next task found for this task");
			}
		} else {
			System.out.println("No next tasks have been set for this task.");
		}
		return null;
	}

}
