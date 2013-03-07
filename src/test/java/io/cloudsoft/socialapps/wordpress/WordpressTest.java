package io.cloudsoft.socialapps.wordpress;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.basic.Entities;
import brooklyn.test.entity.TestApplicationImpl;

public class WordpressTest {

    private TestApplicationImpl app;
    private WordpressImpl entity;

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        app = new TestApplicationImpl();
        entity = new WordpressImpl();
        app.addChild(entity);
        Entities.startManagement(app);
    }
    
    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app);
    }
    
    @Test
    public void testGenerateAuthenticationKeys() throws Exception {
        String keys = entity.getAuthenticationKeys();
        assertTrue(keys.contains("define('AUTH_KEY'"), "keys="+keys);
    }
}
