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
import edu.uoregon.autograder.model.GraderData;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.GraderRunner;


/**
 * @author Kurt Mueller
 *
 * This servlet processes single grading requests from a web page. That means the web page (in this case, index.jsp) 
 * allows the user to upload a single App Inventor .apk file at a time, and choose from a list of Robotium test .apk files
 * already stored on the server. This gives the server the ability to test different assignments, because it has multiple
 * Robotium test APKs onboard and the user can choose which one to run against her uploaded App Inventor APK.
 * 
 * Form input, including the user's App Inventor file, is processed and used to make a Grader object. This is handed to the
 * GraderRunner, which maintains a queue of Graders to run. The user's browser is immediately redirected to a status page
 * for the new Grader, so they can track its progress and see the result of grading. The status page is currently generated
 * directly by the Grader's toHTML() method, though this is not intended to be a long-term solution.
 * 
 * The list of Robotium APKs on the server is created by this servlet's init method, and used by index.jsp to show the installed
 * APKs in a pulldown. You can access the web form for submitting new Grader jobs at any of three URLs:
 * 
 * http://localhost:8080/autograder
 * http://localhost:8080/autograder/index.jsp
 * http://localhost:8080/autograder/autograderservlet
 * 
 */
public class AutograderServlet extends HttpServlet {

	private static final long serialVersionUID = -1857693746633762176L;
	
	private static final Logger log = Logger.getLogger(AutograderServlet.class.getName());
	
	private static Map<String, String> servletInitParams;
	
	
	/**
	 * This manages the newly-created Graders in a queue, in a separate thread from the servlet. Otherwise
	 * the servlet becomes unresponsive while grading is proceeding.
	 */
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
	
	/**
	 *  may be used in the future to keep track of the package names of installed APKs, so
	 * we can be sure that they are all cleaned up between tests. The 
	 * uninstallAPKs task will only uninstall the APKs that it is trying to
	 * install or has installed, not APKs from other students that will 
	 * have different package names.
	 */
	private static List<String> installedAPKs = new ArrayList<String>();
	
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * 
	 * Serves regular get requests; if there is an "id" parameter, returns the Grader's HTML output for that 
	 * Grader with that id. If not, forwards to the form submission index.jsp.
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		log.info("servlet doGet!");
		
		String graderId = request.getParameter("id");
		Grader grader = null;
		if (graderId != null) {
			ServletOutputStream os = response.getOutputStream();
			grader = runner.getGraderById(graderId);
			// at this point should have the grader we're looking for
			if (grader != null) {
				os.print(grader.toHTML());
			} else {
				// didn't find a grader with the submitted id
				os.print("<html><body>No grader found for this ID</body></html>");
			}
		} else {
			request.getRequestDispatcher("/index.jsp").forward(request, response);
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * 
	 * Processes form submissions to create a Grader object with uploaded form field and file data, and adds the Grader
	 * to the GraderRunner queue.
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

		// generate a unique grader ID and create the grader's tmp dir
		String graderId = generateGraderIdAndMakeGraderTmpDir(servletInitParams.get("TMP_DIR_PATH"));
		
		// this isn't used now, but could be. graderId is hardcoded into the URL to which the user's browser is redirected
		request.getSession().setAttribute("graderId", graderId);
		
		Grader grader = new Grader(graderId);
		
		//log.info("\n\nGrader status: http://localhost:8080/autograder/autograderservlet?id=" + grader.getGraderId() + "\n\n");
		
		// add servlet init params to input set for grader
		if (servletInitParams != null)
			for (String key: servletInitParams.keySet())
				grader.getInput().addString(key, servletInitParams.get(key));
		
		// save the uploaded APK to tmpDir
		parseServletRequestAndWriteToTempDir(request, response, servletInitParams.get("TMP_DIR_PATH"), grader.getGraderId(), grader.getInput());
		log.info("Form input:\n" + grader.getInput().toString());	
		
		//os.print("Checking for running emulator...");
		
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
		
		//disable the Robotium testing app - can save time during testing
		//launchRobotiumTask.status = GraderTask.Status.DISABLED;
		
		// this will start the grader if the queue is empty
		runner.addGraderToQueue(grader);
		
		// redirect browser to status page
		response.sendRedirect("/autograder/autograderservlet?id=" + grader.getGraderId());
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
	 * Takes the multipart file input and parses form fields as well as the uploaded file. Writes the file 
	 * out to the temp dir. Adds entries to the GraderData input for the form fields and uploaded file.
	 * 
	 * @param request
	 * @param response
	 * @param tmpDir the main temp directory
	 * @param graderId the new grader id
	 * @param input the input data for the Grader, including the servlet init parameters
	 * @throws ServletException
	 */
	private void parseServletRequestAndWriteToTempDir(HttpServletRequest request, HttpServletResponse response, String tmpDir, String graderId, GraderData input) 
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
			System.out.println("form items count: " + items.size());
			while (iter.hasNext()) {
				FileItem item = iter.next();

				if (item.isFormField()) {
					log.info("Got a form field: " + item.getFieldName());
					// TODO sanitize input here! don't want "robotium; rm -rf"
					input.addString(item.getFieldName(), item.getString());
				} else {
					log.info("Got an uploaded file: " + item.getFieldName() +
							", name = " + item.getName());

					if (!graderId.equals("0")) {
						String fileName = tmpDir + File.separator + graderId + File.separator + item.getName();
						File uploadedFile = new File(fileName);
						uploadedFile.deleteOnExit();
						item.write(uploadedFile);
						input.addFile(item.getName(), fileName);
					} else {
						System.err.println("Error writing temp file: " + graderId);
					}
				}
			}
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
		
	}

}
