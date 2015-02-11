#
# Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
#
# Description:
#   This is the DCC identifier VCL configuration file for varnish 3.0.4. This file contains the 
#   VCL and backend-definitions.
#
# Notes:
#   It should be included in the main.vcl configuration file.
#
#   After changing this file, you can run either service varnish reload, which will not restart Varnish, 
#   or you can run service varnish restart, which empties the cache.
#
#   Most Varnish-installations use two configuration-files. One of them is used by the operating system 
#   to start Varnish, while the other contains the VCL (this).
#
# See:
#   https://www.varnish-software.com/static/book/VCL_Basics.html
#   https://www.varnish-software.com/static/book/VCL_functions.html
#
# Typical installation location: 
#   /etc/varnish/identifier.vcl
#
# SCM:
#  git@hproxy-dcc.res.oicr.on.ca:/srv/git/dcc-cache.git/identifier.vcl
#

backend identifier {
    .host = "hident-dcc.res.oicr.on.ca";
    .port = "6381";
}

sub vcl_recv {
	if (server.port == 5391) {
		set req.backend = identifier;
	}
}

sub vcl_fetch {
	if (server.port == 5391 && beresp.status == 200 && req.request == "GET") {
		set beresp.ttl = 365d;
		set beresp.http.cache-control = "max-age=31536000";
	}
}
