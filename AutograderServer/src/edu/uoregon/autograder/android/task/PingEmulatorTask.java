package edu.uoregon.autograder.android.task;

import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.ShellAccess;
import edu.uoregon.autograder.util.ShellOutputError;

/**
 * This Task implementation executes a shell command to determine if the Android emulator is 
 * currently running. It sets a next task based on the outcome of the task.
 *
 * @author Kurt Mueller
 */
public class PingEmulatorTask extends GraderTask {

	public static final String TASK_NAME = "PingEmulatorTask";
	
	public static final String ADB_INIT_PARAM_NAME = "ADB_PATH";	// how do we run adb? defined in web.xml
	public static final String OUTPUT_ONLINE = "device";			// 'adb get-state' returns 'device' when emulator/device is up
	public static final String OUTPUT_OFFLINE = "unknown";
	
	// nextStep keys for GraderTask workflow
	public static final String PING_SUCCESS = "ping_success";
	public static final String PING_FAILURE = "ping_failure";
	
	public PingEmulatorTask(Grader grader) {
		super(TASK_NAME, grader);
	}
	
	public void doTask() {
		logStarted();
		String command = getInput().getString(ADB_INIT_PARAM_NAME) + " get-state";
		ShellOutputError result = ShellAccess.execCommandBlocking(command);
		//getOutput().setOutputAndError(getTaskName(), result);
		log("Ping the emulator", result);
		if (result.getOutput() != null && result.getOutput().equals(OUTPUT_ONLINE)) {
			setNextStep(PING_SUCCESS);
			logFinished();	// this will set task's status field to COMPLETED_NORMALLY, for use by workflow
		} else {
			setNextStep(PING_FAILURE);
			logError("Emulator is not responding");
		}
	}

}
