<?php

$wordpress_path = "/var/www/html";
    
require_once( $wordpress_path . '/wp-load.php' ); //not sure if this line is needed
//activate_plugin() is here:
require_once(  $wordpress_path . '/wp-admin/includes/plugin.php');

function wp_activate_plugin( $plugin ) {
    $current = get_option( 'active_plugins' );
    $plugin = plugin_basename( trim( $plugin ) );

    if ( !in_array( $plugin, $current ) ) {
        $current[] = $plugin;
        sort( $current );
        do_action( 'activate_plugin', trim( $plugin ) );
        update_option( 'active_plugins', $current );
        do_action( 'activate_' . trim( $plugin ) );
        do_action( 'activated_plugin', trim( $plugin) );
    }

    return null;
}

?>
<?php
// wp_activate_plugin( 'name/name.php' );
?>
