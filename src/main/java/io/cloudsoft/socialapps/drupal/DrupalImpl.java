package io.cloudsoft.socialapps.drupal;


import java.util.Collection;

import brooklyn.entity.basic.SoftwareProcessImpl;

public class DrupalImpl extends SoftwareProcessImpl implements Drupal {

    @Override
    public Class getDriverInterface() {
        return DrupalDriver.class;
    }

    @Override
    protected Collection<Integer> getRequiredOpenPorts() {
        Collection<Integer> ports = super.getRequiredOpenPorts();
        ports.add(80);
        ports.add(443);
        return ports;
    }
    
    @Override
    protected void connectSensors() {
        super.connectSensors();
        super.connectServiceUpIsRunning();
    }

    @Override
    protected void disconnectSensors() {
        super.disconnectSensors();
        super.disconnectServiceUpIsRunning();
    }
}
