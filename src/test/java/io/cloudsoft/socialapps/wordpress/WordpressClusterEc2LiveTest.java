package io.cloudsoft.socialapps.wordpress;


import java.util.Arrays;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.core.sensor.DependentConfiguration;
import org.apache.brooklyn.entity.AbstractEc2LiveTest;
import org.apache.brooklyn.entity.database.mysql.MySqlNode;
import org.apache.brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import org.apache.brooklyn.test.HttpTestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class WordpressClusterEc2LiveTest extends AbstractEc2LiveTest {

    /*
     * Need CustomNginxController for the custom proxy_set_header
     * See http://zeroturnaround.com/labs/wordpress-protips-go-with-a-clustered-approach/#!/
     *     proxy_set_header Host $host;
     *     proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
     *     proxy_set_header X-Real-IP $remote_addr;
     */

    final static String SCRIPT = WordpressEc2LiveTest.SCRIPT;

    private ControlledDynamicWebAppCluster cluster;

    @Override
    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected void doTest(Location loc) throws Exception {
        MySqlNode mysql = app.createAndManageChild(EntitySpec.create(MySqlNode.class)
                .configure("creationScriptContents", SCRIPT));

        cluster = app.createAndManageChild(EntitySpec.create(ControlledDynamicWebAppCluster.class)
                .configure(ControlledDynamicWebAppCluster.INITIAL_SIZE, 2)
                .configure(ControlledDynamicWebAppCluster.MEMBER_SPEC, EntitySpec.create(Wordpress.class)
                        .configure(Wordpress.DATABASE_UP, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.SERVICE_UP))
                        .configure(Wordpress.DATABASE_HOSTNAME, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.HOSTNAME))
                        .configure(Wordpress.DATABASE_NAME, "wordpress")
                        .configure(Wordpress.DATABASE_USER, "wordpress")
                        .configure(Wordpress.DATABASE_PASSWORD, "password")
                        .configure(Wordpress.WEBLOG_TITLE, "my custom title")
                        .configure(Wordpress.WEBLOG_ADMIN_EMAIL, "aled.sage@gmail.com")));

        app.start(Arrays.asList(loc));

        for (Entity wordpress : cluster.getCluster().getMembers()) {
            String wordpressUrl = wordpress.getAttribute(Wordpress.ROOT_URL);
            HttpTestUtils.assertContentEventuallyContainsText(wordpressUrl, "my custom title");
        }

        String rootUrl = cluster.getAttribute(Wordpress.ROOT_URL);
        HttpTestUtils.assertContentEventuallyContainsText(rootUrl, "my custom title");
    }

    // Convenience for easily running just this one test from Eclipse
    @Override
    @Test(groups = {"Live"})
    public void test_CentOS_6_3() throws Exception {
        super.test_CentOS_6_3();
    }

    @Test(enabled = false)
    public void testDummy() {
    } // Convince testng IDE integration that this really does have test methods
}
