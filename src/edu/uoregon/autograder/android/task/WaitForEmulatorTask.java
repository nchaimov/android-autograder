package edu.uoregon.autograder.android.task;

import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.ShellAccess;
import edu.uoregon.autograder.util.ShellOutputError;

public class WaitForEmulatorTask extends GraderTask {

	public static final String TASK_NAME = "WaitForEmulatorTask";
	
	public static final String ADB_INIT_PARAM_NAME = "ADB_PATH";	// how do we run adb? defined in web.xml
	public static final String OUTPUT_ONLINE = "device";			// 'adb get-state' returns 'device' when emulator/device is up
	public static final String OUTPUT_OFFLINE = "unknown";
	
	public WaitForEmulatorTask(Grader grader) {
		super(TASK_NAME, grader);
	}
	
	public void doTask() {
		logStarted();
		// first kill adb server - wait is flaky unless this happens first
		String command = getInput().getString(ADB_INIT_PARAM_NAME) + " kill-server";
		ShellOutputError result = ShellAccess.execCommandBlocking(command);
		// now wait for device
		command = getInput().getString(ADB_INIT_PARAM_NAME) + " wait-for-device";
		result = ShellAccess.execCommandBlocking(command);
		System.out.println("WaitFor task:\n" + result.toString());
		//getOutput().setOutputAndError(getTaskName(), result);
		log("Waiting for emulator", result);
		logFinished();
	}

}
