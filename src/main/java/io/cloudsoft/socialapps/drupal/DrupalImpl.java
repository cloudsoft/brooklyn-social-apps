package io.cloudsoft.socialapps.drupal;


import java.util.Collection;
import java.util.Map;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.util.collections.MutableMap;

public class DrupalImpl extends SoftwareProcessImpl implements Drupal {

    public DrupalImpl() {
        this(MutableMap.of(), null);
    }

    /**
     * @deprecated Use EntitySpec for constructing, which calls no-arg constructor
     */
    public DrupalImpl(Map flags) {
        this(flags, null);
    }

    /**
     * @deprecated Use EntitySpec for constructing, which calls no-arg constructor
     */
    public DrupalImpl(Entity owner) {
        this(MutableMap.of(), owner);
    }

    /**
     * @deprecated Use EntitySpec for constructing, which calls no-arg constructor
     */
    public DrupalImpl(Map flags, Entity owner) {
        super(flags, owner);
    }

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
