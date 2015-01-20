/*
 * Copyright (c) 2012-2014 The Ontario Institute for Cancer Research. All rights reserved.
 */

/* Script for setting up the dcc-portal schema using H2 1.3.x */

-- Clean
DROP TABLE IF EXISTS user_gene_set;
DROP TABLE IF EXISTS enrichment_analysis;
DROP TABLE IF EXISTS union_analysis;
DROP TABLE IF EXISTS entity_list;

-- Create
CREATE TABLE user_gene_set(
   id   UUID PRIMARY KEY,
   data TEXT 
);
CREATE TABLE enrichment_analysis(
   id   UUID PRIMARY KEY,
   data TEXT 
);
CREATE TABLE union_analysis (
   id   UUID PRIMARY KEY,
   data TEXT
);
CREATE TABLE entity_list (
   id   UUID PRIMARY KEY,
   data TEXT
);

