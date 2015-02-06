package io.cloudsoft.socialapps.wordpress;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(WordpressImpl.class)
public interface Wordpress extends SoftwareProcess, WebAppService, HasShortName {

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION = ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION,
            "3.5.1");

    ConfigKey<Boolean> DATABASE_UP = ConfigKeys.newBooleanConfigKey("database.up", "", true);

    @SetFromFlag("downloadUrl")
    BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(
            SoftwareProcess.DOWNLOAD_URL, "http://wordpress.org/wordpress-${version}.tar.gz");

    @SetFromFlag("templateConfigurationUrl")
    ConfigKey<String> TEMPLATE_CONFIGURATION_URL = ConfigKeys.newStringConfigKey(
            "wordpress.templateConfigurationUrl", 
            "Template file (in freemarker format) for the wp-config.php file",
            "classpath://io/cloudsoft/socialapps/wordpress/wp-config.php");

    @SetFromFlag("templateCustomInstallPhpUrl")
    ConfigKey<String> TEMPLATE_CUSTOM_INSTALL_PHP_URL = ConfigKeys.newStringConfigKey(
            "wordpress.templateCustomInstallPhp", 
            "Template file (in freemarker format) for the custom-install.php file",
            "classpath://io/cloudsoft/socialapps/wordpress/custom-install.php");

    @SetFromFlag("databaseName")
    ConfigKey<String> DATABASE_NAME = ConfigKeys.newStringConfigKey("wordpress.databaseName", "name of the database for WordPress", null);

    @SetFromFlag("databaseUser")
    ConfigKey<String> DATABASE_USER = ConfigKeys.newStringConfigKey("wordpress.databaseUser", "MySql database username", null);

    @SetFromFlag("databasePassword")
    ConfigKey<String> DATABASE_PASSWORD = ConfigKeys.newStringConfigKey("wordpress.databasePassword", "MySql database password", null);

    @SetFromFlag("databaseHostname")
    ConfigKey<String> DATABASE_HOSTNAME = ConfigKeys.newStringConfigKey("wordpress.databaseHostname", "MySql hostname", null);

    @SetFromFlag("weblogTitle")
    ConfigKey<String> WEBLOG_TITLE = ConfigKeys.newStringConfigKey("wordpress.weblog.title", "Title for the weblog", "My default title");

    @SetFromFlag("weblogAdminEmail")
    ConfigKey<String> WEBLOG_ADMIN_EMAIL = ConfigKeys.newStringConfigKey("wordpress.weblog.adminEmail", "E-mail address for the weblog admin user (default to empty)", "");

    @SetFromFlag("weblogAdminPassword")
    // TODO would be nice if empty password causes auto-gen
    ConfigKey<String> WEBLOG_ADMIN_PASSWORD = ConfigKeys.newStringConfigKey("wordpress.weblog.adminPassword", "Password for the weblog admin user (defaults to 'password')", "password");

    @SetFromFlag("isWeblogPublic")
    ConfigKey<Boolean> IS_WEBLOG_PUBLIC = ConfigKeys.newBooleanConfigKey("wordpress.weblog.ispublic", "Whether the weblog is public", true);

    @SetFromFlag("weblogDbCache")
    ConfigKey<Boolean> WEBLOG_DB_CACHE = ConfigKeys.newBooleanConfigKey("wordpress.weblog.db.cache", "Whether the DB cache is turned on", null);

    @SetFromFlag("useW3TotalCache")
    ConfigKey<Boolean> USE_W3_TOTAL_CACHE = ConfigKeys.newBooleanConfigKey("wordpress.w3.total.cache", "Whether W3 Total Cache (optimization) is enabled (recommended)", false);

    @SetFromFlag("httpPort")
    PortAttributeSensorAndConfigKey HTTP_PORT = new PortAttributeSensorAndConfigKey(Attributes.HTTP_PORT, "80");
    
    @SetFromFlag("httpsPort")
    PortAttributeSensorAndConfigKey HTTPS_PORT = new PortAttributeSensorAndConfigKey(Attributes.HTTPS_PORT, "443");
}
