package io.cloudsoft.socialapps.wordpress.examples;

import brooklyn.entity.Entity;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.location.Location;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import com.abiquo.server.core.cloud.VirtualMachineState;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.cloudsoft.socialapps.wordpress.CustomNginxControllerImpl;
import io.cloudsoft.socialapps.wordpress.Wordpress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jclouds.ContextBuilder;
import org.jclouds.abiquo.AbiquoContext;
import org.jclouds.abiquo.domain.cloud.VirtualMachine;
import org.jclouds.abiquo.domain.enterprise.Enterprise;
import org.jclouds.abiquo.domain.infrastructure.Datacenter;
import org.jclouds.abiquo.domain.network.ExternalIp;
import org.jclouds.abiquo.domain.network.ExternalNetwork;
import org.jclouds.abiquo.domain.network.Ip;
import org.jclouds.abiquo.domain.network.Network;
import org.jclouds.abiquo.domain.task.AsyncTask;
import org.jclouds.abiquo.features.services.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.catalog.Catalog;
import brooklyn.enricher.basic.SensorPropagatingEnricher;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntityTypeRegistry;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.entity.webapp.DynamicWebAppCluster;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import brooklyn.util.CommandLineUtil;

import com.google.common.collect.Lists;

import javax.annotation.Nullable;

@Catalog(name="Clustered WordPress", 
        description="A WordPress cluster - the free and open source blogging tool and a content management system - with an nginx load balancer",
        iconUrl="http://www.wordpress.org/about/images/logos/wordpress-logo-notext-rgb.png")
public class ClusteredWordpressApp extends AbstractApplication {
    
    // TODO Currently only works on CentOS or RHEL
    public static final Logger log = LoggerFactory.getLogger(ClusteredWordpressApp.class);
    public static final String DEFAULT_LOCATION = "localhost";

   final static String SCRIPT = "create database wordpress; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'localhost'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'%'  IDENTIFIED BY 'password';" +
            "flush privileges;";
    
    private MySqlNode mysql;
    private ControlledDynamicWebAppCluster cluster;

