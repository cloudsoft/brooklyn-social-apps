package io.cloudsoft.socialapps.drupal;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

import java.util.Arrays;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.location.Location;
import brooklyn.test.HttpTestUtils;
import brooklyn.test.entity.TestApplication;

public class DrupalLiveTest {

    private final static String SCRIPT = "create database drupal; " +
            "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES ON drupal.* TO 'drupal'@'localhost'  IDENTIFIED BY 'password'; " +
            "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES ON drupal.* TO 'drupal'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES ON drupal.* TO 'drupal'@'%'  IDENTIFIED BY 'password';" +
            "FLUSH PRIVILEGES;";
    
    private TestApplication app;
    private Location location;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        app = ApplicationBuilder.newManagedApp(TestApplication.class);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (app != null) Entities.destroyAll(app);
    }

    @Test(groups = "Live")
    public void test() {
        MySqlNode mySqlNode = app.createAndManageChild(BasicEntitySpec.newInstance(MySqlNode.class)
                .configure(MySqlNode.CREATION_SCRIPT_CONTENTS, SCRIPT));

        Drupal drupal = app.createAndManageChild(BasicEntitySpec.newInstance(Drupal.class)
                .configure(Drupal.DATABASE_PORT, attributeWhenReady(mySqlNode, MySqlNode.MYSQL_PORT))
                .configure(Drupal.DATABASE_UP, attributeWhenReady(mySqlNode, MySqlNode.SERVICE_UP))
                .configure(Drupal.DATABASE_HOST, attributeWhenReady(mySqlNode, MySqlNode.HOSTNAME))
                .configure(Drupal.DATABASE_SCHEMA, "drupal")
                .configure(Drupal.DATABASE_USER, "drupal")
                .configure(Drupal.DATABASE_PASSWORD, "password")
                .configure(Drupal.ADMIN_EMAIL, "foo@bar.com"));

        location = app.getManagementContext().getLocationRegistry().resolve("aws-ec2:us-east-1");
        app.start(Arrays.asList(location));

        HttpTestUtils.assertContentEventuallyContainsText("http://" + drupal.getAttribute(Drupal.HOSTNAME) + "/index.php", "Welcome");
    }
}

