package io.cloudsoft.socialapps.wordpress;

import java.io.IOException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.deprecated;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.webapp.WebAppServiceMethods;
import brooklyn.event.feed.ssh.SshFeed;
import brooklyn.event.feed.ssh.SshPollConfig;
import brooklyn.event.feed.ssh.SshValueFunctions;
import brooklyn.location.MachineLocation;
import brooklyn.location.basic.SshMachineLocation;

import com.google.common.base.Function;
import com.google.common.base.Functions;

public class WordpressImpl extends SoftwareProcessImpl implements Wordpress {

    private static final Logger log = LoggerFactory.getLogger(WordpressImpl.class);
    
    private SshFeed sshFeed;

    @Override
    public Class getDriverInterface() {
        return WordpressDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        super.connectServiceUpIsRunning();
        
        setAttribute(Wordpress.ROOT_URL, String.format("http://%s:%s/", getAttribute(Attributes.HOSTNAME), getAttribute(HTTP_PORT)));

        /*
         * Gives stdout such as:
         *     Total Accesses: 2
         *     Total kBytes: 0
         *     Uptime: 20
         *     ReqPerSec: .1
         *     BytesPerSec: 0
         *     BytesPerReq: 0
         *     BusyWorkers: 1
         *     IdleWorkers: 7
         */
        MachineLocation machine = getMachineOrNull();
        if (machine instanceof SshMachineLocation) {
            sshFeed = SshFeed.builder()
                    .entity(this)
                    .machine((SshMachineLocation) machine)
                    .poll(new SshPollConfig<Integer>(REQUEST_COUNT)
                            .period(1000)
                            .command("curl -f -L \"http://127.0.0.1/server-status?auto\"")
                            .onFailureOrException(Functions.constant(-1))
                            .onSuccess(SshValueFunctions.chain(SshValueFunctions.stdout(), new Function<String, Integer>() {
                                @Override
                                public Integer apply(@Nullable String stdout) {
                                    for (String line : stdout.split("\n")) {
                                        if (line.contains("Total Accesses")) {
                                            String val = line.split(":")[1].trim();
                                            return Integer.parseInt(val);
                                        }
                                    }
                                    log.debug("Total Accesses not found in server-status, returning -1 (stdout=" + stdout + ")");
                                    return -1;
                                }
                            })))
                    .build();
        } else {
            log.warn("Location(s) {} not an ssh-machine location, so not polling for request-count", getLocations());
        }

        WebAppServiceMethods.connectWebAppServerPolicies(this);
    }

    @Override
    protected void disconnectSensors() {
        super.disconnectSensors();
        if (sshFeed != null) sshFeed.stop();
    }

    public String getTemplateConfigurationUrl() {
        return getConfig(TEMPLATE_CONFIGURATION_URL);
    }

    public String getTemplateCustomInstallPhpUrl() {
        return getConfig(TEMPLATE_CUSTOM_INSTALL_PHP_URL);
    }

    /**
     * The name of the database for WordPress
     */
    public String getDatabaseName() {
        return getConfig(DATABASE_NAME);
    }

    /**
     * MySQL database username
     */
    public String getDatabaseUserName() {
        return getConfig(DATABASE_USER);
    }

    /**
     * MySQL database password
     */
    public String getDatabasePassword() {
        return getConfig(DATABASE_PASSWORD);
    }

    /**
     * MySQL hostname
     */
    public String getDatabaseHostname() {
        return getConfig(DATABASE_HOSTNAME);
    }

    public String getWeblogTitle() {
        return getConfig(WEBLOG_TITLE);
    }

    public String getWeblogAdminEmail() {
        return getConfig(WEBLOG_ADMIN_EMAIL);
    }

    public String getWeblogAdminPassword() {
        return getConfig(WEBLOG_ADMIN_PASSWORD);
    }

    public String getWeblogPublic() {
        return getConfig(IS_WEBLOG_PUBLIC).toString();
    }

    /**
     * extra WP config inserted into the wp-config.php file
     */
    public String getExtraWpConfig() {
        StringBuilder extras = new StringBuilder();

        Boolean dbCache = getConfig(WEBLOG_DB_CACHE);
        Boolean useW3TotalCache = getConfig(USE_W3_TOTAL_CACHE);

        if (dbCache != null || useW3TotalCache == Boolean.TRUE) {
            String value = dbCache == Boolean.FALSE ? "false" : "true";
            extras.append("define('WP_CACHE', " + value + ");\n");
        }

        return extras.toString();
    }

    /**
     * Authentication Unique Keys and Salts.
     * <p/>
     * You can generate these using WordPress.org's secret-key service at {@linkplain https://api.wordpress.org/secret-key/1.1/salt/}
     * <p/>
     * Should return something in the form:
     * <pre>
     * {@code
     * define('AUTH_KEY',         'put your unique phrase here');
     * define('SECURE_AUTH_KEY',  'put your unique phrase here');
     * define('LOGGED_IN_KEY',    'put your unique phrase here');
     * define('NONCE_KEY',        'put your unique phrase here');
     * define('AUTH_SALT',        'put your unique phrase here');
     * define('SECURE_AUTH_SALT', 'put your unique phrase here');
     * define('LOGGED_IN_SALT',   'put your unique phrase here');
     * define('NONCE_SALT',       'put your unique phrase here');
     * }
     * </pre>
     *
     * @throws IOException
     * 
     * @Deprecated since 0.3.0; see {@link WordpressUtil#getAuthenticationKeys()}
     */
    @Deprecated // is this used by anything?
    public String getAuthenticationKeys() throws IOException {
        return WordpressUtil.getAuthenticationKeys();
    }

    @Override
    public String getShortName() {
        return "wordpress-httpd";
    }
}
