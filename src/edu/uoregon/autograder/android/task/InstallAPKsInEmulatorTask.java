package edu.uoregon.autograder.android.task;

import java.util.ArrayList;

import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.ShellAccess;
import edu.uoregon.autograder.util.ShellOutputError;

/**
 * @author Kurt Mueller
 * 
 * This Task implementation installs the App Inventor (tested) and the Robotium (tester/testing)
 * APKs in the Android emulator. More precisely, it installs all files ending in ".apk" in the
 * list of input files.
 */
public class InstallAPKsInEmulatorTask extends GraderTask{
	
	public static final String ADB_INIT_PARAM_NAME = "ADB_PATH";	
	public static final String TASK_NAME = "InstallAPKsInEmulatorTask";
	public static final String AVD_PARAM_NAME = "avd"; // as specified in the form in index.html
	
	// don't really want full path to emulator here, but exec doesn't get PATH info. This will do for now.
	public static final String ADB_COMMAND = " install ";
	
	public static final String NO_APK_ERROR = "Must upload an APK file!";
	
	public InstallAPKsInEmulatorTask(Grader grader) {
		super(TASK_NAME, grader);
	}

	public void doTask() {
		logStarted();
		ArrayList<String> apkFiles = getInput().getFilePathsByFileSuffix(".apk");
		if (!apkFiles.isEmpty()) {
			for (String apkFile: apkFiles) {

				// uninstall prior to install is now handled by separate UninstallAPKFromEmulatorTask
				String command = getInput().getString(ADB_INIT_PARAM_NAME) + ADB_COMMAND + apkFile;
				ShellOutputError result = ShellAccess.execCommandBlocking(command);
				//getOutput().setOutputAndError(getTaskName()+"-"+apkFile, result);
				log("Installing " + apkFile, result);
			}
			logFinished();
		} else {
			//getOutput().addErrorString(getTaskName(), NO_APK_ERROR);
			logError("No files found in input set");
		}


	}

}
