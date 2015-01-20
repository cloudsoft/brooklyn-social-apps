package io.cloudsoft.socialapps.drupal;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

import java.util.Arrays;
import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.test.HttpTestUtils;
import brooklyn.test.entity.TestApplication;

import com.google.common.collect.ImmutableMap;

public class DrupalEc2LiveTest {

    private final static String SCRIPT = "create database drupal; " +
            "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES ON drupal.* TO 'drupal'@'localhost'  IDENTIFIED BY 'password'; " +
            "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES ON drupal.* TO 'drupal'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES ON drupal.* TO 'drupal'@'%'  IDENTIFIED BY 'password';" +
            "FLUSH PRIVILEGES;";

    // Image: {id=us-east-1/ami-d0f89fb9, providerId=ami-d0f89fb9, name=ubuntu/images/ebs/ubuntu-precise-12.04-amd64-server-20130411.1, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=ubuntu, arch=paravirtual, version=12.04, description=099720109477/ubuntu/images/ebs/ubuntu-precise-12.04-amd64-server-20130411.1, is64Bit=true}, description=099720109477/ubuntu/images/ebs/ubuntu-precise-12.04-amd64-server-20130411.1, version=20130411.1, status=AVAILABLE[available], loginUser=ubuntu, userMetadata={owner=099720109477, rootDeviceType=ebs, virtualizationType=paravirtual, hypervisor=xen}}
    private static final String LOCATION_SPEC = "aws-ec2:us-east-1";
    private static final Map<String, String> LOCATION_FLAGS = ImmutableMap.of("imageId", "us-east-1/ami-d0f89fb9", "loginUser", "ubuntu", "hardwareId", "m1.small");
    
    private TestApplication app;
    private Location location;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        app = ApplicationBuilder.newManagedApp(TestApplication.class);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (app != null) Entities.destroyAll(app.getManagementContext());
    }

    @Test(groups = "Live")
    public void test() {
        MySqlNode mySqlNode = app.createAndManageChild(EntitySpec.create(MySqlNode.class)
                .configure(MySqlNode.CREATION_SCRIPT_CONTENTS, SCRIPT));

        Drupal drupal = app.createAndManageChild(EntitySpec.create(Drupal.class)
                .configure(Drupal.DATABASE_PORT, attributeWhenReady(mySqlNode, MySqlNode.MYSQL_PORT))
                .configure(Drupal.DATABASE_UP, attributeWhenReady(mySqlNode, MySqlNode.SERVICE_UP))
                .configure(Drupal.DATABASE_HOST, attributeWhenReady(mySqlNode, MySqlNode.HOSTNAME))
                .configure(Drupal.DATABASE_SCHEMA, "drupal")
                .configure(Drupal.DATABASE_USER, "drupal")
                .configure(Drupal.DATABASE_PASSWORD, "password")
                .configure(Drupal.ADMIN_EMAIL, "foo@bar.com"));

        location = app.getManagementContext().getLocationRegistry().resolve(LOCATION_SPEC, LOCATION_FLAGS);
        app.start(Arrays.asList(location));

        HttpTestUtils.assertContentEventuallyContainsText("http://" + drupal.getAttribute(Drupal.HOSTNAME) + "/index.php", "Welcome");
    }
}

