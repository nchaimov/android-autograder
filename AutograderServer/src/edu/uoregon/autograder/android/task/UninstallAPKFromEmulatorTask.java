package edu.uoregon.autograder.android.task;

import edu.uoregon.autograder.appinventor.task.AIConstants;
import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.ShellAccess;
import edu.uoregon.autograder.util.ShellOutputError;


/**
 * This Task implementation tries to uninstall the App Inventor (tested) and Robotium (tester/testing)
 * APKs from the Android emulator. This task would typically be performed after testing is complete,
 * to clean the emulator in preparation for future tests. It can also be performed before installing
 * the APKs in the emulator, as a precaution.
 * 
 * In the long run, it would be good to have a task that reliably and completely restores the emulator
 * to some pristine state. This Task tries to do that, but there are many ways in which it could fail,
 * such as App Inventor files changing names between test runs so that old files could be left around
 * and interfere with testing of new files.
 *
 * @author Kurt Mueller
 */
public class UninstallAPKFromEmulatorTask extends GraderTask {
	
	public static final String ADB_INIT_PARAM_NAME = "ADB_PATH";	
	public static final String TASK_NAME = "UninstallAPKsFromEmulatorTask";
	
	public static final String NO_TESTED_PACKAGE_ERROR = "no tested apk package name found in input";
	public static final String NO_ROBOTIUM_PACKAGE_ERROR = "no robotium apk package name found in input";
	
	public static final String ADB_COMMAND = " uninstall ";
	
	public UninstallAPKFromEmulatorTask(Grader grader) {
		super(TASK_NAME, grader);
	}

	public void doTask() {
		logStarted();
		String command;
		// uninstall tested package first
		String testedPackage = getInput().getString(AIConstants.TESTED_APK_PACKAGE_KEY);
		if (testedPackage != null) {
			command = getInput().getString(ADB_INIT_PARAM_NAME) + ADB_COMMAND + testedPackage;
			ShellOutputError result = ShellAccess.execCommandBlocking(command);
			log("Uninstalling app APK", result);
		} else {
			logError("No app APK found in input data");
			return;
		}
		
		// uninstall robotium package next
		String robotiumPackage = getInput().getString(AIConstants.ROBOTIUM_APK_PACKAGE_KEY);
		if (robotiumPackage != null) {
			command = getInput().getString(ADB_INIT_PARAM_NAME) + ADB_COMMAND + robotiumPackage;
			ShellOutputError result = ShellAccess.execCommandBlocking(command);
			log("Uninstalling tester APK", result);
		} else {
			logError("No tester APK found in input data");
			return;
		}
		logFinished();
	}

}
