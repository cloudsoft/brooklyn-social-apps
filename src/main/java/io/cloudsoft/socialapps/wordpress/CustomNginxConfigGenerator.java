package io.cloudsoft.socialapps.wordpress;

import org.apache.brooklyn.entity.proxy.nginx.NginxDefaultConfigGenerator;

public class CustomNginxConfigGenerator extends NginxDefaultConfigGenerator {

    @Override
    protected String getCodeForServerConfig() {
        // See http://zeroturnaround.com/labs/wordpress-protips-go-with-a-clustered-approach/#!/
        // 
        // But fails if use the brooklyn default:
        //     proxy_set_header Host $http_host;
        // instead of:
        //     proxy_set_header Host $host;
        
        return ""+
            "    server_tokens off;\n"+
            "    proxy_set_header Host $host;\n"+
            "    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n"+
            "    proxy_set_header X-Real-IP $remote_addr;\n";
    }
}
