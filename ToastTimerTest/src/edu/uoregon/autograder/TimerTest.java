package edu.uoregon.autograder;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Random;

import junit.framework.Assert;
import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.google.appinventor.components.runtime.Label;
import com.jayway.android.robotium.solo.Solo;
  	

public class TimerTest extends ActivityInstrumentationTestCase2 {

	private Solo solo;

	private static final String TARGET_PACKAGE_ID = "appinventor.ai_ntiller121.mock_midterm";
	private static final String LAUNCHER_ACTIVITY_FULL_CLASSNAME = "appinventor.ai_ntiller121.mock_midterm.Screen1";
	private static Class launcherActivityClass;
	
	private boolean closeApp = true;

	static {
		try {
			launcherActivityClass = Class
					.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public TimerTest() throws ClassNotFoundException {
		super(launcherActivityClass);
	}

	protected Label value = null;
	protected Random rand;

	public void setUp() throws Exception {
		solo = new Solo(getInstrumentation(), getActivity());
		final Activity currentActivity = solo.getCurrentActivity();

		Class<? extends Activity> activityClass = currentActivity.getClass();
		try {
			Field fieldValue = activityClass.getField("LabelValue");
			value = (Label) fieldValue.get(currentActivity);
		} catch (NoSuchFieldException e) {
			Assert.fail(MessageFormat.format(
					"A required field was not found: {0}", e.getMessage()));
		}
	}

	/**
	 * @throws Exception
	 */
	public void testAAllFieldsExist() throws Exception {

		Assert.assertNotNull(value);

		Log.d("robotium", "*** Value: " + value.Text());
	}

	
	/**
	 * This checks for a toast saying "keep playing" case-insensitive (this was
	 * made case-insensitive by adding (?i) to the beginning of our search).
	 * This does not check specifically for a toast, but instead for the text 
	 * to occur somewhere on the page. 
	 * 
	 * After finding it, it then waits 3 seconds to see if it sees it again in this
	 * time.
	 * 
	 * @throws Exception
	 */
	
	public void testBToastCheck() throws Exception {
		if(solo.waitForText("(?i)Keep playing.*")) {
			Assert.assertTrue("Did not find Keep Playing in 3 seconds", solo.waitForText("(?i)Keep playing.*", 1, 3000));
		} else {
			Assert.fail("Never saw Keep Playing");
		}
	}
	


	



	@Override
	public void tearDown() throws Exception {
		Log.d("ROBOTIUM", "Tearing down. CloseApp is: " + closeApp);
		solo.finishOpenedActivities();
		
	}

}
