#
# Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
#
# Description:
#   This is the DCC portal VCL configuration file for varnish 3.0.4. This file contains the 
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
#   /etc/varnish/portal.vcl
#
# SCM:
#  git@hproxy-dcc.res.oicr.on.ca:/srv/git/dcc-cache.git/identifier.vcl
#

# Backend 1
backend www1 {
	.host = "hportal1-dcc.oicr.on.ca";
	.port = "5381";
	.probe = {
    	.url = "/";
		.interval = 5s;
		.timeout = 1s;
		.window = 5;
		.threshold = 3;
	}
}

# Backend 2 (harmlessly duplicated from above)
backend www2 {
	.host = "hportal2-dcc.oicr.on.ca";
	.port = "5381";
	.probe = {
		.url = "/";
		.interval = 5s;
		.timeout = 1s;
		.window = 5;
		.threshold = 3;
	}
}

# URL hash based round-robin director
director www round-robin {
	{ .backend = www1; }
	{ .backend = www2; }
}

/*
director member client {
   {
      .backend = www1;
      .weight = 1;
   }
   {
      .backend = www2;
      .weight = 1;
   }
}
*/

acl banners {
	"localhost";
	"hportal1-dcc.oicr.on.ca";
	"hportal2-dcc.oicr.on.ca";
}

# The first VCL function executed, right after Varnish has decoded the request into its basic data structure. 
# It has four main uses:
#
# - Modifying the client data to reduce cache diversity. E.g., removing any leading “www.” in a URL.
# - Deciding caching policy based on client data. E.g., Not caching POST requests, only caching specific URLs, etc
# - Executing re-write rules needed for specific web applications.
# - Deciding which Web server to use.
sub vcl_recv {
	if (server.port == 5381) {
        set req.backend = www;

		/*
        set req.backend = member;
        set client.identity = client.ip;
		*/

        /* Force banning for remote application cache coherence */
        if (req.request == "BAN") {
                if (!client.ip ~ banners) {
                        error 405 "Not allowed.";
                }

                /* This option is to clear any cached object containing the req.url */
                ban("req.url ~ "+req.url);

                error 200 "Cache cleared successfully.";
        }

        if (req.request == "GET" && req.url ~ "^/api/.*/download.*$") {
		/* Don't reuse connection. See https://www.varnish-cache.org/docs/3.0/tutorial/vcl.html#actions pipe paragraph */
		set req.http.Connection = "close";

                /* Never cache downloads */
                return(pipe);
        }

        if (req.url ~ "^/api/.*/auth.*$") {
		/* Don't reuse connection. See https://www.varnish-cache.org/docs/3.0/tutorial/vcl.html#actions pipe paragraph */
		set req.http.Connection = "close";

                /* Never cache login related*/
                return(pipe);
        }

		if (req.request == "GET" && req.url ~ "^/api/.*/short\?.*$") {
			/* Don't reuse connection. See https://www.varnish-cache.org/docs/3.0/tutorial/vcl.html#actions pipe paragraph */
			set req.http.Connection = "close";

			/* Never cache short URL API */
			return(pipe);
		}

		if (req.url ~ "^/api/.*/analysis.*$") {
			/* Don't reuse connection. See https://www.varnish-cache.org/docs/3.0/tutorial/vcl.html#actions pipe paragraph */
			set req.http.Connection = "close";

			/* Never cache analysis path */
			return(pipe);
		}
	
		if (req.url ~ "^/api/.*/entityset.*$") {
			/* Don't reuse connection. See https://www.varnish-cache.org/docs/3.0/tutorial/vcl.html#actions pipe paragraph */
			set req.http.Connection = "close";

			/* Never cache analysis path */
			return(pipe);
		}
	
        if (req.http.X-Bypass) {
        	/* Never cache bypass requests for load testing */
            return(pipe);
        }

        /* Remove cache busting cookies */
        remove req.http.Cookie;
    }
}


# The backend-counterpart to vcl_recv. In vcl_recv you can use information 
# provided by the client to decide on caching policy, while you use information provided by the server 
# to further decide on a caching policy in vcl_fetch.
sub vcl_fetch {
	if (server.port == 5381) {
		if (beresp.status == 200 && req.request == "GET" && req.url ~ "^/(index\.html)?$") {
			/* Short duration for the index page */
			set beresp.ttl = 1m;
			set beresp.http.cache-control = "max-age=0";
		} else if (beresp.status == 200 && req.request == "GET" && req.url ~ "^/(vendor|styles|scripts|views)/.*$") {
			/* Static resources */
			set beresp.ttl = 1h;
			set beresp.http.cache-control = "max-age=3600";
		} else if (beresp.status == 200 && req.request == "GET") {
			/* Long duration for everything else that was successful */
			set beresp.ttl = 30d;
			set beresp.http.cache-control = "max-age=3600";
		} else {
			/* No TTL on everything else */
			set beresp.ttl = 0s;
		}

		/* Prevent cookie cache-busting */
		unset beresp.http.set-cookie;

		/* Rewrite Vary until dropwizard is upgraded. See https://github.com/dropwizard/dropwizard/issues/494 */
		if (beresp.status == 200 && beresp.http.content-encoding ~ "^gzip$") {
			set beresp.http.vary = "Accept-Encoding";
		}
	}
}

# While the vcl_deliver function is simple, it is also very useful for modifying the output of Varnish. 
# If you need to remove or add a header that isn’t supposed to be stored in the cache, vcl_deliver 
# is the place to do it.
sub vcl_deliver {
	/* For debugging */
	set resp.http.X-Served-By = server.hostname;
	if (obj.hits > 0) {
		set resp.http.X-Cache = "HIT";	
		set resp.http.X-Cache-Hits = obj.hits;
	} else {
		set resp.http.X-Cache = "MISS";	
	}

	return(deliver);
}
