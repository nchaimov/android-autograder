package edu.uoregon.autograder.servlet;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import edu.uoregon.autograder.android.task.PreprocessAppAndTesterAPKsTask;
import edu.uoregon.autograder.android.task.InstallAPKsInEmulatorTask;
import edu.uoregon.autograder.android.task.RunRobotiumTask;
import edu.uoregon.autograder.android.task.PingEmulatorTask;
import edu.uoregon.autograder.android.task.StartEmulatorTask;
import edu.uoregon.autograder.android.task.UninstallAPKFromEmulatorTask;
import edu.uoregon.autograder.android.task.WaitForEmulatorTask;
import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.GraderRunner;

/**
 * @author Kurt Mueller
 *
 * This servlet processes multiple grading requests from a web page. That means the web page (in this case, index2.jsp) 
 * allows the user to upload multiple App Inventor .apk files at a time, and choose from a list of Robotium test .apk files
 * already stored on the server. This gives the server the ability to test different assignments, because it has multiple
 * Robotium test APKs onboard and the user can choose which one to run against her uploaded App Inventor APKs.
 * 
 * This is intended to be used by an instructor or teaching assistant to upload all submitted APKs for an assignment from the
 * students at once, and have them all be graded in sequence.
 * 
 * Form input, including the user's App Inventor files, is processed and used to make Grader objects. These are handed to the
 * GraderRunner, which maintains a queue of Graders to run. The user's browser is immediately redirected to a status page
 * for the GraderRunner, so they can track its progress and see the result of grading. The status page is currently generated
 * directly by the GraderRunner and Grader's toConciseHTML() method, though this is not intended to be a long-term solution.
 * 
 * The list of Robotium APKs on the server is created by this servlet's init method, and used by index2.jsp to show the installed
 * APKs in a pulldown. You can access the web form for submitting new Grader jobs at two URLs:
 * 
 * http://localhost:8080/autograder/index2.jsp
 * http://localhost:8080/autograder/multigraderservlet
 * 
 */
public class MultigraderServlet extends HttpServlet {

	private static final long serialVersionUID = -1857693746633762176L;
	
	private static final Logger log = Logger.getLogger(MultigraderServlet.class.getName());
	
	private static Map<String, String> servletInitParams;
	
