package edu.uoregon.autograder.android.task;

import edu.uoregon.autograder.appinventor.task.AIConstants;
import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.ShellAccess;
import edu.uoregon.autograder.util.ShellOutputError;


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
			//getOutput().setOutputAndError(getTaskName()+" - " + testedPackage, result);
			log("Uninstalling app APK", result);
		} else {
			//getOutput().addErrorString(TASK_NAME, NO_TESTED_PACKAGE_ERROR);
			logError("No app APK found in input data");
			return;
		}
		
		// uninstall robotium package next
		String robotiumPackage = getInput().getString(AIConstants.ROBOTIUM_APK_PACKAGE_KEY);
		if (robotiumPackage != null) {
			command = getInput().getString(ADB_INIT_PARAM_NAME) + ADB_COMMAND + robotiumPackage;
			ShellOutputError result = ShellAccess.execCommandBlocking(command);
			//getOutput().setOutputAndError(getTaskName()+" - " + robotiumPackage, result);
			log("Uninstalling tester APK", result);
		} else {
			//getOutput().addErrorString(TASK_NAME, NO_ROBOTIUM_PACKAGE_ERROR);
			logError("No tester APK found in input data");
			return;
		}
		logFinished();
	}

}
