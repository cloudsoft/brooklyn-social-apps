package io.cloudsoft.socialapps.drupal.examples;

import static org.apache.brooklyn.core.sensor.DependentConfiguration.attributeWhenReady;

import java.util.List;

import org.apache.brooklyn.api.catalog.Catalog;
import org.apache.brooklyn.api.catalog.CatalogConfig;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.entity.AbstractApplication;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.entity.StartableApplication;
import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.enricher.stock.Enrichers;
import org.apache.brooklyn.entity.database.mysql.MySqlNode;
import org.apache.brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import org.apache.brooklyn.entity.webapp.WebAppService;
import org.apache.brooklyn.launcher.BrooklynLauncher;
import org.apache.brooklyn.util.CommandLineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.cloudsoft.socialapps.drupal.Drupal;

/**
 * This example Application starts up a Scalable drupal environment.
 * <p/>
 * TODO: This is work in progres..
 * <p/>
 * <p/>
 * http://www.johnandcailin.com/blog/john/scaling-drupal-open-source-infrastructure-high-traffic-drupal-sites
 */
@Catalog(name="Clusted Drupal App",
    description="Drupal is an open source content management platform. "+
            "A clustered load-balanced drupal (Requires Debian).",
    iconUrl="classpath://io/cloudsoft/socialapps/drupal/drupal-icon.png")
public class ClusteredDrupalApp extends AbstractApplication {

    public static final Logger log = LoggerFactory.getLogger(BasicDrupalApp.class);

    @CatalogConfig(label="Admin e-mail")
    public static final ConfigKey<String> ADMIN_EMAIL = ConfigKeys.newConfigKeyWithDefault(
            Drupal.ADMIN_EMAIL, "foo@example.com");
    
    private final static String SCRIPT = "create database drupal; " +
            "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES ON drupal.* TO 'drupal'@'%'  IDENTIFIED BY 'password'; " +
            "FLUSH PRIVILEGES;";

    private MySqlNode mySqlNode;
    private ControlledDynamicWebAppCluster cluster;

    @Override
    public void init() {
        mySqlNode = addChild(EntitySpec.create(MySqlNode.class)
                .configure(MySqlNode.CREATION_SCRIPT_CONTENTS, SCRIPT));

        EntitySpec<Drupal> drupalSpec = EntitySpec.create(Drupal.class)
                .configure(Drupal.DATABASE_UP, attributeWhenReady(mySqlNode, MySqlNode.SERVICE_UP))
                .configure(Drupal.DATABASE_HOST, attributeWhenReady(mySqlNode, MySqlNode.HOSTNAME))
                .configure(Drupal.DATABASE_PORT, attributeWhenReady(mySqlNode, MySqlNode.MYSQL_PORT))
                .configure(Drupal.DATABASE_SCHEMA, "drupal")
                .configure(Drupal.DATABASE_USER, "drupal")
                .configure(Drupal.DATABASE_PASSWORD, "password")
                .configure(Drupal.ADMIN_EMAIL, getConfig(ADMIN_EMAIL));

        cluster = addChild(EntitySpec.create(ControlledDynamicWebAppCluster.class)
                .configure(ControlledDynamicWebAppCluster.MEMBER_SPEC, drupalSpec)
                .configure(ControlledDynamicWebAppCluster.INITIAL_SIZE, 2));
        
        addEnricher(Enrichers.builder()
                .propagating(WebAppService.ROOT_URL)
                .from(cluster)
                .build());
    }

    // can start in AWS by running this -- or use brooklyn CLI/REST for most clouds, or programmatic/config for set of fixed IP machines
    public static void main(String[] argv) throws Exception {
        List<String> args = Lists.newArrayList(argv);
        String port =  CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", "aws-ec2:us-east-1");

        // Image: {id=us-east-1/ami-7ce17315, providerId=ami-7ce17315, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=debian, arch=paravirtual, version=6.0, description=Debian 6.0.7 (Squeeze),  is64Bit=true}, description=Debian 6.0.7 (Squeeze), version=20091011, status=AVAILABLE[available], loginUser=ubuntu, userMetadata={owner=379101102735, rootDeviceType=instance-store, virtualizationType=paravirtual, hypervisor=xen}}
        // TODO Set for only us-east-1 region, rather than all aws-ec2
        BrooklynProperties brooklynProperties = BrooklynProperties.Factory.newDefault();
        brooklynProperties.put("brooklyn.jclouds.aws-ec2.image-id", "us-east-1/ami-7ce17315");
        brooklynProperties.put("brooklyn.jclouds.aws-ec2.loginUser", "admin");
        brooklynProperties.put("brooklyn.jclouds.cloudservers-uk.image-name-regex", "Debian 6");
        brooklynProperties.remove("brooklyn.jclouds.cloudservers-uk.image-id");
        
        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .brooklynProperties(brooklynProperties)
                .application(EntitySpec.create(StartableApplication.class, ClusteredDrupalApp.class)
                        .displayName("Clustered drupal app"))
                .webconsolePort(port)
                .location(location)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}
