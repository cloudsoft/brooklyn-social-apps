package io.cloudsoft.socialapps.wordpress;

import static com.google.common.base.Preconditions.checkNotNull;

import io.cloudsoft.socialapps.wordpress.examples.ClusteredWordpressApp;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

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

import com.abiquo.server.core.cloud.VirtualMachineState;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import brooklyn.entity.Entity;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxy.nginx.NginxControllerImpl;
import brooklyn.location.Location;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;

public class CustomNginxControllerImpl extends NginxControllerImpl {

   public static final Logger log = LoggerFactory.getLogger(CustomNginxControllerImpl.class);

   protected String getCodeForServerConfig() {
        // See http://zeroturnaround.com/labs/wordpress-protips-go-with-a-clustered-approach/#!/
        // 
        // But fails if use the brooklyn default:
        //     proxy_set_header Host $http_host;
        // instead of:
        //     proxy_set_header Host $host;
        
        return ""+
            "    server_tokens off;\n"+
            "    proxy_set_header Host $host;\n"+
            "    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n"+
            "    proxy_set_header X-Real-IP $remote_addr;\n";
    }

   @Override
   protected void postStart() {
      super.postStart();
      for (Location loc : getLocations()) {
         if (loc instanceof JcloudsLocation) {
            JcloudsLocation jcloudsLocation = ((JcloudsLocation) loc);
            if ("abiquo".equals(jcloudsLocation.getProvider())) {
               AbiquoContext context = ContextBuilder.newBuilder(jcloudsLocation.getProvider())
                     .endpoint(jcloudsLocation.getEndpoint())
                     .credentials(jcloudsLocation.getIdentity(), jcloudsLocation.getCredential())
                     .buildView(AbiquoContext.class);
               customizeEntity(this, context);
            }
         }
      }
   }

   private void customizeEntity(Entity entity, AbiquoContext context) {
      log.info(">>> CustomizeEntity - entity: " + entity);
      try {
         ExternalNetwork externalNetwork = tryFindExternalNetwork(context);
         ExternalIp externalIp = tryFindExternalIp(externalNetwork);
         Iterable<VirtualMachine> vms = context.getCloudService().listVirtualMachines();
         JcloudsSshMachineLocation machine = (JcloudsSshMachineLocation) Iterables.getFirst(entity.getLocations(), null);
         checkNotNull(machine);
         for (VirtualMachine virtualMachine : vms) {
            if (virtualMachine.getNameLabel().equals(machine.getNode().getName())) {
               List<Ip<?, ?>> nics = appendExternalIpToNICs(externalIp, virtualMachine);
               log.info("Setting NIC " + Iterables.toString(nics) + " on virtualMachine(" + virtualMachine.getNameLabel());
               reconfigureNICsOnVirtualMachine(context, externalNetwork, nics, virtualMachine);
            }
         }
         if(entity instanceof NginxController) {
            ((NginxController) entity).restart();
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
      AsyncTask task = virtualMachine.setNics(/*externalNetwork,*/nics);
      monitoringService.getAsyncTaskMonitor().awaitCompletion(task);
      virtualMachine.changeState(VirtualMachineState.ON);
      monitoringService.getVirtualMachineMonitor().awaitState(VirtualMachineState.ON, virtualMachine);
      log.info("virtualMachine(" + virtualMachine.getNameLabel() + ") is " + virtualMachine.getState());
   }

   private List<Ip<?, ?>> appendExternalIpToNICs(ExternalIp externalIp, VirtualMachine virtualMachine) {
      List<Ip<?, ?>> initialIps = virtualMachine.listAttachedNics();
      List<Ip<?, ?>> ips = Lists.newArrayList();
      ips.add(externalIp);
      ips.addAll(initialIps);
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
   
}
