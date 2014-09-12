ICGC DCC - Portal API
===

RESTful API for the ICGC DCC Data Portal. 

Documentation
---

Executable API documentation is available at:

	http://localhost:8080/docs

Administration
---

Administration is available at:

	http://localhost:8081

Development
---

For development data, port forward to the dev cluster:

	ssh -NL 9200:hcn53.res.oicr.on.ca:9200 -NL 9300:hcn53.res.oicr.on.ca:9300 hproxy-dev.res.oicr.on.ca

To run the application:
	
	cd dcc-portal/dcc-portal-api
	mvn -am
	java -jar target/dcc-portal-api-[version].jar server src/test/conf/settings.yml

