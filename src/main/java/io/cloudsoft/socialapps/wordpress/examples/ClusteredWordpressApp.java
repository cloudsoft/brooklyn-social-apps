package io.cloudsoft.socialapps.wordpress.examples;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import brooklyn.catalog.Catalog;
import brooklyn.config.BrooklynProperties;
import brooklyn.enricher.Enrichers;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.entity.webapp.DynamicWebAppCluster;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.util.CommandLineUtil;
import io.cloudsoft.socialapps.wordpress.CustomNginxControllerImpl;
import io.cloudsoft.socialapps.wordpress.Wordpress;

@Catalog(name = "Clustered WordPress",
        description = "A WordPress cluster - the free and open source blogging tool and a content management system - with an nginx load balancer",
        iconUrl = "http://www.wordpress.org/about/images/logos/wordpress-logo-notext-rgb.png")
public class ClusteredWordpressApp extends AbstractApplication {

    // TODO Currently only works on CentOS or RHEL

    public static final Logger log = LoggerFactory.getLogger(ClusteredWordpressApp.class);

    public static String DEFAULT_LOCATION_SPEC = "jclouds:softlayer:dal06";

    final static String SCRIPT = "create database wordpress; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'localhost'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'%'  IDENTIFIED BY 'password';" +
            "flush privileges;";

    private MySqlNode mysql;
    private ControlledDynamicWebAppCluster cluster;

    @Override
    public void init() {
        mysql = addChild(EntitySpec.create(MySqlNode.class)
                .configure("creationScriptContents", SCRIPT));

        cluster = addChild(EntitySpec.create(ControlledDynamicWebAppCluster.class)
                .configure(ControlledDynamicWebAppCluster.CONTROLLER_SPEC, EntitySpec.create(NginxController.class)
                        .configure(NginxController.SERVER_CONF_GENERATOR, new CustomNginxControllerImpl()))
                .configure(ControlledDynamicWebAppCluster.INITIAL_SIZE, 2)
                .configure(ControlledDynamicWebAppCluster.MEMBER_SPEC, EntitySpec.create(Wordpress.class)
                                .configure(Wordpress.DATABASE_UP, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.SERVICE_UP))
                                .configure(Wordpress.DATABASE_HOSTNAME, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.HOSTNAME))
                                .configure(Wordpress.DATABASE_NAME, "wordpress")
                                .configure(Wordpress.DATABASE_USER, "wordpress")
                                .configure(Wordpress.DATABASE_PASSWORD, "password")
                                .configure(Wordpress.WEBLOG_TITLE, "Welcome to WordPress, installed by Brooklyn!")
                                .configure(Wordpress.WEBLOG_ADMIN_EMAIL, BasicWordpressApp.EMAIL)
                                .configure(Wordpress.WEBLOG_ADMIN_PASSWORD, BasicWordpressApp.PASSWORD)
                                .configure(Wordpress.USE_W3_TOTAL_CACHE, true)
                ));

        cluster.getCluster().addPolicy(AutoScalerPolicy.builder()
                .metric(DynamicWebAppCluster.REQUESTS_PER_SECOND_IN_WINDOW_PER_NODE)
                .metricRange(10, 25)
                .sizeRange(2, 5)
                .build());

        addEnricher(
                Enrichers.builder()
                        .propagating(WebAppService.ROOT_URL)
                        .from(cluster)
                        .build());
    }

    public static void main(String[] argv) throws Exception {
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");

        BrooklynProperties brooklynProperties = BrooklynProperties.Factory.newDefault();

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(EntitySpec.create(StartableApplication.class, ClusteredWordpressApp.class)
                        .displayName("Clustered wordpress app"))
                .webconsolePort(port)
                .location(DEFAULT_LOCATION_SPEC)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}
