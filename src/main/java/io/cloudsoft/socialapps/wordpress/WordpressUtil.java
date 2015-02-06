package io.cloudsoft.socialapps.wordpress;

import java.net.URI;

import org.apache.http.client.HttpClient;

import brooklyn.util.http.HttpTool;
import brooklyn.util.http.HttpToolResponse;

import com.google.common.collect.ImmutableMap;

public class WordpressUtil {

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
     */
    public static String getAuthenticationKeys() {
        URI uri = URI.create("https://api.wordpress.org/secret-key/1.1/salt/");
        
        HttpClient client = HttpTool.httpClientBuilder().uri(uri).build();
        HttpToolResponse response = HttpTool.httpGet(client, uri, ImmutableMap.<String, String>of());
        int responseCode = response.getResponseCode();
        String responseContent = response.getContentAsString();
        
        if (responseCode >= 200 && responseCode < 300) {
            return responseContent;
        } else {
            throw new IllegalStateException("Failed to generate Wordpress key: responseCode="+responseCode
                    +"; reason="+response.getReasonPhrase()+"; content="+responseContent);
        }
    }
}
