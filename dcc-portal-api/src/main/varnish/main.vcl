#
# Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
#
# Description:
#   This is the main VCL configuration file for varnish 3.0.4. This file includes portal.vcl and 
#   identifier.vcl configuration.
#
# Notes:
#   You can change the file name by editing /etc/default/varnish.
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
#   /etc/varnish/main.vcl
#
# SCM:
#  git@hproxy-dcc.res.oicr.on.ca:/srv/git/dcc-cache.git/main.vcl
#

include "portal.vcl";
include "identifier.vcl";
