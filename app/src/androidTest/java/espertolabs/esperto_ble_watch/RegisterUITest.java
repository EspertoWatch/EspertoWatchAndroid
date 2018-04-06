package espertolabs.esperto_ble_watch;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RegisterUITest {

    private String mFirstName;
    private String mLastName;
    private String mUsername;
    private String mPassword;
    private String mPasswordDup;


    @Rule
    public ActivityTestRule<RegisterActivity> mActivityRule = new ActivityTestRule<>(
            RegisterActivity.class);

  //  private RegisterActivity activity;
    @Before
    public void initValidString() {
        // Specify a valid string.
        mFirstName = "Espresso";
        mLastName = "Bean";
        mUsername = "ebean";
        mPassword = "coffee";
        mPasswordDup = "coffee";
    }

    @Test
    public void changeText_sameActivity() {
        // Type text and then press the button.
        // Test input fields

        onView(withId(R.id.first_name))
                .perform(typeText(mFirstName), closeSoftKeyboard()); //Note to self this is super cool
        onView(withId(R.id.last_name))
                .perform(typeText(mLastName), closeSoftKeyboard());
        onView(withId(R.id.username))
                .perform(typeText(mUsername), closeSoftKeyboard());
        onView(withId(R.id.password))
                .perform(typeText(mPassword), closeSoftKeyboard());
        onView(withId(R.id.password_dup))
                .perform(typeText(mPasswordDup), closeSoftKeyboard());

        // Check that the text was changed.
        onView(withId(R.id.first_name))
                .check(matches(withText(mFirstName)));
        // Check that the text was changed.
        onView(withId(R.id.last_name))
                .check(matches(withText(mLastName)));
        // Check that the text was changed.
        onView(withId(R.id.username))
                .check(matches(withText(mUsername)));
        // Check that the text was changed.
        onView(withId(R.id.password))
                .check(matches(withText(mPassword)));
        // Check that the text was changed.
        onView(withId(R.id.password_dup))
                .check(matches(withText(mPasswordDup)));
    }

    @Test
    public void checkGoalButton(){
        //click button - see if it changes the text
        onView(withId(R.id.enterGoalSetting)).perform(click());
        onView(withId(R.id.enterGoalSetting))
                .check(matches(withText(R.string.default_setting)));
    }
}