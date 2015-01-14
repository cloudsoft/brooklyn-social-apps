package io.cloudsoft.socialapps.wordpress;

import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

public class WordpressUtilTest {

    @Test(groups="Integration") // Requires internet connection
    public void testFoo() throws Exception {
        // TODO Assert content as well
        String result = WordpressUtil.getAuthenticationKeys();
        assertNotNull(result);
    }
}
