package edu.uoregon.autograder.android.task;

import edu.uoregon.autograder.model.Grader;
import edu.uoregon.autograder.model.GraderTask;
import edu.uoregon.autograder.util.ShellAccess;
import edu.uoregon.autograder.util.ShellOutputError;

/**
 * This Task implementation launches an application in the emulator. See
 * the inline comments for the equivalent adb command that this runs given
 * inputs for an action and class.
 *
 * @author Kurt Mueller
 */
public class LaunchInEmulatorTask extends GraderTask {
	
	public static final String TASK_NAME = "LaunchAppInEmulatorTask";
	
	public static final String ADB_UNLOCK_SCREEN_COMMAND = " shell input keyevent 82";
	
	public static final String ADB_INIT_PARAM_NAME = "ADB_PATH";	// how do we run adb? defined in web.xml
	public static final String ACTION_PARAM_NAME = "action";	// from index.html
	public static final String ACTION_DEFAULT = "android.intent.action.MAIN"; 
	public static final String CLASS_PARAM_NAME = "class";		// from index.html
	// for BirdExplosion, it's appinventor.ai_campusreaderapp.BirdExplosion/.Screen1
	// so: appinventor.ai_<username>.<appname>/.Screen1
	
	// here's how to run the browser through the Android shell:
	// adb shell am start -a android.intent.action.MAIN -n com.android.browser/.BrowserActivity
	// -a refers to <action android:value="android.intent.action.MAIN" /> in AndroidManifest.xml
	// -n refers to <package> + activity class from AndroidManifest.xml
	
	public LaunchInEmulatorTask(Grader grader) {
		super(TASK_NAME, grader);
	}
	
	public void doTask() {
		logStarted();
		// first unlock the screen
		String command = getInput().getString(ADB_INIT_PARAM_NAME) + ADB_UNLOCK_SCREEN_COMMAND;
		ShellOutputError result = ShellAccess.execCommandBlocking(command);
		log("Unlock emulator screen", result);
		// now launch the app
		String actionParam = getInput().getString(ACTION_PARAM_NAME) != null ? getInput().getString(ACTION_PARAM_NAME) : ACTION_DEFAULT;
		String classParam = getInput().getString(CLASS_PARAM_NAME);
		command = getInput().getString(ADB_INIT_PARAM_NAME) + " shell am start -a " + actionParam + " -n " + classParam;
		result = ShellAccess.execCommandBlocking(command);
		log("Launch app", result);
		logFinished();
	}

}
