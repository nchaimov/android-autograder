package edu.uoregon.autograder.model;

import java.util.ArrayList;
import java.util.List;

import edu.uoregon.autograder.android.task.RunRobotiumTask;

/**
 * @author Kurt Mueller
 *
 * This class encapsulates grader functionality for testing one program. A grader executes a sequence of GraderTasks 
 * (stored in the tasks field) sequentially, using input parameters from the input field.
 * 
 */
public class Grader {
	
	public enum Status {
		NEW,
		QUEUED,
		IN_PROGRESS,
		COMPLETED
	}
	
	public static String statusAsString(Status status) {
		if (status == Status.NEW) return "new";
		if (status == Status.QUEUED) return "queued";
		if (status == Status.IN_PROGRESS) return "in progress";
		if (status == Status.COMPLETED) return "completed";
		else return "unknown";
	}
	
	/**
	 * This is a unique id for the grader. A directory with this string's name is created in the
	 * webapp's tmp dir to keep this particular grader's data, including uploaded files, temp files, output data, etc.
	 * The graderId is set by the servlet that processes user input.
	 */
	private String graderId;
	
	public Status status;
	
	
	/**
	 * This is the set of input data that the grader's tasks may need to do their work. It is created by
	 * the servlet that processes user input. The servlet's init parameters (specified in web.xml) are added
	 * to the input, so that they are available to grader tasks. In addition, grader tasks may add data to
	 * the input for use by subsequent tasks in the workflow.
	 */
	private GraderData input;

	/**
	 * We only need the first of these to start workflow, in doTasks(). We iterate over the full list for printing output.
	 */
	private List<GraderTask> tasks;
	
	public Grader(String graderId) {
		this.status = Status.NEW;
		this.graderId = graderId;
		this.input = new GraderData();
		this.tasks = new ArrayList<GraderTask>();
	}
	
	public void addTask(GraderTask task) {
		this.tasks.add(task);
	}

	/**
	 * Get the first task, and if it's not disabled, do it. Then get that task's next task and do it. Repeat until
	 * all tasks are done.
	 */
	public void doTasks() {
		if (tasks != null && !tasks.isEmpty()) {
			GraderTask task = tasks.get(0);
			while (task != null) {
				if (task.status != GraderTask.Status.DISABLED)
					task.doTask();
				task = task.getNextTask();
			}
		}
	}

	public String getGraderId() {
		return graderId;
	}
	
	public GraderData getInput() {
		return input;
	}

	public List<GraderTask> getTasks() {
		return tasks;
	}
	
	/**
	 * Of course, this is not a good long-term solution, because you really shouldn't have a lot of HTML hardcoded in your
	 * java classes, but it works for now. The http-equiv="refresh" content="5" tag causes the page to automatically refresh every
	 * 5 seconds. Would be better to push changes to the web page, but again, this is just a start.
	 * 
	 * @return a String with a complete HTML page that shows the status of the tasks in the grader's workflow, and of the grader itself.
	 */
	public String toHTML() {
		StringBuffer buff = new StringBuffer();
		buff.append("<html><head>");
		buff.append("<meta http-equiv=\"refresh\" content=\"5\" >");
		buff.append("<style type=\"text/css\">body {font-family:Arial, sans-serif;} table,td,th {border:solid black 1px; width:600px; border-collapse:collapse; text-align:left}</style>");
		//buff.append("<style type=\"text/css\">table.inprogress {border:solid #FF0000 1px; width:600px; border-collapse:collapse; text-align:right}</style>");
		buff.append("</head><body>");
		String statusHtml = statusAsString(status);
		if (status == Status.IN_PROGRESS)
			statusHtml = "<font color=\"red\">" + statusHtml + "</font>";
		buff.append("<h3>Testing: " + getInput().getFirstFileName() + "<br>Status: " + statusHtml + "</h3>");
		for (GraderTask task: tasks) {
			buff.append(task.toHTML());
		}
		buff.append("</body></html>");
		return buff.toString();
	}
	
	/**
	 * @return a String with HTML for a single table row (or two table rows, in the case of completed Robotium tasks), showing
	 * the status and results (if complete) of the grader. This is used by the MultigraderServlet, to show the status of all
	 * queued, running, and completed graders on a single page. It does not show the status of all of a grader's tasks, like the 
	 * full-page toHMTL() method above.
	 */
	public String toConciseHTMLTableRow() {
		String testedFilename = input.getFirstFileName();
		if (status == Status.IN_PROGRESS) {
			for (GraderTask task : tasks) {
				if (task.status == task.status.IN_PROGRESS) {
					return("<tr><td>" + input.getFirstFileName() + "</td><td>" + statusAsString(status) + "</td><td>" + task.getTaskName() + "</td></tr>\n");
				}
			}			
		} else if (status == Status.COMPLETED) {
			for (GraderTask task : tasks) {
				if (task.getClass() == RunRobotiumTask.class) {
					String result = "<tr><td>" + input.getFirstFileName() + "</td><td>" + statusAsString(status) + "</td><td>" + task.getTaskName() + "</td></tr>\n";
					for (String key: task.getLog().keySet()) {
						result += "<tr><td></td><td>"+key+"</td><td>"+task.getLog().get(key).replace("\n", "<br>")+"</td></tr>\n";
					}
					return result;
				}
			}
		} else {
			// status = QUEUED
			return("<tr><td>" + input.getFirstFileName() + "</td><td>" + statusAsString(status) + "</td><td></td></tr>\n");
		}
		return "";	// should never reach this...
	}
	
}
