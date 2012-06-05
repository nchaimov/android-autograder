package edu.uoregon.autograder.util;

import java.util.ArrayList;
import java.util.List;

import edu.uoregon.autograder.model.Grader;

public class GraderRunner extends Thread {
	
	private static List<Grader> graderQueue = new ArrayList<Grader>();
	private static List<Grader> oldGraders = new ArrayList<Grader>();
	
	private Grader runningGrader;
	
	/*public GraderRunner() {
		run();
	}*/
	
	public void reset() {
		graderQueue = new ArrayList<Grader>();
		oldGraders = new ArrayList<Grader>();
		runningGrader = null;
	}
	
	public String getConciseHtml() {
		StringBuffer buff = new StringBuffer();
		buff.append("<html><head>");
		buff.append("<meta http-equiv=\"refresh\" content=\"5\" >");
		buff.append("<style type=\"text/css\">body {font-family:Arial, sans-serif;} table,td,th {border:solid black 1px; width:600px; border-collapse:collapse; text-align:left}</style>");
		buff.append("</head><body><table cellpadding=3>");
		buff.append("<tr><td colspan=\"3\"><b>Status of all Grader jobs</b></td></tr>\n");
		for (Grader grader : oldGraders)
			buff.append(grader.toConciseHTMLTableRow());
		if (runningGrader != null) buff.append(runningGrader.toConciseHTMLTableRow());
		for (Grader grader : graderQueue)
			buff.append(grader.toConciseHTMLTableRow());
		buff.append("</table></body></html>");
		return buff.toString();
	}
	
	public synchronized int currentQueueSize() {
		return graderQueue.size();
	}
	
	public int addGraderToQueue(Grader grader) {
		synchronized(graderQueue) {
			grader.status = Grader.Status.QUEUED;
			graderQueue.add(grader);
			return graderQueue.size();
		}
	}
	
	public Grader getGraderById(String graderId) {
		synchronized(graderQueue) {
		if (runningGrader != null && runningGrader.getGraderId().equals(graderId))
			return runningGrader;
		for (Grader grader: graderQueue) {
			if (grader.getGraderId().equals(graderId))
				return grader;
		}
		for (Grader grader: oldGraders) {
			if (grader.getGraderId().equals(graderId))
				return grader;
		}
		return null;
		}
	}

	public void run() {
		while (true) {
			if (graderQueue.isEmpty()) {
				try {
					Thread.sleep(1000 * 10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // 10 seconds
			} else {
				System.out.println("found a new grader to run");
				runningGrader = graderQueue.remove(0);
				runningGrader.status = Grader.Status.IN_PROGRESS;
				runningGrader.doTasks();
				System.out.println("finished running grader");
				runningGrader.status = Grader.Status.COMPLETED;
				oldGraders.add(runningGrader);
				runningGrader = null;
			}
		}
	}
 
}