	private static GraderRunner runner = new GraderRunner();
	static {
		Thread runnerThread = new Thread(runner);
		runnerThread.start();
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 * 
	 * set the list of installed Robotium APKs for use by index.jsp in the form pulldown.
	 * The servlet is loaded at startup time (see web.xml load-on-startup tag), so this 
	 * is performed and ready even if the user goes directly to index.jsp without hitting
	 * the servlet first.
	 */
	public void init(ServletConfig config)throws ServletException {
	    super.init(config);

	    String path = getServletConfig().getInitParameter("ROBOTIUM_DIR"); 

	    File folder = new File(path);
	    File[] listOfFiles = folder.listFiles(); 

	    config.getServletContext().setAttribute("robotium_files", listOfFiles);

	  }
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * 
	 * Serves regular get requests; if there is a "results" parameter, returns the GraderRunner's HTML output.
	 * If not, forwards to the form submission index2.jsp.
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		log.info("servlet doGet!");

		if (request.getParameter("results") != null) {
			ServletOutputStream os = response.getOutputStream();
			if (runner != null) {
				os.print(runner.getConciseHtml());
			} else {
				// didn't find a grader
				os.print("<html><body>No grader found</body></html>");
			}
		} else {			
			request.getRequestDispatcher("/index2.jsp").forward(request, response);
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * 
	 * Processes form submissions to create Grader objects with uploaded form field and file data, and adds the Grader
	 * objects to the GraderRunner queue.
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		log.info("servlet doPost");
		
		// get servlet init params if not already set
		if (servletInitParams == null) {
			Enumeration<String> initParams = getServletConfig().getInitParameterNames();
			if (initParams.hasMoreElements()) 
				servletInitParams = new HashMap<String, String>();
			while (initParams.hasMoreElements()) {
				String key = initParams.nextElement();
				servletInitParams.put(key, getServletConfig().getInitParameter(key));
			}
		}
		
		// TODO need to check for a running GraderRunner and reset it. This would entail
		// stopping running jobs, cleaering the runner queues (see GraderRunner's reset() method),
		// and potentially stopping any running emulator. Would need a new Task for that.
		
		// save the uploaded APKs and create all Graders, adding them to graders and starting GraderRunner
		parseServletRequestAndWriteToTempDir(request, response, servletInitParams.get("TMP_DIR_PATH"));
		
		// redirect browser to status page
		response.sendRedirect("/autograder/multigraderservlet?results=show");
		response.flushBuffer();
	}
	
	/**
	 * Creates a unique graderId, verifying uniqueness by checking for an existing
	 * grader tmp dir in tmpDir with the new id. Chances of a collision are vanishingly
	 * small, but best to check anyway.
	 * 
	 * @param tmpDir the main temp directory in which to create individual Grader temp directories
	 * @return the new Grader id
	 */
	private String generateGraderIdAndMakeGraderTmpDir(String tmpDir) {
		// make a randomized temp dir
		SecureRandom random = new SecureRandom();
		boolean gotUnusedRandom = false;
		long n = 0;
		while (gotUnusedRandom != true) {
			n = random.nextLong();
			if (n == Long.MIN_VALUE) {
				n = 0;      // corner case
			} else {
				n = Math.abs(n);
			}
			File file = new File(tmpDir + File.separator + Long.toString(n));
			if (!file.exists()) {
				file.mkdir();
				file.deleteOnExit();
				gotUnusedRandom = true;
			}
		}
		return Long.toString(n);
	}
	
	
	/**
	 * Takes the multipart file input and parses form fields as well as the uploaded files. Writes the files
	 * out to the temp dirs. There is much duplication between this method and AutograderServlet's doPost method; could
	 * be extracted to a separate class.
	 * 
	 * @param request
	 * @param response
	 * @param tmpDir the main temp directory
	 * @throws ServletException
	 */
	private void parseServletRequestAndWriteToTempDir(HttpServletRequest request, HttpServletResponse response, String tmpDir) 
			throws ServletException{
		
		try {		
			// Create a factory for disk-based file items
			DiskFileItemFactory factory = new DiskFileItemFactory();

			// Set factory constraints
			factory.setRepository(new File(tmpDir));
			ServletFileUpload upload = new ServletFileUpload(factory);
			response.setContentType("text/plain");

			List<FileItem> items = upload.parseRequest(request);
			
			Iterator<FileItem> iter = items.iterator();
			ArrayList<FileItem> files = new ArrayList<FileItem>();
			ArrayList<FileItem> formFields = new ArrayList<FileItem>();
			// first separate form inputs into fields and files
			while (iter.hasNext()) {
				FileItem item = iter.next();
				
				if (item.isFormField())
					formFields.add(item);
				else 
					files.add(item);
			}
			
			// now iterate through files, creating a new grader for each
			Iterator<FileItem> fileIter = files.iterator();
			while (fileIter.hasNext()) {
				FileItem file = fileIter.next();
				// generate a unique grader ID and create the grader's tmp dir
				String graderId = generateGraderIdAndMakeGraderTmpDir(servletInitParams.get("TMP_DIR_PATH"));				
				Grader grader = new Grader(graderId);
				// add servlet init params to input set for grader
				if (servletInitParams != null)
					for (String key: servletInitParams.keySet())
						grader.getInput().addString(key, servletInitParams.get(key));
				
				// write all form inputs (non-file) to grader inputs
				if (!formFields.isEmpty())
					for (FileItem formItem: formFields)
						grader.getInput().addString(formItem.getFieldName(), formItem.getString());
				
				// write this grader's app file to the temp dir
				String fileName = tmpDir + File.separator + graderId + File.separator + file.getName();
				File uploadedFile = new File(fileName);
				uploadedFile.deleteOnExit();
				file.write(uploadedFile);
				grader.getInput().addFile(file.getName(), fileName);
				
				// create all tasks
				GraderTask processAPKsTask = new PreprocessAppAndTesterAPKsTask(grader);
				grader.addTask(processAPKsTask);
				
				GraderTask pingEmuTask = new PingEmulatorTask(grader);
				grader.addTask(pingEmuTask);
				
				GraderTask startEmuTask = new StartEmulatorTask(grader);
				grader.addTask(startEmuTask);
				
				GraderTask waitForEmuTask = new WaitForEmulatorTask(grader);
				grader.addTask(waitForEmuTask);
				
				GraderTask uninstallAPKTaskPre = new UninstallAPKFromEmulatorTask(grader);
				grader.addTask(uninstallAPKTaskPre);
				
				GraderTask installAPKTask = new InstallAPKsInEmulatorTask(grader);
				grader.addTask(installAPKTask);
				
				//GraderTask launchAppTask = new LaunchInEmulatorTask(grader);
				//grader.addTask(launchAppTask);
				
				GraderTask launchRobotiumTask = new RunRobotiumTask(grader);
				grader.addTask(launchRobotiumTask);
				
				GraderTask uninstallAPKTaskPost = new UninstallAPKFromEmulatorTask(grader);
				grader.addTask(uninstallAPKTaskPost);
				
				// configure task workflow; only task that makes a decision is the Ping Emulator task. Rest use defaults.
				processAPKsTask.addDefaultNextTask(pingEmuTask);
				pingEmuTask.addResultTaskMapping(PingEmulatorTask.PING_SUCCESS, uninstallAPKTaskPre);
				pingEmuTask.addResultTaskMapping(PingEmulatorTask.PING_FAILURE, startEmuTask);
				pingEmuTask.addDefaultNextTask(startEmuTask);
				startEmuTask.addDefaultNextTask(waitForEmuTask);
				waitForEmuTask.addDefaultNextTask(uninstallAPKTaskPre);
				uninstallAPKTaskPre.addDefaultNextTask(installAPKTask);
				installAPKTask.addDefaultNextTask(launchRobotiumTask);
				launchRobotiumTask.addDefaultNextTask(uninstallAPKTaskPost);
				
				// add grader to list of graders and start running it
				runner.addGraderToQueue(grader);
			}
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
		
	}

}
