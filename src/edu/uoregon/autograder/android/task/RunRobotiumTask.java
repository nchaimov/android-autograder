package edu.uoregon.autograder.android.task;

import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.ShellAccess;
import edu.uoregon.autograder.util.ShellOutputError;

/**
 * @author Kurt Mueller
 *
 * This Task implementation launches the Robotium test on the Android emulator once both the
 * App Inventor APK and the Robotium APK have been installed.
 * 
 *  If the test is not set to run "headless" in the web interface, then the screen is unlocked
 *  first so that testing can be seen in the emulator.
 */
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
		
		if (getInput().getString(HEADLESS_PARAM_NAME) == null) {
			// first unlock the screen
			System.out.println("\n\nUnlocking screen\n\n");
			command = getInput().getString(ADB_INIT_PARAM_NAME) + ADB_UNLOCK_SCREEN_COMMAND;
			result = ShellAccess.execCommandBlocking(command);
			log("Unlock the emulator screen", result);
		}
		
		// now launch the app
		//String classParam = getInput().getString(PACKAGE_PARAM_NAME) + "/" + CLASS_NAME;
		// TODO: the PreprocessAppAndTesterAPKsTask should extract the package of the Robotium (tester)
		// APK during preprocessing, and add it to the inputs for later use here in classParam. For now,
		// the package is hard-coded to be edu.uoregon.autograder, so the Robotium app must be built in
		// this package.
		String classParam = "edu.uoregon.autograder" + "/" + CLASS_NAME;
		command = getInput().getString(ADB_INIT_PARAM_NAME) + " shell am instrument -w " + classParam;
		result = ShellAccess.execCommandBlocking(command);
		log("Launch testing app", result);
		logFinished();
	}
	
}
