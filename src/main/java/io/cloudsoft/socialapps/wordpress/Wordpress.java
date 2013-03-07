package io.cloudsoft.socialapps.wordpress;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(WordpressImpl.class)
public interface Wordpress extends SoftwareProcess, WebAppService {

    @SetFromFlag("version")
    BasicConfigKey<String> SUGGESTED_VERSION = new BasicConfigKey<String>(
    		SoftwareProcess.SUGGESTED_VERSION, "3.5.1");

    public static final BasicConfigKey<Boolean> DATABASE_UP = new BasicConfigKey<Boolean>(
            Boolean.class, "database.up", "", true);

    @SetFromFlag("downloadUrl")
    BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(
            SoftwareProcess.DOWNLOAD_URL, "http://wordpress.org/wordpress-${version}.tar.gz");

    @SetFromFlag("templateConfigurationUrl")
    ConfigKey<String> TEMPLATE_CONFIGURATION_URL = new BasicConfigKey<String>(
            String.class, "wordpress.templateConfigurationUrl", "Template file (in freemarker format) for the wp-config.php file", 
            "classpath://io/cloudsoft/socialapps/wordpress/wp-config.php");

    @SetFromFlag("templateCustomInstallPhpUrl")
    ConfigKey<String> TEMPLATE_CUSTOM_INSTALL_PHP_URL = new BasicConfigKey<String>(
            String.class, "wordpress.templateCustomInstallPhp", "Template file (in freemarker format) for the custom-install.php file", 
            "classpath://io/cloudsoft/socialapps/wordpress/custom-install.php");

    @SetFromFlag("databaseName")
    ConfigKey<String> DATABASE_NAME = new BasicConfigKey<String>(
            String.class, "wordpress.databaseName", "name of the database for WordPress", null);

    @SetFromFlag("databaseUser")
    ConfigKey<String> DATABASE_USER = new BasicConfigKey<String>(
            String.class, "wordpress.databaseUser", "MySql database username", null);

    @SetFromFlag("databasePassword")
    ConfigKey<String> DATABASE_PASSWORD = new BasicConfigKey<String>(
            String.class, "wordpress.databasePassword", "MySql database password", null);

    @SetFromFlag("databaseHostname")
    ConfigKey<String> DATABASE_HOSTNAME = new BasicConfigKey<String>(
            String.class, "wordpress.databaseHostname", "MySql hostname", null);
    
    @SetFromFlag("weblogTitle")
    ConfigKey<String> WEBLOG_TITLE = new BasicConfigKey<String>(
            String.class, "wordpress.weblog.title", "Title for the weblog", "My default title");
    
    @SetFromFlag("weblogAdminEmail")
    ConfigKey<String> WEBLOG_ADMIN_EMAIL = new BasicConfigKey<String>(
            String.class, "wordpress.weblog.adminEmail", "E-mail address for the weblog admin user", "myuser@mydomain.com");
    
    @SetFromFlag("isWeblogPublic")
    ConfigKey<Boolean> IS_WEBLOG_PUBLIC = new BasicConfigKey<Boolean>(
            Boolean.class, "wordpress.weblog.ispublic", "Whether the weblog is public", true);
    
    @SetFromFlag("httpPort")
    PortAttributeSensorAndConfigKey HTTP_PORT = new PortAttributeSensorAndConfigKey(Attributes.HTTP_PORT, "80");
}
