package io.cloudsoft.socialapps.wordpress;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class WordpressUtilTest {

    @Test(groups="Integration") // Requires internet connection
    public void testFoo() throws Exception {
        String keys = WordpressUtil.getAuthenticationKeys();
        assertTrue(keys.contains("define('AUTH_KEY'"), "keys="+keys);
    }
}
