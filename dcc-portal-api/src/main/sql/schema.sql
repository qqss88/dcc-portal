/*
 * Copyright (c) 2012-2014 The Ontario Institute for Cancer Research. All rights reserved.
 */

/* Script for setting up the dcc-portal schema using PostgreSQL 9.2.4 */

-- CREATE DATABASE dcc_portal;
-- CREATE USER dcc WITH PASSWORD 'dcc';
-- GRANT ALL PRIVILEGES ON DATABASE dcc_portal to dcc;
-- 
-- GRANT SELECT, INSERT ON user_gene_set TO dcc;

/* Initialize */

DROP TABLE IF EXISTS user_gene_set;
DROP TABLE IF EXISTS enrichment_analysis;

/* Create tables */

CREATE TABLE user_gene_set(
   id   UUID NOT NULL,
   data TEXT NOT NULL,
   
   PRIMARY KEY(id) 
);

CREATE TABLE enrichment_analysis(
   id   UUID NOT NULL,
   data TEXT NOT NULL,
   
   PRIMARY KEY(id) 
);
