###MGA: Multi-genome alignment contaminant screen for high-throughput sequence data

MGA is a quality control tool for high-throughput sequence data. It screens for
contaminants by aligning sequence reads in FASTQ format against a series of
reference genomes using Bowtie and against a set of adapter sequences using
exonerate.

MGA samples a subset of the reads, by default 100000, prior to alignment against
reference genome sequences and adapters. This reduces considerably the overall run
time. In addition, the reads are trimmed to 36 bases by default, prior to alignment
by Bowtie. This is to ensure consistency of the output mapping and error rates across
runs of differing lengths. Exonerate alignment against adapters uses the full-length
sequences.

####Installing and running MGA

Please see README file for details of how to install MGA from a pre-packaged release
and how to run MGA.

####Building MGA from source

MGA is built using [Apache Maven](http://maven.apache.org), a software project
management and build automation tool.

Details on how to install and run Maven can be found
[here](http://maven.apache.org).

Maven will automatically download dependencies from a central Maven repository. 
MGA is dependent on a workflow system also developed at CRUK-CI. We are in the
process of releasing the workflow system and making the workflow dependencies
available on a publicly-accessible Maven repository. In the interim, however,
the workflow libraries contained within the MGA project can be used to build
MGA.

1. Clone the MGA project

	git clone https://github.com/crukci-bioinformatics/MGA.git

2. Install workflow system jar files into the local maven repository

	cd workflow
	./install_workflow_jars.sh
	cd ..

3. Build and package MGA

	mvn package

4. Unpack MGA to installation directory

	tar zxf target/mga-1.x-distribution.tar.gz

This will create a directory named mga-1.x which can be moved to the desired
installation directory.

