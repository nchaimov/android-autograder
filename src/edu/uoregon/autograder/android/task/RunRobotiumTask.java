package edu.uoregon.autograder.android.task;

import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.ShellAccess;
import edu.uoregon.autograder.util.ShellOutputError;



public class RunRobotiumTask extends GraderTask {

	public static final String TASK_NAME = "RunRobotiumTask";
		
	public static final String ADB_INIT_PARAM_NAME = "ADB_PATH";	// how do we run adb? defined in web.xml
	public static final String HEADLESS_PARAM_NAME = "headless"; // as specified in the form in index.html
	public static final String PACKAGE_PARAM_NAME = "ROBOTIUM_PACKAGE";
	public static final String ADB_UNLOCK_SCREEN_COMMAND = " shell input keyevent 82";
	public static final String CLASS_NAME = "android.test.InstrumentationTestRunner";
	
	// launch robotium:
	// adb shell am instrument -w com.testcalculator/android.test.InstrumentationTestRunner 
	
	public RunRobotiumTask(Grader grader) {
		super(TASK_NAME, grader);
	}
	
	public void doTask() {
		logStarted();
		String command;
		ShellOutputError result;
		
		if (getInput().getString(HEADLESS_PARAM_NAME) != null) {
			// first unlock the screen
			command = getInput().getString(ADB_INIT_PARAM_NAME) + ADB_UNLOCK_SCREEN_COMMAND;
			result = ShellAccess.execCommandBlocking(command);
			log("Unlock the emulator screen", result);
		}
		
		// now launch the app
		//String classParam = getInput().getString(PACKAGE_PARAM_NAME) + "/" + CLASS_NAME;
		String classParam = "edu.uoregon.autograder" + "/" + CLASS_NAME;
		command = getInput().getString(ADB_INIT_PARAM_NAME) + " shell am instrument -w " + classParam;
		result = ShellAccess.execCommandBlocking(command);
		//getOutput().setOutputAndError(getTaskName(), result);
		log("Launch testing app", result);
		logFinished();
	}
	
}
