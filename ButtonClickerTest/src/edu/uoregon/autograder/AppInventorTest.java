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

public class AppInventorTest extends ActivityInstrumentationTestCase2 {

	private Solo solo;

	private static final String TARGET_PACKAGE_ID = "appinventor.ai_nchaimov.stm_training";
	private static final String LAUNCHER_ACTIVITY_FULL_CLASSNAME = "appinventor.ai_nchaimov.stm_training.Screen1";
	private static Class launcherActivityClass;

	static {
		try {
			launcherActivityClass = Class
					.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public AppInventorTest() throws ClassNotFoundException {
		super(launcherActivityClass);
	}

	protected Label wrong = null;
	protected Label right = null;
	protected Label skipped = null;
	protected Label target = null;
	protected Random rand;

	public void setUp() throws Exception {
		solo = new Solo(getInstrumentation(), getActivity());
		rand = new Random();
		final Activity currentActivity = solo.getCurrentActivity();

		Class<? extends Activity> activityClass = currentActivity.getClass();
		try {
			Field fieldWrong = activityClass.getField("LabelWrong");
			Field fieldRight = activityClass.getField("LabelRight");
			Field fieldSkipped = activityClass.getField("LabelSkipped");
			Field fieldTarget = activityClass.getField("LabelTarget");
			wrong = (Label) fieldWrong.get(currentActivity);
			right = (Label) fieldRight.get(currentActivity);
			skipped = (Label) fieldSkipped.get(currentActivity);
			target = (Label) fieldTarget.get(currentActivity);
		} catch (NoSuchFieldException e) {
			Assert.fail(MessageFormat.format(
					"A required field was not found: {0}", e.getMessage()));
		}
	}

	/**
	 * @throws Exception
	 */
	public void testAllFieldsExist() throws Exception {

		Assert.assertNotNull(wrong);
		Assert.assertNotNull(right);
		Assert.assertNotNull(skipped);
		Assert.assertNotNull(target);

		Log.d("robotium", "*** TARGET: " + target.Text());
		Log.d("robotium", "*** RIGHT: " + right.Text());
		Log.d("robotium", "*** WRONG: " + wrong.Text());
		Log.d("robotium", "*** SKIPPED: " + skipped.Text());
	}

	public void testSkippedAnswers() throws Exception {
		for (int i = 0; i < 20; ++i) {
			int beforeSkipped = Integer.parseInt(skipped.Text());
			int beforeRight = Integer.parseInt(right.Text());
			int beforeWrong = Integer.parseInt(wrong.Text());

			solo.clickOnButton("SKIP");
			int afterSkipped = Integer.parseInt(skipped.Text());
			int afterRight = Integer.parseInt(right.Text());
			int afterWrong = Integer.parseInt(wrong.Text());

			Assert.assertEquals("Skipping increments skipped count",
					beforeSkipped + 1, afterSkipped);
			Assert.assertEquals("Skipping leaves right count unchanged",
					beforeRight, afterRight);
			Assert.assertEquals("Skipping leaves wrong count unchanged",
					beforeWrong, afterWrong);
		}
	}

	public void testCorrectAnswers() throws Exception {
		for (int i = 0; i < 20; ++i) {
			int beforeTarget = Integer.parseInt(target.Text());
			int beforeRight = Integer.parseInt(right.Text());
			int beforeWrong = Integer.parseInt(wrong.Text());
			int beforeSkipped = Integer.parseInt(skipped.Text());
			solo.clickOnButton(Integer.toString(beforeTarget));
			int afterRight = Integer.parseInt(right.Text());
			int afterWrong = Integer.parseInt(wrong.Text());
			int afterSkipped = Integer.parseInt(skipped.Text());
			Assert.assertEquals(
					"Clicking correct button increments correct count",
					beforeRight + 1, afterRight);
			Assert.assertEquals(
					"Clicking correct button leaves wrong count unchanged",
					beforeWrong, afterWrong);
			Assert.assertEquals(
					"Clicking correct button leaves skipped count unchanged",
					beforeSkipped, afterSkipped);
		}
	}

	public void testIncorrectAnswers() throws Exception {
		for (int i = 0; i < 20; ++i) {
			int beforeTarget = Integer.parseInt(target.Text());
			int beforeRight = Integer.parseInt(right.Text());
			int beforeWrong = Integer.parseInt(wrong.Text());
			int beforeSkipped = Integer.parseInt(skipped.Text());
			int randomNum = rand.nextInt(6 - 1 + 1) + 1;
			while (randomNum == beforeTarget) {
				randomNum = rand.nextInt(6 - 1 + 1) + 1;
			}
			solo.clickOnButton(Integer.toString(randomNum));
			int afterRight = Integer.parseInt(right.Text());
			int afterWrong = Integer.parseInt(wrong.Text());
			int afterSkipped = Integer.parseInt(skipped.Text());
			Assert.assertEquals(
					"Clicking incorrect button leaves correct count unchanged",
					beforeRight, afterRight);
			Assert.assertEquals(
					"Clicking incorrect button increments wrong count",
					beforeWrong + 1, afterWrong);
			Assert.assertEquals(
					"Clicking incorrect button leaves skipped count unchanged",
					beforeSkipped, afterSkipped);
		}
	}

	@Override
	public void tearDown() throws Exception {
		solo.finishOpenedActivities();
	}
}