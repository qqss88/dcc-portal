-- Clean
DROP TABLE IF EXISTS genelist;
DROP SEQUENCE IF EXISTS genelist_sequence;

-- Create
CREATE SEQUENCE genelist_sequence INCREMENT BY 1 START WITH 0;
CREATE TABLE genelist(
   id BIGINT NOT NULL PRIMARY KEY,
   data TEXT 
);
