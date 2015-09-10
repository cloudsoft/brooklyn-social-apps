package io.cloudsoft.socialapps.wordpress.examples;

import java.util.List;

import org.apache.brooklyn.api.catalog.Catalog;
import org.apache.brooklyn.api.catalog.CatalogConfig;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.entity.AbstractApplication;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.StartableApplication;
import org.apache.brooklyn.core.sensor.DependentConfiguration;
import org.apache.brooklyn.entity.database.mysql.MySqlNode;
import org.apache.brooklyn.launcher.BrooklynLauncher;
import org.apache.brooklyn.util.CommandLineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.cloudsoft.socialapps.wordpress.Wordpress;

@Catalog(name = "Simple WordPress",
        description = "WordPress - the free and open source blogging tool and a content management system",
        iconUrl = "http://www.wordpress.org/about/images/logos/wordpress-logo-notext-rgb.png")
public class BasicWordpressApp extends AbstractApplication {

    // TODO Currently only works on CentOS or RHEL

    public static final Logger log = LoggerFactory.getLogger(BasicWordpressApp.class);
    
    public static String DEFAULT_LOCATION_SPEC = "jclouds:softlayer:dal06";

    @CatalogConfig(label="Weblog admin e-mail")
    public static final ConfigKey<String> WEBLOG_ADMIN_EMAIL = ConfigKeys.newConfigKeyWithDefault(
            Wordpress.WEBLOG_ADMIN_EMAIL, "foo@example.com");
    
    @CatalogConfig(label="Weblog admin password")
    public static final ConfigKey<String> WEBLOG_ADMIN_PASSWORD = ConfigKeys.newConfigKeyWithDefault(
            Wordpress.WEBLOG_ADMIN_PASSWORD, "pa55w0rd");

    static final String SCRIPT = "create database wordpress; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'localhost'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'%'  IDENTIFIED BY 'password';" +
            "flush privileges;";

    @Override
    public void init() {
        MySqlNode mysql = addChild(EntitySpec.create(MySqlNode.class)
                .configure("creationScriptContents", SCRIPT));

        Wordpress wordpress = addChild(EntitySpec.create(Wordpress.class)
                        .configure(Wordpress.DATABASE_UP, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.SERVICE_UP))
                        .configure(Wordpress.DATABASE_HOSTNAME, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.HOSTNAME))
                        .configure(Wordpress.DATABASE_NAME, "wordpress")
                        .configure(Wordpress.DATABASE_USER, "wordpress")
                        .configure(Wordpress.DATABASE_PASSWORD, "password")
                        .configure(Wordpress.WEBLOG_TITLE, "Welcome to WordPress, installed by Brooklyn!")
                        .configure(Wordpress.WEBLOG_ADMIN_EMAIL, getConfig(WEBLOG_ADMIN_EMAIL))
                        .configure(Wordpress.WEBLOG_ADMIN_PASSWORD, getConfig(WEBLOG_ADMIN_PASSWORD))
                        .configure(Wordpress.USE_W3_TOTAL_CACHE, true)
        );
    }

    public static void main(String[] argv) throws Exception {
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(EntitySpec.create(StartableApplication.class, BasicWordpressApp.class)
                        .displayName("Simple wordpress app"))
                .webconsolePort(port)
                .location(DEFAULT_LOCATION_SPEC)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}
