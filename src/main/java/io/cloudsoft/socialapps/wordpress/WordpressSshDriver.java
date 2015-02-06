package io.cloudsoft.socialapps.wordpress;


import static brooklyn.util.ssh.BashCommands.alternatives;
import static brooklyn.util.ssh.BashCommands.installPackage;
import static brooklyn.util.ssh.BashCommands.ok;
import static brooklyn.util.ssh.BashCommands.sudo;
import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.Entities;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.ResourceUtils;
import brooklyn.util.os.Os;
import brooklyn.util.ssh.BashCommands;

public class WordpressSshDriver extends AbstractSoftwareProcessSshDriver implements WordpressDriver {

    // FIXME Only tested on CentOS
    // e.g. on debian, need to use /etc/init.d/apache2 instead of /etc/init.d/httpd

    // FIXME Some good security advice which we should follow at:
    // http://jeffreifman.com/detailed-wordpress-guide-for-aws/install-wordpress/
    // Also look at DrupalSshDriver. It does some good `chown`, `chgrp` and `chmod`.

    // FIXME There are other commands at:
    // http://jeffreifman.com/detailed-wordpress-guide-for-aws/install-wordpress/
    // to do with /etc/apache2/sites-available, `a2ensite wordpress` etc; not doing those currently.

    // FIXME Haven't set up email-server, so admin e-mails are not sent
    // See debian-specific postfix setup in DrupalSshDriver.

