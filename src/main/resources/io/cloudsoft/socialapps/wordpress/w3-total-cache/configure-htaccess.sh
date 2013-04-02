#!/bin/bash

cd /var/www/html

cat >> .htaccess << __END_OF_CONFIG__
# BEGIN W3TC Browser Cache
<IfModule mod_deflate.c>
  <IfModule mod_headers.c>
    Header append Vary User-Agent env=!dont-vary
  </IfModule>
  AddOutputFilterByType DEFLATE text/css text/x-component application/x-javascript application/javascript text/javascript text/x-js text/html text/richtext image/svg+xml text/plain text/xsd text/xsl text/xml image/x-icon application/json
  <IfModule mod_mime.c>
    # DEFLATE by extension
    AddOutputFilter DEFLATE js css htm html xml
  </IfModule>
</IfModule>
# END W3TC Browser Cache
__END_OF_CONFIG__

