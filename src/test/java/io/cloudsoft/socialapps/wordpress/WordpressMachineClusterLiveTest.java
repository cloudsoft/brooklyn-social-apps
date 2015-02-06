package io.cloudsoft.socialapps.wordpress;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.location.basic.FixedListMachineProvisioningLocation;
import brooklyn.location.basic.PortRanges;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.management.Task;
import brooklyn.test.HttpTestUtils;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.net.Networking;

public class WordpressMachineClusterLiveTest {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(WordpressMachineClusterLiveTest.class);

    // TODO Substitute for your own machine details here
    private String hostname1 = "ec2-23-22-180-129.compute-1.amazonaws.com";
    private String hostname2 = "ec2-54-234-220-127.compute-1.amazonaws.com";
    private String user = "aled";

    private SshMachineLocation machine1;
    private SshMachineLocation machine2;
    private FixedListMachineProvisioningLocation<SshMachineLocation> machinePool;
    private TestApplication app;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        InetAddress addr = Networking.getInetAddressWithFixedName(hostname1);
        InetAddress addr2 = Networking.getInetAddressWithFixedName(hostname2);
        machine1 = new SshMachineLocation(MutableMap.of("user", user, "address", addr));//, SshMachineLocation.PRIVATE_KEY_FILE, "/Users/aled/.ssh/id_rsa"));
        machine2 = new SshMachineLocation(MutableMap.of("user", user, "address", addr2));//, SshMachineLocation.PRIVATE_KEY_FILE, "/Users/aled/.ssh/id_rsa"));
        machinePool = new FixedListMachineProvisioningLocation<SshMachineLocation>(MutableMap.of("machines", ImmutableList.of(machine1, machine2)));
        app = ApplicationBuilder.newManagedApp(TestApplication.class);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app.getManagementContext());
    }

    @Test(groups = "Live")
    public void testStartsAndStops() throws Exception {
        MySqlNode mysql = app.createAndManageChild(EntitySpec.create(MySqlNode.class)
                .configure(MySqlNode.CREATION_SCRIPT_CONTENTS, WordpressEc2LiveTest.SCRIPT)
                .configure(MySqlNode.MYSQL_PORT, PortRanges.fromInteger(3306)));

        NginxController nginx = app.createAndManageChild(EntitySpec.create(NginxController.class));

        ControlledDynamicWebAppCluster cluster = app.createAndManageChild(EntitySpec.create(ControlledDynamicWebAppCluster.class)
                .configure(ControlledDynamicWebAppCluster.CONTROLLER, nginx)
                .configure(ControlledDynamicWebAppCluster.INITIAL_SIZE, 2)
                .configure(ControlledDynamicWebAppCluster.MEMBER_SPEC, EntitySpec.create(Wordpress.class)
                        .configure(Wordpress.DATABASE_UP, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.SERVICE_UP))
                        .configure(Wordpress.DATABASE_HOSTNAME, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.HOSTNAME))
                        .configure(Wordpress.DATABASE_NAME, "wordpress")
                        .configure(Wordpress.DATABASE_USER, "wordpress")
                        .configure(Wordpress.DATABASE_PASSWORD, "password")
                        .configure(Wordpress.WEBLOG_TITLE, "my custom title")
                        .configure(Wordpress.WEBLOG_ADMIN_EMAIL, "aled.sage@gmail.com")));

        Task<Void> task1 = mysql.invoke(Startable.START, ImmutableMap.of("locations", ImmutableList.of(machine1)));
        Task<Void> task2 = nginx.invoke(Startable.START, ImmutableMap.of("locations", ImmutableList.of(machine1)));
        Task<Void> task3 = cluster.invoke(Startable.START, ImmutableMap.of("locations", ImmutableList.of(machinePool)));

        task1.get();
        task2.get();
        task3.get();

        String rootUrl = cluster.getAttribute(Wordpress.ROOT_URL);
        HttpTestUtils.assertContentEventuallyContainsText(rootUrl, "my custom title");

        for (Entity wordpress : cluster.getCluster().getMembers()) {
            String wordpressUrl = wordpress.getAttribute(Wordpress.ROOT_URL);
            HttpTestUtils.assertContentEventuallyContainsText(wordpressUrl, "my custom title");
        }

        cluster.stop();
        HttpTestUtils.assertHttpStatusCodeEventuallyEquals(rootUrl, 404);
    }
}
