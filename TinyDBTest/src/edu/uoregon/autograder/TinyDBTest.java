package edu.uoregon.autograder;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Random;

import junit.framework.Assert;

import com.google.appinventor.components.runtime.Label;
import com.jayway.android.robotium.solo.Solo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
  	

public class TinyDBTest extends ActivityInstrumentationTestCase2 {

	private Solo solo;

	private static final String TARGET_PACKAGE_ID = "appinventor.ai_ntiller121.clicker_apk_check";
	private static final String LAUNCHER_ACTIVITY_FULL_CLASSNAME = "appinventor.ai_ntiller121.clicker_apk_check.Screen1";
	private static Class launcherActivityClass;


	static {
		try {
			launcherActivityClass = Class
					.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public TinyDBTest() throws ClassNotFoundException {
		super(launcherActivityClass);
	}

	protected Label value = null;
	protected Random rand;

	/**
	 * Sets up the Robotium tests. This is run after every test
	 * unless the test is formed into a test suite.
	 * 
	 * Defines what labels are being looked at. In our example, we just defined
	 * a Label called value and in setUp, we set it equal to the Label called LabelValue.
	 * The default names of the Labels are Label**** where **** is provided by the user.
	 * 
	 * @throws Exception
	 */
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
	 * This test is created to make certain that all defined Fields are not
	 * null. In this example, the only Field that we have defined is the Label
	 * value, so we assert that it is not null and then print out what text
	 * it holds. 
	 * 
	 * @throws Exception
	 */
	public void testAAllFieldsExist() throws Exception {

		Assert.assertNotNull(value);

		Log.d("robotium", "*** Value: " + value.Text());
	}
	
	/**
	 * This test checks whether a value exists in the TinyDB used by AppInventor.
	 * AppInventor's use of TinyDB is done by using SharedPreferences called "TinyDB".
	 * As a result, we can use reflection to get the SharedPreferences. The command 
	 * 
	 * 'this.getInstrumentation().getTargetContext().getSharedPreferences("TinyDB", 0)'
	 * 
	 * will return a reference to the SharedPreferences used by AppInventor. From there,
	 * you can attempt to get your value from the SharedPreferences. The value returned from
	 * TinyDB will always be a String including double-quotes at the beginning and end. So,
	 * if you saved 8 to the TinyDB, "8" will be returned. Keep that in mind when checking the
	 * values.
	 * 
	 * @throws Exception
	 */

	public void testBDatabase() throws Exception {
		String shownValue = value.Text();
		SharedPreferences sharedPreferences = this.getInstrumentation().getTargetContext().getSharedPreferences("TinyDB", 0);
		String storedValue = sharedPreferences.getString("clicker", "-1");
		storedValue = storedValue.substring(1, storedValue.length() - 1);
		Assert.assertEquals("Checking stored value",
				shownValue, storedValue);
	}
	
	/**
	 * This test runs through 3 times, first saving the text in our value Label (which is 
	 * LabelValue Field), clicking the CLICK button, and then checking the value of the 
	 * text in our value Label against its previous, saved version to verify that it is now
	 * 1 more than previously. So, the result is that if the CLICK button is clicked, the 
	 * LabelValue Field should increase by 1. 
	 * 
	 * Commented out is a call to click our QUIT button. However, this does not work. As of
	 * Robotium 3.2.1, Robotium does not support clicking a QUIT button. If, as in our case,
	 * you have a TinyDB write linked to the QUIT button, it is suggested that you instead
	 * update your TinyDB on another button click. 
	 * 
	 * @throws Exception
	 */
	public void testBFirstClicks() throws Exception {
		for (int i = 0; i < 3; ++i) {

			int beforeValue = Integer.parseInt(value.Text());

			solo.clickOnButton("CLICK");
		
			int afterValue = Integer.parseInt(value.Text());

			Assert.assertEquals("Clicking CLICK increments value count",
					beforeValue + 1, afterValue);
		}
		//solo.clickOnButton("QUIT");
		
	}
	/**
	 * A straight-forward test that simply clicks the RESET button and 
	 * verifies that the value now in the LabelValue is 0. 
	 * 
	 * @throws Exception
	 */
	public void testCClearClick() throws Exception {
		solo.clickOnButton("RESET");
		int afterValue  = Integer.parseInt(value.Text());
		Assert.assertEquals("clicking RESET clears the value", 0, afterValue);
	
	}

	

	/** 
	 * The tearDown method that is called at the end of every test (unless
	 * the test is part of a test suite). The basic tearDown simply finishes
	 * any of the open activities.
	 * 
	 * @throws Exception
	 */
	@Override
	public void tearDown() throws Exception {
		solo.finishOpenedActivities();
		
	}

}
