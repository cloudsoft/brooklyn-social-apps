Brooklyn Social Apps Roll-out
======================

This project contains Brooklyn entities for the social applications like Drupal and Wordpress. It also contains a sample applications
which deploys it to Amazon.

### Prerequisites

This has been tested to use Debian 6.0. Your milage may vary when used with other flavors of *nix.

### Compile

To compile brooklyn-social-apps, simply `mvn clean install` in the project root.

### Run

Follow the [getting started instructions](http://brooklyncentral.github.com/use/guide/quickstart/index.html), then add the social apps to your catalog.xml (or launch manually.)

```xml
<catalog>
  ...
  <template type="io.cloudsoft.socialapps.drupal.examples.BasicDrupalApp" name="Basic Drupal App">
    <description>Brooklyn Social Apps - Basic</description>
  </template>
  <template type="io.cloudsoft.socialapps.drupal.examples.ClusteredDrupalApp" name="Clustered Drupal App">
    <description>Brooklyn Social Apps - Cluster</description>
  </template>
  ...
  <classpath>
    ...
    <entry>file://path/to/your/jars</entry>
    ...
  </classpath>
</catalog>


```

### Setup

In both cases you'll need cloud credentials in `~/.brooklyn/brooklyn.properties`:

```
brooklyn.location.named.Rackspace\ US\ -\ Debian = jclouds:cloudservers-us
brooklyn.location.named.Rackspace\ US\ -\ Debian.identity = username
brooklyn.location.named.Rackspace\ US\ -\ Debian.credential = 3d____________<snip>__________cd
brooklyn.location.named.Rackspace\ US\ -\ Debian.private-key-file = ~/.ssh/id_rsa
brooklyn.location.named.Rackspace\ US\ -\ Debian.image-name-regex = Debian
```

Most other clouds should work too, with minor variations to the code, as will fixed IP machines (bare-metal/byon).


### Finally

This software is (c) 2012 Cloudsoft Corporation, released as open source under the Apache License v2.0.

Any questions drop a line to brooklyn-users@googlegroups.com !
