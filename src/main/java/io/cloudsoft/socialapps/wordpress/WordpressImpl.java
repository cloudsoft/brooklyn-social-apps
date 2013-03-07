package io.cloudsoft.socialapps.wordpress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.feed.function.FunctionFeed;
import brooklyn.event.feed.function.FunctionPollConfig;

import com.google.common.base.Functions;
import com.google.common.io.ByteStreams;

public class WordpressImpl extends SoftwareProcessImpl implements Wordpress {

    protected FunctionFeed serviceUpFeed;

    @Override
	public Class getDriverInterface() {
		return WordpressDriver.class;
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

        setAttribute(Wordpress.ROOT_URL, String.format("http://%s:%s/", getAttribute(Attributes.HOSTNAME), getAttribute(HTTP_PORT)));

        serviceUpFeed = FunctionFeed.builder()
                .entity(this)
                .poll(new FunctionPollConfig<Object, Boolean>(SERVICE_UP)
                        .period(500, TimeUnit.MILLISECONDS)
                        .callable(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                return getDriver().isRunning();
                            }
                        })
                        .onError(Functions.constant(Boolean.FALSE)))
                .build();
    }

    @Override
    protected void disconnectSensors() {
        super.disconnectSensors();

        if (serviceUpFeed != null) serviceUpFeed.stop();
    }
    
    public String getTemplateConfigurationUrl() {
        return getConfig(TEMPLATE_CONFIGURATION_URL);
    }
    
    public String getTemplateCustomInstallPhpUrl() {
        return getConfig(TEMPLATE_CUSTOM_INSTALL_PHP_URL);
    }
    
	/** The name of the database for WordPress */
	public String getDatabaseName() {
	    return getConfig(DATABASE_NAME);
	}

	/** MySQL database username */
	public String getDatabaseUserName() {
        return getConfig(DATABASE_USER);
	}

	/** MySQL database password */
	public String getDatabasePassword() {
        return getConfig(DATABASE_PASSWORD);
	}

	/** MySQL hostname */
	public String getDatabaseHostname() {
        return getConfig(DATABASE_HOSTNAME);
	}

	public String getWeblogTitle() {
        return getConfig(WEBLOG_TITLE);
	}
	
	public String getWeblogAdminEmail() {
        return getConfig(WEBLOG_ADMIN_EMAIL);
	    
	}
	public String getWeblogPublic() {
	    return getConfig(IS_WEBLOG_PUBLIC).toString();
	}

	/**
	 * Authentication Unique Keys and Salts.
	 *
	 * You can generate these using WordPress.org's secret-key service at {@linkplain https://api.wordpress.org/secret-key/1.1/salt/}
	 * 
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
	 * @throws IOException 
	 */
	public String getAuthenticationKeys() throws IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("https://api.wordpress.org/secret-key/1.1/salt/");
        HttpResponse httpResponse = httpClient.execute(httpGet);
        
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteStreams.copy(httpResponse.getEntity().getContent(), out);
            byte[] content = out.toByteArray();
            return new String(content);
        } finally {
            EntityUtils.consume(httpResponse.getEntity());
        }
	}
}
