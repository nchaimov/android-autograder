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

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import edu.uoregon.autograder.android.task.AddLinesToAndroidManifestTask;
import edu.uoregon.autograder.android.task.InstallAPKInEmulatorTask;
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
 * @author kurteous
 *
 */
public class AutograderServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1857693746633762176L;
	
	private static final Logger log = Logger.getLogger(AutograderServlet.class.getName());
	
	private static Map<String, String> servletInitParams;
	
	//private static List<Grader> graders = new ArrayList<Grader>();
	private static GraderRunner runner = new GraderRunner();
	static {
		Thread runnerThread = new Thread(runner);
		runnerThread.start();
	}
	// may be used to keep track of the package names of installed APKs, so
	// we can be sure that they are all cleaned up between tests. The 
	// uninstallAPKs task will only uninstall the APKs that it is trying to
	// install or has installed, not APKs from other students that will 
	// have different package names.
	private static List<String> installedAPKs = new ArrayList<String>();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		log.info("servlet doGet!");
		//doPost(request, response);
		
		String graderId = request.getParameter("id");
		Grader grader = null;
		if (graderId != null) {
			ServletOutputStream os = response.getOutputStream();
			/*Iterator<Grader> it = graders.iterator();
			Grader tempGrader = null;
			while (it.hasNext()) {
				tempGrader = it.next();
				if (tempGrader.getGraderId().equals(graderId)) {
					grader = tempGrader;
					break;
				}
			}*/
			grader = runner.getGraderById(graderId);
			// at this point should have the grader we're looking for
			if (grader != null) {
				os.print(grader.toHTML());
			} else {
				// didn't find a grader with the submitted id
				// TODO return an error message
				os.print("<html><body>No grader found for this ID</body></html>");
			}
		} else {
			// get list of Robotium tester files
			 // Directory path here
			  String path = getServletConfig().getInitParameter("ROBOTIUM_DIR"); 
			 
			  File folder = new File(path);
			  File[] listOfFiles = folder.listFiles(); 
			  
			  request.setAttribute("robotium_files", listOfFiles);
			  /*for (int i = 0; i < listOfFiles.length; i++) 
			  {
			 
			   if (listOfFiles[i].isFile()) 
			   {
			   files = listOfFiles[i].getName();
			   System.out.println(files);
			      }
			  }*/
			
			request.getRequestDispatcher("/index.jsp").forward(request, response);
		}
	}
	
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
		
		request.getSession().setAttribute("graderId", graderId);
		
		Grader grader = new Grader(graderId);
		//graders.add(grader);
		
		log.info("\n\nGrader status: http://localhost:8080/autograder/autograderservlet?id=" + grader.getGraderId() + "\n\n");
		
		// add servlet init params to input set for grader
		if (servletInitParams != null)
			for (String key: servletInitParams.keySet())
				grader.getInput().addString(key, servletInitParams.get(key));
		
		// save the uploaded APK to tmpDir
		parseServletRequestAndWriteToTempDir(request, response, servletInitParams.get("TMP_DIR_PATH"), grader.getGraderId(), grader.getInput());
		log.info("Form input:\n" + grader.getInput().toString());
		
		//ServletOutputStream os = response.getOutputStream();
		//os.print("\nInput data:\n");
		//os.print(grader.getInput().toString());	
		
		//os.print("Checking for running emulator...");
		
		// create all tasks
		GraderTask processAPKsTask = new AddLinesToAndroidManifestTask(grader);
		grader.addTask(processAPKsTask);
		GraderTask pingEmuTask = new PingEmulatorTask(grader);
		grader.addTask(pingEmuTask);
		GraderTask startEmuTask = new StartEmulatorTask(grader);
		grader.addTask(startEmuTask);
		GraderTask waitForEmuTask = new WaitForEmulatorTask(grader);
		grader.addTask(waitForEmuTask);
		GraderTask uninstallAPKTaskPre = new UninstallAPKFromEmulatorTask(grader);
		grader.addTask(uninstallAPKTaskPre);
		GraderTask installAPKTask = new InstallAPKInEmulatorTask(grader);
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
		
		//disable the Robotium testing app
		//launchRobotiumTask.status = GraderTask.Status.DISABLED;
		
		// this will start the grader if the queue is empty
		runner.addGraderToQueue(grader);
		//grader.doTasks();
		
		// redirect browser to status page - NOT WORKING!
		response.sendRedirect("/autograder/autograderservlet?id=" + grader.getGraderId());
		response.flushBuffer();
