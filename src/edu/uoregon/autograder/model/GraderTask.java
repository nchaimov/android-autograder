package edu.uoregon.autograder.model;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.uoregon.autograder.util.ShellOutputError;

/**
 * @author Kurt Mueller
 *
 * An abstract class that provides many fields and methods needed by concrete GraderTask implementations.
 * A sequence of GraderTasks is combined in a chain, with each task having potentially many possible next tasks,
 * depending on the outcome of the task. For instance, in the case of our Android emulator testing, the
 * PingEmulatorTask has two possible next tasks: if the ping is successful, meaning the emulator is already
 * running, then the next task is UninstallAPKFromEmulatorTask (to make sure that emulator is cleaned up from
 * previous runs). If, however, the ping is unsuccessful, then the next task is StartEmulatorTask to start up
 * the emulator.
 * 
 *  See AutograderServlet for construction of the task workflow. Most tasks have a default next task, because 
 *  they don't make a decision based on the outcome of their processing; rather, they always go to the same next task.
 */
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
	
	/**
	 * The parent grader, to which the concrete GraderTask implementation belongs.
	 */
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
	
	/**
	 * The idea is that this will map commands to results, so a typical entry will look like:
	 * "install Robotium APK" : "success"
	 */
	private Map<String, String> log;
	
	public Map<String, String> getLog() { return log; }
	
	protected void log(String command, String result) {
		log.put(command,  result);
	}
	
	/**
	 * See ShellAccess and ShellOutputError for more about this special log function.
	 * ShellAccess captures both stdout and stderr to a ShellOutputError object, which
	 * must be split into two sets of outputs and identified as stdout or stderr as appropriate.
	 * 
	 * @param command the executed command, or a reasonable description of it
	 * @param oe the ShellOutputError that was generated when the ShellAccess was executed.
	 */
	protected void log(String command, ShellOutputError oe) {
		if (oe.getOutput() != null)
			log(command+" stdout", oe.getOutput());
		if (oe.getError() != null)
			log(command+" stderr", oe.getError());
	}
	
	/**
	 *  when starting the task
	 */
	protected void logStarted() {
		//log(TASK_STATUS, STARTED);
		status = Status.IN_PROGRESS;
	}
	
	/**
	 *  when finished with the task but don't know status
	 */
	protected void logFinished() {
		status = Status.COMPLETED;	// not necessarily, could be abnormal
	}

	/**
	 *  when finished with the task with an error
	 * @param errorString
	 */
	protected void logError(String errorString) {
		log(TASK_STATUS, ERROR_PREFIX + errorString);
		status = Status.ERROR;
	}
	
	/**
	 *  just log a successful operation during execution; this does NOT signal finishing the task
	 * @param taskString
	 */
	protected void logSuccess(String taskString) {
		log(taskString, SUCCESS);
	}
	
	public Status getStatus() {
		return status;
	}
	
	protected GraderData getInput() {
		return parent.getInput();
	}
	
	protected String getGraderId() {
		return parent.getGraderId();
	}
	
	/**
	 * Each concrete GraderTask implementation must implement this method, which is called during workflow 
	 * execution by the parent Grader.
	 */
	public abstract void doTask();
	
	public String getTaskName() {
		return taskName;
	}
	
	
	/**
	 * @return a String with an HTML table representing the task's status and log output
	 */
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
	
	/**
	 * This maintains the mapping between possible results and next tasks to execute for each of those results
	 */
	private Map<String, GraderTask> resultTaskMap;
	
	/**
	 * The key to look for in the key-task resultTaskMap upon completion of the task, to find the next task to execute.
	 * Set by GraderTask implementations upon completion, if no default next task is set
	 */
	private String nextStepKey;
	
	public void setNextStep(String nextStep) {
		this.nextStepKey = nextStep;
	}

	/**
	 * Used by servlet to configure workflow for this task. For each possible result and next task pair, 
	 * add a mapping with this method.
	 * 
	 * @param result the expected result
	 * @param task the task to execute next when that result is obtained
	 */
	public void addResultTaskMapping(String result, GraderTask task) {
		if (resultTaskMap == null) {
			resultTaskMap = new LinkedHashMap<String, GraderTask>();
		}
		resultTaskMap.put(result, task);
	}

	
	/**
	 * Use this to set a default next task to execute, that will be called regardless of the outcome
	 * of running this task.
	 * 
	 * @param task the next task to execute by default
	 */
	public void addDefaultNextTask(GraderTask task) {
		if (resultTaskMap == null) {
			resultTaskMap = new LinkedHashMap<String, GraderTask>();
		}
		resultTaskMap.put(DEFAULT_TASK, task);
	}

	
	/**
	 * Look through the resultTaskMap to see if the result stored in nextStepKey has a corresponding
	 * result-next task pair. If not, could be because a) there is no resultTaskMap, in which case we
	 * are at the end of the workflow, or b) there is no entry in resultTaskMap for the result in 
	 * nextStepKey (perhaps nextStepKey was never set upon task completion). In the latter case, look for
	 * a default next task in the resultTaskMap and if found, return it. 
	 * 
	 * @return the next task to execute, if found
	 */
	public GraderTask getNextTask() {
		if (resultTaskMap != null) {
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
