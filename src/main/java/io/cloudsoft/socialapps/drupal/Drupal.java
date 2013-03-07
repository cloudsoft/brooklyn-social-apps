package io.cloudsoft.socialapps.drupal;


import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(DrupalImpl.class)
public interface Drupal extends SoftwareProcess, WebAppService {

    @SetFromFlag("version")
    public static final BasicConfigKey<String> SUGGESTED_VERSION =
            new BasicConfigKey<String>(SoftwareProcess.SUGGESTED_VERSION, "7.17");

    public static final BasicConfigKey<Boolean> DATABASE_UP =
            new BasicConfigKey<Boolean>(Boolean.class, "database.up", "",true);

    @SetFromFlag("databaseDriver")
    public static final BasicConfigKey<String> DATABASE_DRIVER =
            new BasicConfigKey<String>(String.class, "database.driver", "The driver to use (mysql,postgresql,...)", "mysql");

    @SetFromFlag("databaseSchema")
    public static final BasicConfigKey<String> DATABASE_SCHEMA =
            new BasicConfigKey<String>(String.class, "database.schema", "The database schema to use", "drupal");

    @SetFromFlag("databaseUser")
    public static final BasicConfigKey<String> DATABASE_USER =
            new BasicConfigKey<String>(String.class, "database.user", "The database user to use");

    @SetFromFlag("databasePassword")
    public static final BasicConfigKey<String> DATABASE_PASSWORD =
            new BasicConfigKey<String>(String.class, "database.password", "The password of the database user");

    @SetFromFlag("databaseHost")
    public static final BasicConfigKey<String> DATABASE_HOST =
            new BasicConfigKey<String>(String.class, "database.host", "The database host", "127.0.0.1");

    @SetFromFlag("databasePort")
    public static final BasicConfigKey<Integer> DATABASE_PORT =
            new BasicConfigKey<Integer>(Integer.class, "database.port", "The database port", 3306);

    @SetFromFlag("siteName")
    public static final BasicConfigKey<String> SITE_NAME =
            new BasicConfigKey<String>(String.class, "site.name", "The name of the site", "my_site");

    @SetFromFlag("siteMail")
    public static final BasicConfigKey<String> SITE_MAIL =
            new BasicConfigKey<String>(String.class, "site.mail", "The email address of the site", "my_site@me.com");

    @SetFromFlag("adminName")
    public static final BasicConfigKey<String> ADMIN_NAME =
            new BasicConfigKey<String>(String.class, "admin.name", "The name of the admin", "admin");

    @SetFromFlag("adminPassword")
    public static final BasicConfigKey<String> ADMIN_PASSWORD =
            new BasicConfigKey<String>(String.class, "admin.password", "The password of the admin", "password");

    @SetFromFlag("adminEmail")
    public static final BasicConfigKey<String> ADMIN_EMAIL =
            new BasicConfigKey<String>(String.class, "admin.email", "The email of the admin", null);
}
