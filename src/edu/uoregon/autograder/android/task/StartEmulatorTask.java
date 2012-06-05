package edu.uoregon.autograder.android.task;

import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.ShellAccess;
import edu.uoregon.autograder.util.ShellOutputError;

public class StartEmulatorTask extends GraderTask{
	
	public static final String TASK_NAME = "StartEmulatorTask";
	public static final String AVD_PARAM_NAME = "avd"; // as specified in the form in index.html
	public static final String DEFAULT_AVD_INIT_PARAM_NAME = "DEFAULT_AVD_NAME";
	public static final String HEADLESS_PARAM_NAME = "headless"; // as specified in the form in index.html
	
	public static final String EMULATOR_INIT_PARAM_NAME = "EMULATOR_PATH";	// how do we start the emulator? defined in web.xml
	public static final String OPTION_HEADLESS = " -no-window";
	
	public static final String NO_AVD_ERROR = "Must specify an AVD in the form";
	
	public StartEmulatorTask(Grader grader) {
		super(TASK_NAME, grader);
	}
	
	public void doTask() {
		logStarted();
		String avdName = getInput().getString(AVD_PARAM_NAME);
		if (avdName == null)
			avdName = getInput().getString(DEFAULT_AVD_INIT_PARAM_NAME);
		
		if (avdName != null) {
			String command = getInput().getString(EMULATOR_INIT_PARAM_NAME) + " -avd " + avdName;
			if (getInput().getString(HEADLESS_PARAM_NAME) != null) command += OPTION_HEADLESS;
			ShellOutputError result = ShellAccess.execCommandNoBlocking(command);
			//getOutput().setOutputAndError(getTaskName(), result);
			log("Starting emulator", result);
			logFinished();
		} else {
			//getOutput().addString(getTaskName() + "-error", NO_AVD_ERROR);
			logError("No AVD found; check configuration and make sure that an AVD is specified in web.xml or passed in through the web interface.");
		}
		
	}

}
