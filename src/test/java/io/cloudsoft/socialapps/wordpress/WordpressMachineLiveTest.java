package io.cloudsoft.socialapps.wordpress;

import static org.testng.Assert.assertTrue;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.location.basic.PortRanges;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.management.Task;
import brooklyn.test.HttpTestUtils;
import brooklyn.test.TestUtils;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.MutableMap;
import brooklyn.util.NetworkUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class WordpressMachineLiveTest {

    private static final Logger LOG = LoggerFactory.getLogger(WordpressMachineLiveTest.class);

    // TODO Substitute for your own machine details here
    private String hostname1 = "ec2-23-22-180-129.compute-1.amazonaws.com";
    private String user = "aled";
    
    private SshMachineLocation machine1;
    private TestApplication app;

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        InetAddress addr = NetworkUtils.getInetAddressWithFixedName(hostname1);
        machine1 = new SshMachineLocation(MutableMap.of("user", user, "address", addr));//, SshMachineLocation.PRIVATE_KEY_FILE, "/Users/aled/.ssh/id_rsa"));
        app = ApplicationBuilder.newManagedApp(TestApplication.class);
    }
    
    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app);
    }
    
    @Test(groups="Live")
    public void testStartsAndStops() throws Exception {
        final MySqlNode mysql = app.createAndManageChild(BasicEntitySpec.newInstance(MySqlNode.class)
                .configure(MySqlNode.CREATION_SCRIPT_CONTENTS, WordpressEc2LiveTest.SCRIPT)
                .configure(MySqlNode.MYSQL_PORT, PortRanges.fromInteger(3306)));

        final Wordpress wordpress = app.createAndManageChild(BasicEntitySpec.newInstance(Wordpress.class)
                .configure(Wordpress.DATABASE_UP, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.SERVICE_UP))
                .configure(Wordpress.DATABASE_HOSTNAME, "127.0.0.1")
                .configure(Wordpress.DATABASE_NAME, "wordpress")
                .configure(Wordpress.DATABASE_USER, "wordpress")
                .configure(Wordpress.DATABASE_PASSWORD, "password")
                .configure(Wordpress.WEBLOG_TITLE, "my custom title")
                .configure(Wordpress.WEBLOG_ADMIN_EMAIL, "aled.sage@gmail.com"));
        
        Task<Void> task1 = mysql.invoke(Startable.START, ImmutableMap.of("locations", ImmutableList.of(machine1)));
        Task<Void> task2 = wordpress.invoke(Startable.START, ImmutableMap.of("locations", ImmutableList.of(machine1)));
        
        task1.get();
        task2.get();
        
        final String url = wordpress.getAttribute(Wordpress.ROOT_URL);
        
        // Should get back our wordpress blog
        HttpTestUtils.assertContentEventuallyContainsText(url, "my custom title");

        // Should get request count
        TestUtils.executeUntilSucceeds(new Runnable() {
            @Override public void run() {
                Integer count = wordpress.getAttribute(Wordpress.REQUEST_COUNT);
                assertTrue(count != null && count > 0, "count="+count);
            }});

        // Should get an average request count (we drive some load to stimulate this as well)
        TestUtils.executeUntilSucceeds(new Runnable() {
            @Override public void run() {
                HttpTestUtils.assertHttpStatusCodeEquals(url, 200);
                Double avg = wordpress.getAttribute(Wordpress.AVG_REQUESTS_PER_SECOND);
                assertTrue(avg != null && avg > 0, "avg="+avg);
            }});

        wordpress.stop();
        HttpTestUtils.assertUrlUnreachable(url);
    }
}
