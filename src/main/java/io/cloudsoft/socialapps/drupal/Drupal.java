package io.cloudsoft.socialapps.drupal;


import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.entity.software.base.SoftwareProcess;
import org.apache.brooklyn.entity.webapp.WebAppService;
import org.apache.brooklyn.util.core.flags.SetFromFlag;

@ImplementedBy(DrupalImpl.class)
public interface Drupal extends SoftwareProcess, WebAppService {

    @SetFromFlag("version")
    public static final ConfigKey<String> SUGGESTED_VERSION =
            ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION, "7.17");

    public static final ConfigKey<Boolean> DATABASE_UP =
            ConfigKeys.newBooleanConfigKey("database.up", "", true);

    @SetFromFlag("databaseDriver")
    public static final ConfigKey<String> DATABASE_DRIVER =
            ConfigKeys.newStringConfigKey("database.driver", "The driver to use (mysql,postgresql,...)", "mysql");

    @SetFromFlag("databaseSchema")
    public static final ConfigKey<String> DATABASE_SCHEMA =
            ConfigKeys.newStringConfigKey("database.schema", "The database schema to use", "drupal");

    @SetFromFlag("databaseUser")
    public static final ConfigKey<String> DATABASE_USER =
            ConfigKeys.newStringConfigKey("database.user", "The database user to use");

    @SetFromFlag("databasePassword")
    public static final ConfigKey<String> DATABASE_PASSWORD =
            ConfigKeys.newStringConfigKey("database.password", "The password of the database user");

    @SetFromFlag("databaseHost")
    public static final ConfigKey<String> DATABASE_HOST =
            ConfigKeys.newStringConfigKey("database.host", "The database host", "127.0.0.1");

    @SetFromFlag("databasePort")
    public static final ConfigKey<Integer> DATABASE_PORT =
            ConfigKeys.newIntegerConfigKey("database.port", "The database port", 3306);

    @SetFromFlag("siteName")
    public static final ConfigKey<String> SITE_NAME =
            ConfigKeys.newStringConfigKey("site.name", "The name of the site", "my_site");

    @SetFromFlag("siteMail")
    public static final ConfigKey<String> SITE_MAIL =
            ConfigKeys.newStringConfigKey("site.mail", "The email address of the site", "my_site@me.com");

    @SetFromFlag("adminName")
    public static final ConfigKey<String> ADMIN_NAME =
            ConfigKeys.newStringConfigKey("admin.name", "The name of the admin", "admin");

    @SetFromFlag("adminPassword")
    public static final ConfigKey<String> ADMIN_PASSWORD =
            ConfigKeys.newStringConfigKey("admin.password", "The password of the admin", "password");

    @SetFromFlag("adminEmail")
    public static final ConfigKey<String> ADMIN_EMAIL =
            ConfigKeys.newStringConfigKey("admin.email", "The email of the admin", null);
}