    public WordpressSshDriver(WordpressImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public WordpressImpl getEntity() {
        return (WordpressImpl) super.getEntity();
    }

    protected String getWwwDir() {
        return "/var/www/html";
    }

    @Override
    public void install() {

        List<String> urls = resolver.getTargets();
        String saveAs = resolver.getFilename();

        List<String> commands = new LinkedList<String>();

        commands.add(BashCommands.installPackage(of(
                "yum", "httpd php php-mysql php-gd",
                "apt", "apache2 php5 php5-mysql php5-gd php-mbstring libapache2-mod-php5 libapache2-mod-auth-mysql"), null));

        // as per willomitzer comment at http://googolflex.com/?p=482 (only needed if selinux is on this box)
        commands.add(ok(sudo("setsebool -P httpd_can_network_connect 1")));

        commands.addAll(BashCommands.commandsToDownloadUrlsAs(urls, saveAs));
        commands.add(BashCommands.INSTALL_TAR);
        commands.add(BashCommands.INSTALL_UNZIP);
        commands.add("tar xzfv " + saveAs);

        newScript(INSTALLING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
    }

    @Override
    public void customize() {
        // this call allows it to wait for the database startup to complete, if configured with:
        //     .configure(Wordpress.DATABASE_HOSTNAME, DependentConfiguration.attributeWhenReady(mySqlNode, MySqlNode.HOSTNAME))
        log.info("Waiting till the database is up");
        entity.getConfig(Wordpress.DATABASE_UP);
        log.info("Database is up and running");

        String configFileContents = processTemplate(getEntity().getTemplateConfigurationUrl());
        String destinationConfigFile = format("/tmp/wp-config.php", getWwwDir());
        getMachine().copyTo(new ByteArrayInputStream(configFileContents.getBytes()), destinationConfigFile);

        String customInstallFileContents = processTemplate(getEntity().getTemplateCustomInstallPhpUrl());
        String destinationCustomInstallFile = format("/tmp/custom-install.php", getWwwDir());
        getMachine().copyTo(new ByteArrayInputStream(customInstallFileContents.getBytes()), destinationCustomInstallFile);

        List<String> commands = new LinkedList<String>();
        commands.add(sudo(format("mkdir -p %s", getWwwDir())));
        commands.add(sudo(format("cp -R %s/* %s/", getExpandedInstallDir(), getWwwDir())));
        commands.add(format("cd %s", getWwwDir()));
        commands.add(sudo("cp /tmp/wp-config.php ."));
        commands.add(sudo("cp /tmp/custom-install.php ."));
        commands.add(format("php custom-install.php"));

        // Get metrics from apache httpd
        // Note ugliness of sed+cp is because wrapping `sed -i.bak '...'` in `sudo -E -n -s --` 
        // breaks on CentOS, because of quotes.
        String httpdConfFile = "/etc/httpd/conf/httpd.conf";
        String httpdConfTempFile = "/tmp/httpd.conf-" + entity.getId();
        String serverStatusConf =
                "<Location /server-status>\n" +
                        "SetHandler server-status\n" +
                        "Order Deny,Allow\n" +
                        "Deny from all\n" +
                        "Allow from localhost\n" +
                        "Allow from 127.0.0.1\n" +
                        "</Location>\n";
        commands.add(format("sed 's/^#ExtendedStatus On/ExtendedStatus On/' %s > %s", httpdConfFile, httpdConfTempFile));

        // TODO Don't add this multiple times!
        // if [ \"`grep -E \"^SetHandler server-status\" "+httpdConfTempFile+"`\" == \"\" ]; then\n"
        commands.add("cat >> " + httpdConfTempFile + " << END_CONF_" + entity.getId() + "\n" +
                serverStatusConf + "\n" +
                "END_CONF_" + entity.getId() + "\n");

        commands.add(sudo(format("cp %s %s", httpdConfTempFile, httpdConfFile)));

        // start it -- as customizing plugins requires this
        commands.add(sudo("/etc/init.d/httpd restart"));

        newScript(CUSTOMIZING).
                body.append(commands).execute();

        if (entity.getConfig(Wordpress.USE_W3_TOTAL_CACHE) == Boolean.TRUE) {
            log.info("Activating W3 Total Cache for " + entity);
            addW3TotalCache();
        }
    }

    @Override
    public void launch() {
        List<String> commands = new LinkedList<String>();
        commands.add(sudo("/etc/init.d/httpd start"));

        newScript(LAUNCHING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
    }

    @Override
    public boolean isRunning() {
        List<String> commands = new LinkedList<String>();
        commands.add(sudo("/etc/init.d/httpd status"));

        return newScript(CHECK_RUNNING).
                body.append(commands).execute() == 0;
    }

    @Override
    public void stop() {
        List<String> commands = new LinkedList<String>();
        commands.add(sudo("/etc/init.d/httpd stop"));

        newScript(STOPPING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
    }

    @Override
    public void preInstall() {
        resolver = Entities.newDownloader(this);
        setExpandedInstallDir(Os.mergePaths(getInstallDir(), resolver.getUnpackedDirectoryName("wordpress")));
    }

    public void addW3TotalCache() {
        try {

            // TOOD see aled's suggested better way using download manager,
            // in comment at:  https://github.com/cloudsoft/brooklyn-social-apps/pull/9

            String name = "w3-total-cache";
            String version = "0.9.2.8";
            String filename = name + "." + version + ".zip";
            //        http://downloads.wordpress.org/plugin/w3-total-cache.0.9.2.8.zip
            String url = "http://downloads.wordpress.org/plugin/" + filename;

            String pluginsDir = getWwwDir() + "/wp-content/plugins/";

            int result = newScript("checking for plugin " + name).body.append("ls " + pluginsDir + name).execute();
            if (result == 0) {
                log.warn("Detected " + name + " already installed; skipping install and configuration");
                return;
            }

            newScript("installing plugin " + name).
                    failOnNonZeroResultCode().
                    body.append("cd /tmp").
                    body.append(BashCommands.commandsToDownloadUrlsAs(Arrays.asList(url), filename)).
                    body.append("cd " + pluginsDir).
                    body.append(sudo("unzip /tmp/" + filename)).
                    body.append("rm /tmp/" + filename).
                    execute();

            // TODO /var/www/html is hardcoded in most places below

            // update .htaccess (bash)
            String configureHtaccessUrl = "classpath://io/cloudsoft/socialapps/wordpress/w3-total-cache/configure-htaccess.sh";
            getMachine().copyTo(new ResourceUtils(this).getResourceFromUrl(configureHtaccessUrl), "/tmp/" + name + "-configure-htaccess.sh");
            newScript("updating htaccess for " + name).
                    failOnNonZeroResultCode().
                    body.append(sudo("chmod +x /tmp/" + name + "-configure-htaccess.sh")).
                    body.append(sudo("/tmp/" + name + "-configure-htaccess.sh")).
                    execute();

            // activate the plugin (php code)
            String activateCodeUrl = "classpath://io/cloudsoft/socialapps/wordpress/wp-activate-plugin.php";
            String activateApp = new ResourceUtils(this).getResourceAsString(activateCodeUrl);
            activateApp += "<?php\n\n" + "wp_activate_plugin( '" + name + "/" + name + ".php' );" + "\n" + "?>\n";
            getMachine().copyTo(new StringReader(activateApp), "/tmp/wp-activate-plugin-" + name + ".php");
            newScript("activating plugin for " + name).
                    failOnNonZeroResultCode().
                    body.append(sudo("chmod 777 " + getWwwDir() + "/wp-content")).
                    body.append(sudo("php -f " + "/tmp/wp-activate-plugin-" + name + ".php")).
                    execute();

            // TODO more hardcoded values in following files
            // (easy to make e.g. settings a config key, however, so user can set their favourite settings)

            // apply settings (use DB CACHE by default)
            // need: define('WP_CACHE', true); -- should already be in wp-config.php
            String settingsUrl = "classpath://io/cloudsoft/socialapps/wordpress/w3-total-cache/settings.php";
            String settings = new ResourceUtils(this).getResourceAsString(settingsUrl);
            getMachine().copyTo(new StringReader(settings), "/tmp/" + name + "-settings.php");
            String saveConfigUrl = "classpath://io/cloudsoft/socialapps/wordpress/w3-total-cache/save-config.php";
            String saveConfig = new ResourceUtils(this).getResourceAsString(saveConfigUrl);
            getMachine().copyTo(new StringReader(saveConfig), "/tmp/" + name + "-save-config.php");
            newScript("applying settings for " + name).
                    failOnNonZeroResultCode().
                    // ensure this tmp dir has been created
                            body.append(sudo("mkdir -p " + getWwwDir() + "/wp-content/cache/tmp")).
                    // chmod for these files (and i think others recently created)
                            body.append(sudo("chmod -R 777 " + getWwwDir() + "/wp-content")).
                    body.append(sudo("chown -R root:root " + getWwwDir() + "/wp-content")).
                    body.append(sudo("php -f /tmp/" + name + "-save-config.php")).
                    body.append(sudo("chmod -R 777 " + getWwwDir() + "/wp-content")).
                    body.append(sudo("chown -R root:root " + getWwwDir() + "/wp-content")).
                    execute();

            // TODO this seems to happen too soon in some installs; a sleep might work,
            // but there's no real need assuming the machine is properly secured
//            // clean up - revert permissions back
//            newScript("cleaning up after activation of "+name).
//                failOnNonZeroResultCode().
//                body.append(sudo("chmod 755 "+getWwwDir()+"/wp-content")).
//                execute();
        } catch (Exception e) {
            log.warn("Unable to activate w3-total-cache optimization plugin: " + e, e);
        }
    }
}
