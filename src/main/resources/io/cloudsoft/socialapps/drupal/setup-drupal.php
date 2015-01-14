<?php

define('DRUPAL_ROOT', getcwd());
define('MAINTENANCE_MODE', 'install');

require_once DRUPAL_ROOT . '/includes/install.core.inc';

$settings = array(
    'parameters' => array(
        'profile' => 'standard',
        'locale' => 'en',
    ),
    'forms' => array(
        'install_settings_form' => array(
            'driver' => '$database_driver',
            'mysql' => array(
                'database' => '$database_schema',
                'username' => '$database_user',
                'password' => '$database_password',
                'host' => '$database_host',
                'port' => '$database_port',
                'db_prefix' => '',
            ),
        ),
        'install_configure_form' => array(
            'site_name' => '$site_name',
            'site_mail' => '$site_mail',
            'account' => array(
                'name' => '$admin_name',
                'mail' => '$admin_email',
                'pass' => array(
                    'pass1' => '$admin_password',
                    'pass2' => '$admin_password',
                ),
            ),
            'update_status_module' => array(
                1 => TRUE,
                2 => TRUE,
            ),
            'clean_url' => FALSE,
        ),
    ),
);

install_drupal($settings);