/*		
		// for now, just try processing APKs
		// TODO currently broken, can't run aapt as part of apktool for some reason; permission denied error
		log.info("Processing uploaded and Robotium APKs");
		processAPKsTask.doTask();
		
		// this is now handled by the processAPKs task, since that task creates the tester APK in grader's tmp dir
		//grader.getInput().addFile(grader.getInput().getString("ROBOTIUM_APK"), grader.getInput().getString("TMP_DIR_PATH") + 
		//		File.separator + grader.getGraderId() + File.separator + grader.getInput().getString("ROBOTIUM_APK"));
		
		// set next tasks for ping task
		//pingEmuTask.addResultTaskMapping(result, task)

		// ping the emulator to see if it's already running
		log.info("Pinging emulator...");
		pingEmuTask.doTask();
		
		//os.print("\nOutput data after ping:\n");
		//os.print(grader.getOutput().toString());
		log.info("ping output: " + grader.getOutput().getOutputString(pingEmuTask.getTaskName()));
		if (pingEmuTask.getStatus() != GraderTask.Status.COMPLETED_NORMALLY) {
			// start emulator
			log.info("Starting emulator...");
			//os.print("\nStarting emulator...\n");
			
			startEmuTask.doTask();
			
			// wait for emulator to startup
			log.info("Waiting for emulator to come up...");
			//os.print("\nWaiting for emulator to come up...\n");
			
			waitForEmuTask.doTask();
		}
		
		// first try uninstalling app and robotium apks from emulator
		log.info("Uninstalling APKs from emulator");
		//os.print("\nUninstalling APKs first, to clean up emulator");
		uninstallAPKTaskPre.doTask();

		// install in emulator
		log.info("Installing APKs in emulator...");
		//os.print("\nInstalling APKs in emulator...\n");
		
		installAPKTask.doTask();*/
		
		// not launching app directly
/*		// add required class info to input; parse from APK filename
		String classParam = null;
		String apkFileName = grader.getInput().getFirstFileName();
		if (apkFileName != null) {
			classParam = "appinventor.ai_" + grader.getInput().getString("username")+".";  // TODO clean this all up, move elsewhere, refactor hardcoded "username"
			classParam += apkFileName.substring(0, apkFileName.lastIndexOf("."));
			classParam += "/.Screen1";
		}
		log.info("classParam: " + classParam);
		if (classParam != null) {
			grader.getInput().addString(LaunchInEmulatorTask.CLASS_PARAM_NAME, classParam);
		// run the installed app
			log.info("Launching the installed app...");
			os.print("\nLaunching the installed app...\n");
			
			launchAppTask.doTask();
		} else
			os.print("\nUnable to launch app; can't create valid class name\n"); // TODO better error reporting; what went wrong?
*/			
		// run robotium
		//launchRobotiumTask.doTask();
		
		// uninstall after testing
/*		uninstallAPKTaskPost.doTask(); */
		
		//os.print("\nInput data:\n");
		//os.print(grader.getInput().toString());	
		// return results to user
		//os.print("\nOutput data:\n");
		//os.print(grader.getOutput().toString());
		//os.flush();
		//os.close();
	}
	
	
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
	 * out to the temp dir and returns a GraderInput object with the form fields and the file name and saved path info.
	 * 
	 * @param request
	 * @param response
	 * @param tmpDir
	 * @return GraderInput, the object created to keep track of the form field and file input info.
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
			while (iter.hasNext()) {
				FileItem item = iter.next();
				//InputStream stream = item.openStream();

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
