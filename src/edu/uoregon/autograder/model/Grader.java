package edu.uoregon.autograder.model;

import java.util.ArrayList;
import java.util.List;

import edu.uoregon.autograder.android.task.RunRobotiumTask;

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
	
	private String graderId;
	
	public Status status;
	
	private GraderData input;
	//private GraderOutput output;
	//TODO only using the first of these, just to start workflow. Don't really need all of them, except for printing output. Hmm.
	private List<GraderTask> tasks;
	
	public Grader(String graderId) {
		this.status = Status.NEW;
		this.graderId = graderId;
		this.input = new GraderData();
		//this.output = new GraderOutput();
		this.tasks = new ArrayList<GraderTask>();
	}
	
	public void addTask(GraderTask task) {
		this.tasks.add(task);
	}

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

	public String describeTasks() {
		if (tasks != null && !tasks.isEmpty()) {
			StringBuffer buff = new StringBuffer();
			for (GraderTask task : tasks) {
				buff.append(task.getTaskName() + "\n");
			}
			return buff.toString();
		}
		return null;
	}

	public String getGraderId() {
		return graderId;
	}
	
	public GraderData getInput() {
		return input;
	}

/*	public GraderOutput getOutput() {
		return output;
	}*/

	public List<GraderTask> getTasks() {
		return tasks;
	}
	
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
