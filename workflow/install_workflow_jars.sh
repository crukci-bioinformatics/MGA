#!/bin/sh
version=1.4.0
for module in core groovy notifiers lsf
do
	mvn install:install-file \
		-DgroupId=org.cruk.workflow \
		-DartifactId=workflow-${module} \
		-Dversion=${version} \
		-Dfile=workflow-${module}-${version}.jar \
		-Dpackaging=jar \
		-DpomFile=workflow-${module}-${version}.pom
done

version=1.4
mvn install:install-file \
	-DgroupId=org.cruk.workflow \
	-DartifactId=workflow-master \
	-Dversion=${version} \
	-Dfile=workflow-master-${version}.pom \
	-Dpackaging=pom

