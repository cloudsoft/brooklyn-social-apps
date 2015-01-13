package io.cloudsoft.socialapps.wordpress.examples;

import io.cloudsoft.socialapps.wordpress.CustomNginxControllerImpl;
import io.cloudsoft.socialapps.wordpress.Wordpress;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.catalog.Catalog;
import brooklyn.config.ConfigKey;
import brooklyn.enricher.Enrichers;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityAndAttribute;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.entity.webapp.DynamicWebAppCluster;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.Location;
import brooklyn.location.access.BrooklynAccessUtils;
import brooklyn.location.access.PortForwardManager;
import brooklyn.management.ManagementContext;
import brooklyn.networking.portforwarding.NoopPortForwarder;
import brooklyn.networking.subnet.PortForwarder;
import brooklyn.networking.subnet.SubnetTier;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.util.CommandLineUtil;
import brooklyn.util.net.Cidr;
import brooklyn.util.net.Protocol;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Catalog(name = "Clustered WordPress",
        description = "A WordPress cluster - the free and open source blogging tool and a content management system - with an nginx load balancer",
        iconUrl = "http://www.wordpress.org/about/images/logos/wordpress-logo-notext-rgb.png")
public class ClusteredWordpressApp extends AbstractApplication {

    public static final ConfigKey<Cidr> MANAGEMENT_ACCESS_CIDR = ConfigKeys.newConfigKeyWithDefault(BrooklynAccessUtils.MANAGEMENT_ACCESS_CIDR, Cidr.UNIVERSAL);
    public static final ConfigKey<Cidr> SUBNET_ACCESS_CIDR = ConfigKeys.newConfigKeyWithDefault(BrooklynAccessUtils.MANAGEMENT_ACCESS_CIDR, new Cidr("10.44.0.0/24"));

    public static final Logger log = LoggerFactory.getLogger(ClusteredWordpressApp.class);

    public static final String DEFAULT_LOCATION_SPEC = "softlayer:ams01";

    static final String SCRIPT = "create database wordpress; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'localhost'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'%'  IDENTIFIED BY 'password';" +
            "flush privileges;";

    private MySqlNode mysql;
    private ControlledDynamicWebAppCluster cluster;
    //Subnet Tier Configurations
    protected PortForwardManager portForwardManager;
    protected SubnetTier subnetTier;

    @Override
    public void init() {
        configure(BrooklynAccessUtils.MANAGEMENT_ACCESS_CIDR, getConfig(MANAGEMENT_ACCESS_CIDR));
        ManagementContext managementContext = getApplication().getManagementContext();
        portForwardManager = (PortForwardManager) managementContext.getLocationRegistry().resolve("portForwardManager(scope=global)");

        PortForwarder portForwarder = new NoopPortForwarder(portForwardManager);

        subnetTier = addChild(EntitySpec.create(SubnetTier.class)
                .configure(SubnetTier.PORT_FORWARDER, portForwarder)
                .configure(SubnetTier.PORT_FORWARDING_MANAGER, portForwardManager)
                .configure(SubnetTier.SUBNET_CIDR, getConfig(SUBNET_ACCESS_CIDR))
                .displayName("Wordpress Cluster Deployment Subnet Tier"));

        mysql = subnetTier.addChild(EntitySpec.create(MySqlNode.class)
                .configure("creationScriptContents", SCRIPT));

        cluster = subnetTier.addChild(EntitySpec.create(ControlledDynamicWebAppCluster.class)
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

    @Override
    public void start(Collection<? extends Location> locations) {
        super.start(locations);

        for (Entity member : Iterables.filter(cluster.getChildren(), Predicates.instanceOf(Wordpress.class))) {
            subnetTier.openPortForwardingAndAdvertise(
                    new EntityAndAttribute<Integer>(member, Wordpress.HTTP_PORT),
                    Optional.<Integer>absent(),
                    Protocol.TCP,
                    Cidr.UNIVERSAL,
                    new EntityAndAttribute<String>(member, Wordpress.ROOT_URL));
        }
    }

    public static void main(String[] argv) throws Exception {
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(EntitySpec.create(StartableApplication.class, ClusteredWordpressApp.class)
                        .displayName("Clustered Wordpress app"))
                .webconsolePort(port)
                .location(DEFAULT_LOCATION_SPEC)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}