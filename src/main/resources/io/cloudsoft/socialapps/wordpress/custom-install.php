<?php
define('WP_INSTALLING', true);

require_once('wp-config.php');
require_once ('wp-admin/upgrade-functions.php');

// Let's check to make sure WP isn't already installed.
if ( is_blog_installed() ) die("Blog already installed; no-op");


$weblog_title = "${entity.weblogTitle}";
$admin_email = "${entity.weblogAdminEmail}";
$public = ${entity.weblogPublic};
$password = ${entity.weblogAdminPassword};

$result = wp_install($weblog_title, __('admin'), $admin_email, $public, null, $password);
extract($result);
?>

Automated WordPress install
<?php printf(__('Result is %1$s'), $result); ?>
<?php exit ?>