    @Override
    public void init() {
        EntityTypeRegistry typeRegistry = getManagementContext().getEntityManager().getEntityTypeRegistry();
        typeRegistry.registerImplementation(NginxController.class, CustomNginxControllerImpl.class);
        
        mysql = addChild(EntitySpec.create(MySqlNode.class)
                .configure("creationScriptContents", SCRIPT));

        cluster = addChild(EntitySpec.create(ControlledDynamicWebAppCluster.class)
                .configure(ControlledDynamicWebAppCluster.INITIAL_SIZE, 1)
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

        SensorPropagatingEnricher.newInstanceListeningTo(cluster, WebAppService.ROOT_URL).addToEntityAndEmitAll(this);
    }

   @Override
   public void postStart(Collection<? extends Location> locations) {
      for (Location loc : locations) {
         if (loc instanceof JcloudsLocation) {
            JcloudsLocation jcloudsLocation = ((JcloudsLocation) loc);
            if ("abiquo".equals(jcloudsLocation.getProvider())) {
               AbiquoContext context = ContextBuilder.newBuilder(jcloudsLocation.getProvider())
                       .endpoint(jcloudsLocation.getEndpoint())
                       .credentials(jcloudsLocation.getIdentity(), jcloudsLocation.getCredential())
                       .buildView(AbiquoContext.class);
               customizeEntity(cluster.getController(), context);
            }
         }
      }
      super.postStart(locations);
   }

   private void customizeEntity(Entity entity, AbiquoContext context) {
      try {
         ExternalNetwork externalNetwork = tryFindExternalNetwork(context);
         ExternalIp externalIp = tryFindExternalIp(externalNetwork);
         Iterable<VirtualMachine> vms = context.getCloudService().listVirtualMachines();
         JcloudsSshMachineLocation machine = (JcloudsSshMachineLocation) Iterables.getOnlyElement(entity.getLocations());
         for (VirtualMachine virtualMachine : vms) {
            if (virtualMachine.getNameLabel().equals(machine.getNode().getName())) {
               List<Ip<?, ?>> nics = appendExternalIpToNICs(externalIp, virtualMachine);
               log.info("Setting NIC " + Iterables.toString(nics) + " on virtualMachine(" + virtualMachine.getNameLabel());
               reconfigureNICsOnVirtualMachine(context, externalNetwork, nics, virtualMachine);
            }
         }
      } finally {
         context.close();
      }
   }

   private void reconfigureNICsOnVirtualMachine(AbiquoContext context, ExternalNetwork externalNetwork, List<Ip<?, ?>> nics, VirtualMachine virtualMachine) {
      MonitoringService monitoringService = context.getMonitoringService();
      virtualMachine.changeState(VirtualMachineState.OFF);
      monitoringService.getVirtualMachineMonitor().awaitState(VirtualMachineState.OFF, virtualMachine);
      log.info("virtualMachine(" + virtualMachine.getNameLabel() + ") is " + virtualMachine.getState());
      AsyncTask task = virtualMachine.setNics(externalNetwork, nics);
      monitoringService.getAsyncTaskMonitor().awaitCompletion(task);
      virtualMachine.changeState(VirtualMachineState.ON);
      monitoringService.getVirtualMachineMonitor().awaitState(VirtualMachineState.ON, virtualMachine);
      log.info("virtualMachine(" + virtualMachine.getNameLabel() + ") is " + virtualMachine.getState());
   }

   private List<Ip<?, ?>> appendExternalIpToNICs(ExternalIp externalIp, VirtualMachine virtualMachine) {
      List<Ip<?, ?>> initialIps = virtualMachine.listAttachedNics();
      List<Ip<?, ?>> ips = Lists.newArrayList();
      ips.addAll(initialIps);
      ips.add(externalIp);
      return ips;
   }

   private ExternalIp tryFindExternalIp(ExternalNetwork externalNetwork) {
      Optional<ExternalIp> optionalExternalIp = Optional.of(externalNetwork.listUnusedIps().get(0));
      if(optionalExternalIp.isPresent()) {
         return optionalExternalIp.get();
      } else {
         throw new IllegalStateException("Cannot find an available externalIp in external network " +
                 externalNetwork);
      }
   }

   private ExternalNetwork tryFindExternalNetwork(AbiquoContext context) {
      Optional<ExternalNetwork> optionalExternalNetwork = Optional.absent();
      Enterprise enterprise = context.getAdministrationService().getCurrentEnterprise();
      List<Datacenter> datacenters = enterprise.listAllowedDatacenters();
      while (!optionalExternalNetwork.isPresent() && datacenters.listIterator().hasNext()) {
         ExternalNetwork externalNetwork = enterprise.findExternalNetwork(datacenters.listIterator().next(),
                 new Predicate<Network<ExternalIp>>() {
                    @Override
                    public boolean apply(@Nullable Network<ExternalIp> input) {
                       return input != null && input.getName().startsWith("CLPU0_IPAC");
                    }
                 });
         optionalExternalNetwork = Optional.of(externalNetwork);
      }
      if(optionalExternalNetwork.isPresent()) {
         return optionalExternalNetwork.get();
      } else {
         throw new IllegalStateException("Cannot find an available externalNetwork in any datacenters " +
                 Iterables.toString(datacenters));
      }
   }

   public static void main(String[] argv) throws Exception {
        List<String> args = Lists.newArrayList(argv);
        String port =  CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        List<String> locations = new ArrayList<String>();
        while (true) {
           String l = CommandLineUtil.getCommandLineOption(args, "--location", null);
           if (l!=null) locations.add(l);
           else break;
        }
        if (locations.isEmpty()) locations.add(DEFAULT_LOCATION);

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
              .application(EntitySpecs.appSpec(ClusteredWordpressApp.class)
              .displayName("Clustered wordpress app"))
              .webconsolePort(port)
              .locations(locations)
              .start();

        Entities.dumpInfo(launcher.getApplications());    }
}
