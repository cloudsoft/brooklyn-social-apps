package io.cloudsoft.socialapps.drupal;


import groovy.time.TimeDuration;

import java.util.Collection;
import java.util.Map;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.adapter.FunctionSensorAdapter;
import brooklyn.util.MutableMap;

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
        FunctionSensorAdapter serviceUpAdapter = sensorRegistry.register(new ServiceUpSensorAdapter());
        serviceUpAdapter.poll(SERVICE_UP);
    }

    private class ServiceUpSensorAdapter extends FunctionSensorAdapter {

        public ServiceUpSensorAdapter() {
            //we want to scan every 10 seconds.
            super(MutableMap.of("period", new TimeDuration(0, 0, 10, 0)));
        }

        @Override
        public Object call() {
            return getDriver().isRunning();
        }
    }
}
