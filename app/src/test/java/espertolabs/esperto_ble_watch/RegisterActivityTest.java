package espertolabs.esperto_ble_watch;

import org.junit.Test;

import static org.junit.Assert.*;

public class RegisterActivityTest {
    @Test
    public void passwordValidTest() throws Exception {
        RegisterActivity activity = new RegisterActivity();
        assertEquals(activity.checkPassword("password","password"), true);
        assertFalse(activity.checkPassword("pass1", "pass2"));
    }
}
