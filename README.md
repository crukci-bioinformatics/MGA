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

Please see [README](README) file for details of how to install and run MGA from
a [pre-packaged release](https://github.com/crukci-bioinformatics/MGA/releases).

####Building MGA from source

MGA is built using Apache Maven, a software project management and build
automation tool. Details on how to install and run Maven can be found
[here](http://maven.apache.org).

Maven will automatically download dependencies from a central Maven repository. 
MGA is dependent on a workflow system also developed at CRUK-CI. We are in the
process of releasing the workflow system and making the workflow dependencies
available on a publicly-accessible Maven repository. In the interim, however,
the workflow libraries and two other dependencies not available in the public
Maven repository (Apache Xerces XML Schema and PsychoPath Path 2.0 Processor)
are included in a local Maven repository under the maven subdirectory and are
used in building MGA.

1. Clone the MGA project

        git clone https://github.com/crukci-bioinformatics/MGA.git

2. Build and package MGA

        cd MGA
        mvn package

3. Unpack MGA to installation directory (substituting for the version number as appropriate)

        tar zxf target/mga-1.x-distribution.tar.gz

This will create a directory named mga-1.x which can be moved to the desired
installation directory.

