package edu.uoregon.autograder.util;

import java.util.ArrayList;
import java.util.List;

import edu.uoregon.autograder.model.Grader;

/**
 * This class manages a queue of Graders, executing them sequentially in a separate thread from the servlet so that
 * the servlet can continue to process new grading requests and serve status pages for queued or finished requests.
 * 
 * A GraderRunner is created statically by a servlet, so there should be only one GraderRunner per running servlet.
 * This is based on a model in which the hosting platform only runs a single Android emulator at a time, so all
 * grading requests have to be processed sequentially, not in parallel. If you are able to run multiple emulators at once,
 * then you would want multiple GraderRunners, or perhaps a single GraderRunner that can execute Graders on the different
 * emulators, pulling from an emulator pool. For now, we just use a single emulator because it's a CPU-intensive beast.
 *
 * @author Kurt Mueller
 */
public class GraderRunner extends Thread {
  /**
	 * Graders that are waiting to be run
	 */
	private static List<Grader> graderQueue = new ArrayList<Grader>();
	
	/**
	 * Graders that have already been run. We keep them around so that we can access their status pages.
	 */
	private static List<Grader> oldGraders = new ArrayList<Grader>();
	
	/**
	 * The Grader that is currently being tested. While being tested, it is not in graderQueue or in oldGraders, just here.
	 */
	private Grader runningGrader;
	
	/**
	 * Reset the GraderRunner queues between batch submissions, for instance by the MultigraderServlet. Otherwise it will
	 * keep adding jobs to the existing queues if you submit the same form page multiple times.
	 */
	public void reset() {
		graderQueue = new ArrayList<Grader>();
		oldGraders = new ArrayList<Grader>();
		runningGrader = null;
	}
	
	
	/**
	 * Generates a complete HTML page with a single row (or two rows in the case of completed Graders) for each Grader in the queues,
	 * including the currently running Grader. Calls each Grader's toConciseHTMLTableRow() method to get details for each Grader.
	 * 
	 * @return the HTML String for the status page
	 */
	public synchronized String getConciseHtml() {
		StringBuffer buff = new StringBuffer();
		buff.append("<html><head>");
		buff.append("<meta http-equiv=\"refresh\" content=\"5\" >");
		buff.append("<style type=\"text/css\">body {font-family:Arial, sans-serif;} table,td,th {border:solid black 1px; width:600px; border-collapse:collapse; text-align:left}</style>");
		buff.append("</head><body><table cellpadding=3>");
		buff.append("<tr><td colspan=\"3\"><b>Status of all Grader jobs</b></td></tr>\n");
		for (Grader grader : oldGraders)
			buff.append(grader.toConciseHTMLTableRow());
		if (getRunningGrader() != null) buff.append(getRunningGrader().toConciseHTMLTableRow());
		for (Grader grader : graderQueue)
			buff.append(grader.toConciseHTMLTableRow());
		buff.append("</table></body></html>");
		return buff.toString();
	}
	
	
	/**
	 * @return the number of Graders in the queue, waiting to be processed. Could be used to estimate
	 * waiting time on the form submission page.
	 */
	public synchronized int currentQueueSize() {
		return graderQueue.size();
	}
	
	public synchronized int addGraderToQueue(Grader grader) {
		//synchronized(graderQueue) {
			grader.status = Grader.Status.QUEUED;
			graderQueue.add(grader);
			return graderQueue.size();
		//}
	}
	
	public synchronized Grader getGraderById(String graderId) {
		//synchronized(graderQueue) {
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
	//	}
	}
      
  public synchronized String printQueues() {
    String result = "";
    if (runningGrader != null) result += "running: " + runningGrader.getGraderId() + "\n";
    else result += "running: null\n";
    result += "old:\n";
    for (Grader grader: oldGraders) {
      result += grader.getGraderId() + "\n";
    }
    result += "queued:\n";
    for (Grader grader: graderQueue) {
      result += grader.getGraderId() + "\n";
    }
    return result;
  }
	
	private synchronized void setRunningGrader(Grader grader) {
	  this.runningGrader = grader;
	}
	
	private synchronized Grader getRunningGrader() {
	  return runningGrader;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 * 
	 * the run loop for the GraderRunner thread, which looks in the graderQueue every 10 seconds for a new Grader 
	 * to run. When a new Grader is found, it pulls the Grader from the queue, runs it, moves it to the oldGraders queue,
	 * and then checks the graderQueue for more Graders.
	 */
	public void run() {
	  synchronized(graderQueue) {
		while (!Thread.currentThread().isInterrupted()) {
			if (graderQueue.isEmpty()) {
				try {
					Thread.sleep(1000 * 10); // 10 seconds
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			} else {
				System.out.println("found a new grader to run");
				//if (graderQueue.size() > 0) {
					setRunningGrader(graderQueue.get(0));
					graderQueue.remove(0);
					System.out.println("\n\nqueue size: " + graderQueue.size());
					getRunningGrader().status = Grader.Status.IN_PROGRESS;
					getRunningGrader().doTasks();
					System.out.println("finished running grader");
					getRunningGrader().status = Grader.Status.COMPLETED;
					oldGraders.add(getRunningGrader());
					setRunningGrader(null);
				//} else {
				//	System.out.println("Nearly tried to access element 0 of 0-size array!");
				//}
			}
		}
	}
}
 
}
