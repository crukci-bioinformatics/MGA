MGA
===

Multi-genome alignment contaminant screen for high-throughput sequence data
---------------------------------------------------------------------------

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

