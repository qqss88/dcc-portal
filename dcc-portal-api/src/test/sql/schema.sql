/*
 * Copyright (c) 2012-2014 The Ontario Institute for Cancer Research. All rights reserved.
 */

/* Script for setting up the dcc-portal schema using H2 1.3.x */

-- Clean
DROP TABLE IF EXISTS user_gene_set;

-- Create
CREATE TABLE user_gene_set(
   id   UUID PRIMARY KEY,
   data TEXT 
);
