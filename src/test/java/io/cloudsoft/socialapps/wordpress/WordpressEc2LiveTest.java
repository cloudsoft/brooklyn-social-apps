package io.cloudsoft.socialapps.wordpress;


import static org.testng.Assert.assertTrue;

import java.util.Arrays;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.core.sensor.DependentConfiguration;
import org.apache.brooklyn.entity.AbstractEc2LiveTest;
import org.apache.brooklyn.entity.database.mysql.MySqlNode;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.test.HttpTestUtils;
import org.testng.annotations.Test;

public class WordpressEc2LiveTest extends AbstractEc2LiveTest {

    final static String SCRIPT = "create database wordpress; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'localhost'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'%'  IDENTIFIED BY 'password';" +
            "flush privileges;";

    @Override
    protected void doTest(Location loc) throws Exception {
        MySqlNode mysql = app.createAndManageChild(EntitySpec.create(MySqlNode.class)
                .configure("creationScriptContents", SCRIPT));

        final Wordpress wordpress = app.createAndManageChild(EntitySpec.create(Wordpress.class)
                .configure(Wordpress.DATABASE_UP, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.SERVICE_UP))
                .configure(Wordpress.DATABASE_HOSTNAME, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.HOSTNAME))
                .configure(Wordpress.DATABASE_NAME, "wordpress")
                .configure(Wordpress.DATABASE_USER, "wordpress")
                .configure(Wordpress.DATABASE_PASSWORD, "password")
                .configure(Wordpress.WEBLOG_TITLE, "my custom title")
                .configure(Wordpress.WEBLOG_ADMIN_EMAIL, "aled.sage@gmail.com"));

        app.start(Arrays.asList(loc));

        final String url = wordpress.getAttribute(Wordpress.ROOT_URL);
        HttpTestUtils.assertContentEventuallyContainsText(url, "my custom title");

        // Should get request count
        Asserts.succeedsEventually(new Runnable() {
            @Override
            public void run() {
                Integer count = wordpress.getAttribute(Wordpress.REQUEST_COUNT);
                assertTrue(count != null && count > 0, "count=" + count);
            }
        });

        // Should get an average request count (we drive some load to stimulate this as well)
        Asserts.succeedsEventually(new Runnable() {
            @Override
            public void run() {
                HttpTestUtils.assertHttpStatusCodeEquals(url, 200);
                Double avg = wordpress.getAttribute(Wordpress.REQUESTS_PER_SECOND_IN_WINDOW);
                assertTrue(avg != null && avg > 0, "avg=" + avg);
            }
        });
    }

    // Convenience for easily running just this one test from Eclipse.
    // Note not all distros are supported; CentOS is.
    @Override
    @Test(groups = {"Live"})
    public void test_CentOS_6_3() throws Exception {
        super.test_CentOS_6_3();
    }
    
    // TODO Fails because /etc/init.d/httpd not found; see fixme comments at top of WordpressSshDriver
    @Override
    @Test(groups = {"Live"}, enabled=false)
    public void test_Ubuntu_12_0() throws Exception {
        super.test_Ubuntu_12_0();
    }
}
