# Installing Third Party Software for the MGA Pipeline

## Before Installing Software

The code blocks of these instructions can be copy-and-paste into a terminal
to build each tool. There are two environment variables that should be set
in your shell before doing so though.

For the current CRUK-CI cluster or our _sol-srv_ machines, set `SOFTWARE_ROOT` to:

    export SOFTWARE_ROOT=/home/bioinformatics/pipelinesoftware/mga/$(uname -r | cut -d '.' -f 6)

Also set `SOFTWARE_DOWNLOAD` relative to `SOFTWARE_ROOT`:

    export SOFTWARE_DOWNLOAD=$SOFTWARE_ROOT/../download

If you are installing this software outside of CRUK-CI, please choose a suitable
directory for the software.

## Prerequisites

### Java

It is assumed Java is generally available on the system. You will need Java 8 or above.

### Compilers

You will need GCC C++ installed to allow the other packages to build.


## Third Party Tools

### Samtools 1.6

```
cd $SOFTWARE_DOWNLOAD
wget https://github.com/samtools/samtools/releases/download/1.6/samtools-1.6.tar.bz2

cd $SOFTWARE_ROOT
tar xvfj $SOFTWARE_DOWNLOAD/samtools-1.6.tar.bz2
cd samtools-1.6
make all
```

### Exonerate 2.2.0

```
cd $SOFTWARE_DOWNLOAD
wget http://ftp.ebi.ac.uk/pub/software/vertebrategenomics/exonerate/exonerate-2.2.0-x86_64.tar.gz

cd $SOFTWARE_ROOT
tar xvfz $SOFTWARE_DOWNLOAD/exonerate-2.2.0-x86_64.tar.gz
```

### HISAT 2.1.0

```
cd $SOFTWARE_DOWNLOAD
wget ftp://ftp.ccb.jhu.edu/pub/infphilo/hisat2/downloads/hisat2-2.1.0-Linux_x86_64.zip

cd $SOFTWARE_ROOT
unzip $SOFTWARE_DOWNLOAD/hisat2-2.1.0-Linux_x86_64.zip
```

### Bowtie 1.1.1

Bowtie 1 is the legacy aligner originally used for MGA.

```
cd $SOFTWARE_DOWNLOAD
wget https://downloads.sourceforge.net/project/bowtie-bio/bowtie/1.1.1/bowtie-1.1.1-linux-x86_64.zip

cd $SOFTWARE_ROOT
unzip $SOFTWARE_DOWNLOAD/bowtie-1.1.1-linux-x86_64.zip
```

