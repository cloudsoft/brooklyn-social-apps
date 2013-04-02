<?php

$wordpress_path = "/var/www/html";
    
require_once( $wordpress_path . '/wp-load.php' ); //not sure if this line is needed
require_once( $wordpress_path . '/wp-admin/includes/plugin.php');
require_once( $wordpress_path . '/wp-content/plugins/w3-total-cache/lib/W3/Plugin/TotalCacheAdmin.php');

echo "WordPress: Applying programmatic W3 Total Cache settings\n";

$config = new W3_Config();
$imported = $config->import('/tmp/w3-total-cache-settings.php');
if (!$imported) {
    echo "WordPress: Failed to import W3 Total Cache settings\n";
    exit(1);
}

$x = new W3_Plugin_TotalCacheAdmin();
$x->run();
$result = $x->config_save($config, $x->_config_admin);

if (!$result) {
  echo "WordPress: Failed to save W3 Total Cache settings\n";
  exit(1);
}

?>
