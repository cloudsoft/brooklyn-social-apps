package io.cloudsoft.socialapps.wordpress.examples;

import io.cloudsoft.socialapps.wordpress.Wordpress;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.catalog.Catalog;
import brooklyn.catalog.CatalogConfig;
import brooklyn.config.BrooklynProperties;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.util.CommandLineUtil;

import com.google.common.collect.Lists;

@Catalog(name="Simple WordPress", 
        description="WordPress - the free and open source blogging tool and a content management system",
        iconUrl="http://www.wordpress.org/about/images/logos/wordpress-logo-notext-rgb.png")
public class BasicWordpressApp extends AbstractApplication {
    
    // TODO Currently only works on CentOS or RHEL
    
    public static final Logger log = LoggerFactory.getLogger(BasicWordpressApp.class);

    @CatalogConfig(label="Weblog admin e-mail")
    public static final ConfigKey<String> WEBLOG_ADMIN_EMAIL = ConfigKeys.newConfigKeyWithDefault(
            Wordpress.WEBLOG_ADMIN_EMAIL, "foo@example.com");
    
    @CatalogConfig(label="Weblog admin password")
    public static final ConfigKey<String> WEBLOG_ADMIN_PASSWORD = ConfigKeys.newConfigKeyWithDefault(
            Wordpress.WEBLOG_ADMIN_PASSWORD, "pa55w0rd");
    
    final static String SCRIPT = "create database wordpress; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'localhost'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'%'  IDENTIFIED BY 'password';" +
            "flush privileges;";
    
    private MySqlNode mysql;
    private Wordpress wordpress;

    @Override
    public void init() {
        mysql = addChild(BasicEntitySpec.newInstance(MySqlNode.class)
                .configure("creationScriptContents", SCRIPT));

        wordpress = addChild(BasicEntitySpec.newInstance(Wordpress.class)
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
        String port =  CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", "aws-ec2:us-east-1");

        // Image: {id=us-east-1/ami-7d7bfc14, providerId=ami-7d7bfc14, name=RightImage_CentOS_6.3_x64_v5.8.8.5, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=centos, arch=paravirtual, version=6.0, description=rightscale-us-east/RightImage_CentOS_6.3_x64_v5.8.8.5.manifest.xml, is64Bit=true}, description=rightscale-us-east/RightImage_CentOS_6.3_x64_v5.8.8.5.manifest.xml, version=5.8.8.5, status=AVAILABLE[available], loginUser=root, userMetadata={owner=411009282317, rootDeviceType=instance-store, virtualizationType=paravirtual, hypervisor=xen}}
        // TODO Set for only us-east-1 region, rather than all aws-ec2
        BrooklynProperties brooklynProperties = BrooklynProperties.Factory.newDefault();
        brooklynProperties.put("brooklyn.jclouds.aws-ec2.image-id", "us-east-1/ami-7ce17315");
        brooklynProperties.put("brooklyn.jclouds.cloudservers-uk.image-name-regex", "CentOS 6.0");
        brooklynProperties.remove("brooklyn.jclouds.cloudservers-uk.image-id");

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .brooklynProperties(brooklynProperties)
                .application(EntitySpecs.appSpec(BasicWordpressApp.class)
                        .displayName("Simple wordpress app"))
                .webconsolePort(port)
                .location(location)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}
